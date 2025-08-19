package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.Slog;

/**
 * UID Spoofing Helper to handle UID validation issues in virtual environments
 * This provides utilities for bypassing UID checks in system services
 */
public class UIDSpoofingHelper {
    private static final String TAG = "UIDSpoofingHelper";
    
    /**
     * Get the appropriate UID for system operations
     * This helps bypass UID validation issues
     */
    public static int getSystemUID() {
        try {
            // Return system UID (1000) for system-level operations
            return Process.SYSTEM_UID;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get system UID, using fallback", e);
            return 1000; // Fallback to system UID
        }
    }
    
    /**
     * Get the appropriate UID for package operations
     * This helps bypass package UID validation issues
     */
    public static int getPackageUID(String packageName) {
        try {
            Context context = BlackBoxCore.getContext();
            if (context != null) {
                // Try to get the real UID for the package
                PackageManager pm = context.getPackageManager();
                if (pm != null) {
                    int uid = pm.getPackageUid(packageName, 0);
                    if (uid > 0) { // Valid UIDs are positive numbers
                        Slog.d(TAG, "Found real UID for package " + packageName + ": " + uid);
                        return uid;
                    }
                }
            }
            
            // Fallback to system UID if package UID not found
            Slog.w(TAG, "Package UID not found for " + packageName + ", using system UID");
            return getSystemUID();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get package UID for " + packageName + ", using system UID", e);
            return getSystemUID();
        }
    }
    
    /**
     * Get the appropriate UID for job scheduling operations
     * This helps bypass job scheduler UID validation issues
     */
    public static int getJobSchedulingUID(String targetPackage) {
        try {
            // For job scheduling, we need to use the target package's UID
            int packageUid = getPackageUID(targetPackage);
            
            // If we got a valid package UID, use it
            if (packageUid > 0) { // Valid UIDs are positive numbers
                Slog.d(TAG, "Using package UID for job scheduling: " + packageUid);
                return packageUid;
            }
            
            // Fallback to system UID
            Slog.w(TAG, "Using system UID for job scheduling as fallback");
            return getSystemUID();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get job scheduling UID, using system UID", e);
            return getSystemUID();
        }
    }
    
    /**
     * Check if the current UID needs spoofing for a specific operation
     */
    public static boolean needsUIDSpoofing(String operation, String targetPackage) {
        try {
            int currentUid = Process.myUid();
            int targetUid = getPackageUID(targetPackage);
            
            // If current UID doesn't match target package UID, we need spoofing
            boolean needsSpoofing = currentUid != targetUid;
            
            Slog.d(TAG, "UID spoofing check: current=" + currentUid + 
                       ", target=" + targetUid + ", needsSpoofing=" + needsSpoofing);
            
            return needsSpoofing;
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to check UID spoofing need, assuming yes", e);
            return true; // Assume we need spoofing if check fails
        }
    }
    
    /**
     * Get the best UID for a specific system operation
     */
    public static int getBestUIDForOperation(String operation, String targetPackage) {
        try {
            switch (operation) {
                case "job_schedule":
                case "job_enqueue":
                    return getJobSchedulingUID(targetPackage);
                    
                case "content_provider":
                case "settings_access":
                    return getSystemUID();
                    
                case "package_operation":
                    return getPackageUID(targetPackage);
                    
                default:
                    // For unknown operations, try package UID first, then system UID
                    int packageUid = getPackageUID(targetPackage);
                    if (packageUid > 0) { // Valid UIDs are positive numbers
                        return packageUid;
                    }
                    return getSystemUID();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get best UID for operation " + operation + ", using system UID", e);
            return getSystemUID();
        }
    }
    
    /**
     * Log UID information for debugging
     */
    public static void logUIDInfo(String operation, String targetPackage) {
        try {
            int currentUid = Process.myUid();
            int bestUid = getBestUIDForOperation(operation, targetPackage);
            boolean needsSpoofing = needsUIDSpoofing(operation, targetPackage);
            
            Slog.d(TAG, String.format("UID Info - Operation: %s, Package: %s, Current: %d, Best: %d, NeedsSpoofing: %s",
                    operation, targetPackage, currentUid, bestUid, needsSpoofing));
                    
        } catch (Exception e) {
            Slog.w(TAG, "Failed to log UID info", e);
        }
    }
}
