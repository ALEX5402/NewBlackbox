package top.niunaijun.blackbox.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.R;
import top.niunaijun.blackbox.utils.Slog;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.OvershootInterpolator;


public class LauncherActivity extends Activity {
    public static final String TAG = "SplashScreen";

    public static final String KEY_INTENT = "launch_intent";
    public static final String KEY_PKG = "launch_pkg";
    public static final String KEY_USER_ID = "launch_user_id";
    private boolean isRunning = false;

    public static void launch(Intent intent, int userId) {
        try {
            Intent splash = new Intent();
            splash.setClass(BlackBoxCore.getContext(), LauncherActivity.class);
            
            splash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            splash.putExtra(LauncherActivity.KEY_INTENT, intent);
            splash.putExtra(LauncherActivity.KEY_PKG, intent.getPackage());
            splash.putExtra(LauncherActivity.KEY_USER_ID, userId);
            BlackBoxCore.getContext().startActivity(splash);
            Slog.d(TAG, "LauncherActivity.launch() called for package: " + intent.getPackage());
        } catch (Exception e) {
            Slog.e(TAG, "Error in LauncherActivity.launch()", e);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            
            Intent intent = getIntent();
            if (intent == null) {
                Slog.w(TAG, "Intent is null, finishing activity");
                finish();
                return;
            }
            
            Intent launchIntent = intent.getParcelableExtra(KEY_INTENT);
            String packageName = intent.getStringExtra(KEY_PKG);
            int userId = intent.getIntExtra(KEY_USER_ID, 0);

            if (launchIntent == null || packageName == null) {
                Slog.w(TAG, "Missing launch intent or package name, finishing activity");
                finish();
                return;
            }

            Slog.d(TAG, "LauncherActivity.onCreate() for package: " + packageName + ", userId: " + userId);

            
            PackageInfo packageInfo = getPackageInfoWithFallback(packageName, userId);
            
            if (packageInfo == null) {
                Slog.w(TAG, "Package info not available for " + packageName + ", but proceeding with launch");
                
            } else {
                Slog.d(TAG, "Successfully retrieved package info for " + packageName);
            }
            
            
            Drawable drawable = null;
            String appName = packageName;
            try {
                if (packageInfo != null && packageInfo.applicationInfo != null) {
                    PackageManager pm = getPackageManager();
                    drawable = pm.getApplicationIcon(packageInfo.applicationInfo);
                    CharSequence label = pm.getApplicationLabel(packageInfo.applicationInfo);
                    if (label != null) appName = label.toString();
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to load app icon or name for " + packageName + ": " + e.getMessage());
            }
            setContentView(R.layout.activity_launcher);
            ImageView iconView = findViewById(R.id.iv_icon);
            TextView nameView = findViewById(R.id.tv_app_name);
            if (nameView != null) {
                nameView.setText(appName);
                nameView.setAlpha(0f);
                nameView.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .start();
            }
            if (iconView != null && drawable != null) {
                iconView.setImageDrawable(drawable);
                iconView.setScaleX(0.7f);
                iconView.setScaleY(0.7f);
                iconView.setAlpha(0f);
                iconView.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .alpha(1f)
                    .setDuration(350)
                    .setInterpolator(new OvershootInterpolator())
                    .withEndAction(() -> iconView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start())
                    .start();
            }
            
            
            launchAppAsync(launchIntent, userId);
            
        } catch (Exception e) {
            Slog.e(TAG, "Critical error in LauncherActivity.onCreate()", e);
            finish();
        }
    }

    
    private PackageInfo getPackageInfoWithFallback(String packageName, int userId) {
        try {
            
            return BlackBoxCore.getBPackageManager().getPackageInfo(packageName, 0, userId);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get package info for " + packageName + " (attempt 1): " + e.getMessage());
            
            try {
                
                return BlackBoxCore.getBPackageManager().getPackageInfo(packageName, 
                    android.content.pm.PackageManager.GET_META_DATA, userId);
            } catch (Exception e2) {
                Slog.w(TAG, "Failed to get package info for " + packageName + " (attempt 2): " + e2.getMessage());
                
                try {
                    
                    android.content.pm.ApplicationInfo appInfo = BlackBoxCore.getBPackageManager()
                        .getApplicationInfo(packageName, 0, userId);
                    
                    if (appInfo != null) {
                        
                        PackageInfo fallbackInfo = new PackageInfo();
                        fallbackInfo.packageName = packageName;
                        fallbackInfo.applicationInfo = appInfo;
                        fallbackInfo.versionCode = 1;
                        fallbackInfo.versionName = "1.0";
                        fallbackInfo.firstInstallTime = System.currentTimeMillis();
                        fallbackInfo.lastUpdateTime = System.currentTimeMillis();
                        
                        Slog.d(TAG, "Created fallback PackageInfo for " + packageName);
                        return fallbackInfo;
                    }
                } catch (Exception e3) {
                    Slog.w(TAG, "Failed to get application info for " + packageName + ": " + e3.getMessage());
                }
            }
        }
        
        return null;
    }

    
    private void launchAppAsync(final Intent launchIntent, final int userId) {
        new Thread(() -> {
            try {
                Slog.d(TAG, "Starting app launch in background thread");
                
                
                Thread.sleep(100);
                
                
                BlackBoxCore.getBActivityManager().startActivity(launchIntent, userId);
                
                Slog.d(TAG, "App launch initiated successfully");
            } catch (Exception e) {
                Slog.e(TAG, "Error launching app", e);
                
                
                runOnUiThread(() -> {
                    try {
                        
                        Slog.e(TAG, "Failed to launch app: " + e.getMessage());
                    } catch (Exception uiException) {
                        Slog.e(TAG, "Error showing error message", uiException);
                    }
                });
            }
        }, "AppLaunchThread").start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRunning) {
            finish();
        }
    }
}
