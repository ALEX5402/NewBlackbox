package top.niunaijun.blackbox.utils;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class to throttle Binder transactions and prevent process freezing
 * due to too many failed transactions in a short time period.
 */
public class TransactionThrottler {
    private static final String TAG = "TransactionThrottler";
    
    private static final int MAX_FAILURES_PER_WINDOW = 50; // Much less aggressive
    private static final long WINDOW_DURATION_MS = 15000; // Longer window
    private static final long THROTTLE_DURATION_MS = 1000; // Shorter throttle
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong windowStartTime = new AtomicLong(0);
    private final AtomicLong lastThrottleTime = new AtomicLong(0);
    
    /**
     * Check if we should throttle transactions due to too many recent failures
     * @return true if transactions should be throttled
     */
    public boolean shouldThrottle() {
        long currentTime = System.currentTimeMillis();
        long windowStart = windowStartTime.get();
        
        // Reset window if it has expired
        if (currentTime - windowStart > WINDOW_DURATION_MS) {
            failureCount.set(0);
            windowStartTime.set(currentTime);
            return false;
        }
        
        // Check if we're in a throttle period
        long lastThrottle = lastThrottleTime.get();
        if (currentTime - lastThrottle < THROTTLE_DURATION_MS) {
            return true;
        }
        
        // Check if we've exceeded the failure threshold
        if (failureCount.get() >= MAX_FAILURES_PER_WINDOW) {
            lastThrottleTime.set(currentTime);
            Log.w(TAG, "Throttling transactions due to " + failureCount.get() + " failures in window");
            return true;
        }
        
        return false;
    }
    
    /**
     * Record a transaction failure
     */
    public void recordFailure() {
        long currentTime = System.currentTimeMillis();
        long windowStart = windowStartTime.get();
        
        // Initialize window if not set
        if (windowStart == 0) {
            windowStartTime.set(currentTime);
        }
        
        // Reset window if it has expired
        if (currentTime - windowStart > WINDOW_DURATION_MS) {
            failureCount.set(1);
            windowStartTime.set(currentTime);
        } else {
            failureCount.incrementAndGet();
        }
        
        Log.d(TAG, "Recorded transaction failure, count: " + failureCount.get());
    }
    
    /**
     * Reset the failure count (call when transactions start succeeding)
     */
    public void reset() {
        failureCount.set(0);
        windowStartTime.set(0);
        lastThrottleTime.set(0);
        Log.d(TAG, "Reset transaction throttler");
    }
    
    /**
     * Get current failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }
}


