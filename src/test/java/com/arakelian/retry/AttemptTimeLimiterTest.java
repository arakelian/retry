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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

/**
 * @author Jason Dunkelberger (dirkraft)
 */
public class AttemptTimeLimiterTest {

    static class SleepyOut implements Callable<Void> {

        final long sleepMs;

        SleepyOut(final long sleepMs) {
            this.sleepMs = sleepMs;
        }

        @Override
        public Void call() throws Exception {
            Thread.sleep(sleepMs);
            System.out.println("I'm awake now");
            return null;
        }
    }

    Retryer<Void> r = RetryerBuilder.<Void> newBuilder()
            .withAttemptTimeLimiter(AttemptTimeLimiters.<Void> fixedTimeLimit(1, TimeUnit.SECONDS)).build();

    @Test
    public void testAttemptTimeLimit() throws RetryException, ExecutionException {
        r.call(new SleepyOut(0L));

        try {
            r.call(new SleepyOut(10 * 1000L));
            fail("Expected timeout exception");
        } catch (final ExecutionException e) {
            // expected
            assertEquals(TimeoutException.class, e.getCause().getClass());
        }
    }
}
