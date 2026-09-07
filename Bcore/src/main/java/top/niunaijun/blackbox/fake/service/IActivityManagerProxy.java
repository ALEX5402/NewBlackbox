package top.niunaijun.blackbox.fake.service;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;
import java.util.ArrayList;

import black.android.app.BRActivityManagerNative;
import black.android.app.BRActivityManagerOreo;
import black.android.app.BRIActivityManagerContentProviderHolder;
import black.android.content.BRContentProviderHolderOreo;
import black.android.content.BRContentProviderNative;
import black.android.os.BRServiceManager;
import black.android.util.BRSingleton;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.entity.am.RunningAppProcessInfo;
import top.niunaijun.blackbox.entity.am.RunningServiceInfo;
import top.niunaijun.blackbox.fake.delegate.ContentProviderDelegate;
import top.niunaijun.blackbox.fake.delegate.InnerReceiverDelegate;
import top.niunaijun.blackbox.fake.delegate.ServiceConnectionDelegate;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.hook.ProxyMethods;
import top.niunaijun.blackbox.fake.hook.ScanClass;
import top.niunaijun.blackbox.fake.service.base.UidMethodProxy;
import top.niunaijun.blackbox.fake.service.context.providers.ContentProviderStub;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.ParceledListSliceCompat;


@ScanClass(ActivityManagerCommonProxy.class)
public class IActivityManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IActivityManagerProxy";

    public IActivityManagerProxy() {
        super(BRServiceManager.get().getService(Context.ACTIVITY_SERVICE));
    }

    @Override
    protected Object getWho() {
        if (BuildCompat.isOreo()) {
            Object service = BRActivityManagerOreo.get().getService();
            if (service != null) {
                return service;
            }
        }
        return BRActivityManagerNative.get().getDefault();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.ACTIVITY_SERVICE);
        Object singleton;
        if (BuildCompat.isOreo()) {
            singleton = BRActivityManagerOreo.get().IActivityManagerSingleton();
        } else {
            singleton = BRActivityManagerNative.get().gDefault();
        }
        if (singleton != null) {
            BRSingleton.get(singleton)._set_mInstance(proxyInvocation);
        }
        Slog.d(TAG, "IActivityManagerProxy injected");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new UidMethodProxy("handleIncomingUser", 0));
    }

    @ProxyMethods({"getContentProvider", "getContentProviderExternal"})
    public static class GetContentProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return handleGetContentProvider(who, method, args);
        }
    }

    @ProxyMethods({"checkPermission", "checkUidPermission", "checkComponentPermission", "checkUriPermission"})
    public static class CheckPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof String) {
                String permission = (String) args[0];
                if (isGoogleOrPrivilegedPermission(permission)) {
                    return PackageManager.PERMISSION_GRANTED;
                }
            }
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Integer && ((Integer) args[i]) == BActivityThread.getBUid()) {
                        args[i] = BlackBoxCore.getHostUid();
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("startService")
    public static class StartService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String resolvedType = firstResolvedType(args);
            if (intent == null) {
                return method.invoke(who, args);
            }
            ResolveInfo info = BlackBoxCore.getBPackageManager().resolveService(
                    intent, 0, resolvedType, BActivityThread.getUserId());
            if (info == null || info.serviceInfo == null) {
                MethodParameterUtils.replaceFirstAppPkg(args);
                return method.invoke(who, args);
            }
            BlackBoxCore.getBActivityManager().startService(intent, resolvedType, false, BActivityThread.getUserId());
            if (intent.getComponent() != null) {
                return intent.getComponent();
            }
            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
        }
    }

    @ProxyMethod("stopService")
    public static class StopService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String resolvedType = firstResolvedType(args);
            if (intent != null) {
                int result = BlackBoxCore.getBActivityManager().stopService(intent, resolvedType, BActivityThread.getUserId());
                if (result >= 0) {
                    return result;
                }
            }
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethods({"bindService", "bindServiceInstance"})
    public static class BindService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            IServiceConnection conn = MethodParameterUtils.getFirstParam(args, IServiceConnection.class);
            String resolvedType = firstResolvedType(args);
            if (intent == null) {
                return method.invoke(who, args);
            }
            ResolveInfo info = BlackBoxCore.getBPackageManager().resolveService(
                    intent, 0, resolvedType, BActivityThread.getUserId());
            if (info == null) {
                MethodParameterUtils.replaceFirstAppPkg(args);
                return method.invoke(who, args);
            }
            Intent proxyIntent = BlackBoxCore.getBActivityManager().bindService(
                    intent, conn != null ? conn.asBinder() : null, resolvedType, BActivityThread.getUserId());
            if (conn != null) {
                IServiceConnection proxyConn = ServiceConnectionDelegate.createProxy(conn, intent);
                replaceFirstInstance(args, IServiceConnection.class, proxyConn);
            }
            if (proxyIntent != null) {
                replaceFirstInstance(args, Intent.class, proxyIntent);
            }
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("unbindService")
    public static class UnbindService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            IServiceConnection conn = MethodParameterUtils.getFirstParam(args, IServiceConnection.class);
            if (conn != null) {
                BlackBoxCore.getBActivityManager().unbindService(conn.asBinder(), BActivityThread.getUserId());
                IServiceConnection delegate = ServiceConnectionDelegate.getDelegate(conn.asBinder());
                if (delegate != null) {
                    replaceFirstInstance(args, IServiceConnection.class, delegate);
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("peekService")
    public static class PeekService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String resolvedType = firstResolvedType(args);
            if (intent != null) {
                IBinder binder = BlackBoxCore.getBActivityManager().peekService(intent, resolvedType, BActivityThread.getUserId());
                if (binder != null) {
                    return binder;
                }
            }
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethods({"registerReceiver", "registerReceiverWithFeature", "registerReceiverForAllUsers"})
    public static class RegisterReceiver extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof IIntentReceiver) {
                        args[i] = InnerReceiverDelegate.createProxy((IIntentReceiver) args[i]);
                        break;
                    }
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethods({"broadcastIntent", "broadcastIntentWithFeature"})
    public static class BroadcastIntent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            if (intent == null) {
                return method.invoke(who, args);
            }
            String resolvedType = firstResolvedType(args);
            Intent proxy = BlackBoxCore.getBActivityManager().sendBroadcast(intent, resolvedType, BActivityThread.getUserId());
            if (proxy != null) {
                ProxyBroadcastRecord.saveStub(proxy, intent, BActivityThread.getUserId());
                proxy.setComponent(new ComponentName(BlackBoxCore.getHostPkg(), ProxyManifest.getProxyReceiver()));
                replaceFirstInstance(args, Intent.class, proxy);
            }
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getRunningAppProcesses")
    public static class GetRunningAppProcesses extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            RunningAppProcessInfo info = BlackBoxCore.getBActivityManager()
                    .getRunningAppProcesses(BActivityThread.getAppPackageName(), BActivityThread.getUserId());
            if (info != null && info.mAppProcessInfoList != null) {
                return info.mAppProcessInfoList;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getServices")
    public static class GetServices extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            RunningServiceInfo info = BlackBoxCore.getBActivityManager()
                    .getRunningServices(BActivityThread.getAppPackageName(), BActivityThread.getUserId());
            if (info != null && info.mRunningServiceInfoList != null) {
                return info.mRunningServiceInfoList;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getIntentSender")
    public static class GetIntentSender extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            Object result = method.invoke(who, args);
            if (result instanceof IInterface) {
                BlackBoxCore.getBActivityManager().getIntentSender(
                        ((IInterface) result).asBinder(),
                        BActivityThread.getAppPackageName(),
                        BActivityThread.getBUid());
            }
            return result;
        }
    }

    @ProxyMethod("getPackageForIntentSender")
    public static class GetPackageForIntentSender extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof IInterface) {
                String pkg = BlackBoxCore.getBActivityManager()
                        .getPackageForIntentSender(((IInterface) args[0]).asBinder());
                if (pkg != null) {
                    return pkg;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getUidForIntentSender")
    public static class GetUidForIntentSender extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof IInterface) {
                int uid = BlackBoxCore.getBActivityManager()
                        .getUidForIntentSender(((IInterface) args[0]).asBinder());
                if (uid > 0) {
                    return uid;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getHistoricalProcessExitReasons")
    public static class GetHistoricalProcessExitReasons extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (ParceledListSliceCompat.isReturnParceledListSlice(method)) {
                return ParceledListSliceCompat.create(new ArrayList<>());
            }
            return new ArrayList<>();
        }
    }

    private static Object handleGetContentProvider(Object who, Method method, Object[] args) throws Throwable {
        int authIndex = getProviderAuthIndex(method, args);
        if (args == null || authIndex < 0 || authIndex >= args.length || !(args[authIndex] instanceof String)) {
            return method.invoke(who, args);
        }
        String auth = (String) args[authIndex];
        if (ProxyManifest.isProxy(auth)) {
            return method.invoke(who, args);
        }

        replaceCallingPackage(method, args);

        ProviderInfo providerInfo = BlackBoxCore.getBPackageManager()
                .resolveContentProvider(auth, PackageManager.GET_META_DATA, BActivityThread.getUserId());
        if (providerInfo != null) {
            Object holder = obtainHolder(who, method, args, authIndex);
            if (holder == null) {
                return null;
            }
            IBinder binder = BlackBoxCore.getBActivityManager().acquireContentProviderClient(providerInfo);
            IInterface provider = binder != null ? BRContentProviderNative.get().asInterface(binder) : null;
            if (provider == null) {
                ContentProviderDelegate.update(holder, auth);
                return holder;
            }
            String wrapPkg = providerInfo.packageName;
            if (wrapPkg == null || wrapPkg.isEmpty()) {
                wrapPkg = BActivityThread.getAppPackageName();
            }
            if (wrapPkg == null || wrapPkg.isEmpty()) {
                wrapPkg = BlackBoxCore.getHostPkg();
            }
            applyHolder(holder, providerInfo, new ContentProviderStub().wrapper(provider, wrapPkg));
            return holder;
        }

        Object holder;
        try {
            holder = method.invoke(who, args);
        } catch (Throwable t) {
            Throwable cause = unwrap(t);
            if (isProviderPermissionError(cause) || ContentProviderDelegate.isGoogleProviderAuthority(auth)) {
                Slog.w(TAG, "getContentProvider denied for " + auth + ", using host stub: " + cause.getMessage());
                holder = obtainHolder(who, method, args, authIndex);
                if (holder == null) {
                    return null;
                }
                ContentProviderDelegate.update(holder, auth);
                return holder;
            }
            throw cause;
        }
        if (holder != null) {
            ContentProviderDelegate.update(holder, auth);
        }
        return holder;
    }

    private static void replaceCallingPackage(Method method, Object[] args) {
        if (!BuildCompat.isQ() || args == null || args.length < 2) {
            return;
        }
        if (method.getName().contains("External")) {
            return;
        }
        if (args[1] instanceof String) {
            args[1] = BlackBoxCore.getHostPkg();
        }
    }

    private static Object obtainHolder(Object who, Method method, Object[] args, int authIndex) throws Throwable {
        Object saved = args[authIndex];
        try {
            int pid = BActivityThread.getAppPid();
            if (pid < 0) {
                pid = 0;
            }
            args[authIndex] = ProxyManifest.getProxyAuthorities(pid);
            replaceCallingPackage(method, args);
            return method.invoke(who, args);
        } catch (Throwable t) {
            Slog.w(TAG, "Failed to obtain provider holder: " + t.getMessage());
            return null;
        } finally {
            args[authIndex] = saved;
        }
    }

    private static void applyHolder(Object holder, ProviderInfo info, IInterface provider) {
        if (BuildCompat.isOreo()) {
            if (info != null) {
                BRContentProviderHolderOreo.get(holder)._set_info(info);
            }
            BRContentProviderHolderOreo.get(holder)._set_provider(provider);
            BRContentProviderHolderOreo.get(holder)._set_noReleaseNeeded(true);
        } else {
            if (info != null) {
                BRIActivityManagerContentProviderHolder.get(holder)._set_info(info);
            }
            BRIActivityManagerContentProviderHolder.get(holder)._set_provider(provider);
            BRIActivityManagerContentProviderHolder.get(holder)._set_noReleaseNeeded(true);
        }
    }

    private static int getProviderAuthIndex(Method method, Object[] args) {
        if (method.getName().contains("External")) {
            if (args != null && args.length > 1 && args[0] instanceof String && args[1] instanceof String) {
                return 1;
            }
            return 0;
        }
        return BuildCompat.isQ() ? 2 : 1;
    }

    private static boolean isGoogleOrPrivilegedPermission(String permission) {
        if (permission == null) {
            return false;
        }
        return permission.startsWith("com.google.android.gms.permission.")
                || permission.startsWith("com.google.android.providers.gsf.permission.")
                || permission.equals("com.google.android.providers.settings.permission.WRITE_GSETTINGS")
                || permission.equals("com.google.android.providers.settings.permission.READ_GSETTINGS")
                || permission.equals("com.google.android.gms.permission.INTERNAL_BROADCAST")
                || permission.startsWith("com.google.android.setupwizard.")
                || permission.startsWith("com.android.vending.")
                || permission.equals("android.permission.WRITE_SECURE_SETTINGS")
                || permission.equals("android.permission.WRITE_SETTINGS")
                || permission.equals("android.permission.GET_ACCOUNTS")
                || permission.equals("android.permission.MANAGE_ACCOUNTS")
                || permission.equals("android.permission.USE_CREDENTIALS")
                || permission.equals("android.permission.AUTHENTICATE_ACCOUNTS")
                || permission.equals("android.permission.ACCOUNT_MANAGER");
    }

    private static boolean isProviderPermissionError(Throwable error) {
        if (error == null) {
            return false;
        }
        if (error instanceof SecurityException) {
            return true;
        }
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("permission denial")
                || lower.contains("internal_broadcast")
                || lower.contains("cannot write to google settings")
                || lower.contains("google settings table");
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause != cause.getCause()
                && (cause instanceof RuntimeException || cause instanceof java.lang.reflect.InvocationTargetException
                || cause instanceof java.lang.reflect.UndeclaredThrowableException)) {
            cause = cause.getCause();
        }
        return cause != null ? cause : error;
    }

    private static String firstResolvedType(Object[] args) {
        if (args == null) {
            return null;
        }
        boolean skippedPkg = false;
        for (Object arg : args) {
            if (arg instanceof String) {
                if (!skippedPkg) {
                    skippedPkg = true;
                    continue;
                }
                return (String) arg;
            }
        }
        return null;
    }

    private static void replaceFirstInstance(Object[] args, Class<?> type, Object value) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (type.isInstance(args[i])) {
                args[i] = value;
                return;
            }
        }
    }
}
