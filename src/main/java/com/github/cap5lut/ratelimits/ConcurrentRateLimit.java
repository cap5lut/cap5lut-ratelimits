package com.github.cap5lut.ratelimits;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Lock-free {@link RateLimit} implementation.
 */
public class ConcurrentRateLimit implements RateLimit {
    /**
     * Rate limit state.
     */
    private final class State {
        /**
         * Next reset time stamp in nano seconds.
         */
        private final long nextReset;

        /**
         * Remaining rate limit slots.
         */
        private final long remaining;

        /**
         * Rate ID.
         */
        private final long rateID;

        /**
         * Resetting thread.
         */
        private final Thread resetter;

        /**
         * Creates a new initial state.
         */
        private State() {
            this(System.nanoTime() + resetInterval, capacity, 0, null);
        }

        /**
         * Creates a new state.
         * @param nextReset Next reset timestamp in nano seconds.
         * @param remaining Remaining slots.
         * @param rateID Rate ID.
         * @param resetter Resetting thread.
         */
        private State(long nextReset, long remaining, long rateID, Thread resetter) {
            this.nextReset = nextReset;
            this.remaining = remaining;
            this.rateID = rateID;
            this.resetter = resetter;
        }

        /**
         * Gets the state with the current thread as resetter.
         * @return Returns the resetting state.
         */
        private State getResettingState() {
            return new State(nextReset, remaining, rateID, Thread.currentThread());
        }

        /**
         * Gets the resetted state.
         * @return Returns the resetted state.
         */
        private State getResettedState() {
            long skippedRates = (long) (Math.floor((System.nanoTime() - nextReset) / (resetInterval * 1.0d)) + 1);
            return new State(nextReset + skippedRates * resetInterval, capacity, rateID + skippedRates, null);
        }

        /**
         * Gets the decremented state.
         * @return Returns the decremented state.
         */
        private State getDecrementedState() {
            if(remaining < 1) {
                throw new IllegalStateException("remaining must not become lower than zero");
            }

            return new State(nextReset, remaining - 1, rateID, null);
        }

        /**
         * Gets the incremented state.
         * @return Returns the incremented state.
         */
        private State getIncrementedState() {
            if(remaining == capacity) {
                throw new IllegalStateException("remaining must not become greater than the capacity");
            }
            return new State(nextReset, remaining + 1, rateID, resetter);
        }

        /**
         * Checks if the state is outdated.
         * @return Returns {@code true} if the state is outdated.
         */
        private boolean hasToUpdate() {
            return System.nanoTime() >= nextReset;
        }

        /**
         * Checks if waiting for the next reset is needed.
         * @return Returns {@code true} if waiting for the next reset is needed.
         */
        private boolean hasToWait() {
            return System.nanoTime() < nextReset;
        }

        /**
         * Checks if a given rate ID is the current rate ID.
         * @param rateID Rate ID.
         * @return Returns {@code true} if the given rate ID is the current rate ID.
         */
        private boolean isCurrentRate(long rateID) {
            return this.rateID == rateID;
        }

        /**
         * Checks if there are no remaining slots available.
         * @return Returns {@code true} if there are no remaining slots available.
         */
        private boolean isEmpty() {
            return remaining == 0;
        }

        /**
         * Checks if the state will be resetted.
         * @return Returns {@code true} if the state will be resetted.
         */
        private boolean isResetting() {
            return resetter != null;
        }

        /**
         * Checks if the current thread is the resetting thread.
         * @return Returns {@code true} if the current thread is the resetting thread.
         */
        private boolean isResetter() {
            return resetter == Thread.currentThread();
        }
    }

    /**
     * Yielder.
     */
    private final Yielder yielder;

    /**
     * Rate limit capacity.
     */
    private final long capacity;

    /**
     * Rate limit reset interval in nano seconds.
     */
    private final long resetInterval;

    /**
     * Current rate limit state.
     */
    private final AtomicStampedReference<State> currentState;

    /**
     * Creates a new rate limit with the default yielder.
     * @param capacity Rate limit capacity.
     * @param resetInterval Rate limit reset interval.
     * @param resetUnit Rate limit reset interval unit.
     */
    public ConcurrentRateLimit(long capacity, long resetInterval, TimeUnit resetUnit) {
        this(capacity, resetInterval, resetUnit, defaultYielder);
    }

    /**
     * Creates a new rate limit.
     * @param capacity Rate limit capacity.
     * @param resetInterval Rate limit reset interval.
     * @param resetUnit Rate limit reset interval unit.
     * @param yielder Yielder.
     */
    public ConcurrentRateLimit(long capacity, long resetInterval, TimeUnit resetUnit, Yielder yielder) {
        this.capacity = capacity;
        this.resetInterval = resetUnit.toNanos(resetInterval);
        this.yielder = yielder;
        currentState = new AtomicStampedReference<>(new State(), 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long acquireAndGetRateID() throws InterruptedException {
        int[] stampHolder = new int[1];
        State current;
        State next;
        while(true) {
            current = getUpdatedState(stampHolder);
            if(current.isEmpty()) {
                // TODO: can this be done differently by letting all threads try to reset?
                if (current.isResetter()) {
                    if (current.hasToWait()) { // wait until its time to reset
                        yielder.yield();
                        continue;
                    }
                    else {
                        next = current.getResettedState(); // its time to reset
                    }
                }
                else if(current.isResetting()){ // wait for reset
                    yielder.yield();
                    continue;
                }
                else { // resetting needs to be started
                    next = current.getResettingState();
                }
            }
            else{
                next = current.getDecrementedState();
                if(currentState.compareAndSet(current, current.getDecrementedState(), stampHolder[0], stampHolder[0] + 1)) {
                    return next.rateID;
                }
                continue;
            }

            // set the new state
            currentState.compareAndSet(current, next, stampHolder[0], stampHolder[0] + 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel(long rateID) {
        int[] stampHolder = new int[1];
        State current;
        while(true) {
            current = getUpdatedState(stampHolder);

            if(!current.isCurrentRate(rateID)) { // is not the same rate id anymore
                return false;
            }
            if(currentState.compareAndSet(current, current.getIncrementedState(), stampHolder[0], stampHolder[0] + 1)) {
                return true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCapacity() {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCurrentRateID() {
        return getUpdatedState().rateID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNextReset() {
        return getUpdatedState().nextReset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRemaining() {
        return getUpdatedState().remaining;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getResetInterval() {
        return resetInterval;
    }

    /**
     * Updates the current state for missing resets.
     * @return Returns the updated state.
     */
    private State getUpdatedState() {
        return getUpdatedState(new int[1]);
    }

    /**
     * Updates the current state for missing resets.
     * @param stampHolder Stamp holder.
     * @return Returns the updated state.
     */
    private State getUpdatedState(int[] stampHolder) {
        State current;
        State next;
        do {
            current = currentState.get(stampHolder);
            if(!current.hasToUpdate()) {
                return current;
            }
            next = current.getResettedState();
        } while(!currentState.compareAndSet(current, next, stampHolder[0], stampHolder[0] + 1));
        return next;
    }
}
