package top.niunaijun.blackbox.fake.service;

import android.content.pm.PackageInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;

import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Enhanced WebView update service proxy to handle WebView provider issues,
 * package validation, and provide comprehensive WebView service support.
 */
public class IWebViewUpdateServiceProxy extends ClassInvocationStub {
    public static final String TAG = "IWebViewUpdateServiceProxy";

    public IWebViewUpdateServiceProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null; // Not needed for class method hooks
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Hook WebView update service methods
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook getCurrentWebViewPackage() to return a valid WebView package
    @ProxyMethod("getCurrentWebViewPackage")
    public static class GetCurrentWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewUpdateService: getCurrentWebViewPackage called");
            
            try {
                // Try to get the current WebView package from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewUpdateService: Successfully got current WebView package");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewUpdateService: Failed to get current WebView package", e);
            }
            
            // Return null to let the system use the default WebView provider
            Slog.d(TAG, "WebViewUpdateService: Returning null to use system default");
            return null;
        }
    }

    // Hook getValidWebViewPackages() to return valid packages
    @ProxyMethod("getValidWebViewPackages")
    public static class GetValidWebViewPackages extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewUpdateService: getValidWebViewPackages called");
            
            try {
                // Try to get valid WebView packages from the system
                Object result = method.invoke(who, args);
                if (result != null && result instanceof PackageInfo[]) {
                    PackageInfo[] packages = (PackageInfo[]) result;
                    if (packages.length > 0) {
                        Slog.d(TAG, "WebViewUpdateService: Found " + packages.length + " valid WebView packages");
                        return result;
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewUpdateService: Failed to get valid WebView packages", e);
            }
            
            // Return empty array to prevent WebView provider conflicts
            Slog.d(TAG, "WebViewUpdateService: Returning empty array to prevent conflicts");
            return new PackageInfo[0];
        }
    }

    // Hook isMultiProcessEnabled() to return true
    @ProxyMethod("isMultiProcessEnabled")
    public static class IsMultiProcessEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewUpdateService: isMultiProcessEnabled called, returning true");
            return true;
        }
    }

    // Hook getWebViewPackages() to return available WebView packages
    @ProxyMethod("getWebViewPackages")
    public static class GetWebViewPackages extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewUpdateService: getWebViewPackages called");
            
            try {
                // Try to get WebView packages from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewUpdateService: Successfully got WebView packages");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewUpdateService: Failed to get WebView packages", e);
            }
            
            // Return empty list if system call fails
            return new ArrayList<>();
        }
    }

    // Hook getWebViewProviderInfo() to return provider information
    @ProxyMethod("getWebViewProviderInfo")
    public static class GetWebViewProviderInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewUpdateService: getWebViewProviderInfo called");
            
            try {
                // Try to get WebView provider info from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewUpdateService: Successfully got WebView provider info");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewUpdateService: Failed to get WebView provider info", e);
            }
            
            // Return null if system call fails
            return null;
        }
    }

    // Hook isWebViewPackage() to validate WebView packages
    @ProxyMethod("isWebViewPackage")
    public static class IsWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String packageName = (String) args[0];
                Slog.d(TAG, "WebViewUpdateService: isWebViewPackage called for: " + packageName);
                
                // Check if it's a known WebView package
                if (isKnownWebViewPackage(packageName)) {
                    Slog.d(TAG, "WebViewUpdateService: " + packageName + " is a known WebView package");
                    return true;
                }
            }
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "WebViewUpdateService: Failed to check WebView package", e);
                return false;
            }
        }
        
        private boolean isKnownWebViewPackage(String packageName) {
            if (packageName == null) return false;
            
            // List of known WebView packages
            return packageName.equals("com.google.android.webview") ||
                   packageName.equals("com.google.android.webview.dev") ||
                   packageName.equals("com.google.android.webview.beta") ||
                   packageName.equals("com.google.android.webview.canary") ||
                   packageName.equals("com.android.webview") ||
                   packageName.equals("com.huawei.webview") ||
                   packageName.equals("com.samsung.android.webview") ||
                   packageName.equals("com.oneplus.webview");
        }
    }

    // Hook getWebViewProvider() to get the current WebView provider
    @ProxyMethod("getWebViewProvider")
    public static class GetWebViewProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewUpdateService: getWebViewProvider called");
            
            try {
                // Try to get WebView provider from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewUpdateService: Successfully got WebView provider");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewUpdateService: Failed to get WebView provider", e);
            }
            
            // Return null if system call fails
            return null;
        }
    }

    // Hook enableWebViewPackage() to enable WebView packages
    @ProxyMethod("enableWebViewPackage")
    public static class EnableWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String packageName = (String) args[0];
                Slog.d(TAG, "WebViewUpdateService: enableWebViewPackage called for: " + packageName);
            }
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "WebViewUpdateService: Failed to enable WebView package", e);
                // Return success to prevent crashes
                return true;
            }
        }
    }

    // Hook disableWebViewPackage() to disable WebView packages
    @ProxyMethod("disableWebViewPackage")
    public static class DisableWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String packageName = (String) args[0];
                Slog.d(TAG, "WebViewUpdateService: disableWebViewPackage called for: " + packageName);
            }
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "WebViewUpdateService: Failed to disable WebView package", e);
                // Return success to prevent crashes
                return true;
            }
        }
    }
}
