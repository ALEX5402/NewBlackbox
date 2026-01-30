package top.niunaijun.blackbox.fake.service;

import android.os.Build;
import android.os.Process;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Proxy to handle browser-specific (GeckoView/Chromium) issues in virtualized environment.
 * Browsers use advanced sandboxing and multi-process architecture that can conflict with virtualization.
 */
public class BrowserEngineProxy extends ClassInvocationStub {
    public static final String TAG = "BrowserEngineProxy";

    public BrowserEngineProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Not needed for class method hooks
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    /**
     * Hook Process.start() to handle browser child process creation
     * Browsers (especially Gecko/Chromium) spawn isolated child processes for rendering
     */
    @ProxyMethod("start")
    public static class ProcessStart extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Let the process start normally
                Object result = method.invoke(who, args);
                
                // Log for debugging
                if (args != null && args.length > 0) {
                    Slog.d(TAG, "Process.start() called with args count: " + args.length);
                }
                
                return result;
            } catch (Exception e) {
                Slog.e(TAG, "Process.start() failed: " + e.getMessage(), e);
                // Re-throw to let the app handle it
                throw e;
            }
        }
    }

    /**
     * Hook Process.setArgV0() which browsers use to rename child processes
     */
    @ProxyMethod("setArgV0")
    public static class SetArgV0 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String argV0 = (String) args[0];
                Slog.d(TAG, "Process.setArgV0() called: " + argV0);
            }
            return method.invoke(who, args);
        }
    }

    /**
     * Hook Process.killProcess() to log unexpected terminations
     */
    @ProxyMethod("killProcess")
    public static class KillProcess extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                int pid = (int) args[0];
                Slog.d(TAG, "Process.killProcess() called for PID: " + pid);
            }
            return method.invoke(who, args);
        }
    }
}
