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


public class XiaomiPermissionManager {
    private static final String TAG = "XiaomiPermissionManager";
    private static final XiaomiPermissionManager sInstance = new XiaomiPermissionManager();
    
    
    private static final String XIAOMI_AUTOSTART_PERMISSION = "miui.permission.USE_INTERNAL_GENERAL_API";
    private static final String XIAOMI_BATTERY_OPTIMIZATION = "miui.permission.OPTIMIZE_POWER";
    private static final String XIAOMI_BACKGROUND_RUNNING = "miui.permission.RUN_IN_BACKGROUND";
    private static final String XIAOMI_NOTIFICATION_PERMISSION = "miui.permission.POST_NOTIFICATIONS";
    
    
    private static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    
    
    private static final String XIAOMI_AUTOSTART_SETTINGS = "miui.intent.action.OP_AUTO_START";
    private static final String XIAOMI_BATTERY_SETTINGS = "miui.intent.action.POWER_HIDE_MODE_APP_LIST";
    private static final String XIAOMI_NOTIFICATION_SETTINGS = "miui.intent.action.NOTIFICATION_SETTINGS";
    
    public static XiaomiPermissionManager get() {
        return sInstance;
    }
    
    private XiaomiPermissionManager() {}
    
    
    public boolean isXiaomiDevice() {
        return BuildCompat.isMIUI() || 
               Build.MANUFACTURER.toLowerCase().contains("xiaomi") ||
               Build.BRAND.toLowerCase().contains("xiaomi") ||
               Build.DISPLAY.toLowerCase().contains("hyperos");
    }
    
    
    public boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            
            return true;
        }
        
        Context context = BlackBoxCore.getContext();
        return ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
    
    
    public boolean isXiaomiBackgroundPermissionGranted() {
        if (!isXiaomiDevice()) {
            return true; 
        }
        
        Context context = BlackBoxCore.getContext();
        
        
        try {
            
            if (ContextCompat.checkSelfPermission(context, XIAOMI_AUTOSTART_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                Slog.d(TAG, "Xiaomi autostart permission not granted");
                return false;
            }
            
            
            if (ContextCompat.checkSelfPermission(context, XIAOMI_BATTERY_OPTIMIZATION) != PackageManager.PERMISSION_GRANTED) {
                Slog.d(TAG, "Xiaomi battery optimization permission not granted");
                return false;
            }
            
            
            if (ContextCompat.checkSelfPermission(context, XIAOMI_BACKGROUND_RUNNING) != PackageManager.PERMISSION_GRANTED) {
                Slog.d(TAG, "Xiaomi background running permission not granted");
                return false;
            }
            
            
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
            
            grantPermissionThroughReflection(POST_NOTIFICATIONS);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to grant notification permission automatically: " + e.getMessage());
        }
    }
    
    
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
            
            grantPermissionThroughReflection(XIAOMI_AUTOSTART_PERMISSION);
            grantPermissionThroughReflection(XIAOMI_BATTERY_OPTIMIZATION);
            grantPermissionThroughReflection(XIAOMI_BACKGROUND_RUNNING);
            grantPermissionThroughReflection(XIAOMI_NOTIFICATION_PERMISSION);
            
            
            disableBatteryOptimization();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to grant Xiaomi background permissions automatically: " + e.getMessage());
        }
    }
    
    
    public void grantAllRequiredPermissions() {
        Slog.d(TAG, "Granting all required permissions for optimal functionality");
        
        
        grantNotificationPermission();
        
        
        grantXiaomiBackgroundPermissions();
        
        
        createNotificationChannels();
        
        Slog.d(TAG, "Permission grant process completed");
    }
    
    
    public void openXiaomiPermissionSettings() {
        if (!isXiaomiDevice()) {
            Slog.w(TAG, "Not a Xiaomi device, cannot open Xiaomi settings");
            return;
        }
        
        Context context = BlackBoxCore.getContext();
        
        try {
            
            Intent autostartIntent = new Intent(XIAOMI_AUTOSTART_SETTINGS);
            autostartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(autostartIntent);
            Slog.d(TAG, "Opened Xiaomi autostart settings");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to open Xiaomi autostart settings: " + e.getMessage());
            
            
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
    
    
    private void grantPermissionThroughReflection(String permission) {
        try {
            Context context = BlackBoxCore.getContext();
            
            
            PackageManager packageManager = context.getPackageManager();
            
            
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
    
    
    public boolean areAllPermissionsGranted() {
        boolean notificationGranted = isNotificationPermissionGranted();
        boolean xiaomiGranted = isXiaomiBackgroundPermissionGranted();
        
        Slog.d(TAG, "Permission status - Notification: " + notificationGranted + ", Xiaomi: " + xiaomiGranted);
        
        return notificationGranted && xiaomiGranted;
    }
    
    
    public String getPermissionStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Permission Status Summary:\n");
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            summary.append("• POST_NOTIFICATIONS: ").append(isNotificationPermissionGranted() ? "GRANTED" : "DENIED").append("\n");
        } else {
            summary.append("• POST_NOTIFICATIONS: NOT_REQUIRED (Android < 12)\n");
        }
        
        
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
