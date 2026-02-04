package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;


public class IXiaomiMiuiServicesProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiMiuiServicesProxy";

    public IXiaomiMiuiServicesProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        
        if (XiaomiDeviceDetector.isXiaomiDevice()) {
            Slog.d(TAG, "IXiaomiMiuiServices proxy initialized for MIUI service crash prevention on " + 
                    XiaomiDeviceDetector.getDeviceModel() + " (MIUI " + XiaomiDeviceDetector.getMiuiVersion() + ")");
        } else {
            Slog.d(TAG, "IXiaomiMiuiServices proxy initialized for MIUI service crash prevention");
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("MiuiCameraCoveredManager")
    public static class MiuiCameraCoveredManager extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (NullPointerException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Attempt to invoke virtual method") && 
                    message.contains("android.provider.MiuiSettings$SettingsCloudData$CloudData.getString")) {
                    Slog.w(TAG, "MIUI Camera Manager NullPointerException on Xiaomi, creating safe fallback: " + message);
                    return createSafeMiuiCameraManagerForXiaomi();
                }
                throw e;
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "MIUI Camera Manager UID mismatch on Xiaomi, creating safe fallback: " + message);
                    return createSafeMiuiCameraManagerForXiaomi();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in MIUI Camera Manager hook: " + e.getMessage());
                
                return createSafeMiuiCameraManagerForXiaomi();
            }
        }
    }

    
    @ProxyMethod("MiuiForceDarkConfig")
    public static class MiuiForceDarkConfig extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in MIUI Force Dark Config hook: " + e.getMessage());
                
                return null;
            }
        }
    }

    
    @ProxyMethod("MiuiMonitorThread")
    public static class MiuiMonitorThread extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in MIUI Monitor Thread hook: " + e.getMessage());
                
                return null;
            }
        }
    }

    
    @ProxyMethod("MiuiDownscaleImpl")
    public static class MiuiDownscaleImpl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in MIUI Downscale Implementation hook: " + e.getMessage());
                
                return null;
            }
        }
    }

    
    @ProxyMethod("ForceDarkHelperStubImpl")
    public static class ForceDarkHelperStubImpl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in Force Dark Helper Stub Implementation hook: " + e.getMessage());
                
                return null;
            }
        }
    }

    
    @ProxyMethod("HandWritingStubImpl")
    public static class HandWritingStubImpl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in Hand Writing Stub Implementation hook: " + e.getMessage());
                
                return null;
            }
        }
    }

    
    @ProxyMethod("OplusScrollToTopManager")
    public static class OplusScrollToTopManager extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in Oplus Scroll To Top Manager hook: " + e.getMessage());
                
                return null;
            }
        }
    }

    
    @ProxyMethod("OplusOverScrollerExtImpl")
    public static class OplusOverScrollerExtImpl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in Oplus Over Scroller Extension Implementation hook: " + e.getMessage());
                
                return null;
            }
        }
    }

    
    private static Object createSafeMiuiCameraManagerForXiaomi() {
        try {
            
            Class<?> miuiCameraManagerClass = Class.forName("android.provider.MiuiSettings$SettingsCloudData$CloudData");
            
            
            Object miuiCameraManager = null;
            
            try {
                
                java.lang.reflect.Constructor<?> constructor = miuiCameraManagerClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                miuiCameraManager = constructor.newInstance();
                Slog.d(TAG, "Created safe MiuiCameraManager for Xiaomi using default constructor");
            } catch (Exception e) {
                Slog.w(TAG, "Could not create safe MiuiCameraManager for Xiaomi: " + e.getMessage());
                return null;
            }
            
            return miuiCameraManager;
        } catch (Exception e) {
            Slog.w(TAG, "Error creating safe MiuiCameraManager for Xiaomi: " + e.getMessage());
            return null;
        }
    }
}
