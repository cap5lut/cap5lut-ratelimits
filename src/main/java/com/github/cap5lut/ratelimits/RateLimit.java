package com.github.cap5lut.ratelimits;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

// TODO: stuff like discord returns information about the current ratelimit state, so and update method is needed
// TODO: instead of integer rate limits use floating rate limits + acquire(float load) to be able to consume more than
// TODO: just a slot but a specified value (eg. acquire(1.5f); in case ur rate limit isnt represented by slots but for example
// TODO: how much MB you are able to download. RateLimitSlot should then be renamed because it isnt solely a "slot" anymore
// TODO: but a specific amount (AcquiredRateLimitQuantity)

/**
 * Structure representing a rate limit.
 */
public interface RateLimit {
    /**
     * Rate limit factory.
     */
    @FunctionalInterface
    interface Factory {
        RateLimit create(long capacity, long resetInterval, TimeUnit resetUnit);
    }

    /**
     * Yielding method.
     */
    @FunctionalInterface
    interface Yielder {
        /**
         * Yields the thread.
         */
        void yield();
    }

    /**
     * Default resolution yielder.
     */
    Yielder defaultYielder = new Yielder() {
        @Override
        public void yield() {
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
        }
    };

    /**
     * High resolution yielder.
     */
    Yielder highResYielder = new Yielder() {
        @Override
        public void yield() {
            Thread.yield();
        }
    };

    /**
     * Low resolution yielder.
     */
    Yielder lowResYielder = new Yielder() {
        @Override
        public void yield() {
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
            Thread.yield();
        }
    };

    /**
     * Acquires a slot from all rate limits.
     * NOTE: It will release unused slots.
     * @param rateLimits Rate limits to acquire a slot from.
     */
    static void acquireAll(RateLimit... rateLimits) throws InterruptedException {
        if(rateLimits.length == 1) { // speed up if only one rate limit was passed
            rateLimits[0].acquire();
            return;
        }

        Arrays.sort(rateLimits, RateLimit::sortByLongestAcquirationFirst);

        RateLimitSlot[] slots = new RateLimitSlot[rateLimits.length];
        for(int i = 0; i < slots.length; i++) {
            slots[i] = rateLimits[i].acquire();
        }

        while(!RateLimitSlot.areValid(slots)) {
            // cancel valid slots
            for(RateLimitSlot slot: slots) {
                slot.cancel();
            }

            // sort to reacquire
            Arrays.sort(slots, RateLimitSlot::sortByLongestReacquirationFirst);

            // reacquire
            for(RateLimitSlot slot: slots) {
                slot.reacquire();
            }
        }

    }

    /**
     * Compares which of the given rate limits takes probably longer to acquire a slot from.
     * @param o1 Rate limit 1.
     * @param o2 Rate limit 2.
     * @return Returns {@code 1} if {@code o1} takes longer to acquire a slot, {@code -1} if {@code o2} takes longer,
     *         or {@code 0} if it takes equally long to acquire a slot.
     */
    static int sortByLongestAcquirationFirst(RateLimit o1, RateLimit o2) {
            // if o1.remaining is less than o2.remaining, then o1 is lesser
            long order = o1.getRemaining() - o2.getRemaining();
            // if o1.nextReset is bigger than o2.nextReset, then o1 is lesser
            return (int) (order != 0 ? order : o2.getNextReset() - o1.getNextReset());
    }

    /**
     * Acquires a slot in the current rate limit.
     * NOTE: This method will block until the next slot is acquirable.
     * @return Returns a {@link RateLimitSlot} instance representing the acquired slot.
     * @throws InterruptedException if the thread was interrupted while acquiring a slot.
     */
    default RateLimitSlot acquire() throws InterruptedException {
        return new RateLimitSlot(this, acquireAndGetRateID());
    }

    /**
     * Acquires a slot in the current rate limit.
     * NOTE: This method will block until the next slot is acquirable.
     * @return Returns the current rate ID.
     * @throws InterruptedException if the thread was interrupted while acquiring a slot.
     */
    long acquireAndGetRateID() throws InterruptedException;

    /**
     * Tries to cancel an acquired slot.
     * @param rateID Rate ID the slot was acquired from.
     * @return Returns {@code true} if the acquired slot could be returned to the rate limit.
     */
    boolean cancel(long rateID);

    /**
     * Rate limit slot amount per rate.
     * @return Returns the Rate limit slot amount.
     */
    long getCapacity();

    /**
     * Gets the next reset tick in nano seconds.
     * @return Returns the next reset tick in nano seconds.
     */
    long getNextReset();

    /**
     * Gets the remaining slots.
     * @return Returns the remaining slots.
     */
    long getRemaining();

    /**
     * Gets the current rate id.
     * @return Return the current rate id.
     */
    long getCurrentRateID();

    /**
     * Gets the reset interval in nano seconds.
     * @return Returns the reset interval in nano seconds.
     */
    long getResetInterval();

    /**
     * Wrap an instance rate limited.
     * @param instance Instance to wrap.
     * @param <T> Instance type.
     * @return Returns the rate limited wrapped instance.
     */
    default <T> RateLimited<T> limit(T instance) {
        return new RateLimited<T>(instance, this);
    }
}
