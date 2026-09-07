package top.niunaijun.blackbox.fake.service;

import android.content.pm.PackageManager;

import java.lang.reflect.Method;

import black.android.app.BRActivityThread;
import black.android.app.BRContextImpl;
import black.android.os.BRServiceManager;
import black.android.permission.BRIPermissionManagerStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.service.base.PkgMethodProxy;
import top.niunaijun.blackbox.fake.service.base.ValueMethodProxy;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


public class IPermissionManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IPermissionManagerProxy";

    private static final String P = "permissionmgr";

    public IPermissionManagerProxy() {
        super(BRServiceManager.get().getService(P));
    }

    @Override
    protected Object getWho() {
        return BRIPermissionManagerStub.get().asInterface(BRServiceManager.get().getService(P));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("permissionmgr");
        BRActivityThread.getWithException()._set_sPermissionManager(proxyInvocation);
        
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new ValueMethodProxy("addPermissionAsync", true));
        addMethodHook(new ValueMethodProxy("addPermission", true));
        addMethodHook(new ValueMethodProxy("performDexOpt", true));
        addMethodHook(new ValueMethodProxy("performDexOptIfNeeded", false));
        addMethodHook(new ValueMethodProxy("performDexOptSecondary", true));
        addMethodHook(new ValueMethodProxy("addOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("removeOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("checkDeviceIdentifierAccess", false));
        addMethodHook(new PkgMethodProxy("shouldShowRequestPermissionRationale"));
        addMethodHook(new CheckPermission());
        addMethodHook(new CheckUidPermission());
        if (BuildCompat.isOreo()) {
            addMethodHook(new ValueMethodProxy("notifyDexLoad", 0));
            addMethodHook(new ValueMethodProxy("notifyPackageUse", 0));
            addMethodHook(new ValueMethodProxy("setInstantAppCookie", false));
            addMethodHook(new ValueMethodProxy("isInstantApp", false));
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("checkPermission")
    public static class CheckPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (isGoogleInternalPermission(args)) {
                return PackageManager.PERMISSION_GRANTED;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("checkUidPermission")
    public static class CheckUidPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (isGoogleInternalPermission(args)) {
                return PackageManager.PERMISSION_GRANTED;
            }
            return method.invoke(who, args);
        }
    }

    private static boolean isGoogleInternalPermission(Object[] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof String)) {
            return false;
        }
        String permission = (String) args[0];
        return permission.startsWith("com.google.android.gms.permission.")
                || permission.startsWith("com.google.android.providers.gsf.permission.")
                || permission.equals("com.google.android.providers.settings.permission.WRITE_GSETTINGS")
                || permission.equals("com.google.android.providers.settings.permission.READ_GSETTINGS")
                || permission.equals("com.google.android.gms.permission.INTERNAL_BROADCAST")
                || permission.startsWith("com.google.android.setupwizard.")
                || permission.startsWith("com.android.vending.")
                || permission.equals("android.permission.WRITE_SECURE_SETTINGS")
                || permission.equals("android.permission.WRITE_SETTINGS")
                || permission.equals("android.permission.ACCOUNT_MANAGER")
                || permission.equals("android.permission.GET_ACCOUNTS")
                || permission.equals("android.permission.MANAGE_ACCOUNTS")
                || permission.equals("android.permission.USE_CREDENTIALS")
                || permission.equals("android.permission.AUTHENTICATE_ACCOUNTS")
                || permission.equals("android.permission.READ_PRIVILEGED_PHONE_STATE")
                || permission.equals("android.permission.MODIFY_PHONE_STATE")
                || permission.equals("android.permission.READ_PHONE_STATE")
                || permission.equals("android.permission.READ_PHONE_NUMBERS");
    }

}
