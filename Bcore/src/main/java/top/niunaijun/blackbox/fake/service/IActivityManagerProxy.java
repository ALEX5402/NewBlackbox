package top.niunaijun.blackbox.fake.service;

import android.Manifest;
import android.app.ActivityManager;
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
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

import black.android.app.BRActivityManagerNative;
import black.android.app.BRActivityManagerOreo;
import black.android.app.BRLoadedApkReceiverDispatcher;
import black.android.app.BRLoadedApkReceiverDispatcherInnerReceiver;
import black.android.app.BRLoadedApkServiceDispatcher;
import black.android.app.BRLoadedApkServiceDispatcherInnerConnection;
import black.android.content.BRContentProviderNative;
import black.android.content.pm.BRUserInfo;
import black.android.util.BRSingleton;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.env.AppSystemEnv;
import top.niunaijun.blackbox.entity.AppConfig;
import top.niunaijun.blackbox.entity.am.RunningAppProcessInfo;
import top.niunaijun.blackbox.entity.am.RunningServiceInfo;
import top.niunaijun.blackbox.fake.delegate.ContentProviderDelegate;
import top.niunaijun.blackbox.fake.delegate.InnerReceiverDelegate;
import top.niunaijun.blackbox.fake.delegate.ServiceConnectionDelegate;
import top.niunaijun.blackbox.fake.frameworks.BActivityManager;
import top.niunaijun.blackbox.fake.frameworks.BPackageManager;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.hook.ScanClass;
import top.niunaijun.blackbox.fake.service.base.PkgMethodProxy;
import top.niunaijun.blackbox.fake.service.context.providers.ContentProviderStub;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;
import top.niunaijun.blackbox.proxy.record.ProxyPendingRecord;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.compat.ActivityManagerCompat;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.ParceledListSliceCompat;
import top.niunaijun.blackbox.utils.compat.TaskDescriptionCompat;
import top.niunaijun.blackbox.utils.Slog;

import static android.content.Context.RECEIVER_EXPORTED;
import static android.content.Context.RECEIVER_NOT_EXPORTED;
import static android.content.pm.PackageManager.GET_META_DATA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * updated by alex5402 on 3/30/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
@ScanClass(ActivityManagerCommonProxy.class)
public class IActivityManagerProxy extends ClassInvocationStub {
    public static final String TAG = "ActivityManagerStub";

    @Override
    protected Object getWho() {
        Object iActivityManager = null;
        if (BuildCompat.isOreo()) {
            iActivityManager = BRActivityManagerOreo.get().IActivityManagerSingleton();
        } else if (BuildCompat.isL()) {
            iActivityManager = BRActivityManagerNative.get().gDefault();
        }
        return BRSingleton.get(iActivityManager).get();
    }

    @Override
    protected void inject(Object base, Object proxy) {
        Object iActivityManager = null;
        if (BuildCompat.isOreo()) {
            iActivityManager = BRActivityManagerOreo.get().IActivityManagerSingleton();
        } else if (BuildCompat.isL()) {
            iActivityManager = BRActivityManagerNative.get().gDefault();
        }
        BRSingleton.get(iActivityManager)._set_mInstance(proxy);
    }

    @Override
    public boolean isBadEnv() {
        return getProxyInvocation() != getWho();
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new PkgMethodProxy("getAppStartMode"));
        addMethodHook(new PkgMethodProxy("setAppLockedVerifying"));
        addMethodHook(new PkgMethodProxy("reportJunkFromApp"));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            // Handle SecurityExceptions gracefully for all ActivityManager calls
            String methodName = method.getName();
            Slog.w(TAG, "ActivityManager invoke: SecurityException in " + methodName + ", returning safe default", e);
            
            // Return appropriate default values based on method
            if (methodName.startsWith("set") || methodName.startsWith("update")) {
                return null; // For setter methods, return null (success)
            } else if (methodName.startsWith("get") || methodName.startsWith("query")) {
                return null; // For getter methods, return null (empty result)
            } else if (methodName.startsWith("start") || methodName.startsWith("bind")) {
                return false; // For start/bind methods, return false (not started)
            } else if (methodName.startsWith("stop") || methodName.startsWith("unbind")) {
                return true; // For stop/unbind methods, return true (stopped)
            } else {
                return null; // Default fallback
            }
        } catch (Exception e) {
            Slog.e(TAG, "ActivityManager invoke: Unexpected error in " + method.getName(), e);
            return super.invoke(proxy, method, args);
        }
    }

    @ProxyMethod("getContentProvider")
    public static class GetContentProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int authIndex = getAuthIndex();
            Object auth = args[authIndex];
            Object content = null;

            if (auth instanceof String) {
                if (ProxyManifest.isProxy((String) auth)) {
                    return method.invoke(who, args);
                }

                if (BuildCompat.isQ()) {
                    args[1] = BlackBoxCore.getHostPkg();
                }

                if (auth.equals("settings")
                        || auth.equals("media")
                        || auth.equals("telephony")
                        || ((String) auth).contains("com.google.android.gms")
                        || ((String) auth).contains("com.android.vending")
                        || ((String) auth).contains("com.google.android.gsf")
                        || auth.equals("com.google.android.gms.chimera")
                        || auth.equals("com.huawei.android.launcher.settings")
                        || auth.equals("com.hihonor.android.launcher.settings")) {
                    content = method.invoke(who, args);
                    ContentProviderDelegate.update(content, (String) auth);
                    return content;
                } else {
                    // Log.d(TAG, "hook getContentProvider: " + auth);

                    ProviderInfo providerInfo = BlackBoxCore.getBPackageManager()
                            .resolveContentProvider(
                                    (String) auth, GET_META_DATA, BActivityThread.getUserId());
                    if (providerInfo == null) {
                        // Fallback removed to avoid using original Google services
                        return null;
                    }

                    // Log.d(TAG, "hook app: " + auth);
                    IBinder providerBinder = null;
                    if (BActivityThread.getAppPid() != -1) {
                        AppConfig appConfig = BlackBoxCore.getBActivityManager()
                                .initProcess(
                                        providerInfo.packageName,
                                        providerInfo.processName,
                                        BActivityThread.getUserId());
                        if (appConfig.bPID != BActivityThread.getAppPid()) {
                            providerBinder = BlackBoxCore.getBActivityManager()
                                    .acquireContentProviderClient(providerInfo);
                        }
                        args[authIndex] = ProxyManifest.getProxyAuthorities(appConfig.bPID);
                        args[getUserIndex()] = BlackBoxCore.getHostUserId();
                    }
                    if (providerBinder == null)
                        return null;

                    content = method.invoke(who, args);
                    Reflector.with(content).field("info").set(providerInfo);
                    Reflector.with(content)
                            .field("provider")
                            .set(
                                    new ContentProviderStub()
                                            .wrapper(
                                                    BRContentProviderNative.get().asInterface(providerBinder),
                                                    providerInfo.packageName));
                }

                return content;
            }
            return method.invoke(who, args);
        }

        protected int getAuthIndex() {
            // 10.0
            if (BuildCompat.isQ()) {
                return 2;
            } else {
                return 1;
            }
        }

        protected int getUserIndex() {
            return getAuthIndex() + 1;
        }
    }

    @ProxyMethod("startService")
    public static class StartService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[1];
            String resolvedType = (String) args[2];
            ResolveInfo resolveInfo = BlackBoxCore.getBPackageManager().resolveService(intent, 0, resolvedType, BActivityThread.getUserId());
            if (resolveInfo == null) {
                return method.invoke(who, args);
            }

            int requireForegroundIndex = getRequireForeground();
            boolean requireForeground = false;
            if (requireForegroundIndex != -1) {
                requireForeground = (boolean) args[requireForegroundIndex];
            }
            return BlackBoxCore.getBActivityManager().startService(intent, resolvedType, requireForeground, BActivityThread.getUserId());
        }

        public int getRequireForeground() {
            if (BuildCompat.isOreo()) {
                return 3;
            }
            return -1;
        }
    }

    @ProxyMethod("stopService")
    public static class StopService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Intent intent = (Intent) args[1];
                String resolvedType = (String) args[2];
                
                                        // Check if the service belongs to the current app
                        if (intent != null && intent.getComponent() != null) {
                            String servicePackage = intent.getComponent().getPackageName();
                            String currentPackage = BActivityThread.getAppPackageName();

                            // If trying to stop a service from a different package, return false instead of crashing
                            if (!servicePackage.equals(currentPackage)) {
                                Slog.w(TAG, "StopService: Attempting to stop service from different package: " +
                                        servicePackage + " (current: " + currentPackage + "), returning false");
                                return false;
                            }
                        }
                
                return BlackBoxCore.getBActivityManager().stopService(intent, resolvedType, BActivityThread.getUserId());
            } catch (SecurityException e) {
                Slog.w(TAG, "StopService: SecurityException caught, returning false", e);
                return false;
            } catch (Exception e) {
                Slog.e(TAG, "StopService: Error stopping service", e);
                return false;
            }
        }
    }

    @ProxyMethod("stopServiceToken")
    public static class StopServiceToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                ComponentName componentName = (ComponentName) args[0];
                IBinder token = (IBinder) args[1];
                
                                        // Check if the service belongs to the current app
                        if (componentName != null) {
                            String servicePackage = componentName.getPackageName();
                            String currentPackage = BActivityThread.getAppPackageName();

                            // If trying to stop a service from a different package, return true instead of crashing
                            if (!servicePackage.equals(currentPackage)) {
                                Slog.w(TAG, "StopServiceToken: Attempting to stop service from different package: " +
                                        servicePackage + " (current: " + currentPackage + "), returning true");
                                return true;
                            }
                        }
                
                BlackBoxCore.getBActivityManager().stopServiceToken(componentName, token, BActivityThread.getUserId());
                return true;
            } catch (SecurityException e) {
                Slog.w(TAG, "StopServiceToken: SecurityException caught, returning true", e);
                return true;
            } catch (Exception e) {
                Slog.e(TAG, "StopServiceToken: Error stopping service token", e);
                return true;
            }
        }
    }

    @ProxyMethod("setActivityLocusContext")
    public static class SetActivityLocusContext extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                                        // Check if the target package matches the current app
                        if (args != null && args.length >= 2) {
                            String targetPackage = (String) args[1];
                            String currentPackage = BActivityThread.getAppPackageName();

                            // If trying to set locus context for a different package, return success instead of crashing
                            if (targetPackage != null && !targetPackage.equals(currentPackage)) {
                                Slog.w(TAG, "SetActivityLocusContext: Attempting to set locus context for different package: " +
                                        targetPackage + " (current: " + currentPackage + "), returning success");
                                return null; // Return null for success
                            }
                        }
                
                // Proceed with original call if package matches
                return method.invoke(who, args);
            } catch (SecurityException e) {
                Slog.w(TAG, "SetActivityLocusContext: SecurityException caught, returning success", e);
                return null; // Return null for success
            } catch (Exception e) {
                Slog.e(TAG, "SetActivityLocusContext: Error setting locus context", e);
                return null; // Return null for success
            }
        }
    }

    public static Object BindServiceCommon(Object who, Method method, Object[] args,int callingPackageIndex) throws Throwable {
        try {
            Intent intent = (Intent) args[2];
            String resolvedType = (String) args[3];
            IServiceConnection connection = (IServiceConnection) args[4];

            // Check for null intent
            if (intent == null) {
                Slog.w(TAG, "BindServiceCommon: Intent is null, proceeding with original call");
                return method.invoke(who, args);
            }

            //int flags = MethodParameterUtils.toInt(args[5]);

            int userId = intent.getIntExtra("_B_|_UserId", -1);
            userId = userId == -1 ? BActivityThread.getUserId() : userId;
            ResolveInfo resolveInfo = BlackBoxCore.getBPackageManager().resolveService(intent, 0, resolvedType, userId);
            if (resolveInfo != null || AppSystemEnv.isOpenPackage(intent.getComponent())) {
                Intent proxyIntent = BlackBoxCore.getBActivityManager().bindService(intent,
                        connection == null ? null : connection.asBinder(),
                        resolvedType,
                        userId);
                if (connection != null) {
                    if (intent.getComponent() == null && resolveInfo != null) {
                        intent.setComponent(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
                    }
                    IServiceConnection proxy = ServiceConnectionDelegate.createProxy(connection, intent);
                    args[4] = proxy;

                    WeakReference<?> weakReference = BRLoadedApkServiceDispatcherInnerConnection.get(connection).mDispatcher();
                    if (weakReference != null) {
                        BRLoadedApkServiceDispatcher.get(weakReference.get())._set_mConnection(proxy);
                    }
                }

                //Log.d(TAG,"Intent:" + intent + "-->" + "proxyIntent:" + proxyIntent + ",flag:" + intent.getFlags() + "proxyFlag:" + proxyIntent.getFlags());
                if (proxyIntent != null && proxyIntent.getComponent() != null && 
                    proxyIntent.getComponent().getPackageName().equals(BlackBoxCore.getHostPkg())){
                    int flagsIndex = getFlagsIndex(args);
                    if (flagsIndex >= 0) {
                        int flags = MethodParameterUtils.toInt(args[flagsIndex]);
                        flags &= ~Context.BIND_EXTERNAL_SERVICE;
                        args[flagsIndex] = flags;
                    }
                }
                args[callingPackageIndex] = BlackBoxCore.getHostPkg();

                if (proxyIntent != null) {
                    args[2] = proxyIntent;
                    return method.invoke(who, args);
                }
            }
            return method.invoke(who, args);
        } catch (Exception e) {
            Slog.e(TAG, "BindServiceCommon: Unexpected error", e);
            return method.invoke(who, args);
        }
    }

    private static int getFlagsIndex(Object[] args) {
        // Heuristic: flags is the first Number after IServiceConnection position (index 4)
        for (int i = 5; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Integer || arg instanceof Long) {
                return i;
            }
        }
        return -1;
    }
    @ProxyMethod("bindService")
    public static class BindService extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return BindServiceCommon(who,method,args,6);
        }

        @Override
        protected boolean isEnable() {
            return BlackBoxCore.get().isBlackProcess() || BlackBoxCore.get().isServerProcess();
        }
    }

    // android 14 add
    @ProxyMethod("bindServiceInstance")
    public static class bindServiceInstance extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return BindServiceCommon(who,method,args,7);
        }

        @Override
        protected boolean isEnable() {
            return BlackBoxCore.get().isBlackProcess() || BlackBoxCore.get().isServerProcess();
        }
    }

    // 10.0
    @ProxyMethod("bindIsolatedService")
    public static class BindIsolatedService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // instanceName
            args[6] = null;
            return BindServiceCommon(who,method,args,7);
        }

        @Override
        protected boolean isEnable() {
            return BlackBoxCore.get().isBlackProcess() || BlackBoxCore.get().isServerProcess();
        }
    }

    @ProxyMethod("unbindService")
    public static class UnbindService extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            IServiceConnection iServiceConnection = (IServiceConnection) args[0];
            if (iServiceConnection == null) {
                return method.invoke(who, args);
            }
            BlackBoxCore.getBActivityManager().unbindService(iServiceConnection.asBinder(), BActivityThread.getUserId());
            ServiceConnectionDelegate delegate = ServiceConnectionDelegate.getDelegate(iServiceConnection.asBinder());
            if (delegate != null) {
                args[0] = delegate;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getRunningAppProcesses")
    public static class GetRunningAppProcesses extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            RunningAppProcessInfo runningAppProcesses = BActivityManager.get().getRunningAppProcesses(BActivityThread.getAppPackageName(), BActivityThread.getUserId());
            if (runningAppProcesses == null) {
                return new ArrayList<>();
            }
            return runningAppProcesses.mAppProcessInfoList;
        }
    }

    @ProxyMethod("getServices")
    public static class GetServices extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            RunningServiceInfo runningServices = BActivityManager.get().getRunningServices(BActivityThread.getAppPackageName(), BActivityThread.getUserId());
            if (runningServices == null) {
                return new ArrayList<>();
            }
            return runningServices.mRunningServiceInfoList;
        }
    }

    @ProxyMethod("getIntentSender")
    public static class GetIntentSender extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int type = (int) args[0];
            Intent[] intents = (Intent[]) args[getIntentsIndex(args)];
            MethodParameterUtils.replaceFirstAppPkg(args);

            for (int i = 0; i < intents.length; i++) {
                Intent intent = intents[i];
                switch (type) {
                    case ActivityManagerCompat.INTENT_SENDER_ACTIVITY:
                        Intent shadow = new Intent();
                        shadow.setComponent(new ComponentName(BlackBoxCore.getHostPkg(), ProxyManifest.getProxyPendingActivity(BActivityThread.getAppPid())));
                        ProxyPendingRecord.saveStub(shadow, intent, BActivityThread.getUserId());
                        intents[i] = shadow;
                        break;
                }
            }
            IInterface invoke = (IInterface) method.invoke(who, args);
            if (invoke != null) {
                String[] packagesForUid = BPackageManager.get().getPackagesForUid(BActivityThread.getCallingBUid());
                if (packagesForUid.length < 1) {
                    packagesForUid = new String[]{BlackBoxCore.getHostPkg()};
                }
                BlackBoxCore.getBActivityManager().getIntentSender(invoke.asBinder(), packagesForUid[0], BActivityThread.getCallingBUid());
            }
            return invoke;
        }

        private int getIntentsIndex(Object[] args) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent[]) {
                    return i;
                }
            }
            if (BuildCompat.isR()) {
                return 6;
            } else {
                return 5;
            }
        }
    }

    @ProxyMethod("getPackageForIntentSender")
    public static class getPackageForIntentSender extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            IInterface invoke = (IInterface) args[0];
            return BlackBoxCore.getBActivityManager().getPackageForIntentSender(invoke.asBinder());
        }
    }

    @ProxyMethod("getUidForIntentSender")
    public static class getUidForIntentSender extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            IInterface invoke = (IInterface) args[0];
            return BlackBoxCore.getBActivityManager().getUidForIntentSender(invoke.asBinder());
        }
    }

    @ProxyMethod("getIntentSenderWithSourceToken")
    public static class GetIntentSenderWithSourceToken extends GetIntentSender {
    }

    @ProxyMethod("getIntentSenderWithFeature")
    public static class GetIntentSenderWithFeature extends GetIntentSender {
    }

    @ProxyMethod("broadcastIntentWithFeature")
    public static class BroadcastIntentWithFeature extends BroadcastIntent {
    }

    @ProxyMethod("broadcastIntent")
    public static class BroadcastIntent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int intentIndex = getIntentIndex(args);
            Intent intent = (Intent) args[intentIndex];
            String resolvedType = (String) args[intentIndex + 1];
            Intent proxyIntent = BlackBoxCore.getBActivityManager().sendBroadcast(intent, resolvedType, BActivityThread.getUserId());
            if (proxyIntent != null) {
                proxyIntent.setExtrasClassLoader(BActivityThread.getApplication().getClassLoader());
                ProxyBroadcastRecord.saveStub(proxyIntent, intent, BActivityThread.getUserId());
                args[intentIndex] = proxyIntent;
            }
            // ignore permission
            for (int i = 0; i < args.length; i++) {
                Object o = args[i];
                if (o instanceof String[]) {
                    args[i] = null;
                }
            }
            return method.invoke(who, args);
        }

        int getIntentIndex(Object[] args) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof Intent) {
                    return i;
                }
            }
            return 1;
        }
    }

    @ProxyMethod("unregisterReceiver")
    public static class unregisterReceiver extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("finishReceiver")
    public static class finishReceiver extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("publishService")
    public static class PublishService extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("peekService")
    public static class PeekService extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceLastAppPkg(args);
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            IBinder peek = BlackBoxCore.getBActivityManager().peekService(intent, resolvedType, BActivityThread.getUserId());
            return peek;
        }
    }

    // todo
    @ProxyMethod("sendIntentSender")
    public static class SendIntentSender extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    // android 11 add
    @ProxyMethod("registerReceiverWithFeature")
    public static class RegisterReceiverWithFeature extends MethodHook{
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            int receiverIndex = getReceiverIndex();
            if (args[receiverIndex] != null) {
                IIntentReceiver intentReceiver = (IIntentReceiver) args[receiverIndex];
                IIntentReceiver proxy = InnerReceiverDelegate.createProxy(intentReceiver);

                WeakReference<?> weakReference = BRLoadedApkReceiverDispatcherInnerReceiver.get(intentReceiver).mDispatcher();
                if (weakReference != null) {
                    BRLoadedApkReceiverDispatcher.get(weakReference.get())._set_mIIntentReceiver(proxy);
                }

                args[receiverIndex] = proxy;
            }
            // ignore permission
            if (args[getPermissionIndex()] != null) {
                args[getPermissionIndex()] = null;
            }

            if (BuildCompat.isU()) {
                int flagsIndex = args.length - 1;
                int flags = (int)args[flagsIndex];
                if((flags & RECEIVER_NOT_EXPORTED) == 0 && (flags & RECEIVER_EXPORTED) == 0){
                    flags |= RECEIVER_NOT_EXPORTED;
                }
                args[flagsIndex] = flags;
            }

            return method.invoke(who, args);
        }

        public int getReceiverIndex() {
            if (BuildCompat.isS()) {
                return 4;
            }
            return 3;
        }

        public int getPermissionIndex() {
            if (BuildCompat.isS()) {
                return 6;
            }
            return 5;
        }
    }

    //maxTargetSdk=29
    @ProxyMethod("registerReceiver")
    public static class RegisterReceiver extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            int receiverIndex = 2;
            if (args[receiverIndex] != null) {
                IIntentReceiver intentReceiver = (IIntentReceiver) args[receiverIndex];
                IIntentReceiver proxy = InnerReceiverDelegate.createProxy(intentReceiver);

                WeakReference<?> weakReference = BRLoadedApkReceiverDispatcherInnerReceiver.get(intentReceiver).mDispatcher();
                if (weakReference != null) {
                    BRLoadedApkReceiverDispatcher.get(weakReference.get())._set_mIIntentReceiver(proxy);
                }

                args[receiverIndex] = proxy;
            }
            int permissionIndex = 4;
            // ignore permission
            if (args[permissionIndex] != null) {
                args[permissionIndex] = null;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("grantUriPermission")
    public static class GrantUriPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceLastUid(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setServiceForeground")
    public static class setServiceForeground extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Allow foreground service promotion to proceed, and neutralize service type enforcement on Android 14+
            // Find an int argument that represents foregroundServiceType and zero it out
            for (int i = args.length - 1; i >= 0; i--) {
                if (args[i] instanceof Integer) {
                    args[i] = 0; // remove service type requirements (e.g., MICROPHONE)
                    break;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getHistoricalProcessExitReasons")
    public static class getHistoricalProcessExitReasons extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return ParceledListSliceCompat.create(new ArrayList<>());
        }
    }

    @ProxyMethod("getCurrentUser")
    public static class getCurrentUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object blackBox = BRUserInfo.get()._new(BActivityThread.getUserId(), "BlackBox", BRUserInfo.get().FLAG_PRIMARY());
            return blackBox;
        }
    }

    @ProxyMethod("checkPermission")
    public static class checkPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceLastUid(args);
            String permission = (String) args[0];
            if (permission.equals(Manifest.permission.ACCOUNT_MANAGER)
                    || permission.equals(Manifest.permission.SEND_SMS)) {
                return PackageManager.PERMISSION_GRANTED;
            }
            
            // Handle all audio-related permissions comprehensively
            if (isAudioPermission(permission)) {
                Slog.d(TAG, "ActivityManager checkPermission: Granting audio permission: " + permission);
                return PackageManager.PERMISSION_GRANTED;
            }

            // Handle storage/media read permissions so apps can query MediaStore
            if (isStorageOrMediaPermission(permission)) {
                Slog.d(TAG, "ActivityManager checkPermission: Granting storage/media permission: " + permission);
                return PackageManager.PERMISSION_GRANTED;
            }
            
            return method.invoke(who, args);
        }
    }

    // Keep in sync with IPackageManagerProxy helper
    private static boolean isAudioPermission(String permission) {
        if (permission == null) return false;
        return permission.equals(Manifest.permission.RECORD_AUDIO)
                || permission.equals(Manifest.permission.CAPTURE_AUDIO_OUTPUT)
                || permission.equals(Manifest.permission.MODIFY_AUDIO_SETTINGS)
                || permission.equals("android.permission.FOREGROUND_SERVICE_MICROPHONE")
                || permission.equals("android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION")
                || permission.equals("android.permission.FOREGROUND_SERVICE_CAMERA")
                || permission.equals("android.permission.FOREGROUND_SERVICE_LOCATION")
                || permission.equals("android.permission.FOREGROUND_SERVICE_HEALTH")
                || permission.equals("android.permission.FOREGROUND_SERVICE_DATA_SYNC")
                || permission.equals("android.permission.FOREGROUND_SERVICE_SPECIAL_USE")
                || permission.equals("android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED")
                || permission.equals("android.permission.FOREGROUND_SERVICE_PHONE_CALL")
                || permission.equals("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE");
    }

    @ProxyMethod("checkUriPermission")
    public static class checkUriPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return PERMISSION_GRANTED;
        }
    }

    // Keep in sync with IPackageManagerProxy helper
    private static boolean isStorageOrMediaPermission(String permission) {
        if (permission == null) return false;
        if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)
                || permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return true;
        }
        if (permission.equals(Manifest.permission.READ_MEDIA_AUDIO)
                || permission.equals(Manifest.permission.READ_MEDIA_VIDEO)
                || permission.equals(Manifest.permission.READ_MEDIA_IMAGES)
                || permission.equals("android.permission.READ_MEDIA_VISUAL")
                || permission.equals("android.permission.READ_MEDIA_AURAL")
                || permission.equals(Manifest.permission.ACCESS_MEDIA_LOCATION)) {
            return true;
        }
        if (permission.equals("android.permission.READ_MEDIA_AUDIO_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_VIDEO_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_IMAGES_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                || permission.equals("android.permission.READ_MEDIA_AURAL_USER_SELECTED")) {
                return true;
        }
        return false;
    }

    // for < Android 10
    @ProxyMethod("setTaskDescription")
    public static class SetTaskDescription extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ActivityManager.TaskDescription td = (ActivityManager.TaskDescription) args[1];
            args[1] = TaskDescriptionCompat.fix(td);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setRequestedOrientation")
    public static class setRequestedOrientation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    @ProxyMethod("registerUidObserver")
    public static class registerUidObserver extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    @ProxyMethod("unregisterUidObserver")
    public static class unregisterUidObserver extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    @ProxyMethod("updateConfiguration")
    public static class updateConfiguration extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }
}
