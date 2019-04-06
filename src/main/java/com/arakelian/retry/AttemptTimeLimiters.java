/*
 * Copyright 2012-2015 Ray Holder
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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

/**
 * Factory class for instances of {@link AttemptTimeLimiter}
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public class AttemptTimeLimiters {

    @Immutable
    private static final class FixedAttemptTimeLimit<V> implements AttemptTimeLimiter<V> {

        private final TimeLimiter timeLimiter;
        private final long duration;
        private final TimeUnit timeUnit;

        public FixedAttemptTimeLimit(final long duration, @Nonnull final TimeUnit timeUnit) {
            this(SimpleTimeLimiter.create(Executors.newCachedThreadPool()), duration, timeUnit);
        }

        public FixedAttemptTimeLimit(
                final long duration,
                @Nonnull final TimeUnit timeUnit,
                @Nonnull final ExecutorService executorService) {
            this(SimpleTimeLimiter.create(executorService), duration, timeUnit);
        }

        private FixedAttemptTimeLimit(
                @Nonnull final TimeLimiter timeLimiter,
                final long duration,
                @Nonnull final TimeUnit timeUnit) {
            Preconditions.checkNotNull(timeLimiter);
            Preconditions.checkNotNull(timeUnit);
            this.timeLimiter = timeLimiter;
            this.duration = duration;
            this.timeUnit = timeUnit;
        }

        @Override
        public V call(final Callable<V> callable) throws Exception {
            return timeLimiter.callWithTimeout(callable, duration, timeUnit);
        }
    }

    @Immutable
    private static final class NoAttemptTimeLimit<V> implements AttemptTimeLimiter<V> {
        @Override
        public V call(final Callable<V> callable) throws Exception {
            return callable.call();
        }
    }

    /**
     * For control over thread management, it is preferable to offer an {@link ExecutorService}
     * through the other factory method, {@link #fixedTimeLimit(long, TimeUnit, ExecutorService)}.
     *
     * @param duration
     *            that an attempt may persist before being circumvented
     * @param timeUnit
     *            of the 'duration' arg
     * @param <V>
     *            the type of the computation result
     * @return an {@link AttemptTimeLimiter} with a fixed time limit for each attempt
     */
    public static <V> AttemptTimeLimiter<V> fixedTimeLimit(
            final long duration,
            @Nonnull final TimeUnit timeUnit) {
        Preconditions.checkNotNull(timeUnit);
        return new FixedAttemptTimeLimit<>(duration, timeUnit);
    }

    /**
     * @param duration
     *            that an attempt may persist before being circumvented
     * @param timeUnit
     *            of the 'duration' arg
     * @param executorService
     *            used to enforce time limit
     * @param <V>
     *            the type of the computation result
     * @return an {@link AttemptTimeLimiter} with a fixed time limit for each attempt
     */
    public static <V> AttemptTimeLimiter<V> fixedTimeLimit(
            final long duration,
            @Nonnull final TimeUnit timeUnit,
            @Nonnull final ExecutorService executorService) {
        Preconditions.checkNotNull(timeUnit);
        return new FixedAttemptTimeLimit<>(duration, timeUnit, executorService);
    }

    /**
     * @param <V>
     *            The type of the computation result.
     * @return an {@link AttemptTimeLimiter} impl which has no time limit
     */
    public static <V> AttemptTimeLimiter<V> noTimeLimit() {
        return new NoAttemptTimeLimit<>();
    }

    private AttemptTimeLimiters() {
    }
}
