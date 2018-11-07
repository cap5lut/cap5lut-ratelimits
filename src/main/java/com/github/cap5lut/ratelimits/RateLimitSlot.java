package com.github.cap5lut.ratelimits;

/**
 * Represents an acquired rate limit slot.
 */
public class RateLimitSlot {
    /**
     * Checks if all rate limit slots are valid.
     * @param rateLimitSlots Rate limit slots to test.
     * @return Returns {@code true} if all rate limit slots are valid.
     */
    public static boolean areValid(RateLimitSlot...rateLimitSlots) {
        for(RateLimitSlot slot: rateLimitSlots) {
            if(!slot.isValid()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Compares which of the given rate limits slots takes probably longer to reacquire a slot.
     * @param o1 Rate limit slot 1.
     * @param o2 Rate limit slot 2.
     * @return Returns {@code 1} if {@code o1} takes longer to reacquire a slot, {@code -1} if {@code o2} takes longer,
     *         or {@code 0} if it takes equally long to reacquire a slot.
     */
    public static int sortByLongestReacquirationFirst(RateLimitSlot o1, RateLimitSlot o2) {
        return RateLimit.sortByLongestAcquirationFirst(o1.getRateLimit(), o2.getRateLimit());
    }

    /**
     * Originating rate limit.
     */
    private final RateLimit rateLimit;

    /**
     * Originating rate limit ID.
     */
    private long rateID;

    /**
     * Creates a rate limit slot.
     * @param rateLimit Originating rate limit.
     * @param rateID Originating rate ID.
     */
    public RateLimitSlot(RateLimit rateLimit, long rateID) {
        this.rateLimit = rateLimit;
        this.rateID = rateID;
    }

    /**
     * Cancel the acquired slot.
     */
    public void cancel() {
        rateLimit.cancel(rateID);
        --rateID;
    }

    /**
     * Gets the originating rate limit.
     * @return Returns the originating rate limit.
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * Checks if the slot is still valid.
     * @return Returns true if the slot is still valid.
     */
    public boolean isValid() {
        return rateLimit.getCurrentRateID() == rateID;
    }

    /**
     * Acquires a new slot.
     * @throws InterruptedException if the thread was interrupted.
     */
    public void reacquire() throws InterruptedException {
        rateID = rateLimit.acquireAndGetRateID();
    }
}
