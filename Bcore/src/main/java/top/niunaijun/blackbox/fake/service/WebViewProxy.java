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


public class WebViewProxy extends ClassInvocationStub {
    public static final String TAG = "WebViewProxy";

    public WebViewProxy() {
        super();
    }

    
    @Override
    protected Object getWho() {
        return null; 
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("<init>")
    public static class Constructor extends MethodHook {
        private static boolean sDirectorySuffixSet = false;

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (!sDirectorySuffixSet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    String processSuffix = BActivityThread.getUserId() + "_" + android.os.Process.myPid();
                    android.webkit.WebView.setDataDirectorySuffix(processSuffix);
                    sDirectorySuffixSet = true;
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to set WebView data directory suffix", e);
                }
            }
            try {
                return method.invoke(who, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause() != null ? e.getCause() : e;
            } catch (Exception e) {
                throw e;
            }
        }
    }

    
    @ProxyMethod("setDataDirectorySuffix")
    public static class SetDataDirectorySuffix extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0) {
                    String suffix = (String) args[0];
                    Slog.d(TAG, "WebView: setDataDirectorySuffix called with: " + suffix);
                    
                    
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
                return null; 
            }
        }
    }

    
    @ProxyMethod("getDataDirectory")
    public static class GetDataDirectory extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "WebView: getDataDirectory called, returning unique directory");
                
                
                Context context = BlackBoxCore.getContext();
                if (context != null) {
                    String packageName = context.getPackageName();
                    String userId = String.valueOf(BActivityThread.getUserId());
                    String uniqueDir = context.getApplicationInfo().dataDir + "/webview_" + userId + "_" + android.os.Process.myPid();
                    
                    
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
                
                return "/data/data/" + BlackBoxCore.getHostPkg() + "/webview_fallback";
            }
        }
    }

    
    @ProxyMethod("getInstance")
    public static class GetWebViewDatabaseInstance extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebView: getInstance called for WebViewDatabase");
            
            try {
                
                Context context = BlackBoxCore.getContext();
                if (context != null) {
                    
                    String packageName = context.getPackageName();
                    String userId = String.valueOf(BActivityThread.getUserId());
                    String uniqueDbPath = context.getApplicationInfo().dataDir + "/webview_db_" + userId + "_" + android.os.Process.myPid();
                    
                    
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

    
    @ProxyMethod("loadUrl")
    public static class LoadUrl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String url = (String) args[0];
                Slog.d(TAG, "WebView: loadUrl called with: " + url);
                
                
                if (url != null && url.startsWith("file://")) {
                    
                    Slog.d(TAG, "WebView: Handling file URL: " + url);
                }
            }
            
            return method.invoke(who, args);
        }
    }
}
