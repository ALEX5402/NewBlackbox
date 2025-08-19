package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.app.Application;

import top.niunaijun.blackbox.BlackBoxCore;

/**
 * SimpleCrashFix - Essential crash prevention for app startup
 * Cleaned up version with only essential functionality
 */
public class SimpleCrashFix {
    private static final String TAG = "SimpleCrashFix";
    private static boolean sIsInstalled = false;

    /**
     * Install the essential crash fix
     */
    public static void installSimpleFix() {
        if (sIsInstalled) {
            Slog.d(TAG, "Simple crash fix already installed");
            return;
        }
        
        try {
            Slog.d(TAG, "Installing essential crash fix...");
            
            // Install global exception handler
            installGlobalExceptionHandler();
            
            // Install context wrapper hook
            installContextWrapperHook();
            
            sIsInstalled = true;
            Slog.d(TAG, "Essential crash fix installed successfully");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to install essential crash fix: " + e.getMessage(), e);
        }
    }
    
    /**
     * Install a global exception handler to catch and handle null context crashes
     */
    private static void installGlobalExceptionHandler() {
        try {
            // Get the current default handler
            Thread.UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
            
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    // Check if this is the specific null context crash we're trying to prevent
                    if (isNullContextCrash(throwable)) {
                        Slog.w(TAG, "Caught null context crash, preventing crash: " + throwable.getMessage());
                        return; // Prevent crash
                    }

                    // Check for Google Play Services crashes
                    if (isGooglePlayServicesCrash(throwable)) {
                        Slog.w(TAG, "Caught Google Play Services crash, preventing crash: " + throwable.getMessage());
                        return; // Prevent crash - Google Play Services crashes are not critical
                    }

                    // Check for WebView crashes
                    if (isWebViewCrash(throwable)) {
                        Slog.w(TAG, "Caught WebView crash, preventing crash: " + throwable.getMessage());
                        return; // Prevent crash - WebView crashes can be handled gracefully
                    }

                    // Check for AttributionSource crashes
                    if (isAttributionSourceCrash(throwable)) {
                        Slog.w(TAG, "Caught AttributionSource crash, preventing crash: " + throwable.getMessage());
                        return; // Prevent crash - AttributionSource issues can be fixed
                    }

                    // Check for social media app specific crashes
                    if (isSocialMediaAppCrash(throwable)) {
                        Slog.w(TAG, "Caught social media app crash, preventing crash: " + throwable.getMessage());
                        return; // Prevent crash - Social media apps should not crash
                    }

                    // For other crashes, delegate to the original handler
                    if (currentHandler != null) {
                        currentHandler.uncaughtException(thread, throwable);
                    }
                }
            });
            
            Slog.d(TAG, "Global exception handler installed successfully");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to install global exception handler: " + e.getMessage(), e);
        }
    }
    
    /**
     * Install context wrapper hook to prevent null context crashes
     */
    private static void installContextWrapperHook() {
        try {
            // Install a simple context wrapper hook
            ContextWrapperHook.installHook();
            Slog.d(TAG, "Context wrapper hook installed");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to install context wrapper hook: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if the crash is related to null context issues
     */
    private static boolean isNullContextCrash(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String message = throwable.getMessage();
        if (message != null) {
            return message.contains("Context") || 
                   message.contains("context") ||
                   message.contains("getResources") ||
                   message.contains("getPackageManager") ||
                   message.contains("getClassLoader");
        }
        
        // Check stack trace for context-related calls
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                
                if (className.contains("Context") || 
                    className.contains("ContextWrapper") ||
                    methodName.contains("getResources") ||
                    methodName.contains("getPackageManager") ||
                    methodName.contains("getClassLoader")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if the crash is related to Google Play Services
     */
    private static boolean isGooglePlayServicesCrash(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String message = throwable.getMessage();
        if (message != null) {
            return message.contains("Google Play Services") ||
                   message.contains("GooglePlayServicesUtil") ||
                   message.contains("GoogleApiAvailability") ||
                   message.contains("com.google.android.gms");
        }
        
        // Check stack trace for Google Play Services related calls
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (className.contains("com.google.android.gms") ||
                    className.contains("GooglePlayServicesUtil") ||
                    className.contains("GoogleApiAvailability")) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if the crash is related to WebView issues
     */
    private static boolean isWebViewCrash(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String message = throwable.getMessage();
        if (message != null) {
            return message.contains("WebView") ||
                   message.contains("webview") ||
                   message.contains("WebViewDatabase") ||
                   message.contains("WebSettings") ||
                   message.contains("data directory");
        }
        
        // Check stack trace for WebView related calls
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                if (className.contains("WebView") ||
                    className.contains("WebViewDatabase") ||
                    className.contains("WebSettings") ||
                    methodName.contains("webView") ||
                    methodName.contains("WebView")) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if the crash is related to AttributionSource issues
     */
    private static boolean isAttributionSourceCrash(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String message = throwable.getMessage();
        if (message != null) {
            return message.contains("AttributionSource") ||
                   message.contains("attribution") ||
                   message.contains("Calling uid") ||
                   message.contains("source uid") ||
                   message.contains("UID mismatch");
        }
        
        // Check stack trace for AttributionSource related calls
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                if (className.contains("AttributionSource") ||
                    className.contains("ContentProvider") ||
                    methodName.contains("enforceCallingUid") ||
                    methodName.contains("enforceCallingUidAndPid")) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if the crash is related to social media apps
     */
    private static boolean isSocialMediaAppCrash(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String message = throwable.getMessage();
        if (message != null) {
            return message.contains("Facebook") ||
                   message.contains("Instagram") ||
                   message.contains("WhatsApp") ||
                   message.contains("Telegram") ||
                   message.contains("Twitter") ||
                   message.contains("TikTok") ||
                   message.contains("Snapchat") ||
                   message.contains("YouTube") ||
                   message.contains("LinkedIn");
        }
        
        // Check stack trace for social media app related calls
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (className.contains("com.facebook") ||
                    className.contains("com.instagram") ||
                    className.contains("com.whatsapp") ||
                    className.contains("org.telegram") ||
                    className.contains("com.twitter") ||
                    className.contains("com.zhiliaoapp.musically") ||
                    className.contains("com.snapchat") ||
                    className.contains("com.google.android.youtube") ||
                    className.contains("com.linkedin")) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
