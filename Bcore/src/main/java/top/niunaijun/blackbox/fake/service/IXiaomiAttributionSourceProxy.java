package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;


public class IXiaomiAttributionSourceProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiAttributionSourceProxy";

    public IXiaomiAttributionSourceProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        
        if (XiaomiDeviceDetector.isXiaomiDevice()) {
            Slog.d(TAG, "IXiaomiAttributionSource proxy initialized for MIUI UID mismatch prevention on " + 
                    XiaomiDeviceDetector.getDeviceModel() + " (MIUI " + XiaomiDeviceDetector.getMiuiVersion() + ")");
        } else {
            Slog.d(TAG, "IXiaomiAttributionSource proxy initialized for MIUI UID mismatch prevention");
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("AttributionSource")
    public static class AttributionSourceConstructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in AttributionSource constructor, creating safe fallback: " + message);
                    return AttributionSourceUtils.createSafeAttributionSource();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi AttributionSource constructor hook: " + e.getMessage());
                
                return AttributionSourceUtils.createSafeAttributionSource();
            }
        }
    }

    
    @ProxyMethod("enforceCallingUid")
    public static class EnforceCallingUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "Xiaomi enforceCallingUid bypassed to prevent UID mismatch crashes");
            return null; 
        }
    }

    
    @ProxyMethod("enforceCallingUidAndPid")
    public static class EnforceCallingUidAndPid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "Xiaomi enforceCallingUidAndPid bypassed to prevent UID mismatch crashes");
            return null; 
        }
    }

    
    @ProxyMethod("fromParcel")
    public static class FromParcel extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                Object result = method.invoke(who, args);
                
                
                if (result != null) {
                    AttributionSourceUtils.fixAttributionSourceUid(result);
                }
                
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi fromParcel hook: " + e.getMessage());
                
                return AttributionSourceUtils.createSafeAttributionSource();
            }
        }
    }
}
