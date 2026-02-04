package top.niunaijun.blackbox.fake.service;

import android.os.Build;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


public class IMiuiSecurityManagerProxy extends BinderInvocationStub {
    public static final String TAG = "MiuiSecurityManagerProxy";
    private static final String SERVICE_NAME = "miui.security.SecurityManager";

    public IMiuiSecurityManagerProxy() {
        super(BRServiceManager.get().getService(SERVICE_NAME));
    }

    @Override
    protected Object getWho() {
        Object service = BRServiceManager.get().getService(SERVICE_NAME);
        if (service == null) {
            return null;
        }
        try {
            
            Class<?> stubClass = Class.forName("miui.security.ISecurityManager$Stub");
            Method asInterface = stubClass.getMethod("asInterface", android.os.IBinder.class);
            return asInterface.invoke(null, service);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get ISecurityManager interface: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(SERVICE_NAME);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    public boolean isEnable() {
        
        return BuildCompat.isMIUI() || 
               Build.MANUFACTURER.toLowerCase().contains("xiaomi") ||
               Build.BRAND.toLowerCase().contains("xiaomi") ||
               Build.DISPLAY.toLowerCase().contains("hyperos");
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new SetAppPrivacyStatus());
        addMethodHook(new GetAppPrivacyStatus());
        addMethodHook(new SetAppPermissionControlOpen());
        addMethodHook(new IsAppPermissionControlOpen());
        addMethodHook(new SetWakeUpTime());
        addMethodHook(new GetWakeUpTime());
        addMethodHook(new SetTrackWakeUp());
        addMethodHook(new IsTrackWakeUp());
        addMethodHook(new SetGameMode());
        addMethodHook(new IsGameMode());
        addMethodHook(new PushNewNotification());
        addMethodHook(new IsAllowStartActivity());
        addMethodHook(new SetAppRunningControlEnabled());
        addMethodHook(new GetAppRunningControlEnabled());
        addMethodHook(new CheckAccessControl());
        addMethodHook(new SaveIcon());
        addMethodHook(new GetIcon());
        addMethodHook(new IsValidDevice());
        addMethodHook(new ActuallyCheckPermission());
        addMethodHook(new CheckSmsBlocked());
        addMethodHook(new SetCurrentUserId());
        addMethodHook(new GetCurrentUserId());
    }

    @ProxyMethod("setAppPrivacyStatus")
    public static class SetAppPrivacyStatus extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            String packageName = args.length > 0 ? (String) args[0] : "unknown";
            Slog.d(TAG, "SetAppPrivacyStatus: Bypassing privacy status for " + packageName);
            return true; 
        }
    }

    @ProxyMethod("getAppPrivacyStatus")
    public static class GetAppPrivacyStatus extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            String packageName = args.length > 0 ? (String) args[0] : "unknown";
            Slog.d(TAG, "GetAppPrivacyStatus: Returning default status for " + packageName);
            return 0; 
        }
    }

    @ProxyMethod("setAppPermissionControlOpen")
    public static class SetAppPermissionControlOpen extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SetAppPermissionControlOpen: Bypassing permission control setting");
            return true;
        }
    }

    @ProxyMethod("isAppPermissionControlOpen")
    public static class IsAppPermissionControlOpen extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "IsAppPermissionControlOpen: Returning false (disabled)");
            return false; 
        }
    }

    @ProxyMethod("setWakeUpTime")
    public static class SetWakeUpTime extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SetWakeUpTime: Bypassing wake up time setting");
            return true;
        }
    }

    @ProxyMethod("getWakeUpTime")
    public static class GetWakeUpTime extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GetWakeUpTime: Returning default wake up time");
            return 0L; 
        }
    }

    @ProxyMethod("setTrackWakeUp")
    public static class SetTrackWakeUp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SetTrackWakeUp: Bypassing track wake up setting");
            return true;
        }
    }

    @ProxyMethod("isTrackWakeUp")
    public static class IsTrackWakeUp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "IsTrackWakeUp: Returning false (disabled)");
            return false;
        }
    }

    @ProxyMethod("setGameMode")
    public static class SetGameMode extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SetGameMode: Bypassing game mode setting");
            return true;
        }
    }

    @ProxyMethod("isGameMode")
    public static class IsGameMode extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "IsGameMode: Returning false (not in game mode)");
            return false;
        }
    }

    @ProxyMethod("pushNewNotification")
    public static class PushNewNotification extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "PushNewNotification: Bypassing notification push");
            return true;
        }
    }

    @ProxyMethod("isAllowStartActivity")
    public static class IsAllowStartActivity extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "IsAllowStartActivity: Allowing activity start");
            return true; 
        }
    }

    @ProxyMethod("setAppRunningControlEnabled")
    public static class SetAppRunningControlEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SetAppRunningControlEnabled: Bypassing running control setting");
            return true;
        }
    }

    @ProxyMethod("getAppRunningControlEnabled")
    public static class GetAppRunningControlEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GetAppRunningControlEnabled: Returning false (disabled)");
            return false; 
        }
    }

    @ProxyMethod("checkAccessControl")
    public static class CheckAccessControl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "CheckAccessControl: Allowing access");
            return true; 
        }
    }

    @ProxyMethod("saveIcon")
    public static class SaveIcon extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SaveIcon: Bypassing icon save");
            return true;
        }
    }

    @ProxyMethod("getIcon")
    public static class GetIcon extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GetIcon: Returning null icon");
            return null; 
        }
    }

    @ProxyMethod("isValidDevice")
    public static class IsValidDevice extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "IsValidDevice: Returning true (valid device)");
            return true; 
        }
    }

    @ProxyMethod("actuallyCheckPermission")
    public static class ActuallyCheckPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "ActuallyCheckPermission: Granting permission");
            return 0; 
        }
    }

    @ProxyMethod("checkSmsBlocked")
    public static class CheckSmsBlocked extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "CheckSmsBlocked: Returning false (not blocked)");
            return false; 
        }
    }

    @ProxyMethod("setCurrentUserId")
    public static class SetCurrentUserId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SetCurrentUserId: Bypassing user ID setting");
            return true;
        }
    }

    @ProxyMethod("getCurrentUserId")
    public static class GetCurrentUserId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GetCurrentUserId: Returning current user ID");
            return 0; 
        }
    }
}
