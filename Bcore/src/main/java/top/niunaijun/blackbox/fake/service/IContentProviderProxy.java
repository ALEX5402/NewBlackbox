package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;

/**
 * IContentProvider Proxy to handle AttributionSource UID issues on Android 12+
 * This prevents crashes related to ContentProvider UID enforcement
 */
public class IContentProviderProxy extends ClassInvocationStub {
    public static final String TAG = "IContentProviderProxy";

    public IContentProviderProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        // Return null since this is a global hook, not a specific service
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        // This proxy handles ContentProvider calls globally
        Slog.d(TAG, "IContentProvider proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook for query method to fix AttributionSource issues
    @ProxyMethod("query")
    public static class Query extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in query hook: " + e.getMessage());
                // Return null cursor to prevent crashes
                return null;
            }
        }
    }

    // Hook for insert method to fix AttributionSource issues
    @ProxyMethod("insert")
    public static class Insert extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in insert hook: " + e.getMessage());
                // Return null URI to prevent crashes
                return null;
            }
        }
    }

    // Hook for update method to fix AttributionSource issues
    @ProxyMethod("update")
    public static class Update extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in update hook: " + e.getMessage());
                // Return 0 to prevent crashes
                return 0;
            }
        }
    }

    // Hook for delete method to fix AttributionSource issues
    @ProxyMethod("delete")
    public static class Delete extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in delete hook: " + e.getMessage());
                // Return 0 to prevent crashes
                return 0;
            }
        }
    }

    // Hook for call method to fix AttributionSource issues (used by Settings.System, etc.)
    @ProxyMethod("call")
    public static class Call extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in call method, returning safe default: " + message);
                    return null; // Return null to prevent crashes
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in call hook: " + e.getMessage());
                // Return null to prevent crashes
                return null;
            }
        }
    }

    // Hook for getString method to fix UID issues
    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getString, returning safe default: " + message);
                    return ""; // Return empty string to prevent crashes
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in getString hook: " + e.getMessage());
                // Return empty string to prevent crashes
                return "";
            }
        }
    }

    // Hook for getStringForUser method to fix UID issues
    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getStringForUser, returning safe default: " + message);
                    return ""; // Return empty string to prevent crashes
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in getStringForUser hook: " + e.getMessage());
                // Return empty string to prevent crashes
                return "";
            }
        }
    }
}
