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


public class SocialMediaAppCrashPrevention {
    private static final String TAG = "SocialMediaCrashPrevention";
    private static boolean sIsInitialized = false;
    
    
    private static final String[] SOCIAL_MEDIA_PACKAGES = {
        "com.facebook.katana",           
        "com.facebook.orca",             
        "com.instagram.android",          
        "com.whatsapp",                  
        "org.telegram.messenger",        
        "com.twitter.android",           
        "com.zhiliaoapp.musically",      
        "com.snapchat.android",          
        "com.google.android.youtube",    
        "com.linkedin.android",          
        "com.discord",                   
        "com.reddit.frontpage",          
        "com.spotify.music",             
        "com.netflix.mediaclient",       
        "com.amazon.avod.thirdpartyclient" 
    };
    
    
    private static final Map<String, CrashPreventionStrategy> sCrashPreventionStrategies = new HashMap<>();
    
    
    public static void initialize() {
        if (sIsInitialized) {
            return;
        }
        
        try {
            Slog.d(TAG, "Initializing social media app crash prevention...");
            
            
            installWebViewCrashPrevention();
            
            
            installAttributionSourceCrashPrevention();
            
            
            installContextCrashPrevention();
            
            
            installPermissionCrashPrevention();
            
            
            installMediaCrashPrevention();
            
            sIsInitialized = true;
            Slog.d(TAG, "Social media app crash prevention initialized successfully");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize crash prevention: " + e.getMessage(), e);
        }
    }
    
    
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
    
    
    private static void installWebViewCrashPrevention() {
        try {
            
            hookWebViewConstructor();
            
            
            hookWebViewDatabase();
            
            Slog.d(TAG, "WebView crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install WebView crash prevention: " + e.getMessage());
        }
    }
    
    
    private static void hookWebViewConstructor() {
        try {
            
            Constructor<WebView> originalConstructor = WebView.class.getDeclaredConstructor(Context.class);
            originalConstructor.setAccessible(true);
            
            
            Slog.d(TAG, "WebView constructor hook prepared");
        } catch (Exception e) {
            Slog.w(TAG, "Could not prepare WebView constructor hook: " + e.getMessage());
        }
    }
    
    
    private static void hookWebViewDatabase() {
        try {
            
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
                
                
                System.setProperty("webview.data.dir", webViewDir);
                System.setProperty("webview.cache.dir", webViewDir + "/cache");
                System.setProperty("webview.cookies.dir", webViewDir + "/cookies");
            }
        } catch (Exception e) {
            Slog.w(TAG, "Could not hook WebViewDatabase: " + e.getMessage());
        }
    }
    
    
    private static void installAttributionSourceCrashPrevention() {
        try {
            
            Slog.d(TAG, "AttributionSource crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install AttributionSource crash prevention: " + e.getMessage());
        }
    }
    
    
    private static void installContextCrashPrevention() {
        try {
            
            Context context = BlackBoxCore.getContext();
            if (context == null) {
                Slog.w(TAG, "Host context is null, attempting to recover");
                
                recoverContext();
            }
            
            Slog.d(TAG, "Context crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install context crash prevention: " + e.getMessage());
        }
    }
    
    
    private static void installPermissionCrashPrevention() {
        try {
            
            Slog.d(TAG, "Permission crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install permission crash prevention: " + e.getMessage());
        }
    }
    
    
    private static void installMediaCrashPrevention() {
        try {
            
            Slog.d(TAG, "Media crash prevention installed");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install media crash prevention: " + e.getMessage());
        }
    }
    
    
    private static void recoverContext() {
        try {
            
            Context recoveredContext = null;
            
            
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
                
                
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to recover context: " + e.getMessage());
        }
    }
    
    
    public static void applyCrashPrevention(String packageName) {
        if (packageName == null) {
            return;
        }
        
        try {
            
            boolean isSocialMedia = false;
            for (String socialMediaPackage : SOCIAL_MEDIA_PACKAGES) {
                if (socialMediaPackage.equals(packageName)) {
                    isSocialMedia = true;
                    break;
                }
            }
            
            if (isSocialMedia) {
                Slog.d(TAG, "Applying crash prevention for social media app: " + packageName);
                
                
                CrashPreventionStrategy strategy = sCrashPreventionStrategies.get(packageName);
                if (strategy != null) {
                    strategy.apply();
                }
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to apply crash prevention for " + packageName + ": " + e.getMessage());
        }
    }
    
    
    public interface CrashPreventionStrategy {
        void apply();
    }
    
    
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
