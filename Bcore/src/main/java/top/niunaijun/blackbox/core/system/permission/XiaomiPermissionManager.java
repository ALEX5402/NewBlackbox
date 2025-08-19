package top.niunaijun.blackbox.core.system.permission;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

/**
 * Comprehensive permission manager for Xiaomi HyperOS devices
 * Handles Android 12+ notification permissions and Xiaomi background running permissions
 */
public class XiaomiPermissionManager {
    private static final String TAG = "XiaomiPermissionManager";
    private static final XiaomiPermissionManager sInstance = new XiaomiPermissionManager();
    
    // Xiaomi-specific permission constants
    private static final String XIAOMI_AUTOSTART_PERMISSION = "miui.permission.USE_INTERNAL_GENERAL_API";
    private static final String XIAOMI_BATTERY_OPTIMIZATION = "miui.permission.OPTIMIZE_POWER";
    private static final String XIAOMI_BACKGROUND_RUNNING = "miui.permission.RUN_IN_BACKGROUND";
    private static final String XIAOMI_NOTIFICATION_PERMISSION = "miui.permission.POST_NOTIFICATIONS";
    
    // Android 12+ notification permission
    private static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    
    // Xiaomi system settings
    private static final String XIAOMI_AUTOSTART_SETTINGS = "miui.intent.action.OP_AUTO_START";
    private static final String XIAOMI_BATTERY_SETTINGS = "miui.intent.action.POWER_HIDE_MODE_APP_LIST";
    private static final String XIAOMI_NOTIFICATION_SETTINGS = "miui.intent.action.NOTIFICATION_SETTINGS";
    
    public static XiaomiPermissionManager get() {
        return sInstance;
    }
    
    private XiaomiPermissionManager() {}
    
    /**
     * Check if this is a Xiaomi/HyperOS device
     */
    public boolean isXiaomiDevice() {
        return BuildCompat.isMIUI() || 
               Build.MANUFACTURER.toLowerCase().contains("xiaomi") ||
               Build.BRAND.toLowerCase().contains("xiaomi") ||
               Build.DISPLAY.toLowerCase().contains("hyperos");
    }
    
    /**
     * Check if notification permission is granted (Android 12+)
     */
    public boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Android 12- doesn't require explicit notification permission
            return true;
        }
        
        Context context = BlackBoxCore.getContext();
        return ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if Xiaomi background running permission is granted
     */
    public boolean isXiaomiBackgroundPermissionGranted() {
        if (!isXiaomiDevice()) {
            return true; // Not a Xiaomi device
        }
        
        Context context = BlackBoxCore.getContext();
        
        // Check various Xiaomi permissions
        try {
            // Check autostart permission
            if (ContextCompat.checkSelfPermission(context, XIAOMI_AUTOSTART_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                Slog.d(TAG, "Xiaomi autostart permission not granted");
                return false;
            }
            
            // Check battery optimization
            if (ContextCompat.checkSelfPermission(context, XIAOMI_BATTERY_OPTIMIZATION) != PackageManager.PERMISSION_GRANTED) {
                Slog.d(TAG, "Xiaomi battery optimization permission not granted");
                return false;
            }
            
            // Check background running
            if (ContextCompat.checkSelfPermission(context, XIAOMI_BACKGROUND_RUNNING) != PackageManager.PERMISSION_GRANTED) {
                Slog.d(TAG, "Xiaomi background running permission not granted");
                return false;
            }
            
            // Check notification permission
            if (ContextCompat.checkSelfPermission(context, XIAOMI_NOTIFICATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                Slog.d(TAG, "Xiaomi notification permission not granted");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Error checking Xiaomi permissions: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Grant notification permission automatically (Android 12+)
     */
    public void grantNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Slog.d(TAG, "Notification permission not required on Android " + Build.VERSION.SDK_INT);
            return;
        }
        
        if (isNotificationPermissionGranted()) {
            Slog.d(TAG, "Notification permission already granted");
            return;
        }
        
        Slog.d(TAG, "Attempting to grant notification permission automatically");
        
        try {
            // Try to grant permission through reflection
            grantPermissionThroughReflection(POST_NOTIFICATIONS);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to grant notification permission automatically: " + e.getMessage());
        }
    }
    
    /**
     * Grant Xiaomi background running permissions automatically
     */
    public void grantXiaomiBackgroundPermissions() {
        if (!isXiaomiDevice()) {
            Slog.d(TAG, "Not a Xiaomi device, skipping background permission grant");
            return;
        }
        
        if (isXiaomiBackgroundPermissionGranted()) {
            Slog.d(TAG, "Xiaomi background permissions already granted");
            return;
        }
        
        Slog.d(TAG, "Attempting to grant Xiaomi background permissions automatically");
        
        try {
            // Grant various Xiaomi permissions
            grantPermissionThroughReflection(XIAOMI_AUTOSTART_PERMISSION);
            grantPermissionThroughReflection(XIAOMI_BATTERY_OPTIMIZATION);
            grantPermissionThroughReflection(XIAOMI_BACKGROUND_RUNNING);
            grantPermissionThroughReflection(XIAOMI_NOTIFICATION_PERMISSION);
            
            // Also try to disable battery optimization
            disableBatteryOptimization();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to grant Xiaomi background permissions automatically: " + e.getMessage());
        }
    }
    
    /**
     * Grant all required permissions for optimal functionality
     */
    public void grantAllRequiredPermissions() {
        Slog.d(TAG, "Granting all required permissions for optimal functionality");
        
        // Grant notification permission (Android 12+)
        grantNotificationPermission();
        
        // Grant Xiaomi background permissions
        grantXiaomiBackgroundPermissions();
        
        // Create notification channels
        createNotificationChannels();
        
        Slog.d(TAG, "Permission grant process completed");
    }
    
    /**
     * Open Xiaomi permission settings for manual configuration
     */
    public void openXiaomiPermissionSettings() {
        if (!isXiaomiDevice()) {
            Slog.w(TAG, "Not a Xiaomi device, cannot open Xiaomi settings");
            return;
        }
        
        Context context = BlackBoxCore.getContext();
        
        try {
            // Try to open Xiaomi autostart settings
            Intent autostartIntent = new Intent(XIAOMI_AUTOSTART_SETTINGS);
            autostartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(autostartIntent);
            Slog.d(TAG, "Opened Xiaomi autostart settings");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to open Xiaomi autostart settings: " + e.getMessage());
            
            // Fallback to general app settings
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                settingsIntent.setData(uri);
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settingsIntent);
                Slog.d(TAG, "Opened general app settings as fallback");
            } catch (Exception e2) {
                Slog.e(TAG, "Failed to open any settings: " + e2.getMessage());
            }
        }
    }
    
    /**
     * Open notification permission settings
     */
    public void openNotificationSettings() {
        Context context = BlackBoxCore.getContext();
        
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Slog.d(TAG, "Opened notification settings");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to open notification settings: " + e.getMessage());
        }
    }
    
    /**
     * Create notification channels for the app
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        
        Context context = BlackBoxCore.getContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager == null) {
            Slog.w(TAG, "NotificationManager not available");
            return;
        }
        
        try {
            // Create main notification channel
            android.app.NotificationChannel mainChannel = new android.app.NotificationChannel(
                "blackbox_main",
                "BlackBox Core",
                NotificationManager.IMPORTANCE_HIGH
            );
            mainChannel.setDescription("Core BlackBox functionality notifications");
            mainChannel.enableLights(true);
            mainChannel.enableVibration(true);
            mainChannel.setShowBadge(true);
            
            notificationManager.createNotificationChannel(mainChannel);
            Slog.d(TAG, "Created main notification channel");
            
            // Create background service channel
            android.app.NotificationChannel backgroundChannel = new android.app.NotificationChannel(
                "blackbox_background",
                "BlackBox Background",
                NotificationManager.IMPORTANCE_LOW
            );
            backgroundChannel.setDescription("Background service notifications");
            backgroundChannel.enableLights(false);
            backgroundChannel.enableVibration(false);
            backgroundChannel.setShowBadge(false);
            
            notificationManager.createNotificationChannel(backgroundChannel);
            Slog.d(TAG, "Created background notification channel");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create notification channels: " + e.getMessage());
        }
    }
    
    /**
     * Disable battery optimization for the app
     */
    private void disableBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        
        Context context = BlackBoxCore.getContext();
        
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                return;
            }
            
            String packageName = context.getPackageName();
            boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName);
            
            if (!isIgnoringBatteryOptimizations) {
                Slog.d(TAG, "App is not ignoring battery optimizations, attempting to disable");
                
                // Try to disable through reflection
                try {
                    Method setIgnoreBatteryOptimizations = PowerManager.class.getMethod(
                        "setIgnoreBatteryOptimizations", String.class, boolean.class
                    );
                    setIgnoreBatteryOptimizations.invoke(powerManager, packageName, true);
                    Slog.d(TAG, "Successfully disabled battery optimization through reflection");
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to disable battery optimization through reflection: " + e.getMessage());
                }
            } else {
                Slog.d(TAG, "App is already ignoring battery optimizations");
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Error checking battery optimization: " + e.getMessage());
        }
    }
    
    /**
     * Grant permission through reflection (requires system privileges)
     */
    private void grantPermissionThroughReflection(String permission) {
        try {
            Context context = BlackBoxCore.getContext();
            
            // Try to use PackageManager to grant permission
            PackageManager packageManager = context.getPackageManager();
            
            // Use reflection to call grantRuntimePermission
            Method grantRuntimePermission = PackageManager.class.getMethod(
                "grantRuntimePermission", String.class, String.class, int.class
            );
            
            String packageName = context.getPackageName();
            int userId = android.os.Process.myUserHandle().hashCode();
            
            grantRuntimePermission.invoke(packageManager, packageName, permission, userId);
            Slog.d(TAG, "Successfully granted permission: " + permission);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to grant permission " + permission + " through reflection: " + e.getMessage());
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    public boolean areAllPermissionsGranted() {
        boolean notificationGranted = isNotificationPermissionGranted();
        boolean xiaomiGranted = isXiaomiBackgroundPermissionGranted();
        
        Slog.d(TAG, "Permission status - Notification: " + notificationGranted + ", Xiaomi: " + xiaomiGranted);
        
        return notificationGranted && xiaomiGranted;
    }
    
    /**
     * Get a summary of current permission status
     */
    public String getPermissionStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Permission Status Summary:\n");
        
        // Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            summary.append("• POST_NOTIFICATIONS: ").append(isNotificationPermissionGranted() ? "GRANTED" : "DENIED").append("\n");
        } else {
            summary.append("• POST_NOTIFICATIONS: NOT_REQUIRED (Android < 12)\n");
        }
        
        // Xiaomi permissions
        if (isXiaomiDevice()) {
            summary.append("• Xiaomi Autostart: ").append(isXiaomiBackgroundPermissionGranted() ? "GRANTED" : "DENIED").append("\n");
            summary.append("• Xiaomi Battery Optimization: ").append(isXiaomiBackgroundPermissionGranted() ? "DISABLED" : "ENABLED").append("\n");
            summary.append("• Xiaomi Background Running: ").append(isXiaomiBackgroundPermissionGranted() ? "ALLOWED" : "RESTRICTED").append("\n");
        } else {
            summary.append("• Xiaomi Permissions: NOT_APPLICABLE\n");
        }
        
        return summary.toString();
    }
}
