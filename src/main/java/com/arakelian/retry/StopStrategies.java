/*
 * Copyright 2012-2015 Ray Holder
 * Modifications copyright 2017-2018 Robert Huffman
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

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

/**
 * Factory class for {@link StopStrategy} instances.
 *
 * @author JB
 */
public final class StopStrategies {
    @Immutable
    private static final class NeverStopStrategy implements StopStrategy {
        @Override
        public boolean shouldStop(final Attempt failedAttempt) {
            return false;
        }
    }

    @Immutable
    private static final class StopAfterAttemptStrategy implements StopStrategy {
        private final int maxAttemptNumber;

        StopAfterAttemptStrategy(final int maxAttemptNumber) {
            Preconditions.checkArgument(
                    maxAttemptNumber >= 1,
                    "maxAttemptNumber must be >= 1 but is %d",
                    maxAttemptNumber);
            this.maxAttemptNumber = maxAttemptNumber;
        }

        @Override
        public boolean shouldStop(final Attempt failedAttempt) {
            return failedAttempt.getAttemptNumber() >= maxAttemptNumber;
        }
    }

    @Immutable
    private static final class StopAfterDelayStrategy implements StopStrategy {
        private final long maxDelay;

        StopAfterDelayStrategy(final long maxDelay) {
            Preconditions.checkArgument(maxDelay >= 0L, "maxDelay must be >= 0 but is %d", maxDelay);
            this.maxDelay = maxDelay;
        }

        @Override
        public boolean shouldStop(final Attempt failedAttempt) {
            return failedAttempt.getDelaySinceFirstAttempt() >= maxDelay;
        }
    }

    private static final StopStrategy NEVER_STOP = new NeverStopStrategy();

    /**
     * Returns a stop strategy which never stops retrying. It might be best to try not to abuse
     * services with this kind of behavior when small wait intervals between retry attempts are
     * being used.
     *
     * @return a stop strategy which never stops
     */
    @SuppressWarnings("WeakerAccess")
    public static StopStrategy neverStop() {
        return NEVER_STOP;
    }

    /**
     * Returns a stop strategy which stops after N failed attempts.
     *
     * @param attemptNumber
     *            the number of failed attempts before stopping
     * @return a stop strategy which stops after {@code attemptNumber} attempts
     */
    @SuppressWarnings("WeakerAccess")
    public static StopStrategy stopAfterAttempt(final int attemptNumber) {
        return new StopAfterAttemptStrategy(attemptNumber);
    }

    /**
     * Returns a stop strategy which stops after a given delay. If an unsuccessful attempt is made,
     * this {@link StopStrategy} will check if the amount of time that's passed from the first
     * attempt has exceeded the given delay amount. If it has exceeded this delay, then using this
     * strategy causes the retrying to stop.
     *
     * @param delayInMillis
     *            the delay, in milliseconds, starting from first attempt
     * @return a stop strategy which stops after {@code delayInMillis} time in milliseconds
     * @deprecated Use {@link #stopAfterDelay(long, TimeUnit)} instead.
     */
    @Deprecated
    public static StopStrategy stopAfterDelay(final long delayInMillis) {
        return stopAfterDelay(delayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns a stop strategy which stops after a given delay. If an unsuccessful attempt is made,
     * this {@link StopStrategy} will check if the amount of time that's passed from the first
     * attempt has exceeded the given delay amount. If it has exceeded this delay, then using this
     * strategy causes the retrying to stop.
     *
     * @param duration
     *            the delay, starting from first attempt
     * @param timeUnit
     *            the unit of the duration
     * @return a stop strategy which stops after {@code delayInMillis} time in milliseconds
     */
    @SuppressWarnings("WeakerAccess")
    public static StopStrategy stopAfterDelay(final long duration, @Nonnull final TimeUnit timeUnit) {
        Preconditions.checkNotNull(timeUnit, "The time unit may not be null");
        return new StopAfterDelayStrategy(timeUnit.toMillis(duration));
    }

    private StopStrategies() {
    }
}
