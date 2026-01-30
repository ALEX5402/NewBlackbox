package top.niunaijun.blackbox.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class for managing storage permissions on Android 11+ (SDK 30+)
 * Provides utilities for requesting and checking MANAGE_EXTERNAL_STORAGE permission.
 * 
 * Created by alex5402
 */
public class StoragePermissionHelper {
    
    private static final String TAG = "StoragePermissionHelper";
    
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 1001;
    public static final int REQUEST_CODE_MANAGE_STORAGE = 1002;
    
    /**
     * Check if the app has all files access permission (MANAGE_EXTERNAL_STORAGE on Android 11+)
     */
    public static boolean hasAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            // On older versions, regular storage permission is sufficient
            return true;
        }
    }
    
    /**
     * Check if the app has basic storage permissions (READ/WRITE_EXTERNAL_STORAGE)
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12: Check MANAGE_EXTERNAL_STORAGE or fallback to READ
            return Environment.isExternalStorageManager() ||
                   ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 10 and below
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Check if full file access is available (either granted or not needed)
     */
    public static boolean hasFullFileAccess(Context context) {
        return hasAllFilesAccess() && hasStoragePermission(context);
    }
    
    /**
     * Request MANAGE_EXTERNAL_STORAGE permission by opening system settings
     * Call this for Android 11+ when you need all files access
     */
    public static void requestAllFilesAccess(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                } catch (Exception e) {
                    // Fallback: open general manage all files settings
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                }
            }
        }
    }
    
    /**
     * Request MANAGE_EXTERNAL_STORAGE permission with a custom package name
     * Useful for requesting on behalf of sandboxed apps
     */
    public static void requestAllFilesAccessForPackage(Activity activity, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + packageName));
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            } catch (Exception e) {
                // Fallback: open general manage all files settings
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            }
        }
    }
    
    /**
     * Request basic storage permissions (READ/WRITE_EXTERNAL_STORAGE)
     * For Android 13+, requests granular media permissions
     */
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Request granular media permissions
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                    },
                    REQUEST_CODE_STORAGE_PERMISSION);
        } else {
            // Android 12 and below
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE_STORAGE_PERMISSION);
        }
    }
    
    /**
     * Request all necessary storage permissions for full file access
     * This includes MANAGE_EXTERNAL_STORAGE on Android 11+
     */
    public static void requestFullFileAccess(Activity activity) {
        // First request basic permissions
        if (!hasStoragePermission(activity)) {
            requestStoragePermission(activity);
        }
        
        // Then request all files access if on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestAllFilesAccess(activity);
        }
    }
    
    /**
     * Check if we need to show permission rationale for storage permission
     */
    public static boolean shouldShowStorageRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, 
                    Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, 
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }
    
    /**
     * Get a user-friendly message about why storage permission is needed
     */
    public static String getPermissionRationale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return "This app needs access to all files to properly manage sandboxed applications. " +
                   "Please grant 'All files access' permission in the settings.";
        } else {
            return "This app needs storage permission to properly manage sandboxed applications. " +
                   "Please grant storage permission.";
        }
    }
    
    /**
     * Handle the result from permission request
     * @return true if all required permissions are now granted
     */
    public static boolean handlePermissionResult(Activity activity, int requestCode, 
            String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            // Check if any permission was granted
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    Slog.d(TAG, "Storage permission granted");
                    return true;
                }
            }
            Slog.w(TAG, "Storage permission denied");
            return false;
        }
        return false;
    }
    
    /**
     * Handle the result from MANAGE_EXTERNAL_STORAGE settings activity
     * Should be called in onActivityResult
     */
    public static boolean handleAllFilesAccessResult(int requestCode) {
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                boolean granted = Environment.isExternalStorageManager();
                if (granted) {
                    Slog.d(TAG, "All files access granted");
                } else {
                    Slog.w(TAG, "All files access denied");
                }
                return granted;
            }
        }
        return false;
    }
}
