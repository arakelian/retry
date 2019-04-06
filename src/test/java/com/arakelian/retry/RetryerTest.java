/*
 * copyright 2017-2018 Robert Huffman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arakelian.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.arakelian.retry.BlockStrategy;
import com.arakelian.retry.RetryException;
import com.arakelian.retry.Retryer;
import com.arakelian.retry.RetryerBuilder;
import com.arakelian.retry.StopStrategies;

class RetryerTest {

    /**
     * Callable that throws an exception on a specified attempt (indexed starting with 1). Calls
     * before the interrupt attempt throw an Exception.
     */
    private static class Interrupter implements Callable<Void>, Runnable {

        private final int interruptAttempt;

        private int invocations;

        Interrupter(final int interruptAttempt) {
            this.interruptAttempt = interruptAttempt;
        }

        @Override
        public Void call() throws InterruptedException {
            invocations++;
            if (invocations == interruptAttempt) {
                throw new InterruptedException("Interrupted invocation " + invocations);
            } else {
                throw new RuntimeException("Throwing on invocaion " + invocations);
            }
        }

        @Override
        public void run() throws RuntimeException {
            try {
                call();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * BlockStrategy that interrupts the thread
     */
    private class InterruptingBlockStrategy implements BlockStrategy {

        private final int invocationToInterrupt;

        private int currentInvocation;

        InterruptingBlockStrategy(final int invocationToInterrupt) {
            this.invocationToInterrupt = invocationToInterrupt;
        }

        @Override
        public void block(final long sleepTime) throws InterruptedException {
            ++currentInvocation;
            if (currentInvocation == invocationToInterrupt) {
                throw new InterruptedException("Block strategy interrupted itself");
            } else {
                Thread.sleep(sleepTime);
            }
        }
    }

    private static class Thrower implements Callable<Void>, Runnable {

        private final Class<? extends Throwable> throwableType;

        private final int successAttempt;

        private int invocations = 0;

        Thrower(final Class<? extends Throwable> throwableType, final int successAttempt) {
            this.throwableType = throwableType;
            this.successAttempt = successAttempt;
        }

        @Override
        public Void call() throws Exception {
            invocations++;
            if (invocations == successAttempt) {
                return null;
            }
            if (Error.class.isAssignableFrom(throwableType)) {
                throw (Error) throwable();
            }
            throw (Exception) throwable();
        }

        @Override
        public void run() {
            invocations++;
            if (invocations == successAttempt) {
                return;
            }
            if (Error.class.isAssignableFrom(throwableType)) {
                throw (Error) throwable();
            }
            throw (RuntimeException) throwable();
        }

        private Throwable throwable() {
            try {
                return throwableType.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException("Failed to create throwable of type " + throwableType);
            }
        }
    }

    private static Stream<Arguments> checkedAndUnchecked() {
        return Stream.concat(
                unchecked(),
                Stream.of(Arguments.of(Exception.class), Arguments.of(IOException.class)));
    }

    private static Stream<Arguments> unchecked() {
        return Stream.of(
                Arguments.of(Error.class),
                Arguments.of(RuntimeException.class),
                Arguments.of(NullPointerException.class));
    }

    @Test
    public void testCallWhenBlockerIsInterrupted() throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(10))
                .withBlockStrategy(new InterruptingBlockStrategy(3)).build();
        final Thrower thrower = new Thrower(Exception.class, 5);
        boolean interrupted = false;
        try {
            retryer.call(thrower);
            fail("Should have thrown");
        } catch (final InterruptedException e) {
            interrupted = true;
        }
        // noinspection ConstantConditions
        assertTrue(interrupted);
        assertEquals(3, thrower.invocations);
    }

    @Test
    public void testRunWhenBlockerIsInterrupted() throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(10))
                .withBlockStrategy(new InterruptingBlockStrategy(3)).build();
        final Thrower thrower = new Thrower(Exception.class, 5);
        boolean interrupted = false;
        try {
            retryer.run(thrower);
            fail("Should have thrown");
        } catch (final InterruptedException e) {
            interrupted = true;
        }
        // noinspection ConstantConditions
        assertTrue(interrupted);
        assertEquals(3, thrower.invocations);
    }

    @Test
    void testCallThatIsInterrupted() throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(10)).build();
        final Interrupter thrower = new Interrupter(4);
        boolean interrupted = false;
        try {
            retryer.call(thrower);
            fail("Should have thrown");
        } catch (final InterruptedException ignored) {
            interrupted = true;
        } catch (final Exception e) {
            System.out.println(e);
        }

        // noinspection ConstantConditions
        assertTrue(interrupted);
        assertEquals(4, thrower.invocations);
    }

    @ParameterizedTest
    @MethodSource("checkedAndUnchecked")
    void testCallThrowsSubclassWithRetryOnException(final Class<? extends Throwable> throwable)
            throws Exception {
        @SuppressWarnings("unchecked")
        final Class<? extends Throwable> superclass = (Class<? extends Throwable>) throwable.getSuperclass();
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfExceptionOfType(superclass).build();
        final Thrower thrower = new Thrower(throwable, 5);
        retryer.call(thrower);
        assertEquals(5, thrower.invocations);
    }

    @ParameterizedTest
    @MethodSource("checkedAndUnchecked")
    void testCallThrowsWhenRetriesAreStopped(final Class<? extends Throwable> throwable) throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfExceptionOfType(throwable)
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        final Thrower thrower = new Thrower(throwable, 5);
        try {
            retryer.call(thrower);
            fail("Should have thrown");
        } catch (final RetryException e) {
            assertSame(e.getCause().getClass(), throwable);
        }
        assertEquals(3, thrower.invocations);
    }

    @ParameterizedTest
    @MethodSource("checkedAndUnchecked")
    void testCallThrowsWithNoRetryOnException(final Class<? extends Throwable> throwable) throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().build();
        final Thrower thrower = new Thrower(throwable, 5);
        try {
            retryer.call(thrower);
            fail("Should have thrown");
        } catch (final RetryException e) {
            assertSame(e.getCause().getClass(), throwable);
        }
        assertEquals(1, thrower.invocations);
    }

    @ParameterizedTest
    @MethodSource("checkedAndUnchecked")
    void testCallThrowsWithRetryOnException(final Class<? extends Throwable> throwable) throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfExceptionOfType(Throwable.class).build();
        final Thrower thrower = new Thrower(throwable, 5);
        retryer.call(thrower);
        assertEquals(5, thrower.invocations);
    }

    @Test
    void testRunThatIsInterrupted() throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(10)).build();
        final Interrupter thrower = new Interrupter(4);
        boolean interrupted = false;
        try {
            retryer.run(thrower);
            fail("Should have thrown");
        } catch (final InterruptedException ignored) {
            interrupted = true;
        }

        // noinspection ConstantConditions
        assertTrue(interrupted);
        assertEquals(4, thrower.invocations);
    }

    @ParameterizedTest
    @MethodSource("unchecked")
    void testRunThrowsSubclassWithRetryOnException(final Class<? extends Throwable> throwable)
            throws Exception {
        @SuppressWarnings("unchecked")
        final Class<? extends Throwable> superclass = (Class<? extends Throwable>) throwable.getSuperclass();
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfExceptionOfType(superclass).build();
        final Thrower thrower = new Thrower(throwable, 5);
        retryer.run(thrower);
        assertEquals(5, thrower.invocations);
    }

    @ParameterizedTest
    @MethodSource("unchecked")
    void testRunThrowsWhenRetriesAreStopped(final Class<? extends Throwable> throwable) throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfExceptionOfType(throwable)
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        final Thrower thrower = new Thrower(throwable, 5);
        try {
            retryer.run(thrower);
            fail("Should have thrown");
        } catch (final RetryException e) {
            assertSame(e.getCause().getClass(), throwable);
        }
        assertEquals(3, thrower.invocations);
    }

    @ParameterizedTest
    @MethodSource("unchecked")
    void testRunThrowsWithNoRetryOnException(final Class<? extends Throwable> throwable) throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().build();
        final Thrower thrower = new Thrower(throwable, 5);
        try {
            retryer.run(thrower);
            fail("Should have thrown");
        } catch (final RetryException e) {
            assertSame(e.getCause().getClass(), throwable);
        }
        assertEquals(1, thrower.invocations);
    }

    @ParameterizedTest
    @MethodSource("unchecked")
    void testRunThrowsWithRetryOnException(final Class<? extends Throwable> throwable) throws Exception {
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfExceptionOfType(Throwable.class).build();
        final Thrower thrower = new Thrower(throwable, 5);
        retryer.run(thrower);
        assertEquals(5, thrower.invocations);
    }
}
