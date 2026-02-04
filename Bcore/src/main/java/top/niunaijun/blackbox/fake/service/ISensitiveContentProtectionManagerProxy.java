package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class ISensitiveContentProtectionManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ISensitiveContentProtection";

    public ISensitiveContentProtectionManagerProxy() {
        super(BRServiceManager.get().getService("sensitive_content_protection_service"));
    }

    @Override
    protected Object getWho() {
        try {
            IBinder binder = BRServiceManager.get().getService("sensitive_content_protection_service");
            if (binder == null) return null;
            Class<?> stubClass = Class.forName("android.view.ISensitiveContentProtectionManager$Stub");
            Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
            return asInterface.invoke(null, binder);
        } catch (Exception e) {
            Slog.d(TAG, "getWho error: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (BRServiceManager.get().getService("sensitive_content_protection_service") != null) {
            replaceSystemService("sensitive_content_protection_service");
            Slog.d(TAG, "Hooked SensitiveContentProtectionManagerService");
        } else {
            Slog.d(TAG, "Skipping SensitiveContentProtectionManagerService hook (service not found)");
        }
    }

    @Override
    public boolean isBadEnv() {
        IBinder binder = BRServiceManager.get().getService("sensitive_content_protection_service");
        return binder != null && binder != this;
    }

    @ProxyMethod("setSensitiveContentProtection")
    public static class SetSensitiveContentProtection extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof String) {
                        String pkg = (String) args[i];
                        if (pkg != null && !pkg.equals(BlackBoxCore.getHostPkg())) {
                            Slog.d(TAG, "Fixing package name in setSensitiveContentProtection: " + pkg + " -> " + BlackBoxCore.getHostPkg());
                            args[i] = BlackBoxCore.getHostPkg();
                        }
                    }
                }
            }
            return method.invoke(who, args);
        }
    }
}
