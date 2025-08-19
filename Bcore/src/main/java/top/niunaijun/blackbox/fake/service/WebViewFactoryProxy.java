package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.WebView;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * WebView Factory proxy to handle WebView initialization, provider selection,
 * and factory-related issues that commonly cause WebView problems.
 */
public class WebViewFactoryProxy extends ClassInvocationStub {
    public static final String TAG = "WebViewFactoryProxy";

    public WebViewFactoryProxy() {
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

    // Hook WebViewFactory.getWebViewProviderClass() to return a valid provider
    @ProxyMethod("getWebViewProviderClass")
    public static class GetWebViewProviderClass extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderClass called");
            
            try {
                // Try to get the WebView provider class from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider class");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider class", e);
            }
            
            // Return the default WebView provider class
            try {
                Class<?> webViewClass = WebView.class;
                Slog.d(TAG, "WebViewFactory: Returning default WebView class: " + webViewClass.getName());
                return webViewClass;
            } catch (Exception e) {
                Slog.e(TAG, "WebViewFactory: Failed to get default WebView class", e);
                return null;
            }
        }
    }

    // Hook WebViewFactory.getWebViewProviderPackage() to return a valid package
    @ProxyMethod("getWebViewProviderPackage")
    public static class GetWebViewProviderPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderPackage called");
            
            try {
                // Try to get the WebView provider package from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider package");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider package", e);
            }
            
            // Return a default WebView package name
            String defaultPackage = "com.google.android.webview";
            Slog.d(TAG, "WebViewFactory: Returning default WebView package: " + defaultPackage);
            return defaultPackage;
        }
    }

    // Hook WebViewFactory.getWebViewProviderInfo() to return provider information
    @ProxyMethod("getWebViewProviderInfo")
    public static class GetWebViewProviderInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderInfo called");
            
            try {
                // Try to get the WebView provider info from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider info");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider info", e);
            }
            
            // Return null to let the system use default
            Slog.d(TAG, "WebViewFactory: Returning null to use system default");
            return null;
        }
    }

    // Hook WebViewFactory.isWebViewSupported() to return true
    @ProxyMethod("isWebViewSupported")
    public static class IsWebViewSupported extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: isWebViewSupported called, returning true");
            return true; // Always indicate WebView is supported
        }
    }

    // Hook WebViewFactory.getWebViewPackage() to return a valid package
    @ProxyMethod("getWebViewPackage")
    public static class GetWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewPackage called");
            
            try {
                // Try to get the WebView package from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView package");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView package", e);
            }
            
            // Return a default WebView package
            String defaultPackage = "com.google.android.webview";
            Slog.d(TAG, "WebViewFactory: Returning default WebView package: " + defaultPackage);
            return defaultPackage;
        }
    }

    // Hook WebViewFactory.getWebViewProviderClassLoader() to return a valid class loader
    @ProxyMethod("getWebViewProviderClassLoader")
    public static class GetWebViewProviderClassLoader extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderClassLoader called");
            
            try {
                // Try to get the WebView provider class loader from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider class loader");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider class loader", e);
            }
            
            // Return the current class loader as fallback
            try {
                ClassLoader classLoader = WebView.class.getClassLoader();
                Slog.d(TAG, "WebViewFactory: Returning current class loader: " + classLoader);
                return classLoader;
            } catch (Exception e) {
                Slog.e(TAG, "WebViewFactory: Failed to get current class loader", e);
                return null;
            }
        }
    }

    // Hook WebViewFactory.getWebViewProviderPackageInfo() to return package info
    @ProxyMethod("getWebViewProviderPackageInfo")
    public static class GetWebViewProviderPackageInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderPackageInfo called");
            
            try {
                // Try to get the WebView provider package info from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider package info");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider package info", e);
            }
            
            // Return null to let the system use default
            Slog.d(TAG, "WebViewFactory: Returning null to use system default");
            return null;
        }
    }

    // Hook WebViewFactory.getWebViewProviderClass() with package name parameter
    @ProxyMethod("getWebViewProviderClass")
    public static class GetWebViewProviderClassWithPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String packageName = (String) args[0];
                Slog.d(TAG, "WebViewFactory: getWebViewProviderClass called for package: " + packageName);
            }
            
            try {
                // Try to get the WebView provider class from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider class");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider class", e);
            }
            
            // Return the default WebView class
            try {
                Class<?> webViewClass = WebView.class;
                Slog.d(TAG, "WebViewFactory: Returning default WebView class: " + webViewClass.getName());
                return webViewClass;
            } catch (Exception e) {
                Slog.e(TAG, "WebViewFactory: Failed to get default WebView class", e);
                return null;
            }
        }
    }

    // Hook WebViewFactory.getWebViewProviderClassLoader() with package name parameter
    @ProxyMethod("getWebViewProviderClassLoader")
    public static class GetWebViewProviderClassLoaderWithPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String packageName = (String) args[0];
                Slog.d(TAG, "WebViewFactory: getWebViewProviderClassLoader called for package: " + packageName);
            }
            
            try {
                // Try to get the WebView provider class loader from the system
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider class loader");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider class loader", e);
            }
            
            // Return the current class loader as fallback
            try {
                ClassLoader classLoader = WebView.class.getClassLoader();
                Slog.d(TAG, "WebViewFactory: Returning current class loader: " + classLoader);
                return classLoader;
            } catch (Exception e) {
                Slog.e(TAG, "WebViewFactory: Failed to get current class loader", e);
                return null;
            }
        }
    }
}
