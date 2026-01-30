package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.webkit.WebSettings;

import java.io.File;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.app.BActivityThread;

/**
 * Enhanced WebView proxy to handle WebView data directory conflicts, initialization issues,
 * and provide comprehensive WebView support in virtual environments.
 */
public class WebViewProxy extends ClassInvocationStub {
    public static final String TAG = "WebViewProxy";

    public WebViewProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null; // Not needed for class method hooks
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Not needed for class method hooks
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook WebView constructor to handle data directory conflicts
    @ProxyMethod("<init>")
    public static class Constructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebView: Constructor called, intercepting to prevent data directory conflicts");
            Context context = null;
            try {
                if (args != null && args.length > 0 && args[0] instanceof Context) {
                    context = (Context) args[0];
                } else {
                    context = BlackBoxCore.getContext();
                }

                if (context != null) {
                    // Create unique WebView data directory for this virtual app
                    String packageName = context.getPackageName();
                    String userId = String.valueOf(BActivityThread.getUserId());
                    String uniqueDataDir = context.getApplicationInfo().dataDir + "/webview_" + userId + "_" + android.os.Process.myPid();

                    // Ensure the directory exists
                    File dataDir = new File(uniqueDataDir);
                    if (!dataDir.exists()) {
                        dataDir.mkdirs();
                        Slog.d(TAG, "WebView: Created unique data directory: " + uniqueDataDir);
                    }

                    // Set system properties for WebView
                    System.setProperty("webview.data.dir", uniqueDataDir);
                    System.setProperty("webview.cache.dir", uniqueDataDir + "/cache");
                    System.setProperty("webview.cookies.dir", uniqueDataDir + "/cookies");

                    Slog.d(TAG, "WebView: Set custom data directory: " + uniqueDataDir);
                }

                // Proceed with normal constructor
                Object result = method.invoke(who, args);

                if (result instanceof WebView) {
                    WebView webView = (WebView) result;
                    // Configure WebView for better compatibility
                    configureWebView(webView);
                }

                return result;
            } catch (Exception e) {
                Slog.w(TAG, "WebView: Constructor failed, attempting fallback", e);
                // Try to create a minimal WebView
                return createFallbackWebView(context);
            }
        }
        
        private void configureWebView(WebView webView) {
            try {
                WebSettings settings = webView.getSettings();
                if (settings != null) {
                    // Enable JavaScript
                    settings.setJavaScriptEnabled(true);
                    // Enable DOM storage
                    settings.setDomStorageEnabled(true);
                    // Enable database
                    settings.setDatabaseEnabled(true);
                    // Set cache mode
                    settings.setCacheMode(WebSettings.LOAD_DEFAULT);

                    // Enable AppCache (Restored per user request, deprecated/removed in new SDKs)
                    try {
                        // Use reflection because methods are removed in API 33+ SDK
                        Method setAppCacheEnabled = settings.getClass().getMethod("setAppCacheEnabled", boolean.class);
                        setAppCacheEnabled.invoke(settings, true);

                        if (webView.getContext() != null) {
                            Method setAppCachePath = settings.getClass().getMethod("setAppCachePath", String.class);
                            setAppCachePath.invoke(settings, webView.getContext().getCacheDir().getAbsolutePath());
                        }
                    } catch (Throwable e) {
                        // Method missing on newer Android versions/SDKs, ignore
                        Slog.w(TAG, "WebView: AppCache not supported: " + e.getMessage());
                    }

                    // CRITICAL: Enable network access explicitly
                    settings.setBlockNetworkLoads(false);
                    settings.setBlockNetworkImage(false);

                    // Enable file access for local resources
                    settings.setAllowFileAccess(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        settings.setAllowFileAccessFromFileURLs(true);
                        settings.setAllowUniversalAccessFromFileURLs(true);
                    }

                    // Enable mixed content (for HTTPS sites with HTTP resources)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    }

                    // Set user agent
                    String userAgent = settings.getUserAgentString();
                    if (userAgent != null && !userAgent.contains("BlackBox")) {
                        settings.setUserAgentString(userAgent + " BlackBox");
                    }

                    // Force network available
                    try {
                        webView.setNetworkAvailable(true);
                    } catch (Exception e) {
                        // Ignore
                    }

                    // Allow content access
                    settings.setAllowContentAccess(true);

                    // Disable safe browsing (API 26+) - often fails in sandbox
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                         settings.setSafeBrowsingEnabled(false);
                    }

                    Slog.d(TAG, "WebView: Configured successfully with network access enabled");
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebView: Failed to configure settings", e);
            }
        }
        
        private WebView createFallbackWebView(Context context) {
            try {
                if (context != null) {
                    // Create a minimal WebView with basic configuration
                    WebView webView = new WebView(context);
                    WebSettings settings = webView.getSettings();
                    if (settings != null) {
                        settings.setJavaScriptEnabled(true);
                        settings.setDomStorageEnabled(true);
                    }
                    Slog.d(TAG, "WebView: Created fallback WebView");
                    return webView;
                }
            } catch (Exception e) {
                Slog.e(TAG, "WebView: Failed to create fallback WebView", e);
            }
            return null;
        }
    }

    // Hook WebView.setDataDirectorySuffix() to prevent conflicts
    @ProxyMethod("setDataDirectorySuffix")
    public static class SetDataDirectorySuffix extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0) {
                    String suffix = (String) args[0];
                    Slog.d(TAG, "WebView: setDataDirectorySuffix called with: " + suffix);
                    
                    // Create a unique suffix for each virtual app and process
                    Context context = BlackBoxCore.getContext();
                    String packageName = context != null ? context.getPackageName() : "unknown";
                    String userId = String.valueOf(BActivityThread.getUserId());
                    String uniqueSuffix = suffix + "_" + userId + "_" + android.os.Process.myPid();
                    args[0] = uniqueSuffix;
                    Slog.d(TAG, "WebView: Using unique suffix: " + uniqueSuffix);
                }
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "WebView: setDataDirectorySuffix failed, continuing without suffix", e);
                return null; // Return null to indicate success
            }
        }
    }

    // Hook WebView.getDataDirectory() to return unique directory
    @ProxyMethod("getDataDirectory")
    public static class GetDataDirectory extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "WebView: getDataDirectory called, returning unique directory");
                
                // Return a unique data directory for each virtual app and process
                Context context = BlackBoxCore.getContext();
                if (context != null) {
                    String packageName = context.getPackageName();
                    String userId = String.valueOf(BActivityThread.getUserId());
                    String uniqueDir = context.getApplicationInfo().dataDir + "/webview_" + userId + "_" + android.os.Process.myPid();
                    
                    // Ensure directory exists
                    File dir = new File(uniqueDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    
                    Slog.d(TAG, "WebView: Returning unique data directory: " + uniqueDir);
                    return uniqueDir;
                }
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "WebView: getDataDirectory failed, returning fallback", e);
                // Return a safe fallback directory
                return "/data/data/" + BlackBoxCore.getHostPkg() + "/webview_fallback";
            }
        }
    }

    // Hook WebViewDatabase.getInstance() to prevent conflicts
    @ProxyMethod("getInstance")
    public static class GetWebViewDatabaseInstance extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebView: getInstance called for WebViewDatabase");
            
            try {
                // Get the context from BlackBox
                Context context = BlackBoxCore.getContext();
                if (context != null) {
                    // Create a unique database instance
                    String packageName = context.getPackageName();
                    String userId = String.valueOf(BActivityThread.getUserId());
                    String uniqueDbPath = context.getApplicationInfo().dataDir + "/webview_db_" + userId + "_" + android.os.Process.myPid();
                    
                    // Set database path
                    System.setProperty("webview.database.path", uniqueDbPath);
                    Slog.d(TAG, "WebView: Set unique database path: " + uniqueDbPath);
                }
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "WebView: Failed to get WebViewDatabase instance", e);
                return null;
            }
        }
    }

    // Hook WebView.loadUrl() to handle common issues
    @ProxyMethod("loadUrl")
    public static class LoadUrl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String url = (String) args[0];
                Slog.d(TAG, "WebView: loadUrl called with: " + url);
                
                // Handle common URL issues
                if (url != null && url.startsWith("file://")) {
                    // Ensure file URLs work in virtual environment
                    Slog.d(TAG, "WebView: Handling file URL: " + url);
                }
            }
            
            return method.invoke(who, args);
        }
    }
}
