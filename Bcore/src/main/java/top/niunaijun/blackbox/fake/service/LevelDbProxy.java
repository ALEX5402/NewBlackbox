package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * LevelDB proxy to handle GMS LevelDB lock issues and database problems.
 */
public class LevelDbProxy extends ClassInvocationStub {
    public static final String TAG = "LevelDbProxy";

    public LevelDbProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null; // Not needed for class method hooks
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Hook LevelDB class methods directly
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook LevelDB open method to handle lock issues
    @ProxyMethod("open")
    public static class Open extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("lock") && message.contains("Try again")) {
                    Slog.w(TAG, "LevelDB lock error detected, returning null: " + message);
                    // Return null to indicate database is unavailable
                    return null;
                } else if (message != null && message.contains("IO error")) {
                    Slog.w(TAG, "LevelDB IO error detected, returning null: " + message);
                    // Return null for IO errors
                    return null;
                }
                throw e;
            }
        }
    }

    // Hook LevelDB nativeOpen method to handle native lock issues
    @ProxyMethod("nativeOpen")
    public static class NativeOpen extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && (message.contains("lock") || message.contains("IO error"))) {
                    Slog.w(TAG, "LevelDB native lock/IO error detected, returning null: " + message);
                    // Return null to indicate database is unavailable
                    return null;
                }
                throw e;
            }
        }
    }

    // Hook LevelDB get method to handle null database
    @ProxyMethod("get")
    public static class Get extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (who == null) {
                Slog.w(TAG, "LevelDB get called on null database, returning null");
                return null;
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "LevelDB get error, returning null: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook LevelDB put method to handle null database
    @ProxyMethod("put")
    public static class Put extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (who == null) {
                Slog.w(TAG, "LevelDB put called on null database, returning false");
                return false;
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "LevelDB put error, returning false: " + e.getMessage());
                return false;
            }
        }
    }

    // Hook LevelDB delete method to handle null database
    @ProxyMethod("delete")
    public static class Delete extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (who == null) {
                Slog.w(TAG, "LevelDB delete called on null database, returning false");
                return false;
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "LevelDB delete error, returning false: " + e.getMessage());
                return false;
            }
        }
    }
}
