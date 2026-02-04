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


public class WebViewFactoryProxy extends ClassInvocationStub {
    public static final String TAG = "WebViewFactoryProxy";

    public WebViewFactoryProxy() {
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

    
    @ProxyMethod("getWebViewProviderClass")
    public static class GetWebViewProviderClass extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderClass called");
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider class");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider class", e);
            }
            
            
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

    
    @ProxyMethod("getWebViewProviderPackage")
    public static class GetWebViewProviderPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderPackage called");
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider package");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider package", e);
            }
            
            
            String defaultPackage = "com.google.android.webview";
            Slog.d(TAG, "WebViewFactory: Returning default WebView package: " + defaultPackage);
            return defaultPackage;
        }
    }

    
    @ProxyMethod("getWebViewProviderInfo")
    public static class GetWebViewProviderInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderInfo called");
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider info");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider info", e);
            }
            
            
            Slog.d(TAG, "WebViewFactory: Returning null to use system default");
            return null;
        }
    }

    
    @ProxyMethod("isWebViewSupported")
    public static class IsWebViewSupported extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: isWebViewSupported called, returning true");
            return true; 
        }
    }

    
    @ProxyMethod("getWebViewPackage")
    public static class GetWebViewPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewPackage called");
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView package");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView package", e);
            }
            
            
            String defaultPackage = "com.google.android.webview";
            Slog.d(TAG, "WebViewFactory: Returning default WebView package: " + defaultPackage);
            return defaultPackage;
        }
    }

    
    @ProxyMethod("getWebViewProviderClassLoader")
    public static class GetWebViewProviderClassLoader extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderClassLoader called");
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider class loader");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider class loader", e);
            }
            
            
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

    
    @ProxyMethod("getWebViewProviderPackageInfo")
    public static class GetWebViewProviderPackageInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebViewFactory: getWebViewProviderPackageInfo called");
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider package info");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider package info", e);
            }
            
            
            Slog.d(TAG, "WebViewFactory: Returning null to use system default");
            return null;
        }
    }

    
    @ProxyMethod("getWebViewProviderClass")
    public static class GetWebViewProviderClassWithPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String packageName = (String) args[0];
                Slog.d(TAG, "WebViewFactory: getWebViewProviderClass called for package: " + packageName);
            }
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider class");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider class", e);
            }
            
            
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

    
    @ProxyMethod("getWebViewProviderClassLoader")
    public static class GetWebViewProviderClassLoaderWithPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String packageName = (String) args[0];
                Slog.d(TAG, "WebViewFactory: getWebViewProviderClassLoader called for package: " + packageName);
            }
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "WebViewFactory: Successfully got WebView provider class loader");
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebViewFactory: Failed to get WebView provider class loader", e);
            }
            
            
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
