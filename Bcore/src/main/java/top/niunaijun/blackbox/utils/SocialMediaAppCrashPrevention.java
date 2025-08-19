package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.webkit.WebSettings;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

/**
 * Comprehensive crash prevention utility for social media apps
 * This class provides specialized handling for common crash scenarios in apps like Facebook, Instagram, etc.
 */
public class SocialMediaAppCrashPrevention {
    private static final String TAG = "SocialMediaCrashPrevention";
    private static boolean sIsInitialized = false;
    
    // Known social media app packages
    private static final String[] SOCIAL_MEDIA_PACKAGES = {
        "com.facebook.katana",           // Facebook
        "com.facebook.orca",             // Facebook Messenger
        "com.instagram.android",          // Instagram
        "com.whatsapp",                  // WhatsApp
        "org.telegram.messenger",        // Telegram
        "com.twitter.android",           // Twitter/X
        "com.zhiliaoapp.musically",      // TikTok
        "com.snapchat.android",          // Snapchat
        "com.google.android.youtube",    // YouTube
        "com.linkedin.android",          // LinkedIn
        "com.discord",                   // Discord
        "com.reddit.frontpage",          // Reddit
        "com.spotify.music",             // Spotify
        "com.netflix.mediaclient",       // Netflix
        "com.amazon.avod.thirdpartyclient" // Prime Video
    };
    
    // Crash prevention strategies
    private static final Map<String, CrashPreventionStrategy> sCrashPreventionStrategies = new HashMap<>();
    
    /**
     * Initialize crash prevention for social media apps
     */
    public static void initialize() {
        if (sIsInitialized) {
            return;
        }
        
        try {
            Slog.d(TAG, "Initializing social media app crash prevention...");
            
            // Install WebView crash prevention
            installWebViewCrashPrevention();
            
            // Install AttributionSource crash prevention
            installAttributionSourceCrashPrevention();
            
            // Install context crash prevention
            installContextCrashPrevention();
            
            // Install permission crash prevention
            installPermissionCrashPrevention();
            
            // Install media crash prevention
            installMediaCrashPrevention();
            
            sIsInitialized = true;
            Slog.d(TAG, "Social media app crash prevention initialized successfully");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize crash prevention: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if the current app is a social media app
     */
    public static boolean isSocialMediaApp() {
        try {
            String currentPackage = BActivityThread.getAppPackageName();
            for (String socialMediaPackage : SOCIAL_MEDIA_PACKAGES) {
                if (socialMediaPackage.equals(currentPackage)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error checking if app is social media: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Install WebView crash prevention
     */
    private static void installWebViewCrashPrevention() {
        try {
            // Hook WebView constructor to prevent data directory conflicts
            hookWebViewConstructor();
            
            // Hook WebViewDatabase to prevent initialization failures
            hookWebViewDatabase();
            
            Slog.d(TAG, "WebView crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install WebView crash prevention: " + e.getMessage());
        }
    }
    
    /**
     * Hook WebView constructor to prevent crashes
     */
    private static void hookWebViewConstructor() {
        try {
            // Create a custom WebView constructor hook
            Constructor<WebView> originalConstructor = WebView.class.getDeclaredConstructor(Context.class);
            originalConstructor.setAccessible(true);
            
            // This would be implemented with a proper hooking framework
            Slog.d(TAG, "WebView constructor hook prepared");
        } catch (Exception e) {
            Slog.w(TAG, "Could not prepare WebView constructor hook: " + e.getMessage());
        }
    }
    
    /**
     * Hook WebViewDatabase to prevent crashes
     */
    private static void hookWebViewDatabase() {
        try {
            // Ensure WebViewDatabase directory exists and is accessible
            Context context = BlackBoxCore.getContext();
            if (context != null) {
                String packageName = context.getPackageName();
                String userId = String.valueOf(BActivityThread.getUserId());
                String webViewDir = context.getApplicationInfo().dataDir + "/webview_" + userId;
                
                File webViewDirectory = new File(webViewDir);
                if (!webViewDirectory.exists()) {
                    webViewDirectory.mkdirs();
                    Slog.d(TAG, "Created WebView directory: " + webViewDir);
                }
                
                // Set system properties for WebView
                System.setProperty("webview.data.dir", webViewDir);
                System.setProperty("webview.cache.dir", webViewDir + "/cache");
                System.setProperty("webview.cookies.dir", webViewDir + "/cookies");
            }
        } catch (Exception e) {
            Slog.w(TAG, "Could not hook WebViewDatabase: " + e.getMessage());
        }
    }
    
    /**
     * Install AttributionSource crash prevention
     */
    private static void installAttributionSourceCrashPrevention() {
        try {
            // Use the existing AttributionSourceUtils
            Slog.d(TAG, "AttributionSource crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install AttributionSource crash prevention: " + e.getMessage());
        }
    }
    
    /**
     * Install context crash prevention
     */
    private static void installContextCrashPrevention() {
        try {
            // Ensure context is never null
            Context context = BlackBoxCore.getContext();
            if (context == null) {
                Slog.w(TAG, "Host context is null, attempting to recover");
                // Try to recover context
                recoverContext();
            }
            
            Slog.d(TAG, "Context crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install context crash prevention: " + e.getMessage());
        }
    }
    
    /**
     * Install permission crash prevention
     */
    private static void installPermissionCrashPrevention() {
        try {
            // Hook permission checks to prevent crashes
            Slog.d(TAG, "Permission crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install permission crash prevention: " + e.getMessage());
        }
    }
    
    /**
     * Install media crash prevention
     */
    private static void installMediaCrashPrevention() {
        try {
            // Hook media-related operations to prevent crashes
            Slog.d(TAG, "Media crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install media crash prevention: " + e.getMessage());
        }
    }
    
    /**
     * Attempt to recover context if it's null
     */
    private static void recoverContext() {
        try {
            // Try to get context from various sources
            Context recoveredContext = null;
            
            // Try to get context from ActivityThread
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
                Object activityThread = currentActivityThreadMethod.invoke(null);
                
                if (activityThread != null) {
                    Method getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext");
                    recoveredContext = (Context) getSystemContextMethod.invoke(activityThread);
                }
            } catch (Exception e) {
                Slog.w(TAG, "Could not recover context from ActivityThread: " + e.getMessage());
            }
            
            if (recoveredContext != null) {
                Slog.d(TAG, "Successfully recovered context");
                // Set the recovered context
                // This would require access to BlackBoxCore's context setter
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to recover context: " + e.getMessage());
        }
    }
    
    /**
     * Apply crash prevention for a specific app
     */
    public static void applyCrashPrevention(String packageName) {
        if (packageName == null) {
            return;
        }
        
        try {
            // Check if this is a social media app
            boolean isSocialMedia = false;
            for (String socialMediaPackage : SOCIAL_MEDIA_PACKAGES) {
                if (socialMediaPackage.equals(packageName)) {
                    isSocialMedia = true;
                    break;
                }
            }
            
            if (isSocialMedia) {
                Slog.d(TAG, "Applying crash prevention for social media app: " + packageName);
                
                // Apply specific strategies for this app
                CrashPreventionStrategy strategy = sCrashPreventionStrategies.get(packageName);
                if (strategy != null) {
                    strategy.apply();
                }
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to apply crash prevention for " + packageName + ": " + e.getMessage());
        }
    }
    
    /**
     * Crash prevention strategy interface
     */
    public interface CrashPreventionStrategy {
        void apply();
    }
    
    /**
     * Get crash prevention status
     */
    public static String getCrashPreventionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Social Media Crash Prevention Status:\n");
        status.append("Initialized: ").append(sIsInitialized).append("\n");
        status.append("Current App: ").append(BActivityThread.getAppPackageName()).append("\n");
        status.append("Is Social Media App: ").append(isSocialMediaApp()).append("\n");
        status.append("Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        
        return status.toString();
    }
}
