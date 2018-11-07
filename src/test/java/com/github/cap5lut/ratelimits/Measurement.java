package com.github.cap5lut.ratelimits;

import org.opentest4j.AssertionFailedError;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple time measurement.
 */
public class Measurement {
    /**
     * Measures execution time.
     * @param task Code to measure.
     * @return Returns the measurement for assertion.
     */
    public static Measurement measure(Runnable task) {
        long start = System.nanoTime();
        task.run();
        return new Measurement(System.nanoTime() - start);
    }

    /**
     * Measures execution time.
     * NOTE: The callable's return value will be ignored.
     * If the callable throws an exception the measurement will fail with the cause of the thrown exception.
     * A common useage would be to test some checked exception throwing code, which usually should not throw an exception.
     * @param callable Code to measure.
     * @param <T> Callable's return type.
     * @return Returns the measurement for assertion.
     */
    public static <T> Measurement measure(Callable<T> callable) {
        return measure(() -> {
            try {
                callable.call();
            } catch (Exception e) {
                throw new AssertionFailedError("measurement failed", e);
            }
        });
    }

    /**
     * Execution time in nano seconds.
     */
    private final long duration;

    /**
     * Default comparision threshold.
     */
    private long threshold;

    /**
     * Creates a new measurement instance.
     * @param duration Execution time of the measured code in nano seconds.
     */
    private Measurement(long duration) {
        this.duration = duration;
        setDefaultThreshold();
    }

    /**
     * Gets the current threshold.
     * @return Returns the current threshold.
     */
    public long getThreshold() {
        return threshold;
    }

    /**
     * Sets the default threshold.
     * @return Returns itself for method chaining.
     */
    public Measurement setDefaultThreshold() {
        return setThreshold(15, TimeUnit.MILLISECONDS);
    }

    /**
     * Sets the default threshold of comparision.
     * @param time Threshold time.
     * @param unit Threshold unit.
     * @return Returns itself for method chaining.
     */
    public Measurement setThreshold(long time, TimeUnit unit) {
        threshold = unit.toNanos(time);
        return this;
    }

    /**
     * Asserts if the measured duration is between.
     * @param exclusiveMinimum Exclusive minimum in nano seconds.
     * @param exclusiveMaximum Exclusive maximum in nano seconds.
     * @return Returns itself for method chaining.
     */
    private Measurement assertBetween(long exclusiveMinimum, long exclusiveMaximum) {
        return assertLongerThan(exclusiveMinimum).assertShorterThan(exclusiveMaximum);
    }

    /**
     * Asserts if the measured duration is equal using a threshold.
     * @param time Time in nano seconds.
     * @param threshold Threshold in nano seconds.
     * @return Returns itself for method chaining.
     */
    private Measurement assertEquals(long time, long threshold) {
        return assertLongerThan(time - threshold).assertShorterThan(time + threshold);
    }

    /**
     * Asserts if the measured duration is longer.
     * @param exclusiveMinimum Exclusive minimum in nano seconds.
     * @return Returns itself for method chaining.
     */
    private Measurement assertLongerThan(long exclusiveMinimum) {
        assertTrue(exclusiveMinimum < duration);
        return this;
    }

    /**
     * Asserts if the measured duration is shorter.
     * @param exclusiveMaximum Exclusive maximum in nano seconds.
     * @return Returns itself for method chaining.
     */
    private Measurement assertShorterThan(long exclusiveMaximum) {
        assertTrue(exclusiveMaximum > duration);
        return this;
    }

    /**
     * Checks if the duration equals using the default threshold.
     * @param time Time.
     * @param unit Time unit.
     */
    public void assertEquals(long time, TimeUnit unit) {
        assertEquals(unit.toNanos(time), threshold);

    }

    /**
     * Checks if the duration equals using the given threshold.
     * @param time Time.
     * @param unit Time unit.
     * @param threshold Threshold time.
     * @param thresholdUnit Threshold time unit.
     */
    public void assertEquals(long time, TimeUnit unit, long threshold, TimeUnit thresholdUnit) {
        assertEquals(unit.toNanos(time), thresholdUnit.toNanos(threshold));
    }

    /**
     * Checks if the duration is longer.
     * @param time Time.
     * @param unit Time unit.
     */
    public void assertLongerThan(long time, TimeUnit unit) {
        assertLongerThan(unit.toNanos(time));
    }

    /**
     * Checks if the duration is shorter.
     * @param time Time.
     * @param unit Time unit.
     */
    public void assertShorterThan(long time, TimeUnit unit) {
        assertShorterThan(unit.toNanos(time));
    }
}
