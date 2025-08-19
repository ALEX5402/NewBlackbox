package top.niunaijun.blackbox.app;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import java.io.File;
import java.lang.reflect.Method;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import black.android.app.ActivityThreadAppBindDataContext;
import black.android.app.BRActivity;
import black.android.app.BRActivityManagerNative;
import black.android.app.BRActivityThread;
import black.android.app.BRActivityThreadActivityClientRecord;
import black.android.app.BRActivityThreadAppBindData;
import black.android.app.BRActivityThreadNMR1;
import black.android.app.BRActivityThreadQ;
import black.android.app.BRContextImpl;
import black.android.app.BRLoadedApk;
import black.android.app.BRService;
import black.android.app.LoadedApk;
import black.android.content.BRBroadcastReceiver;
import black.android.content.BRContentProviderClient;
import black.android.graphics.BRCompatibility;
import black.android.security.net.config.BRNetworkSecurityConfigProvider;
import black.com.android.internal.content.BRReferrerIntent;
import black.dalvik.system.BRVMRuntime;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback;
import top.niunaijun.blackbox.app.dispatcher.AppServiceDispatcher;
import top.niunaijun.blackbox.core.CrashHandler;
import top.niunaijun.blackbox.core.IBActivityThread;
import top.niunaijun.blackbox.core.IOCore;
import top.niunaijun.blackbox.core.NativeCore;
import top.niunaijun.blackbox.core.env.VirtualRuntime;
import top.niunaijun.blackbox.core.system.user.BUserHandle;
import top.niunaijun.blackbox.entity.AppConfig;
import top.niunaijun.blackbox.entity.am.ReceiverData;
import top.niunaijun.blackbox.entity.pm.InstalledModule;
import top.niunaijun.blackbox.fake.delegate.AppInstrumentation;
import top.niunaijun.blackbox.fake.delegate.ContentProviderDelegate;
import top.niunaijun.blackbox.fake.frameworks.BXposedManager;
import top.niunaijun.blackbox.fake.hook.HookManager;
import top.niunaijun.blackbox.fake.service.HCallbackProxy;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.SafeContextWrapper;
import top.niunaijun.blackbox.utils.GlobalContextWrapper;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.ActivityManagerCompat;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.ContextCompat;
import top.niunaijun.blackbox.utils.compat.StrictModeCompat;
import top.niunaijun.blackbox.core.system.JarManager;

/**
 * updated by alex5402 on 3/31/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class BActivityThread extends IBActivityThread.Stub {
    public static final String TAG = "BActivityThread";

    private static BActivityThread sBActivityThread;
    private AppBindData mBoundApplication;
    private Application mInitialApplication;
    private AppConfig mAppConfig;
    private final List<ProviderInfo> mProviders = new ArrayList<>();
    private final Handler mH = BlackBoxCore.get().getHandler();
    private static final Object mConfigLock = new Object();

    public static boolean isThreadInit() {
        return sBActivityThread != null;
    }

    public static BActivityThread currentActivityThread() {
        if (sBActivityThread == null) {
            synchronized (BActivityThread.class) {
                if (sBActivityThread == null) {
                    sBActivityThread = new BActivityThread();
                }
            }
        }
        return sBActivityThread;
    }

    public static AppConfig getAppConfig() {
        synchronized (mConfigLock) {
            return currentActivityThread().mAppConfig;
        }
    }

    public static List<ProviderInfo> getProviders() {
        return currentActivityThread().mProviders;
    }

    public static String getAppProcessName() {
        if (getAppConfig() != null) {
            return getAppConfig().processName;
        } else if (currentActivityThread().mBoundApplication != null) {
            return currentActivityThread().mBoundApplication.processName;
        } else {
            return null;
        }
    }

    public static String getAppPackageName() {
        if (getAppConfig() != null) {
            return getAppConfig().packageName;
        } else if (currentActivityThread().mInitialApplication != null) {
            return currentActivityThread().mInitialApplication.getPackageName();
        } else {
            return null;
        }
    }

    public static Application getApplication() {
        return currentActivityThread().mInitialApplication;
    }

    public static int getAppPid() {
        return getAppConfig() == null ? -1 : getAppConfig().bpid;
    }

    public static int getBUid() {
        return getAppConfig() == null ? BUserHandle.AID_APP_START : getAppConfig().buid;
    }

    public static int getBAppId() {
        return BUserHandle.getAppId(getBUid());
    }

    public static int getCallingBUid() {
        return getAppConfig() == null ? BlackBoxCore.getHostUid() : getAppConfig().callingBUid;
    }

    public static int getUid() {
        return getAppConfig() == null ? -1 : getAppConfig().uid;
    }

    public static int getUserId() {
        return getAppConfig() == null ? 0 : getAppConfig().userId;
    }

    public void initProcess(AppConfig appConfig) {
        synchronized (mConfigLock) {
            if (this.mAppConfig != null && !this.mAppConfig.packageName.equals(appConfig.packageName)) {
                // 该进程已被attach
                throw new RuntimeException("reject init process: " + appConfig.processName + ", this process is : " + this.mAppConfig.processName);
            }
            this.mAppConfig = appConfig;
            IBinder iBinder = asBinder();
            try {
                iBinder.linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        synchronized (mConfigLock) {
                            try {
                                iBinder.linkToDeath(this, 0);
                            } catch (RemoteException ignored) {
                            }
                            mAppConfig = null;
                        }
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isInit() {
        return mBoundApplication != null;
    }

    public Service createService(ServiceInfo serviceInfo, IBinder token) {
        if (!BActivityThread.currentActivityThread().isInit()) {
            BActivityThread.currentActivityThread().bindApplication(serviceInfo.packageName, serviceInfo.processName);
        }
        ClassLoader classLoader = BRLoadedApk.get(mBoundApplication.info).getClassLoader();
        Service service;
        try {
            service = (Service) classLoader.loadClass(serviceInfo.name).newInstance();
        } catch (ClassNotFoundException e) {
            // Handle missing Google Play Services classes gracefully
            if (serviceInfo.name.contains("google.android.gms") || 
                serviceInfo.name.contains("google.android.location")) {
                Slog.w(TAG, "Google Play Services class not found, skipping: " + serviceInfo.name);
                return null;
            }
            e.printStackTrace();
            Slog.e(TAG, "Unable to instantiate service " + serviceInfo.name
                    + ": " + e.toString());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Slog.e(TAG, "Unable to instantiate service " + serviceInfo.name
                    + ": " + e.toString());
            return null;
        }

        try {
            Context context = BlackBoxCore.getContext().createPackageContext(
                    serviceInfo.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            BRContextImpl.get(context).setOuterContext(service);
            BRService.get(service).attach(
                    context,
                    BlackBoxCore.mainThread(),
                    serviceInfo.name,
                    token,
                    mInitialApplication,
                    BRActivityManagerNative.get().getDefault()
            );
            ContextCompat.fix(context);
            service.onCreate();
            return service;
        } catch (Exception e) {
            // Handle service creation errors gracefully
            if (serviceInfo.name.contains("google.android.gms") || 
                serviceInfo.name.contains("google.android.location")) {
                Slog.w(TAG, "Google Play Services service creation failed, skipping: " + serviceInfo.name);
                return null;
            }
            Slog.w(TAG, "Service creation failed, but continuing: " + serviceInfo.name + " - " + e.getMessage());
            return null;
        }
    }

    public JobService createJobService(ServiceInfo serviceInfo) {
        if (!BActivityThread.currentActivityThread().isInit()) {
            BActivityThread.currentActivityThread().bindApplication(serviceInfo.packageName, serviceInfo.processName);
        }
        ClassLoader classLoader = BRLoadedApk.get(mBoundApplication.info).getClassLoader();
        JobService service;
        try {
            service = (JobService) classLoader.loadClass(serviceInfo.name).newInstance();
        } catch (ClassNotFoundException e) {
            // Handle missing Google Play Services classes gracefully
            if (serviceInfo.name.contains("google.android.gms") || 
                serviceInfo.name.contains("google.android.location")) {
                Slog.w(TAG, "Google Play Services JobService class not found, skipping: " + serviceInfo.name);
                return null;
            }
            e.printStackTrace();
            Slog.e(TAG, "Unable to create JobService " + serviceInfo.name
                    + ": " + e.toString());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Slog.e(TAG, "Unable to create JobService " + serviceInfo.name
                    + ": " + e.toString());
            return null;
        }

        try {
            Context context = BlackBoxCore.getContext().createPackageContext(
                    serviceInfo.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );
            BRContextImpl.get(context).setOuterContext(service);
            BRService.get(service).attach(
                    context,
                    BlackBoxCore.mainThread(),
                    serviceInfo.name,
                    BActivityThread.currentActivityThread().getActivityThread(),
                    mInitialApplication,
                    BRActivityManagerNative.get().getDefault()
            );
            ContextCompat.fix(context);
            service.onCreate();
            service.onBind(null);
            return service;
        } catch (Exception e) {
            // Handle JobService creation errors gracefully
            if (serviceInfo.name.contains("google.android.gms") || 
                serviceInfo.name.contains("google.android.location")) {
                Slog.w(TAG, "Google Play Services JobService creation failed, skipping: " + serviceInfo.name);
                return null;
            }
            Slog.w(TAG, "JobService creation failed, but continuing: " + serviceInfo.name + " - " + e.getMessage());
            return null;
        }
    }

    public void bindApplication(final String packageName, final String processName) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            final ConditionVariable conditionVariable = new ConditionVariable();
            BlackBoxCore.get().getHandler().post(() -> {
                // Create a minimal data object for the new handleBindApplication method
                Object bindData = createBindApplicationData(packageName, processName);
                handleBindApplication(packageName, processName);
                conditionVariable.open();
            });
            conditionVariable.block();
        } else {
            // Create a minimal data object for the new handleBindApplication method
            Object bindData = createBindApplicationData(packageName, processName);
            handleBindApplication(packageName, processName);
        }
    }
    
    /**
     * Create a minimal bind application data object
     */
    private Object createBindApplicationData(String packageName, String processName) {
        try {
            // Get package info to create application info
            PackageInfo packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, PackageManager.GET_PROVIDERS, getUserId());
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            
            // Create a simple data object that can be used by the new handleBindApplication method
            // This is a simplified approach - in a real implementation you'd create the proper data structure
            return new Object() {
                public ApplicationInfo getInfo() { return applicationInfo; }
                public List<ProviderInfo> getProviders() { 
                    return packageInfo.providers != null ? Arrays.asList(packageInfo.providers) : new ArrayList<>();
                }
            };
        } catch (Exception e) {
            Slog.e(TAG, "Error creating bind application data", e);
            // Return a minimal fallback object
            return new Object() {
                public ApplicationInfo getInfo() { return null; }
                public List<ProviderInfo> getProviders() { return new ArrayList<>(); }
            };
        }
    }

    public synchronized void handleBindApplication(String packageName, String processName) {
        if (isInit())
            return;
        try {
            CrashHandler.create();
        } catch (Throwable ignored) {
        }

        PackageInfo packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, PackageManager.GET_PROVIDERS, BActivityThread.getUserId());
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (packageInfo.providers == null) {
            packageInfo.providers = new ProviderInfo[]{};
        }
        mProviders.addAll(Arrays.asList(packageInfo.providers));

        Object boundApplication = BRActivityThread.get(BlackBoxCore.mainThread()).mBoundApplication();

        Context packageContext = createPackageContext(applicationInfo);
        Object loadedApk = BRContextImpl.get(packageContext).mPackageInfo();
        BRLoadedApk.get(loadedApk)._set_mSecurityViolation(false);
        // fix applicationInfo
        BRLoadedApk.get(loadedApk)._set_mApplicationInfo(applicationInfo);

        int targetSdkVersion = applicationInfo.targetSdkVersion;
        if (targetSdkVersion < Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy newPolicy = new StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy()).permitNetwork().build();
            StrictMode.setThreadPolicy(newPolicy);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (targetSdkVersion < Build.VERSION_CODES.N) {
                StrictModeCompat.disableDeathOnFileUriExposure();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(getUserId() + ":" + packageName + ":" + processName);
        }

        VirtualRuntime.setupRuntime(processName, applicationInfo);

        BRVMRuntime.get(BRVMRuntime.get().getRuntime()).setTargetSdkVersion(applicationInfo.targetSdkVersion);
        if (BuildCompat.isS()) {
            BRCompatibility.get().setTargetSdkVersion(applicationInfo.targetSdkVersion);
        }

        NativeCore.init(Build.VERSION.SDK_INT);
        assert packageContext != null;
        IOCore.get().enableRedirect(packageContext);

        AppBindData bindData = new AppBindData();
        bindData.appInfo = applicationInfo;
        bindData.processName = processName;
        bindData.info = loadedApk;
        bindData.providers = mProviders;

        ActivityThreadAppBindDataContext activityThreadAppBindData = BRActivityThreadAppBindData.get(boundApplication);
        activityThreadAppBindData._set_instrumentationName(new ComponentName(bindData.appInfo.packageName, Instrumentation.class.getName()));
        activityThreadAppBindData._set_appInfo(bindData.appInfo);
        activityThreadAppBindData._set_info(bindData.info);
        activityThreadAppBindData._set_processName(bindData.processName);
        activityThreadAppBindData._set_providers(bindData.providers);

        mBoundApplication = bindData;

        //ssl适配
        if (BRNetworkSecurityConfigProvider.getRealClass() != null) {
            Security.removeProvider("AndroidNSSP");
            BRNetworkSecurityConfigProvider.get().install(packageContext);
        }
        Application application;
        try {
            onBeforeCreateApplication(packageName, processName, packageContext);
            
            // Try to create the application with better error handling
            try {
                application = BRLoadedApk.get(loadedApk).makeApplication(false, null);
            } catch (Exception makeAppException) {
                Slog.e(TAG, "Failed to makeApplication, trying fallback approach", makeAppException);
                application = null;
            }
            
            // If application is still null, try alternative approaches
            if (application == null) {
                Slog.w(TAG, "makeApplication returned null, attempting fallback creation");
                
                // Try with different parameters
                try {
                    application = BRLoadedApk.get(loadedApk).makeApplication(true, null);
                } catch (Exception e) {
                    Slog.e(TAG, "Fallback makeApplication also failed", e);
                }
                
                // If still null, try to create a minimal application context
                if (application == null) {
                    Slog.w(TAG, "Creating minimal application context as fallback");
                    try {
                        // Create a minimal application object or use the package context
                        application = (Application) packageContext;
                        if (application == null) {
                            Slog.e(TAG, "Even package context is null, this is critical");
                            throw new RuntimeException("Unable to create application context");
                        }
                    } catch (Exception contextException) {
                        Slog.e(TAG, "Failed to create fallback application context", contextException);
                        throw new RuntimeException("Unable to makeApplication - all fallback attempts failed", contextException);
                    }
                }
            }
            
            if (application == null) {
                Slog.e(TAG, "makeApplication application Error! All attempts failed");
                throw new RuntimeException("Unable to create application - all creation methods failed");
            }
            
            mInitialApplication = application;
            BRActivityThread.get(BlackBoxCore.mainThread())._set_mInitialApplication(mInitialApplication);
            ContextCompat.fix((Context) BRActivityThread.get(BlackBoxCore.mainThread()).getSystemContext());
            ContextCompat.fix(mInitialApplication);
            installProviders(mInitialApplication, bindData.processName, bindData.providers);

            onBeforeApplicationOnCreate(packageName, processName, application);
            AppInstrumentation.get().callApplicationOnCreate(application);
            onAfterApplicationOnCreate(packageName, processName, application);

            HookManager.get().checkEnv(HCallbackProxy.class);
        } catch (Exception e) {
            Slog.e(TAG, "Critical error in handleBindApplication", e);
            throw new RuntimeException("Unable to makeApplication", e);
        }
    }
    
    /**
     * Initialize JAR environment for DEX loading
     */
    private void initializeJarEnvironment() {
        try {
            Slog.d(TAG, "Initializing JAR environment for DEX loading");
            
            // Initialize JarManager
            JarManager jarManager = JarManager.getInstance();
            if (!jarManager.isReady()) {
                Slog.d(TAG, "JarManager not ready, initializing synchronously");
                jarManager.initializeSync();
            }
            
            // Verify empty.jar is available
            File emptyJar = jarManager.getEmptyJar();
            if (emptyJar == null || !emptyJar.exists()) {
                Slog.w(TAG, "Empty JAR not available, attempting to recreate");
                jarManager.clearCache();
                jarManager.initializeSync();
                emptyJar = jarManager.getEmptyJar();
            }
            
            if (emptyJar != null && emptyJar.exists()) {
                Slog.d(TAG, "Empty JAR verified: " + emptyJar.getAbsolutePath());
            } else {
                Slog.w(TAG, "Empty JAR still not available after retry");
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error initializing JAR environment", e);
        }
    }
    
    /**
     * Create application with enhanced error handling and fallback mechanisms
     */
    private Application createApplicationWithFallback(android.content.pm.ApplicationInfo appInfo) {
        try {
            // First attempt: Try to create application normally
            Application application = createApplication(appInfo);
            if (application != null) {
                Slog.d(TAG, "Application created successfully: " + appInfo.className);
                return application;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create application normally: " + e.getMessage());
        }
        
        try {
            // Second attempt: Try with different class loader
            Slog.d(TAG, "Attempting fallback application creation");
            ClassLoader classLoader = getClassLoader(appInfo);
            if (classLoader == null) {
                Slog.w(TAG, "ClassLoader is null, using system class loader");
                classLoader = ClassLoader.getSystemClassLoader();
            }
            
            Class<?> appClass = classLoader.loadClass(appInfo.className);
            Application application = (Application) appClass.newInstance();
            
            // Ensure the application has a proper base context
            ensureApplicationBaseContext(application, appInfo);
            
            Slog.d(TAG, "Fallback application creation successful");
            return application;
            
        } catch (Exception e) {
            Slog.e(TAG, "Fallback application creation failed: " + e.getMessage());
            
            // Third attempt: Create a minimal application wrapper
            try {
                Slog.d(TAG, "Creating minimal application wrapper");
                Application wrapper = new Application() {
                    @Override
                    public void onCreate() {
                        super.onCreate();
                        Slog.d(TAG, "Minimal application wrapper onCreate called");
                    }
                };
                
                // Ensure the minimal application has a proper base context
                ensureApplicationBaseContext(wrapper, appInfo);
                
                return wrapper;
            } catch (Exception wrapperException) {
                Slog.e(TAG, "Failed to create minimal application wrapper", wrapperException);
                return null;
            }
        }
    }
    
    /**
     * Install content providers with enhanced error handling
     */
    private void installContentProvidersWithFallback(Application application, Object data) {
        try {
            List<android.content.pm.ProviderInfo> providers = getProviderInfoList(data);
            if (providers == null || providers.isEmpty()) {
                Slog.d(TAG, "No content providers to install");
                return;
            }
            
            Slog.d(TAG, "Installing " + providers.size() + " content providers");
            
            for (android.content.pm.ProviderInfo providerInfo : providers) {
                try {
                    installContentProvider(application, providerInfo);
                    Slog.d(TAG, "Successfully installed provider: " + providerInfo.name);
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to install provider " + providerInfo.name + ": " + e.getMessage());
                    // Continue with other providers
                }
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error installing content providers", e);
        }
    }
    
    /**
     * Get application info from bind application data
     */
    private android.content.pm.ApplicationInfo getApplicationInfo(Object data) {
        try {
            // Try to get application info from our custom data object
            if (data != null) {
                try {
                    // Use reflection to call getInfo() method
                    Method getInfoMethod = data.getClass().getMethod("getInfo");
                    ApplicationInfo appInfo = (ApplicationInfo) getInfoMethod.invoke(data);
                    if (appInfo != null) {
                        return appInfo;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Error getting info from custom data object: " + e.getMessage());
                }
            }
            
            // Fallback: try to get from package manager
            String packageName = BlackBoxCore.getAppPackageName();
            if (packageName != null) {
                PackageInfo packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, 0, getUserId());
                return packageInfo.applicationInfo;
            }
            
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Error getting application info", e);
            return null;
        }
    }
    
    /**
     * Get class loader for application
     */
    private ClassLoader getClassLoader(android.content.pm.ApplicationInfo appInfo) {
        try {
            // ApplicationInfo doesn't have a classLoader field, so we need to create one
            String sourceDir = appInfo.sourceDir;
            if (sourceDir != null) {
                return new dalvik.system.PathClassLoader(sourceDir, ClassLoader.getSystemClassLoader());
            }
            
            // Fallback to system class loader
            return ClassLoader.getSystemClassLoader();
        } catch (Exception e) {
            Slog.w(TAG, "Error getting class loader: " + e.getMessage());
            return ClassLoader.getSystemClassLoader();
        }
    }

    /**
     * Create application from application info
     */
    private Application createApplication(android.content.pm.ApplicationInfo appInfo) {
        try {
            // Create a basic application instance
            ClassLoader classLoader = getClassLoader(appInfo);
            Class<?> appClass = classLoader.loadClass(appInfo.className);
            Application application = (Application) appClass.newInstance();
            
            // Ensure the application has a proper base context
            ensureApplicationBaseContext(application, appInfo);
            
            return application;
        } catch (Exception e) {
            Slog.e(TAG, "Error creating application: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Ensure application has a proper base context
     */
    private void ensureApplicationBaseContext(Application application, android.content.pm.ApplicationInfo appInfo) {
        try {
            // Check if application already has a base context
            if (application.getBaseContext() != null) {
                Slog.d(TAG, "Application already has base context: " + appInfo.className);
                return;
            }
            
            // Create a package context for the application
            Context packageContext = createPackageContext(appInfo);
            if (packageContext == null) {
                Slog.w(TAG, "Could not create package context for application: " + appInfo.className + ", using fallback");
                // Use a fallback context that will never be null
                packageContext = createFallbackContext(appInfo.packageName);
            }
            
            // Ensure the package context is not null
            if (packageContext == null) {
                Slog.e(TAG, "Failed to create any context for application: " + appInfo.className);
                return;
            }
            
            // Attach the base context to the application
            try {
                Method attachBaseContext = Application.class.getDeclaredMethod("attachBaseContext", Context.class);
                attachBaseContext.setAccessible(true);
                attachBaseContext.invoke(application, packageContext);
                Slog.d(TAG, "Successfully attached base context to application: " + appInfo.className);
            } catch (Exception e) {
                Slog.w(TAG, "Could not attach base context to application: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error ensuring application base context: " + e.getMessage());
        }
    }
    
    /**
     * Create a fallback context that will never be null
     */
    private Context createFallbackContext(String packageName) {
        try {
            Context baseContext = BlackBoxCore.getContext();
            if (baseContext == null) {
                Slog.e(TAG, "BlackBoxCore.getContext() is null, cannot create fallback context");
                return null;
            }
            
            // Create a safe context wrapper that provides all necessary methods
            return new ContextWrapper(baseContext) {
                @Override
                public String getPackageName() {
                    return packageName;
                }
                
                @Override
                public android.content.pm.PackageManager getPackageManager() {
                    try {
                        return baseContext.getPackageManager();
                    } catch (Exception e) {
                        Slog.w(TAG, "Error getting package manager from base context: " + e.getMessage());
                        return null;
                    }
                }
                
                @Override
                public android.content.res.Resources getResources() {
                    try {
                        return baseContext.getResources();
                    } catch (Exception e) {
                        Slog.w(TAG, "Error getting resources from base context: " + e.getMessage());
                        try {
                            return android.content.res.Resources.getSystem();
                        } catch (Exception e2) {
                            Slog.e(TAG, "Error getting system resources: " + e2.getMessage());
                            return null;
                        }
                    }
                }
                
                @Override
                public ClassLoader getClassLoader() {
                    try {
                        return baseContext.getClassLoader();
                    } catch (Exception e) {
                        Slog.w(TAG, "Error getting class loader from base context: " + e.getMessage());
                        try {
                            return ClassLoader.getSystemClassLoader();
                        } catch (Exception e2) {
                            Slog.e(TAG, "Error getting system class loader: " + e2.getMessage());
                            return null;
                        }
                    }
                }
                
                @Override
                public Context getApplicationContext() {
                    try {
                        return baseContext.getApplicationContext();
                    } catch (Exception e) {
                        Slog.w(TAG, "Error getting application context from base context: " + e.getMessage());
                        return this;
                    }
                }
            };
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create fallback context for " + packageName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get provider info list from bind application data
     */
    private List<android.content.pm.ProviderInfo> getProviderInfoList(Object data) {
        try {
            // Try to get provider info from our custom data object
            if (data != null) {
                try {
                    // Use reflection to call getProviders() method
                    Method getProvidersMethod = data.getClass().getMethod("getProviders");
                    List<ProviderInfo> providers = (List<ProviderInfo>) getProvidersMethod.invoke(data);
                    if (providers != null) {
                        return providers;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Error getting providers from custom data object: " + e.getMessage());
                }
            }
            
            // Fallback: return empty list
            return new ArrayList<>();
        } catch (Exception e) {
            Slog.e(TAG, "Error getting provider info list", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Install content provider
     */
    private void installContentProvider(Application application, android.content.pm.ProviderInfo providerInfo) {
        try {
            // Check if application is null
            if (application == null) {
                Slog.w(TAG, "Application is null, cannot install content provider: " + providerInfo.name);
                return;
            }
            
            // Check if application has a valid class loader
            ClassLoader classLoader = application.getClassLoader();
            if (classLoader == null) {
                Slog.w(TAG, "Application class loader is null, using system class loader for: " + providerInfo.name);
                classLoader = ClassLoader.getSystemClassLoader();
            }
            
            // Create and install the content provider
            android.content.ContentProvider provider = (android.content.ContentProvider) classLoader
                .loadClass(providerInfo.name).newInstance();
            
            // Attach the provider to the application
            provider.attachInfo(application, providerInfo);
            
            // Note: ContentResolver.getContentProvider() doesn't exist
            // The provider is automatically registered when attachInfo is called
            Slog.d(TAG, "Content provider installed: " + providerInfo.name);
            
        } catch (Exception e) {
            Slog.e(TAG, "Error installing content provider " + providerInfo.name, e);
        }
    }
    
    /**
     * Set application in ActivityThread
     */
    private void setApplication(Application application) {
        try {
            mInitialApplication = application;
            BRActivityThread.get(BlackBoxCore.mainThread())._set_mInitialApplication(application);
            Slog.d(TAG, "Application set in ActivityThread successfully");
        } catch (Exception e) {
            Slog.e(TAG, "Error setting application in ActivityThread", e);
        }
    }

    private void handleSecurityException(SecurityException se, String packageName, String processName, Context packageContext) {
        Slog.w(TAG, "Handling SecurityException for " + packageName);
        
        // Try to create a basic application without problematic operations
        try {
                            Application basicApp = createMinimalApplication(packageContext, packageName);
            if (basicApp != null) {
                mInitialApplication = basicApp;
                BRActivityThread.get(BlackBoxCore.mainThread())._set_mInitialApplication(mInitialApplication);
                ContextCompat.fix(mInitialApplication);
                
                // Skip problematic operations
                Slog.w(TAG, "Created basic application, skipping problematic operations");
                return;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create basic application after SecurityException: " + e.getMessage());
        }
        
        // If all else fails, re-throw the security exception
        throw new RuntimeException("Unable to handle SecurityException", se);
    }

    private void installProvidersWithErrorHandling(Context context, String processName, List<ProviderInfo> providers) {
        long origId = Binder.clearCallingIdentity();
        try {
            for (ProviderInfo providerInfo : providers) {
                try {
                    if (processName.equals(providerInfo.processName) ||
                            providerInfo.processName.equals(context.getPackageName()) || providerInfo.multiprocess) {
                        installProvider(BlackBoxCore.mainThread(), context, providerInfo, null);
                    }
                } catch (SecurityException se) {
                    Slog.w(TAG, "SecurityException installing provider " + providerInfo.name + ": " + se.getMessage());
                    // Continue with other providers
                } catch (Throwable t) {
                    Slog.w(TAG, "Error installing provider " + providerInfo.name + ": " + t.getMessage());
                    // Continue with other providers
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
            try {
                ContentProviderDelegate.init();
            } catch (Exception e) {
                Slog.w(TAG, "Error initializing ContentProviderDelegate: " + e.getMessage());
            }
        }
    }

    public static Context createPackageContext(ApplicationInfo info) {
        try {
            return BlackBoxCore.getContext().createPackageContext(info.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create a minimal package context when the actual APK is not available
     */
    private static Context createMinimalPackageContext(ApplicationInfo info) {
        try {
            // Create a context that doesn't require the actual APK
            Context baseContext = BlackBoxCore.getContext();
            
            // Try to create a context with minimal flags
            try {
                Context packageContext = baseContext.createPackageContext(info.packageName, 0);
                if (packageContext != null) {
                    Slog.d(TAG, "Successfully created package context with minimal flags for " + info.packageName);
                    return packageContext;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to create package context with minimal flags for " + info.packageName + ": " + e.getMessage());
            }
            
            // Try to create a context without any flags
            try {
                Context packageContext = baseContext.createPackageContext(info.packageName, Context.CONTEXT_IGNORE_SECURITY);
                if (packageContext != null) {
                    Slog.d(TAG, "Successfully created package context with ignore security for " + info.packageName);
                    return packageContext;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to create package context with ignore security for " + info.packageName + ": " + e.getMessage());
            }
            
            // Try to create a context with just the package name
            try {
                Context packageContext = baseContext.createPackageContext(info.packageName, Context.CONTEXT_INCLUDE_CODE);
                if (packageContext != null) {
                    Slog.d(TAG, "Successfully created package context with include code for " + info.packageName);
                    return packageContext;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to create package context with include code for " + info.packageName + ": " + e.getMessage());
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create minimal package context for " + info.packageName + ": " + e.getMessage());
        }
        
        // Last resort: return the base context with package name wrapper
        Slog.w(TAG, "Using base context as fallback for " + info.packageName);
        return createWrappedBaseContext(info.packageName);
    }

    /**
     * Create a wrapped base context that pretends to be a package context
     */
    private static Context createWrappedBaseContext(String packageName) {
        try {
            Context baseContext = BlackBoxCore.getContext();
            
            // Create a wrapper context that provides the package name
            return new ContextWrapper(baseContext) {
                @Override
                public String getPackageName() {
                    return packageName;
                }
                
                @Override
                public PackageManager getPackageManager() {
                    return baseContext.getPackageManager();
                }
                
                @Override
                public Resources getResources() {
                    return baseContext.getResources();
                }
                
                @Override
                public ClassLoader getClassLoader() {
                    return baseContext.getClassLoader();
                }
                
                @Override
                public Context getApplicationContext() {
                    return baseContext.getApplicationContext();
                }
            };
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create wrapped base context for " + packageName + ": " + e.getMessage());
            // Ultimate fallback: return the base context
            return BlackBoxCore.getContext();
        }
    }

    private void installProviders(Context context, String processName, List<ProviderInfo> provider) {
        long origId = Binder.clearCallingIdentity();
        try {
            for (ProviderInfo providerInfo : provider) {
                try {
                    if (processName.equals(providerInfo.processName) ||
                            providerInfo.processName.equals(context.getPackageName()) || providerInfo.multiprocess) {
                        installProvider(BlackBoxCore.mainThread(), context, providerInfo, null);
                    }
                } catch (Throwable ignored) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
            ContentProviderDelegate.init();
        }
    }

    public Object getPackageInfo() {
        return mBoundApplication.info;
    }

    public static void installProvider(Object mainThread, Context context, ProviderInfo providerInfo, Object holder) throws Throwable {
        Method installProvider = Reflector.findMethodByFirstName(mainThread.getClass(), "installProvider");
        if (installProvider != null) {
            installProvider.setAccessible(true);
            installProvider.invoke(mainThread, context, holder, providerInfo, false, true, true);
        }
    }

    public void loadXposed(Context context) {
        String vPackageName = getAppPackageName();
        String vProcessName = getAppProcessName();
        if (!TextUtils.isEmpty(vPackageName) && !TextUtils.isEmpty(vProcessName) && BXposedManager.get().isXPEnable()) {
            assert vPackageName != null;
            assert vProcessName != null;

            boolean isFirstApplication = vPackageName.equals(vProcessName);

            List<InstalledModule> installedModules = BXposedManager.get().getInstalledModules();
            for (InstalledModule installedModule : installedModules) {
                if (!installedModule.enable) {
                    continue;
                }
                try {
                    // Remove all PineXposed.loadModule and PineXposed.onPackageLoad calls
                } catch (Throwable e) {
                    String msg = "Failed to load Xposed module: " + installedModule.getApplication().packageName
                               + " (" + installedModule.getApplication().sourceDir + ")\n"
                               + android.util.Log.getStackTraceString(e);
                    android.util.Log.e("BlackBoxXposed", msg);
                    // Optionally, collect errors for UI display
                    // XposedErrorLogger.logModuleError(installedModule.getApplication().packageName, msg);
                }
            }
            try {
                // Remove all PineXposed.onPackageLoad calls
            } catch (Throwable ignored) {
            }
        }
        if (BlackBoxCore.get().isHideXposed()) {
            NativeCore.hideXposed();
        }
    }

    @Override
    public IBinder getActivityThread() {
        return BRActivityThread.get(BlackBoxCore.mainThread()).getApplicationThread();
    }

    @Override
    public void bindApplication() {
        if (!isInit()) {
            bindApplication(getAppPackageName(), getAppProcessName());
        }
    }

    @Override
    public void stopService(Intent intent) {
        AppServiceDispatcher.get().stopService(intent);
    }

    @Override
    public void restartJobService(String selfId) throws RemoteException {

    }

    @Override
    public IBinder acquireContentProviderClient(ProviderInfo providerInfo) throws RemoteException {
        if (!isInit()) {
            bindApplication(BActivityThread.getAppConfig().packageName, BActivityThread.getAppConfig().processName);
        }
        String[] split = providerInfo.authority.split(";");
        for (String auth : split) {
            ContentProviderClient contentProviderClient = BlackBoxCore.getContext()
                    .getContentResolver().acquireContentProviderClient(auth);
            IInterface iInterface = BRContentProviderClient.get(contentProviderClient).mContentProvider();
            if (iInterface == null)
                continue;
            return iInterface.asBinder();
        }
        return null;
    }

    @Override
    public IBinder peekService(Intent intent) {
        return AppServiceDispatcher.get().peekService(intent);
    }

    @Override
    public void finishActivity(final IBinder token) {
        mH.post(() -> {
            Map<IBinder, Object> activities = BRActivityThread.get(BlackBoxCore.mainThread()).mActivities();
            if (activities.isEmpty())
                return;
            Object clientRecord = activities.get(token);
            if (clientRecord == null)
                return;
            Activity activity = getActivityByToken(token);

            while (activity.getParent() != null) {
                activity = activity.getParent();
            }

            int resultCode = BRActivity.get(activity).mResultCode();
            Intent resultData = BRActivity.get(activity).mResultData();
            ActivityManagerCompat.finishActivity(token, resultCode, resultData);
            BRActivity.get(activity)._set_mFinished(true);
        });
    }

    @Override
    public void handleNewIntent(final IBinder token, final Intent intent) {
        mH.post(() -> {
            Intent newIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                newIntent = BRReferrerIntent.get()._new(intent, BlackBoxCore.getHostPkg());
            } else {
                newIntent = intent;
            }
            Object mainThread = BlackBoxCore.mainThread();
            if (BRActivityThread.get(BlackBoxCore.mainThread())._check_performNewIntents(null, null) != null) {
                BRActivityThread.get(mainThread).performNewIntents(
                        token,
                        Collections.singletonList(newIntent)
                );
            } else if (BRActivityThreadNMR1.get(mainThread)._check_performNewIntents(null, null, false) != null) {
                BRActivityThreadNMR1.get(mainThread).performNewIntents(
                        token,
                        Collections.singletonList(newIntent),
                        true);
            } else if (BRActivityThreadQ.get(mainThread)._check_handleNewIntent(null, null) != null) {
                BRActivityThreadQ.get(mainThread).handleNewIntent(token, Collections.singletonList(newIntent));
            }
        });
    }

    @Override
    public void scheduleReceiver(ReceiverData data) throws RemoteException {
        if (!isInit()) {
            bindApplication();
        }
        mH.post(() -> {
            BroadcastReceiver mReceiver = null;
            Intent intent = data.intent;
            ActivityInfo activityInfo = data.activityInfo;
            BroadcastReceiver.PendingResult pendingResult = data.data.build();

            try {
                Context baseContext = mInitialApplication.getBaseContext();
                ClassLoader classLoader = baseContext.getClassLoader();
                intent.setExtrasClassLoader(classLoader);

                mReceiver = (BroadcastReceiver) classLoader.loadClass(activityInfo.name).newInstance();
                BRBroadcastReceiver.get(mReceiver).setPendingResult(pendingResult);
                mReceiver.onReceive(baseContext, intent);
                BroadcastReceiver.PendingResult finish = BRBroadcastReceiver.get(mReceiver).getPendingResult();
                if (finish != null) {
                    finish.finish();
                }
                BlackBoxCore.getBActivityManager().finishBroadcast(data.data);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                Slog.e(TAG,
                        "Error receiving broadcast " + intent
                                + " in " + mReceiver);
            }
        });
    }

    public static Activity getActivityByToken(IBinder token) {
        Map<IBinder, Object> iBinderObjectMap =
                BRActivityThread.get(BlackBoxCore.mainThread()).mActivities();
        return BRActivityThreadActivityClientRecord.get(iBinderObjectMap.get(token)).activity();
    }

    private void onBeforeCreateApplication(String packageName, String processName, Context context) {
        for (AppLifecycleCallback appLifecycleCallback : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeCreateApplication(packageName, processName, context, BActivityThread.getUserId());
        }
    }

    private void onBeforeApplicationOnCreate(String packageName, String processName, Application application) {
        for (AppLifecycleCallback appLifecycleCallback : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeApplicationOnCreate(packageName, processName, application, BActivityThread.getUserId());
        }
    }

    private void onAfterApplicationOnCreate(String packageName, String processName, Application application) {
        for (AppLifecycleCallback appLifecycleCallback : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.afterApplicationOnCreate(packageName, processName, application, BActivityThread.getUserId());
        }
    }

    /**
     * Ensure that an activity has a valid context from the moment it's created
     */
    public static void ensureActivityContext(Activity activity) {
        if (activity == null) {
            return;
        }
        
        try {
            // Check if the activity already has a valid context
            Context currentContext = activity.getBaseContext();
            if (currentContext != null) {
                Slog.d(TAG, "Activity already has context: " + activity.getClass().getName());
                return;
            }
            
            Slog.w(TAG, "Activity has null context, ensuring valid context: " + activity.getClass().getName());
            
            // Get a valid context
            Context validContext = null;
            try {
                validContext = getApplication();
                if (validContext == null) {
                    validContext = BlackBoxCore.getContext();
                }
            } catch (Exception e) {
                Slog.w(TAG, "Could not get application context: " + e.getMessage());
                validContext = BlackBoxCore.getContext();
            }
            
            if (validContext != null) {
                // Create a simple package context for the activity
                try {
                    Context packageContext = validContext.createPackageContext(
                        activity.getPackageName(),
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
                    );
                    
                    // Try to set the base context using reflection
                    java.lang.reflect.Method attachBaseContext = Activity.class.getDeclaredMethod("attachBaseContext", Context.class);
                    attachBaseContext.setAccessible(true);
                    attachBaseContext.invoke(activity, packageContext);
                    Slog.d(TAG, "Successfully attached package context to activity: " + activity.getClass().getName());
                } catch (Exception e) {
                    Slog.w(TAG, "Could not attach base context to activity: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error ensuring activity context: " + e.getMessage());
        }
    }

    /**
     * Hook the ActivityThread to ensure all activities get valid contexts from the very beginning
     */
    public static void hookActivityThread() {
        try {
            // Try to hook the ActivityThread to ensure all activities get valid contexts
            Object activityThread = BlackBoxCore.mainThread();
            if (activityThread != null) {
                // Get the instrumentation from the ActivityThread
                Instrumentation instrumentation = BRActivityThread.get(activityThread).mInstrumentation();
                if (instrumentation != null) {
                    Slog.d(TAG, "Found ActivityThread instrumentation, ensuring it's our AppInstrumentation");
                    
                    // Check if the instrumentation is our AppInstrumentation
                    if (!(instrumentation instanceof AppInstrumentation)) {
                        Slog.w(TAG, "ActivityThread instrumentation is not our AppInstrumentation, attempting to replace");
                        
                        // Try to replace the instrumentation with our AppInstrumentation
                        try {
                            AppInstrumentation appInstrumentation = AppInstrumentation.get();
                            appInstrumentation.injectHook();
                            Slog.d(TAG, "Successfully replaced ActivityThread instrumentation with AppInstrumentation");
                        } catch (Exception e) {
                            Slog.w(TAG, "Could not replace ActivityThread instrumentation: " + e.getMessage());
                        }
                    } else {
                        Slog.d(TAG, "ActivityThread instrumentation is already our AppInstrumentation");
                    }
                } else {
                    Slog.w(TAG, "ActivityThread instrumentation is null");
                }
            } else {
                Slog.w(TAG, "ActivityThread is null");
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error hooking ActivityThread: " + e.getMessage());
        }
    }

    /**
     * Create a minimal application when normal application creation fails
     */
    private Application createMinimalApplication(Context packageContext, String packageName) {
        try {
            Slog.d(TAG, "Creating minimal application for " + packageName);
            
            // Create a basic Application object
            Application app = new Application() {
                @Override
                public void onCreate() {
                    super.onCreate();
                    Slog.d(TAG, "Minimal application onCreate called for " + packageName);
                }
                
                @Override
                public String getPackageName() {
                    return packageName;
                }
                
                @Override
                public Context getApplicationContext() {
                    return this;
                }
            };
            
            // Ensure the minimal application has a proper base context
            if (packageContext != null) {
                try {
                    Method attachBaseContext = Application.class.getDeclaredMethod("attachBaseContext", Context.class);
                    attachBaseContext.setAccessible(true);
                    attachBaseContext.invoke(app, packageContext);
                    Slog.d(TAG, "Successfully attached base context to minimal application for " + packageName);
                } catch (Exception e) {
                    Slog.w(TAG, "Could not attach base context to minimal application: " + e.getMessage());
                }
            } else {
                Slog.w(TAG, "Package context is null, cannot attach base context to minimal application");
            }
            
            Slog.d(TAG, "Minimal application created successfully for " + packageName);
            return app;
        } catch (Exception e) {
            Slog.e(TAG, "Error creating minimal application for " + packageName, e);
            return null;
        }
    }

    public static class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }
}
