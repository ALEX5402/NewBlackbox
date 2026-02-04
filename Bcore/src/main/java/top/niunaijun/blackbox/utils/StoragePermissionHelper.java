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


public class StoragePermissionHelper {
    
    private static final String TAG = "StoragePermissionHelper";
    
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 1001;
    public static final int REQUEST_CODE_MANAGE_STORAGE = 1002;
    
    
    public static boolean hasAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            
            return true;
        }
    }
    
    
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            
            return Environment.isExternalStorageManager() ||
                   ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    
    public static boolean hasFullFileAccess(Context context) {
        return hasAllFilesAccess() && hasStoragePermission(context);
    }
    
    
    public static void requestAllFilesAccess(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                } catch (Exception e) {
                    
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                }
            }
        }
    }
    
    
    public static void requestAllFilesAccessForPackage(Activity activity, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + packageName));
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            } catch (Exception e) {
                
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            }
        }
    }
    
    
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                    },
                    REQUEST_CODE_STORAGE_PERMISSION);
        } else {
            
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE_STORAGE_PERMISSION);
        }
    }
    
    
    public static void requestFullFileAccess(Activity activity) {
        
        if (!hasStoragePermission(activity)) {
            requestStoragePermission(activity);
        }
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestAllFilesAccess(activity);
        }
    }
    
    
    public static boolean shouldShowStorageRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, 
                    Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, 
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }
    
    
    public static String getPermissionRationale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return "This app needs access to all files to properly manage sandboxed applications. " +
                   "Please grant 'All files access' permission in the settings.";
        } else {
            return "This app needs storage permission to properly manage sandboxed applications. " +
                   "Please grant storage permission.";
        }
    }
    
    
    public static boolean handlePermissionResult(Activity activity, int requestCode, 
            String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            
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
