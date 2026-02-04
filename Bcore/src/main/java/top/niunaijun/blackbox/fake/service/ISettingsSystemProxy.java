package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;


public class ISettingsSystemProxy extends ClassInvocationStub {
    public static final String TAG = "ISettingsSystemProxy";

    public ISettingsSystemProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        
        Slog.d(TAG, "ISettingsSystem proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getStringForUser, returning safe default: " + message);
                    return ""; 
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in getStringForUser hook: " + e.getMessage());
                
                return "";
            }
        }
    }

    
    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getString, returning safe default: " + message);
                    return ""; 
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in getString hook: " + e.getMessage());
                
                return "";
            }
        }
    }

    
    @ProxyMethod("putString")
    public static class PutString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in putString, returning false: " + message);
                    return false; 
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in putString hook: " + e.getMessage());
                
                return false;
            }
        }
    }
}
