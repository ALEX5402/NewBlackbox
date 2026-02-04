package top.niunaijun.blackbox.fake.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import black.android.app.BRActivityThread;
import black.android.app.BRContextImpl;
import black.android.app.ContextImpl;
import black.android.content.pm.BRPackageManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.env.AppSystemEnv;
import top.niunaijun.blackbox.fake.FakeCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.fake.service.base.PkgMethodProxy;
import top.niunaijun.blackbox.fake.service.base.ValueMethodProxy;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.ParceledListSliceCompat;


public class IPackageManagerProxy extends BinderInvocationStub {
    public static final String TAG = "PackageManagerStub";

    public IPackageManagerProxy() {
        super(BRActivityThread.get().sPackageManager().asBinder());
    }

    @Override
    protected Object getWho() {
        return BRActivityThread.get().sPackageManager();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRActivityThread.get()._set_sPackageManager(proxyInvocation);
        replaceSystemService("package");
        Object systemContext = BRActivityThread.get(BlackBoxCore.mainThread()).getSystemContext();
        BRContextImpl.get(systemContext).getPackageManager();
        PackageManager packageManager = BRContextImpl.get(systemContext).mPackageManager();
        if (packageManager != null) {
            BRPackageManager.get().disableApplicationInfoCache();
            try {
                Reflector.on("android.app.ApplicationPackageManager")
                        .field("mPM")
                        .set(packageManager, proxyInvocation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
        addMethodHook(new ValueMethodProxy("addOnPermissionsChangeListener", 0));
        addMethodHook(new ValueMethodProxy("removeOnPermissionsChangeListener", 0));
        addMethodHook(new SimpleAudioPermissionHook());
        addMethodHook(new CheckSelfPermission());
        addMethodHook(new ShouldShowRequestPermissionRationale());
        addMethodHook(new RequestPermissions());
        addMethodHook(new DisableIconLoading());
        addMethodHook(new SetSplashScreenTheme());
        addMethodHook(new XiaomiSecurityBypass());
    }

    @ProxyMethod("resolveIntent")
    public static class ResolveIntent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            int flags = MethodParameterUtils.toInt(args[2]);
            ResolveInfo resolveInfo = BlackBoxCore.getBPackageManager().resolveIntent(intent, resolvedType, flags, BlackBoxCore.getUserId());
            if (resolveInfo != null) {
                return resolveInfo;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("resolveService")
    public static class ResolveService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = (Intent) args[0];
            String resolvedType = (String) args[1];
            int flags = MethodParameterUtils.toInt(args[2]);
            ResolveInfo resolveInfo = BlackBoxCore.getBPackageManager().resolveService(intent, flags, resolvedType, BlackBoxCore.getUserId());
            if (resolveInfo != null) {
                return resolveInfo;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("setComponentEnabledSetting")
    public static class SetComponentEnabledSetting extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }

    @ProxyMethod("getPackageInfo")
    public static class GetPackageInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String packageName = (String) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            
            
            if ("com.android.vending".equals(packageName)) {
                return createFakeGooglePlayServicesPackageInfo();
            }
            
            PackageInfo packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, flags, BlackBoxCore.getUserId());
            if (packageInfo != null) {
                
                if (packageInfo.requestedPermissions != null && packageInfo.requestedPermissionsFlags != null) {
                    for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
                        String perm = packageInfo.requestedPermissions[i];
                        if (perm != null && (perm.equals(android.Manifest.permission.RECORD_AUDIO)
                                || perm.equals("android.permission.FOREGROUND_SERVICE_MICROPHONE")
                                || perm.equals(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
                                || perm.equals(android.Manifest.permission.CAPTURE_AUDIO_OUTPUT))) {
                            packageInfo.requestedPermissionsFlags[i] |= PackageInfo.REQUESTED_PERMISSION_GRANTED;
                        }
                    }
                }
                return packageInfo;
            }
            if (AppSystemEnv.isOpenPackage(packageName)) {
                return method.invoke(who, args);
            }
            return null;
        }
        
        private PackageInfo createFakeGooglePlayServicesPackageInfo() {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.packageName = "com.android.vending";
            packageInfo.versionName = "33.8.16-21";
            packageInfo.versionCode = 83381621;
            
            ApplicationInfo appInfo = new ApplicationInfo();
            appInfo.packageName = "com.android.vending";
            appInfo.name = "Google Play Store";
            appInfo.flags = ApplicationInfo.FLAG_SYSTEM;
            appInfo.uid = 10001; 
            packageInfo.applicationInfo = appInfo;
            
            Slog.d(TAG, "GetPackageInfo: Providing fake Google Play Services info");
            return packageInfo;
        }
    }

    @ProxyMethod("getPackageUid")
    public static class GetPackageUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProviderInfo")
    public static class GetProviderInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ProviderInfo providerInfo = BlackBoxCore.getBPackageManager().getProviderInfo(componentName, flags, BlackBoxCore.getUserId());
            if (providerInfo != null)
                return providerInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getReceiverInfo")
    public static class GetReceiverInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ActivityInfo receiverInfo = BlackBoxCore.getBPackageManager().getReceiverInfo(componentName, flags, BlackBoxCore.getUserId());
            if (receiverInfo != null)
                return receiverInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getActivityInfo")
    public static class GetActivityInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ActivityInfo activityInfo = BlackBoxCore.getBPackageManager().getActivityInfo(componentName, flags, BlackBoxCore.getUserId());
            if (activityInfo != null)
                return activityInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getServiceInfo")
    public static class GetServiceInfo extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ServiceInfo serviceInfo = BlackBoxCore.getBPackageManager().getServiceInfo(componentName, flags, BlackBoxCore.getUserId());
            if (serviceInfo != null)
                return serviceInfo;
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("getInstalledApplications")
    public static class GetInstalledApplications extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[0]);
            List<ApplicationInfo> installedApplications = BlackBoxCore.getBPackageManager().getInstalledApplications(flags, BlackBoxCore.getUserId());
            return ParceledListSliceCompat.create(installedApplications);
        }
    }

    @ProxyMethod("getInstalledPackages")
    public static class GetInstalledPackages extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[0]);
            List<PackageInfo> installedPackages = BlackBoxCore.getBPackageManager().getInstalledPackages(flags, BlackBoxCore.getUserId());
            return ParceledListSliceCompat.create(installedPackages);
        }
    }

    @ProxyMethod("getApplicationInfo")
    public static class GetApplicationInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String packageName = (String) args[0];

            int flags = MethodParameterUtils.toInt(args[1]);



            ApplicationInfo applicationInfo = BlackBoxCore.getBPackageManager().getApplicationInfo(packageName, flags, BlackBoxCore.getUserId());
            if (applicationInfo != null) {
                return applicationInfo;
            }
            if (AppSystemEnv.isOpenPackage(packageName)) {
                return method.invoke(who, args);
            }
            return null;
        }
    }

    @ProxyMethod("queryContentProviders")
    public static class QueryContentProviders extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int flags = MethodParameterUtils.toInt(args[2]);
            List<ProviderInfo> providers = BlackBoxCore.getBPackageManager().
                    queryContentProviders(BlackBoxCore.getAppProcessName(), BlackBoxCore.getBUid(), flags, BlackBoxCore.getUserId());
            return ParceledListSliceCompat.create(providers);
        }
    }

    @ProxyMethod("queryIntentReceivers")
    public static class QueryBroadcastReceivers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
            String type = MethodParameterUtils.getFirstParam(args, String.class);
            Integer flags = MethodParameterUtils.getFirstParam(args, Integer.class);
            List<ResolveInfo> resolves = BlackBoxCore.getBPackageManager().queryBroadcastReceivers(intent, flags, type, BActivityThread.getUserId());
            Slog.d(TAG, "queryIntentReceivers: " + resolves);

            
            if (BuildCompat.isN()) {
                return ParceledListSliceCompat.create(resolves);
            }

            
            return resolves;
        }
    }

    @ProxyMethod("resolveContentProvider")
    public static class ResolveContentProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String authority = (String) args[0];
            int flags = MethodParameterUtils.toInt(args[1]);
            ProviderInfo providerInfo = BlackBoxCore.getBPackageManager().resolveContentProvider(authority, flags, BActivityThread.getUserId());
            if (providerInfo == null) {
                return method.invoke(who, args);
            }
            return providerInfo;
        }
    }

    @ProxyMethod("canRequestPackageInstalls")
    public static class CanRequestPackageInstalls extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getPackagesForUid")
    public static class GetPackagesForUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int uid = (Integer) args[0];
            if (uid == BlackBoxCore.getHostUid()) {
                args[0] = BActivityThread.getBUid();
                uid = (int) args[0];
            }
            String[] packagesForUid = BlackBoxCore.getBPackageManager().getPackagesForUid(uid);
            Slog.d(TAG, args[0] + " , " + BActivityThread.getAppProcessName() + " GetPackagesForUid: " + Arrays.toString(packagesForUid));
            return packagesForUid;
        }
    }

    @ProxyMethod("getInstallerPackageName")
    public static class GetInstallerPackageName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return "com.android.vending";
        }
    }

    @ProxyMethod("getSharedLibraries")
    public static class GetSharedLibraries extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return ParceledListSliceCompat.create(new ArrayList<>());
        }
    }

    @ProxyMethod("getComponentEnabledSetting")
    public static class getComponentEnabledSetting extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            String packageName = componentName.getPackageName();
            ApplicationInfo applicationInfo = BlackBoxCore.getBPackageManager().getApplicationInfo(packageName,0, BActivityThread.getUserId());





            if(applicationInfo != null){
                return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            }
            if (AppSystemEnv.isOpenPackage(componentName)) {
                return method.invoke(who, args);
            }
            throw new IllegalArgumentException();
        }
    }





    @ProxyMethod("checkPermission")
    public static class SimpleAudioPermissionHook extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String permission = (String) args[0];
            String packageName = (String) args[1];
            
            
            if (isAudioPermission(permission)) {
                Slog.d(TAG, "SimpleAudioPermissionHook: Granting audio permission: " + permission + " to " + packageName);
                return PackageManager.PERMISSION_GRANTED;
            }

            
            if (isStorageOrMediaPermission(permission)) {
                Slog.d(TAG, "SimpleAudioPermissionHook: Granting storage/media permission: " + permission + " to " + packageName);
                return PackageManager.PERMISSION_GRANTED;
            }
            
            
            if (isNotificationOrXiaomiPermission(permission)) {
                Slog.d(TAG, "SimpleAudioPermissionHook: Granting notification/Xiaomi permission: " + permission + " to " + packageName);
                return PackageManager.PERMISSION_GRANTED;
            }
            
            
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("checkSelfPermission")
    public static class CheckSelfPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String permission = (String) args[0];
            String packageName = (String) args[1];
            
            
            if (isAudioPermission(permission)) {
                Slog.d(TAG, "CheckSelfPermission: Granting audio permission: " + permission + " to " + packageName);
                return PackageManager.PERMISSION_GRANTED;
            }

            
            if (isStorageOrMediaPermission(permission)) {
                Slog.d(TAG, "CheckSelfPermission: Granting storage/media permission: " + permission + " to " + packageName);
                return PackageManager.PERMISSION_GRANTED;
            }
            
            
            if (isNotificationOrXiaomiPermission(permission)) {
                Slog.d(TAG, "CheckSelfPermission: Granting notification/Xiaomi permission: " + permission + " to " + packageName);
                return PackageManager.PERMISSION_GRANTED;
            }
            
            
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("shouldShowRequestPermissionRationale")
    public static class ShouldShowRequestPermissionRationale extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String permission = (String) args[0];
            String packageName = (String) args[1];
            
            
            if (isAudioPermission(permission)) {
                Slog.d(TAG, "ShouldShowRequestPermissionRationale: Not showing rationale for audio permission: " + permission);
                return false;
            }

            
            if (isStorageOrMediaPermission(permission)) {
                Slog.d(TAG, "ShouldShowRequestPermissionRationale: Not showing rationale for storage/media permission: " + permission);
                return false;
            }
            
            
            if (isNotificationOrXiaomiPermission(permission)) {
                Slog.d(TAG, "ShouldShowRequestPermissionRationale: Not showing rationale for notification/Xiaomi permission: " + permission);
                return false;
            }
            
            
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("requestPermissions")
    public static class RequestPermissions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String[] permissions = (String[]) args[0];
            String packageName = (String) args[1];
            
            
            
            if (permissions != null) {
                Slog.d(TAG, "RequestPermissions: Allowing permission request flow for: " + java.util.Arrays.toString(permissions));
            }
            
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getDrawable")
    public static class DisableIconLoading extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "Blocking icon loading to prevent resource errors");
            return null; 
        }
    }

    
    private static boolean isStorageOrMediaPermission(String permission) {
        if (permission == null) return false;
        
        if (permission.equals(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                || permission.equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return true;
        }
        
        if (permission.equals(android.Manifest.permission.READ_MEDIA_AUDIO)
                || permission.equals(android.Manifest.permission.READ_MEDIA_VIDEO)
                || permission.equals(android.Manifest.permission.READ_MEDIA_IMAGES)
                || permission.equals("android.permission.READ_MEDIA_VISUAL")
                || permission.equals("android.permission.READ_MEDIA_AURAL")
                || permission.equals(android.Manifest.permission.ACCESS_MEDIA_LOCATION)) {
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

    
    private static boolean isAudioPermission(String permission) {
        if (permission == null) return false;
        return permission.equals(android.Manifest.permission.RECORD_AUDIO)
                || permission.equals(android.Manifest.permission.CAPTURE_AUDIO_OUTPUT)
                || permission.equals(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
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
    
    
    private static boolean isNotificationOrXiaomiPermission(String permission) {
        if (permission == null) return false;
        
        
        if (permission.equals("android.permission.POST_NOTIFICATIONS")) {
            return true;
        }
        
        
        if (permission.equals("miui.permission.USE_INTERNAL_GENERAL_API") ||
            permission.equals("miui.permission.OPTIMIZE_POWER") ||
            permission.equals("miui.permission.RUN_IN_BACKGROUND") ||
            permission.equals("miui.permission.POST_NOTIFICATIONS") ||
            permission.equals("miui.permission.AUTO_START") ||
            permission.equals("miui.permission.BACKGROUND_POPUP_WINDOW") ||
            permission.equals("miui.permission.SHOW_WHEN_LOCKED") ||
            permission.equals("miui.permission.TURN_SCREEN_ON")) {
            return true;
        }
        
        return false;
    }

    @ProxyMethod("setSplashScreenTheme")
    public static class SetSplashScreenTheme extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            
            
            String packageName = args.length > 0 ? (String) args[0] : "unknown";
            Slog.d(TAG, "SetSplashScreenTheme: Bypassing UID check for package: " + packageName);
            
            
            boolean isXiaomi = BuildCompat.isMIUI() || 
                              Build.MANUFACTURER.toLowerCase().contains("xiaomi") ||
                              Build.BRAND.toLowerCase().contains("xiaomi") ||
                              Build.DISPLAY.toLowerCase().contains("hyperos");
                              
            if (isXiaomi) {
                Slog.d(TAG, "SetSplashScreenTheme: Detected Xiaomi/HyperOS, using enhanced bypass");
                
                return null;
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (SecurityException e) {
                Slog.w(TAG, "SetSplashScreenTheme: SecurityException caught, bypassing: " + e.getMessage());
                return null;
            } catch (Exception e) {
                
                if (e.getCause() instanceof SecurityException) {
                    Slog.w(TAG, "SetSplashScreenTheme: SecurityException (wrapped) caught, bypassing: " + e.getCause().getMessage());
                    return null;
                }
                throw e; 
            }
        }
    }

    
    public static class XiaomiSecurityBypass extends MethodHook {
        private static final String[] XIAOMI_SECURITY_METHODS = {
            "setApplicationEnabledSetting",
            "setComponentEnabledSetting", 
            "setInstallLocation",
            "setInstallerPackageName",
            "setPackageStoppedState",
            "setSystemAppState",
            "setApplicationCategoryHint",
            "setApplicationHiddenSettingAsUser",
            "setBlockUninstallForUser",
            "setDefaultBrowserPackageNameAsUser",
            "setDistractingPackageRestrictionsAsUser",
            "setPackagesSuspendedAsUser",
            "setUpdateAvailable",
            "setRequiredForSystemUser",
            "setSystemAppHiddenUntilInstalled",
            "setHarmfulAppWarningEnabled",
            "setKeepUninstalledPackages",
            "verifyIntentFilter",
            "verifyPendingInstall",
            "extendVerificationTimeout",
            "setDefaultHomeActivity",
            "resetApplicationPreferences",
            "clearApplicationProfileData",
            "clearApplicationUserData",
            "deleteApplicationCacheFiles",
            "deleteApplicationCacheFilesAsUser",
            "freeStorageAndNotify",
            "freeStorage",
            "movePackage",
            "movePackageToSd",
            "movePrimaryStorage"
        };

        @Override
        public boolean isEnable() {
            
            return BuildCompat.isMIUI() || 
                   Build.MANUFACTURER.toLowerCase().contains("xiaomi") ||
                   Build.BRAND.toLowerCase().contains("xiaomi") ||
                   Build.DISPLAY.toLowerCase().contains("hyperos");
        }

        @Override
        public String getMethodName() {
            return null; 
        }

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            
            
            for (String securityMethod : XIAOMI_SECURITY_METHODS) {
                if (securityMethod.equals(methodName)) {
                    Slog.d(TAG, "XiaomiSecurityBypass: Intercepting " + methodName + " on Xiaomi/HyperOS");
                    
                    
                    if (method.getReturnType() == void.class) {
                        return null;
                    }
                    
                    else if (method.getReturnType() == boolean.class) {
                        return true;
                    }
                    
                    else if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    
                    else {
                        return null;
                    }
                }
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (SecurityException e) {
                Slog.w(TAG, "XiaomiSecurityBypass: SecurityException in " + methodName + ", bypassing: " + e.getMessage());
                
                if (method.getReturnType() == boolean.class) {
                    return false;
                } else if (method.getReturnType() == int.class) {
                    return -1;
                } else {
                    return null;
                }
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Slog.w(TAG, "XiaomiSecurityBypass: SecurityException (wrapped) in " + methodName + ", bypassing: " + e.getCause().getMessage());
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    } else if (method.getReturnType() == int.class) {
                        return -1;
                    } else {
                        return null;
                    }
                }
                throw e;
            }
        }
    }
}
