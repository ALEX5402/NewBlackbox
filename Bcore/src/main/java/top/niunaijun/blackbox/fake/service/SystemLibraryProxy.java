package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * System library proxy to handle native library loading issues in sandboxed apps.
 */
public class SystemLibraryProxy extends ClassInvocationStub {
    public static final String TAG = "SystemLibraryProxy";

    public SystemLibraryProxy() {
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

    // Hook System.loadLibrary() to handle missing libraries
    @ProxyMethod("loadLibrary")
    public static class LoadLibrary extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof String) {
                String libraryName = (String) args[0];
                Slog.d(TAG, "System: loadLibrary called for: " + libraryName);
                
                // Handle common missing libraries
                if (libraryName.equals("c++_shared") || libraryName.contains("c++")) {
                    Slog.d(TAG, "System: Intercepting c++ library load, returning success");
                    return null; // Return null to indicate success
                }
                
                if (libraryName.contains("flutter") || libraryName.contains("meemo")) {
                    Slog.d(TAG, "System: Intercepting Flutter/Meemo library load, returning success");
                    return null; // Return null to indicate success
                }
            }
            
            // For other libraries, proceed normally
            return method.invoke(who, args);
        }
    }

    // Hook System.load() for absolute path library loading
    @ProxyMethod("load")
    public static class Load extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof String) {
                String libraryPath = (String) args[0];
                Slog.d(TAG, "System: load called for: " + libraryPath);
                
                // Handle common missing libraries
                if (libraryPath.contains("libc++_shared.so") || libraryPath.contains("c++_shared")) {
                    Slog.d(TAG, "System: Intercepting libc++_shared.so load, returning success");
                    return null; // Return null to indicate success
                }
                
                if (libraryPath.contains("flutter") || libraryPath.contains("meemo")) {
                    Slog.d(TAG, "System: Intercepting Flutter/Meemo library load, returning success");
                    return null; // Return null to indicate success
                }
            }
            
            // For other libraries, proceed normally
            return method.invoke(who, args);
        }
    }
}
