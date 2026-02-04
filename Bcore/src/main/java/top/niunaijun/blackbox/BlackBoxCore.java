package top.niunaijun.blackbox;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import black.android.app.BRActivityThread;
import black.android.os.BRUserHandle;
import me.weishu.reflection.Reflection;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.app.LauncherActivity;
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;
import top.niunaijun.blackbox.core.GmsCore;
import top.niunaijun.blackbox.core.NativeCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.DaemonService;
import top.niunaijun.blackbox.core.system.ServiceManager;
import top.niunaijun.blackbox.core.system.user.BUserHandle;
import top.niunaijun.blackbox.core.system.user.BUserInfo;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.entity.pm.InstallResult;

import top.niunaijun.blackbox.fake.delegate.ContentProviderDelegate;
import top.niunaijun.blackbox.fake.frameworks.BActivityManager;
import top.niunaijun.blackbox.fake.frameworks.BJobManager;
import top.niunaijun.blackbox.fake.frameworks.BPackageManager;
import top.niunaijun.blackbox.fake.frameworks.BStorageManager;
import top.niunaijun.blackbox.fake.frameworks.BUserManager;

import top.niunaijun.blackbox.fake.hook.HookManager;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.ShellUtils;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.SimpleCrashFix;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.BundleCompat;

import top.niunaijun.blackbox.utils.provider.ProviderCall;
import top.niunaijun.blackbox.utils.StackTraceFilter;
import top.niunaijun.blackbox.utils.SocialMediaAppCrashPrevention;
import top.niunaijun.blackbox.utils.DexCrashPrevention;
import top.niunaijun.blackbox.utils.NativeCrashPrevention;
import top.niunaijun.blackbox.utils.CrashMonitor;
import top.niunaijun.blackbox.utils.StoragePermissionHelper;
import top.niunaijun.blackbox.utils.LogSender;



@SuppressLint({"StaticFieldLeak", "NewApi"})
@SuppressWarnings({"unchecked", "deprecation"})
public class BlackBoxCore extends ClientConfiguration {
    public static final String TAG = "BlackBoxCore";

    private static final BlackBoxCore sBlackBoxCore = new BlackBoxCore();
    private static Context sContext;
    
    
    static {
        try {
            
            SimpleCrashFix.installSimpleFix();
            Slog.d(TAG, "Simple crash fix installed at class loading time");
            
            StackTraceFilter.install();
            Slog.d(TAG, "Stack trace filter installed at class loading time");
            
            SocialMediaAppCrashPrevention.initialize();
            Slog.d(TAG, "Social media app crash prevention initialized at class loading time");
            
            DexCrashPrevention.initialize();
            Slog.d(TAG, "DEX crash prevention initialized at class loading time");
            
            NativeCrashPrevention.initialize();
            Slog.d(TAG, "Native crash prevention initialized at class loading time");
            
            CrashMonitor.initialize();
            Slog.d(TAG, "Comprehensive crash monitoring initialized at class loading time");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install simple crash fix or stack trace filter at class loading: " + e.getMessage());
        }
    }
    private ProcessType mProcessType;
    private final Map<String, IBinder> mServices = new HashMap<>();
    private Thread.UncaughtExceptionHandler mExceptionHandler;
    private ClientConfiguration mClientConfiguration;
    private final List<AppLifecycleCallback> mAppLifecycleCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final int mHostUid = Process.myUid();
    private final int mHostUserId = BRUserHandle.get().myUserId();

    private boolean mServicesInitialized = false;
    private long mLastServiceInitAttempt = 0;
    private static final long SERVICE_INIT_TIMEOUT_MS = 10000; 
    
    
    private final List<Runnable> mServiceAvailableCallbacks = new ArrayList<>();
    private final Object mServiceCallbackLock = new Object();

    
    private int mCurrentAppUid = -1;
    private String mCurrentAppPackage = null;
    private boolean mIsSandboxedEnvironment = false;

    public static BlackBoxCore get() {
        return sBlackBoxCore;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public static PackageManager getPackageManager() {
        return sContext.getPackageManager();
    }

    public static String getHostPkg() {
        return get().getHostPackageName();
    }

    public static int getHostUid() {
        return get().mHostUid;
    }

    public static int getHostUserId() {
        return get().mHostUserId;
    }

    public static Context getContext() {
        return sContext;
    }

    public Thread.UncaughtExceptionHandler getExceptionHandler() {
        return mExceptionHandler;
    }

    public void setExceptionHandler(Thread.UncaughtExceptionHandler exceptionHandler) {
        mExceptionHandler = exceptionHandler;
    }

    
    public void setCurrentAppUid(int uid, String packageName) {
        mCurrentAppUid = uid;
        mCurrentAppPackage = packageName;
        
        
        if (uid != mHostUid && uid > Process.FIRST_APPLICATION_UID && uid < Process.LAST_APPLICATION_UID) {
            mIsSandboxedEnvironment = true;
            Slog.d("BlackBoxCore", "Detected sandboxed environment for " + packageName + " with UID: " + uid);
        }
    }

    public int getCurrentAppUid() {
        return mCurrentAppUid > 0 ? mCurrentAppUid : mHostUid;
    }

    public String getCurrentAppPackage() {
        return mCurrentAppPackage != null ? mCurrentAppPackage : getHostPackageName();
    }

    public boolean isSandboxedEnvironment() {
        return mIsSandboxedEnvironment;
    }

    
    public int resolveUidForOperation(int originalUid, String operation) {
        try {
            
            if (originalUid > 0 && originalUid < Process.FIRST_APPLICATION_UID) {
                return originalUid;
            }
            
            
            if (originalUid > Process.LAST_APPLICATION_UID) {
                return originalUid;
            }

            
            if (mIsSandboxedEnvironment && mCurrentAppUid > 0) {
                Slog.d("BlackBoxCore", "Resolving UID for " + operation + ": " + originalUid + " -> " + mCurrentAppUid);
                return mCurrentAppUid;
            }

            
            return originalUid;
        } catch (Exception e) {
            Slog.e("BlackBoxCore", "Error resolving UID for " + operation + ": " + e.getMessage());
            return originalUid;
        }
    }

    public boolean areServicesAvailable() {
        if (mServicesInitialized) {
            return true;
        }
        
        try {
            
            if (isMainProcess() && !isBlackProcessRunning()) {
                Slog.w(TAG, "Black process not running, starting it and using fallback services...");
                startBlackProcess();
                
                
                String[] serviceNames = {
                    ServiceManager.ACTIVITY_MANAGER,
                    ServiceManager.PACKAGE_MANAGER,
                    ServiceManager.STORAGE_MANAGER,
                    ServiceManager.USER_MANAGER,
                    ServiceManager.JOB_MANAGER,

                    ServiceManager.ACCOUNT_MANAGER,
                    ServiceManager.LOCATION_MANAGER,
                    ServiceManager.NOTIFICATION_MANAGER
                };
                
                for (String serviceName : serviceNames) {
                    try {
                        IBinder service = createFallbackService(serviceName);
                        if (service != null) {
                            mServices.put(serviceName, service);
                        }
                    } catch (Exception e) {
                        Slog.w(TAG, "Failed to create fallback service: " + serviceName, e);
                    }
                }
                
                mServicesInitialized = true;
                Slog.d(TAG, "Services initialized with fallbacks");
                
                notifyServiceAvailableCallbacks();
                return true;
            }
            
            
            String[] serviceNames = {
                ServiceManager.ACTIVITY_MANAGER,
                ServiceManager.PACKAGE_MANAGER,
                ServiceManager.STORAGE_MANAGER,
                ServiceManager.USER_MANAGER,
                ServiceManager.JOB_MANAGER,

                ServiceManager.ACCOUNT_MANAGER,
                ServiceManager.LOCATION_MANAGER,
                ServiceManager.NOTIFICATION_MANAGER
            };
            
            for (String serviceName : serviceNames) {
                try {
                    getServiceInternal(serviceName);
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to initialize service: " + serviceName + ", trying fallback", e);
                    
                    IBinder fallbackService = createFallbackService(serviceName);
                    if (fallbackService != null) {
                        mServices.put(serviceName, fallbackService);
                    }
                }
            }
            
            Slog.d(TAG, "Services initialized successfully");
            mServicesInitialized = true;
            
            notifyServiceAvailableCallbacks();
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize services, using fallbacks: " + e.getMessage());
            
            try {
                String[] serviceNames = {
                    ServiceManager.ACTIVITY_MANAGER,
                    ServiceManager.PACKAGE_MANAGER,
                    ServiceManager.STORAGE_MANAGER,
                    ServiceManager.USER_MANAGER,
                    ServiceManager.JOB_MANAGER,

                    ServiceManager.ACCOUNT_MANAGER,
                    ServiceManager.LOCATION_MANAGER,
                    ServiceManager.NOTIFICATION_MANAGER
                };
                
                for (String serviceName : serviceNames) {
                    try {
                        IBinder service = createFallbackService(serviceName);
                        if (service != null) {
                            mServices.put(serviceName, service);
                        }
                    } catch (Exception fallbackEx) {
                        Slog.w(TAG, "Failed to create fallback service: " + serviceName, fallbackEx);
                    }
                }
                
                mServicesInitialized = true;
                Slog.d(TAG, "Services initialized with fallbacks after error");
                
                notifyServiceAvailableCallbacks();
                return true;
            } catch (Exception fallbackEx) {
                Slog.e(TAG, "Failed to create fallback services", fallbackEx);
                mServicesInitialized = false;
                return false;
            }
        }
        return true;
    }

    public IBinder getService(String name) {
        if (!areServicesAvailable()) {
            Slog.w(TAG, "Services not available, skipping service request: " + name);
            return null;
        }
        
        return getServiceInternal(name);
    }
    
    private IBinder getServiceInternal(String name) {
        IBinder binder = mServices.get(name);
        if (binder != null && binder.isBinderAlive()) {
            return binder;
        }
        
        
        
        if (isMainProcess() && !isBlackProcessRunning()) {
            Slog.w(TAG, "Main process trying to access service " + name + " but black process not running, starting it...");
            startBlackProcess();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        
        long startTime = System.currentTimeMillis();
        long timeout = 3000; 
        
        try {
            Bundle bundle = new Bundle();
            bundle.putString("_B_|_server_name_", name);
            
            
            Bundle vm = null;
            try {
                vm = ProviderCall.callSafely(ProxyManifest.getBindProvider(), "VM", null, bundle);
            } catch (Exception e) {
                
                if (System.currentTimeMillis() - startTime > timeout) {
                    Slog.w(TAG, "Provider call timeout for service: " + name + ", using fallback");
                    return createFallbackService(name);
                }
                throw e;
            }
            
            if (vm == null) {
                Slog.w(TAG, "Provider call returned null for service: " + name);
                
                return createFallbackService(name);
            }
            
            binder = BundleCompat.getBinder(vm, "_B_|_server_");
            Slog.d(TAG, "getService: " + name + ", " + binder);
            if (binder != null) {
                mServices.put(name, binder);
            } else {
                Slog.w(TAG, "Failed to get binder for service: " + name);
                
                return createFallbackService(name);
            }
            return binder;
        } catch (Exception e) {
            Slog.e(TAG, "Error getting service: " + name + ", creating fallback: " + e.getMessage());
            return createFallbackService(name);
        }
    }
    
    
    private IBinder createFallbackService(String name) {
        try {
            
            
            Slog.w(TAG, "No fallback available for service: " + name + " (avoiding circular dependency)");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Error creating fallback service for " + name, e);
            return null;
        }
    }
    

    
    
    private boolean isBlackProcessRunning() {
        try {
            
            Bundle testBundle = new Bundle();
            testBundle.putString("_B_|_server_name_", "test");
            
            
            try {
                Bundle result = ProviderCall.callSafely(ProxyManifest.getBindProvider(), "VM", null, testBundle);
                if (result != null) {
                    Slog.d(TAG, "Black process is running - SystemCallProvider accessible");
                    return true;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Provider call failed: " + e.getMessage());
            }
            
            
            try {
                String authority = ProxyManifest.getBindProvider();
                if (authority != null && !authority.isEmpty()) {
                    
                    android.content.pm.ProviderInfo providerInfo = getContext().getPackageManager()
                        .resolveContentProvider(authority, 0);
                    if (providerInfo != null) {
                        Slog.d(TAG, "Provider exists but call failed - black process may be starting");
                        return false; 
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Provider resolution failed: " + e.getMessage());
            }
            
            
            try {
                android.app.ActivityManager am = (android.app.ActivityManager) getContext()
                    .getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    for (android.app.ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
                        if (service.service.getClassName().contains("DaemonService")) {
                            Slog.d(TAG, "DaemonService is running - black process active");
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Service check failed: " + e.getMessage());
            }
            
            Slog.d(TAG, "Black process is not running");
            return false;
            
        } catch (Exception e) {
            Slog.w(TAG, "Error checking black process status: " + e.getMessage());
            return false;
        }
    }
    
    
    private void startBlackProcess() {
        try {
            Slog.d(TAG, "Starting black process...");
            
            
            if (!isValidProcessState()) {
                Slog.w(TAG, "Process state is invalid, delaying service start");
                
                scheduleDelayedServiceStart();
                return;
            }
            
            
            Intent intent = new Intent();
            intent.setClass(getContext(), DaemonService.class);
            
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            boolean serviceStarted = false;
            int maxRetries = 3;
            
            for (int retry = 0; retry < maxRetries && !serviceStarted; retry++) {
                try {
                    if (retry > 0) {
                        Slog.d(TAG, "Retry attempt " + (retry + 1) + " for starting DaemonService");
                        
                        if (retry == 1) {
                            
                            Slog.d(TAG, "First retry - continuing immediately");
                        } else {
                            
                            scheduleDelayedRetry(intent, retry);
                            return;
                        }
                    }
                    
                    if (BuildCompat.isOreo()) {
                        getContext().startForegroundService(intent);
                        Slog.d(TAG, "Started DaemonService as foreground service");
                        serviceStarted = true;
                    } else {
                        getContext().startService(intent);
                        Slog.d(TAG, "Started DaemonService as regular service");
                        serviceStarted = true;
                    }
                    
                } catch (SecurityException e) {
                    if (e.getMessage() != null && e.getMessage().contains("MissingForegroundServiceTypeException")) {
                        Slog.w(TAG, "Foreground service type missing, falling back to regular service");
                        try {
                            getContext().startService(intent);
                            Slog.d(TAG, "Started DaemonService as regular service (fallback)");
                            serviceStarted = true;
                        } catch (Exception fallbackEx) {
                            Slog.e(TAG, "Failed to start DaemonService even as regular service: " + fallbackEx.getMessage(), fallbackEx);
                            handleServiceStartFailure(retry, maxRetries, e);
                        }
                    } else if (e.getMessage() != null && e.getMessage().contains("process is bad")) {
                        Slog.w(TAG, "Process is bad, attempting to recover and retry");
                        handleProcessBadError(retry, maxRetries);
                    } else {
                        Slog.e(TAG, "Security exception starting DaemonService: " + e.getMessage(), e);
                        handleServiceStartFailure(retry, maxRetries, e);
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to start DaemonService: " + e.getMessage(), e);
                    handleServiceStartFailure(retry, maxRetries, e);
                }
            }
            
            if (!serviceStarted) {
                Slog.e(TAG, "Failed to start DaemonService after " + maxRetries + " attempts");
                
                tryAlternativeStartupMethods();
                return;
            }
            
            
            scheduleProviderCheck();
            
            Slog.d(TAG, "Started DaemonService to initialize black process");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to start black process", e);
            
            scheduleDelayedServiceStart();
        }
    }
    
    
    private boolean isValidProcessState() {
        try {
            
            if (getContext() == null) {
                Slog.w(TAG, "Context is null, process state invalid");
                return false;
            }
            
            
            if (!isMainProcess()) {
                Slog.w(TAG, "Not in main process, skipping service start");
                return false;
            }
            
            
            try {
                getContext().getPackageName();
            } catch (Exception e) {
                Slog.w(TAG, "Package name access failed, process state invalid: " + e.getMessage());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Process state validation failed: " + e.getMessage());
            return false;
        }
    }
    
    
    private void scheduleDelayedRetry(Intent intent, int retry) {
        try {
            int delayMs = 1000 * retry;
            Slog.d(TAG, "Scheduling delayed retry in " + delayMs + "ms");
            
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Slog.d(TAG, "Executing delayed retry for DaemonService");
                        if (BuildCompat.isOreo()) {
                            getContext().startForegroundService(intent);
                        } else {
                            getContext().startService(intent);
                        }
                        Slog.d(TAG, "Delayed retry successful");
                    } catch (Exception e) {
                        Slog.e(TAG, "Delayed retry failed: " + e.getMessage());
                        
                        tryAlternativeStartupMethods();
                    }
                }
            }, delayMs);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule delayed retry: " + e.getMessage());
            
            tryAlternativeStartupMethods();
        }
    }
    
    
    private void scheduleProviderCheck() {
        try {
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        
                        Bundle testBundle = new Bundle();
                        testBundle.putString("_B_|_server_name_", "test");
                        Bundle result = ProviderCall.callSafely(ProxyManifest.getBindProvider(), "VM", null, testBundle);
                        if (result != null) {
                            Slog.d(TAG, "Black process started successfully, SystemCallProvider is accessible");
                        } else {
                            Slog.w(TAG, "Black process started but SystemCallProvider is not accessible yet");
                        }
                    } catch (Exception e) {
                        Slog.w(TAG, "SystemCallProvider not accessible yet, will retry later: " + e.getMessage());
                    }
                }
            }, 1000); 
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule provider check: " + e.getMessage());
        }
    }
    
    
    private void handleServiceStartFailure(int retry, int maxRetries, Exception e) {
        if (retry < maxRetries - 1) {
            Slog.w(TAG, "Service start failed, will retry. Attempt " + (retry + 1) + " of " + maxRetries);
        } else {
            Slog.e(TAG, "Service start failed after " + maxRetries + " attempts: " + e.getMessage());
            
            tryAlternativeStartupMethods();
        }
    }
    
    
    private void handleProcessBadError(int retry, int maxRetries) {
        if (retry < maxRetries - 1) {
            Slog.w(TAG, "Process is bad, attempting recovery. Attempt " + (retry + 1) + " of " + maxRetries);
            
            
            try {
                
                scheduleProcessRecovery(retry, maxRetries);
                
            } catch (Exception e) {
                Slog.w(TAG, "Process recovery failed: " + e.getMessage());
            }
        } else {
            Slog.e(TAG, "Process recovery failed after " + maxRetries + " attempts");
            
            tryAlternativeStartupMethods();
        }
    }
    
    
    private void scheduleProcessRecovery(int retry, int maxRetries) {
        try {
            int delayMs = 2000; 
            Slog.d(TAG, "Scheduling process recovery in " + delayMs + "ms");
            
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Slog.d(TAG, "Executing process recovery");
                        
                        refreshProcessContext();
                        
                        
                        if (isMainProcess() && !isBlackProcessRunning()) {
                            startBlackProcess();
                        }
                        
                    } catch (Exception e) {
                        Slog.w(TAG, "Process recovery execution failed: " + e.getMessage());
                    }
                }
            }, delayMs);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule process recovery: " + e.getMessage());
        }
    }
    
    
    private void tryAlternativeStartupMethods() {
        Slog.w(TAG, "Trying alternative startup methods...");
        
        try {
            
            Context alternativeContext = getAlternativeContext();
            if (alternativeContext != null) {
                Intent intent = new Intent();
                intent.setClass(alternativeContext, DaemonService.class);
                alternativeContext.startService(intent);
                Slog.d(TAG, "Alternative context startup successful");
                return;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Alternative context startup failed: " + e.getMessage());
        }
        
        try {
            
            Context appContext = getContext().getApplicationContext();
            if (appContext != null && appContext != getContext()) {
                Intent intent = new Intent();
                intent.setClass(appContext, DaemonService.class);
                appContext.startService(intent);
                Slog.d(TAG, "Application context startup successful");
                return;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Application context startup failed: " + e.getMessage());
        }
        
        Slog.e(TAG, "All alternative startup methods failed");
    }
    
    
    private Context getAlternativeContext() {
        try {
            
            Context appContext = getContext().getApplicationContext();
            if (appContext != null) {
                return appContext;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get application context: " + e.getMessage());
        }
        
        return null;
    }
    
    
    private void refreshProcessContext() {
        try {
            
            
            Slog.d(TAG, "Attempting to refresh process context");
            
            
            
        } catch (Exception e) {
            Slog.w(TAG, "Context refresh failed: " + e.getMessage());
        }
    }
    
    
    private void scheduleDelayedServiceStart() {
        try {
            
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Slog.d(TAG, "Executing delayed service start");
                    if (isMainProcess() && !isBlackProcessRunning()) {
                        startBlackProcess();
                    }
                }
            }, 5000); 
            
            Slog.d(TAG, "Scheduled delayed service start in 5 seconds");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule delayed service start: " + e.getMessage());
        }
    }
    
    
    public void ensureBlackProcessInitialized() {
        if (isMainProcess() && !isBlackProcessRunning()) {
            Slog.w(TAG, "Ensuring black process is initialized...");
            startBlackProcess();
            
            
            int maxRetries = 5;
            int retryCount = 0;
            while (retryCount < maxRetries && !isBlackProcessRunning()) {
                try {
                    Thread.sleep(500);
                    retryCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (isBlackProcessRunning()) {
                Slog.d(TAG, "Black process initialized successfully");
            } else {
                Slog.w(TAG, "Black process failed to initialize, using fallback services");
            }
        }
    }

    public void doAttachBaseContext(Context context,
                                   ClientConfiguration clientConfiguration) {
        try {
            
            setEssentialProperties(context, clientConfiguration);
            
            
            try {
                Class<?> windowManagerClass = Class.forName("android.view.WindowManager");
                Field ignoreLeaksField = windowManagerClass.getDeclaredField("mIgnoreWindowLeaks");
                ignoreLeaksField.setAccessible(true);
                
            } catch (Exception e) {
                Slog.w(TAG, "Could not access WindowManager leak field: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set essential properties: " + e.getMessage());
        }

        sContext = context;
        mClientConfiguration = clientConfiguration;
        
        
        installSystemHooks();
        
        initNotificationManager();

        String processName = getProcessName(getContext());
        if (processName.equals(BlackBoxCore.getHostPkg())) {
            mProcessType = ProcessType.Main;
            startLogcat();
        } else if (processName.endsWith(getContext().getString(R.string.black_box_service_name))) {
            mProcessType = ProcessType.Server;
        } else {
            mProcessType = ProcessType.BAppClient;
        }
        if (BlackBoxCore.get().isBlackProcess()) {
            BEnvironment.load();
            if (processName.endsWith("p0")) {

            }

        }
        if (isServerProcess()) {
            if (clientConfiguration.isEnableDaemonService()) {
                try {
                    
                    if (!isValidProcessState()) {
                        Slog.w(TAG, "Server process state is invalid, delaying service start");
                        
                        scheduleDelayedServerServiceStart();
                        return;
                    }
                    
                    Intent intent = new Intent();
                    intent.setClass(getContext(), DaemonService.class);
                    
                    
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    boolean serviceStarted = false;
                    int maxRetries = 3;
                    
                    for (int retry = 0; retry < maxRetries && !serviceStarted; retry++) {
                        try {
                            if (retry > 0) {
                                Slog.d(TAG, "Retry attempt " + (retry + 1) + " for starting DaemonService in server process");
                                
                                if (retry == 1) {
                                    
                                    Slog.d(TAG, "First retry for server process - continuing immediately");
                                } else {
                                    
                                    scheduleDelayedServerRetry(intent, retry);
                                    return;
                                }
                            }
                            
                            if (BuildCompat.isOreo()) {
                                getContext().startForegroundService(intent);
                                Slog.d(TAG, "Started DaemonService as foreground service in server process");
                                serviceStarted = true;
                            } else {
                                getContext().startService(intent);
                                Slog.d(TAG, "Started DaemonService as regular service in server process");
                                serviceStarted = true;
                            }
                            
                        } catch (SecurityException e) {
                            if (e.getMessage() != null && e.getMessage().contains("MissingForegroundServiceTypeException")) {
                                Slog.w(TAG, "Foreground service type missing in server process, falling back to regular service");
                                try {
                                    getContext().startService(intent);
                                    Slog.d(TAG, "Started DaemonService as regular service in server process (fallback)");
                                    serviceStarted = true;
                                } catch (Exception fallbackEx) {
                                    Slog.e(TAG, "Failed to start DaemonService in server process even as regular service: " + fallbackEx.getMessage(), fallbackEx);
                                    handleServerServiceStartFailure(retry, maxRetries, fallbackEx);
                                }
                            } else if (e.getMessage() != null && e.getMessage().contains("process is bad")) {
                                Slog.w(TAG, "Server process is bad, attempting to recover and retry");
                                handleServerProcessBadError(retry, maxRetries);
                            } else {
                                Slog.e(TAG, "Security exception starting DaemonService in server process: " + e.getMessage(), e);
                                handleServerServiceStartFailure(retry, maxRetries, e);
                            }
                        } catch (Exception e) {
                            Slog.e(TAG, "Failed to start DaemonService in server process: " + e.getMessage(), e);
                            handleServerServiceStartFailure(retry, maxRetries, e);
                        }
                    }
                    
                    if (!serviceStarted) {
                        Slog.e(TAG, "Failed to start DaemonService in server process after " + maxRetries + " attempts");
                        
                        tryAlternativeServerStartupMethods();
                    }
                    
                } catch (Exception e) {
                    Slog.e(TAG, "Unexpected error starting DaemonService in server process: " + e.getMessage(), e);
                    
                    scheduleDelayedServerServiceStart();
                }
            }
        }
        
        
        initVpnService();
        
        HookManager.get().init();
    }

    public void doCreate() {
        
        installSystemHooks();
        
        
        long startTime = System.currentTimeMillis();
        long maxInitTime = 10000; 
        
        try {
            
            ensureBlackProcessInitialized();
            
            
            if (System.currentTimeMillis() - startTime > maxInitTime) {
                Slog.w(TAG, "Initialization timeout exceeded, proceeding with fallback services");
            }
            
            ensureProperInitialization();
            
            
            if (isBlackProcess()) {
                ContentProviderDelegate.init();
            }
            if (!isServerProcess()) {
                
                try {
                    ServiceManager.initBlackManager();
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to initialize ServiceManager, continuing with fallback: " + e.getMessage());
                }
                
                
                getBPackageManager().resetTransactionThrottler();
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            Slog.d(TAG, "BlackBox initialization completed in " + totalTime + "ms");
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            Slog.e(TAG, "BlackBox initialization failed after " + totalTime + "ms", e);
            
            
            try {
                if (!isServerProcess()) {
                    ServiceManager.initBlackManager();
                }
            } catch (Exception fallbackEx) {
                Slog.e(TAG, "Fallback initialization also failed", fallbackEx);
            }
        }
    }

    public static Object mainThread() {
        return BRActivityThread.get().currentActivityThread();
    }

    public void startActivity(Intent intent, int userId) {
        if (mClientConfiguration.isEnableLauncherActivity()) {
            LauncherActivity.launch(intent, userId);
        } else {
            getBActivityManager().startActivity(intent, userId);
        }
    }

    public static BJobManager getBJobManager() {
        return BJobManager.get();
    }

    public static BPackageManager getBPackageManager() {
        return BPackageManager.get();
    }

    public static BActivityManager getBActivityManager() {
        return BActivityManager.get();
    }

    public static BStorageManager getBStorageManager() {
        return BStorageManager.get();
    }
    
    
    
    
    public boolean hasStoragePermission() {
        return StoragePermissionHelper.hasStoragePermission(sContext);
    }
    
    
    public boolean hasAllFilesAccess() {
        return StoragePermissionHelper.hasAllFilesAccess();
    }
    
    
    public boolean hasFullFileAccess() {
        return StoragePermissionHelper.hasFullFileAccess(sContext);
    }
    
    
    public void requestStoragePermission(android.app.Activity activity) {
        StoragePermissionHelper.requestStoragePermission(activity);
    }
    
    
    public void requestAllFilesAccess(android.app.Activity activity) {
        StoragePermissionHelper.requestAllFilesAccess(activity);
    }
    
    
    public void requestFullFileAccess(android.app.Activity activity) {
        StoragePermissionHelper.requestFullFileAccess(activity);
    }
    
    
    public boolean handleStoragePermissionResult(android.app.Activity activity, int requestCode, 
            String[] permissions, int[] grantResults) {
        return StoragePermissionHelper.handlePermissionResult(activity, requestCode, permissions, grantResults);
    }
    
    
    public boolean handleAllFilesAccessResult(int requestCode) {
        return StoragePermissionHelper.handleAllFilesAccessResult(requestCode);
    }
    
    
    public static int getStoragePermissionRequestCode() {
        return StoragePermissionHelper.REQUEST_CODE_STORAGE_PERMISSION;
    }
    
    public static int getAllFilesAccessRequestCode() {
        return StoragePermissionHelper.REQUEST_CODE_MANAGE_STORAGE;
    }

    public boolean launchApk(String packageName, int userId) {
        onBeforeMainLaunchApk(packageName, userId);
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!hasAllFilesAccess()) {
                Slog.w(TAG, "All files access not granted for launching: " + packageName);
                
                for (AppLifecycleCallback callback : mAppLifecycleCallbacks) {
                    if (callback.onStoragePermissionNeeded(packageName, userId)) {
                        
                        Slog.d(TAG, "Launch cancelled - host app handling permission request");
                        return false;
                    }
                }
                
                Slog.w(TAG, "Launching without all files access - some file operations may fail");
            }
        }

        Intent launchIntentForPackage = getBPackageManager().getLaunchIntentForPackage(packageName, userId);
        if (launchIntentForPackage == null) {
            return false;
        }
        startActivity(launchIntentForPackage, userId);
        return true;
    }
    public boolean isInstalled(String packageName, int userId) {
        return getBPackageManager().isInstalled(packageName, userId);
    }

    public void uninstallPackageAsUser(String packageName, int userId) {
        getBPackageManager().uninstallPackageAsUser(packageName, userId);
    }

    public void uninstallPackage(String packageName) {
        getBPackageManager().uninstallPackage(packageName);
    }

    public InstallResult installPackageAsUser(String packageName, int userId) {
        try {
            
            if (packageName.equals(getHostPkg())) {
                return new InstallResult().installError("Cannot clone BlackBox app from within BlackBox. This would create infinite recursion and is not allowed for security reasons.");
            }
            
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
            return getBPackageManager().installPackageAsUser(packageInfo.applicationInfo.sourceDir, InstallOption.installBySystem(), userId);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return new InstallResult().installError(e.getMessage());
        }
    }

    public InstallResult installPackageAsUser(File apk, int userId) {
        
        try {
            PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            if (packageInfo != null) {
                String packageName = packageInfo.packageName;
                if (packageName.equals(getHostPkg())) {
                    return new InstallResult().installError("Cannot clone BlackBox app from within BlackBox. This would create infinite recursion and is not allowed for security reasons.");
                }
            }
        } catch (Exception e) {
            
            Slog.w(TAG, "Could not verify package info for APK: " + apk.getAbsolutePath());
        }
        
        return getBPackageManager().installPackageAsUser(apk.getAbsolutePath(), InstallOption.installByStorage(), userId);
    }

    public InstallResult installPackageAsUser(Uri apk, int userId) {
        
        return getBPackageManager().installPackageAsUser(apk.toString(), InstallOption.installByStorage().makeUriFile(), userId);
    }





    public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        return getBPackageManager().getInstalledApplications(flags, userId);
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        return getBPackageManager().getInstalledPackages(flags, userId);
    }

    public void clearPackage(String packageName, int userId) {
        BPackageManager.get().clearPackage(packageName, userId);
    }

    public void stopPackage(String packageName, int userId) {
        BPackageManager.get().stopPackage(packageName, userId);
    }

    public List<BUserInfo> getUsers() {
        return BUserManager.get().getUsers();
    }

    public BUserInfo createUser(int userId) {
        return BUserManager.get().createUser(userId);
    }

    public void deleteUser(int userId) {
        BUserManager.get().deleteUser(userId);
    }

    public List<AppLifecycleCallback> getAppLifecycleCallbacks() {
        return mAppLifecycleCallbacks;
    }

    public void removeAppLifecycleCallback(AppLifecycleCallback appLifecycleCallback) {
        mAppLifecycleCallbacks.remove(appLifecycleCallback);
    }

    public void addAppLifecycleCallback(AppLifecycleCallback appLifecycleCallback) {
        mAppLifecycleCallbacks.add(appLifecycleCallback);
    }

    public boolean isSupportGms() {
        return GmsCore.isSupportGms();
    }

    public boolean isInstallGms(int userId) {
        return GmsCore.isInstalledGoogleService(userId);
    }

    public InstallResult installGms(int userId) {
        return GmsCore.installGApps(userId);
    }

    public boolean uninstallGms(int userId) {
        GmsCore.uninstallGApps(userId);
        return !GmsCore.isInstalledGoogleService(userId);
    }

    
    private enum ProcessType {
        
        Server,
        
        BAppClient,
        
        Main,
    }

    public boolean isBlackProcess() {
        return mProcessType == ProcessType.BAppClient;
    }

    public boolean isMainProcess() {
        return mProcessType == ProcessType.Main;
    }

    public boolean isServerProcess() {
        return mProcessType == ProcessType.Server;
    }

    @Override
    public boolean isHideRoot() {
        return mClientConfiguration.isHideRoot();
    }

    @Override
    public boolean isDisableFlagSecure() {
        return mClientConfiguration.isDisableFlagSecure();
    }



    @Override
    public String getHostPackageName() {
        return mClientConfiguration.getHostPackageName();
    }

    @Override
    public boolean requestInstallPackage(File file, int userId) {
        return mClientConfiguration.requestInstallPackage(file, userId);
    }

    private void startLogcat() {
        new Thread(() -> {
            File logFile = null;
            Context context = getContext();
            String fileName = context.getPackageName() + "_logcat.txt";
            boolean useMediaStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
            
            
            logDeviceInfo();
            
            try {
                if (useMediaStore) {
                    
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain");
                    values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/logs");
                    android.net.Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        try (java.io.OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                            
                            ShellUtils.execCommand("logcat -c", false);
                            java.lang.Process process = Runtime.getRuntime().exec("logcat");
                            try (java.io.InputStream in = process.getInputStream()) {
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, len);
                                }
                            }
                        }
                    }
                } else {
                    
                    File docuentsdir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "logs");
                    if (!docuentsdir.exists()) {
                        docuentsdir.mkdirs();
                    }
                    logFile = new File(docuentsdir, fileName);
                    FileUtils.deleteDir(logFile);
                    ShellUtils.execCommand("logcat -c", false);
                    ShellUtils.execCommand("logcat -f " + logFile.getAbsolutePath(), false);
                }
            } catch (Exception e) {
                Slog.e(TAG, "Failed to save logcat: " + e.getMessage());
            }
        }).start();
    }

    
    private void logDeviceInfo() {
        try {
            Slog.i(TAG, "╔══════════════════════════════════════════════════════════════╗");
            Slog.i(TAG, "║                    DEVICE INFORMATION                        ║");
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            
            
            Slog.i(TAG, "║ Android Version: " + Build.VERSION.RELEASE);
            Slog.i(TAG, "║ SDK Level: " + Build.VERSION.SDK_INT);
            Slog.i(TAG, "║ Build ID: " + Build.ID);
            Slog.i(TAG, "║ Build Display: " + Build.DISPLAY);
            
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Slog.i(TAG, "║ Security Patch: " + Build.VERSION.SECURITY_PATCH);
            }
            
            
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            Slog.i(TAG, "║ Manufacturer: " + Build.MANUFACTURER);
            Slog.i(TAG, "║ Brand: " + Build.BRAND);
            Slog.i(TAG, "║ Model: " + Build.MODEL);
            Slog.i(TAG, "║ Device: " + Build.DEVICE);
            Slog.i(TAG, "║ Product: " + Build.PRODUCT);
            Slog.i(TAG, "║ Board: " + Build.BOARD);
            Slog.i(TAG, "║ Hardware: " + Build.HARDWARE);
            
            
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            Slog.i(TAG, "║ Supported ABIs: " + String.join(", ", Build.SUPPORTED_ABIS));
            if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                Slog.i(TAG, "║ 32-bit ABIs: " + String.join(", ", Build.SUPPORTED_32_BIT_ABIS));
            }
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                Slog.i(TAG, "║ 64-bit ABIs: " + String.join(", ", Build.SUPPORTED_64_BIT_ABIS));
            }
            
            
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            Slog.i(TAG, "║ Fingerprint: " + Build.FINGERPRINT);
            Slog.i(TAG, "║ Type: " + Build.TYPE);
            Slog.i(TAG, "║ Tags: " + Build.TAGS);
            
            
            try {
                Runtime runtime = Runtime.getRuntime();
                long maxMem = runtime.maxMemory() / (1024 * 1024);
                long totalMem = runtime.totalMemory() / (1024 * 1024);
                long freeMem = runtime.freeMemory() / (1024 * 1024);
                Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
                Slog.i(TAG, "║ Max Heap: " + maxMem + " MB");
                Slog.i(TAG, "║ Total Heap: " + totalMem + " MB");
                Slog.i(TAG, "║ Free Heap: " + freeMem + " MB");
                Slog.i(TAG, "║ Used Heap: " + (totalMem - freeMem) + " MB");
            } catch (Exception e) {
                Slog.w(TAG, "║ Memory info unavailable: " + e.getMessage());
            }
            
            
            try {
                Context context = getContext();
                if (context != null) {
                    Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
                    Slog.i(TAG, "║ Package: " + context.getPackageName());
                    android.content.pm.PackageInfo pInfo = context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0);
                    Slog.i(TAG, "║ App Version: " + pInfo.versionName);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Slog.i(TAG, "║ Version Code: " + pInfo.getLongVersionCode());
                    } else {
                        Slog.i(TAG, "║ Version Code: " + pInfo.versionCode);
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "║ App info unavailable: " + e.getMessage());
            }
            
            
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            Slog.i(TAG, "║ Timestamp: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", 
                    java.util.Locale.getDefault()).format(new java.util.Date()));
            Slog.i(TAG, "╚══════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to log device info: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private static String getProcessName(Context context) {
        int pid = Process.myPid();
        String processName = null;
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo info : processes) {
                        if (info.pid == pid) {
                            processName = info.processName;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to get process name using modern API", e);
            }
        }
        
        
        if (processName == null) {
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo info : processes) {
                        if (info.pid == pid) {
                            processName = info.processName;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to get process name using deprecated API", e);
            }
        }
        
        if (processName == null) {
            throw new RuntimeException("processName = null");
        }
        return processName;
    }

    public static boolean is64Bit() {
        if (BuildCompat.isM()) {
            return Process.is64Bit();
        } else {
            return Build.CPU_ABI.equals("arm64-v8a");
        }
    }

    private void initNotificationManager() {
        NotificationManager nm = (NotificationManager) BlackBoxCore.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ONE_ID = BlackBoxCore.getContext().getPackageName() + ".blackbox_core";
        String CHANNEL_ONE_NAME = "blackbox_core";
        if (BuildCompat.isOreo()) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(notificationChannel);
        }
    }

    public void closeCodeInit(){
        try {
            Class entry = Class.forName("top.niunaijun.blackbox.closecode.Entry");
            Method attach = entry.getDeclaredMethod("attach");
            attach.invoke(null);
        } catch (Exception e) {
            Slog.w(TAG, "closeCodeInit reflection failed: " + e.getMessage());
        }
    }
    public void onBeforeMainLaunchApk(String packageName,int userid) {
        for (AppLifecycleCallback appLifecycleCallback : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeMainLaunchApk(packageName,userid);
        }
    }
    public void onBeforeMainApplicationAttach(Application app, Context context) {
        for (AppLifecycleCallback appLifecycleCallback : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeMainApplicationAttach(app, context);
        }
    }
    public void onAfterMainApplicationAttach(Application app, Context context) {
        for (AppLifecycleCallback appLifecycleCallback : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.afterMainApplicationAttach(app, context);
        }
    }
    public void onBeforeMainActivityOnCreate(android.app.Activity activity) {
        for (AppLifecycleCallback appLifecycleCallback : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.beforeMainActivityOnCreate(activity);
        }
    }
    public void onAfterMainActivityOnCreate(android.app.Activity activity) {
        for (AppLifecycleCallback appLifecycleCallback : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            appLifecycleCallback.afterMainActivityOnCreate(activity);
        }
    }


    public static boolean isThreadInit() {
        try {
            return BActivityThread.isThreadInit();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.isThreadInit() failed, returning false", e);
            return false;
        }
    }

    public static BActivityThread currentActivityThread() {
        try {
            return BActivityThread.currentActivityThread();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.currentActivityThread() failed, returning null", e);
            return null;
        }
    }

    public static top.niunaijun.blackbox.entity.AppConfig getAppConfig() {
        try {
            return BActivityThread.getAppConfig();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getAppConfig() failed, returning null", e);
            return null;
        }
    }

    public static String getAppProcessName() {
        try {
            return BActivityThread.getAppProcessName();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getAppProcessName() failed, returning null", e);
            return null;
        }
    }

    public static String getAppPackageName() {
        try {
            return BActivityThread.getAppPackageName();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getAppPackageName() failed, returning null", e);
            return null;
        }
    }

    public static Application getApplication() {
        try {
            return BActivityThread.getApplication();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getApplication() failed, returning null", e);
            return null;
        }
    }

    public static int getAppPid() {
        try {
            return BActivityThread.getAppPid();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getAppPid() failed, returning -1", e);
            return -1;
        }
    }

    public static int getBUid() {
        try {
            return BActivityThread.getBUid();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getBUid() failed, returning -1", e);
            return -1;
        }
    }

    public static int getCallingBUid() {
        try {
            return BActivityThread.getCallingBUid();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getCallingBUid() failed, returning -1", e);
            return -1;
        }
    }

    public static int getUid() {
        try {
            return BActivityThread.getUid();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getUid() failed, returning -1", e);
            return -1;
        }
    }

    public static int getUserId() {
        try {
            return BActivityThread.getUserId();
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.getUserId() failed, returning -1", e);
            return -1;
        }
    }

    public static void ensureActivityContext(android.app.Activity activity) {
        try {
            BActivityThread.ensureActivityContext(activity);
        } catch (Exception e) {
            Slog.w(TAG, "BActivityThread.ensureActivityContext() failed", e);
        }
    }
    
    
    public static void installSystemHooks() {
        try {
            SimpleCrashFix.installSimpleFix();
            Slog.d(TAG, "System hooks installed successfully");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to install system hooks", e);
        }
    }
    
    
    private void setEssentialProperties(Context context, ClientConfiguration clientConfiguration) {
        if (clientConfiguration == null) {
            throw new IllegalArgumentException("ClientConfiguration is null!");
        }

        if(!NativeCore.disableHiddenApi()){
            try {
                Reflection.unseal(context);
            } catch (Throwable t) {
                Slog.w(TAG, "Reflection.unseal failed: " + t.getMessage());
            }
        }

        try {
            NativeCore.disableResourceLoading();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to call native resource disabling: " + e.getMessage());
        }
        
        
        try {
            
            System.setProperty("android.view.WindowManager.IGNORE_WINDOW_LEAKS", "true");
            System.setProperty("android.app.Activity.IGNORE_WINDOW_LEAKS", "true");
            System.setProperty("android.view.WindowManager.SUPPRESS_WINDOW_LEAK_WARNINGS", "true");
            
            
            try {
                Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
                Field disableOverlayField = resourcesManagerClass.getDeclaredField("mDisableOverlayLoading");
                disableOverlayField.setAccessible(true);
                
            } catch (Exception e) {
                Slog.w(TAG, "Could not access ResourcesManager overlay field: " + e.getMessage());
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set essential properties: " + e.getMessage());
        }
    }
    
    
    private void initVpnService() {
        try {
            
            if (mClientConfiguration == null || !mClientConfiguration.isUseVpnNetwork()) {
                Slog.d(TAG, "VPN network mode disabled, using normal network");
                return;
            }
            
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        
                        Intent vpnIntent = new Intent(getContext(), top.niunaijun.blackbox.proxy.ProxyVpnService.class);
                        vpnIntent.setAction("android.net.VpnService");
                        
                        if (BuildCompat.isOreo()) {
                            getContext().startForegroundService(vpnIntent);
                        } else {
                            getContext().startService(vpnIntent);
                        }
                        
                        Slog.d(TAG, "VPN service started successfully for internet access");
                    } catch (Exception e) {
                        Slog.w(TAG, "Failed to start VPN service: " + e.getMessage());
                        
                        
                    }
                }
            }, "VPNServiceInit").start();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to initialize VPN service: " + e.getMessage());
            
        }
    }
    
    
    private static void ensureProperInitialization() {
        try {
            
            Slog.d(TAG, "Ensuring proper initialization order...");
            
            
            try {
                NativeCore.init(android.os.Build.VERSION.SDK_INT);
                Slog.d(TAG, "NativeCore initialized successfully");
            } catch (Exception e) {
                Slog.w(TAG, "NativeCore initialization failed: " + e.getMessage());
            }
            
            
            try {
                ServiceManager.initBlackManager();
                Slog.d(TAG, "ServiceManager initialized successfully");
            } catch (Exception e) {
                Slog.w(TAG, "ServiceManager initialization failed: " + e.getMessage());
            }
            
            
            try {
                BActivityThread.hookActivityThread();
                Slog.d(TAG, "BActivityThread hooks initialized successfully");
            } catch (Exception e) {
                Slog.w(TAG, "BActivityThread hooks initialization failed: " + e.getMessage());
            }
            
            Slog.d(TAG, "Proper initialization order ensured");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to ensure proper initialization order", e);
        }
    }

    
    public static boolean isRunningApplication(String packageName, int userId) {
        
        try {
            
            android.app.ActivityManager am = (android.app.ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            
            
            
            ServiceManager.get();
            top.niunaijun.blackbox.core.system.am.ActivityStack stack =
                (top.niunaijun.blackbox.core.system.am.ActivityStack) ServiceManager.getService(ServiceManager.ACTIVITY_MANAGER);
            if (stack == null) return false;
            java.util.Map<Integer, top.niunaijun.blackbox.core.system.am.TaskRecord> tasks =
                    top.niunaijun.blackbox.utils.Reflector.with(stack).field("mTasks").get();
            if (tasks == null) return false;
            for (top.niunaijun.blackbox.core.system.am.TaskRecord task : tasks.values()) {
                if (task.userId == userId && task.taskAffinity != null && task.taskAffinity.contains(packageName)) {
                    
                    for (top.niunaijun.blackbox.core.system.am.ActivityRecord activity : task.activities) {
                        if (!activity.finished) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "isRunningApplication failed: " + e.getMessage());
        }
        return false;
    }

    
    public void addServiceAvailableCallback(Runnable callback) {
        synchronized (mServiceCallbackLock) {
            if (mServicesInitialized) {
                
                callback.run();
            } else {
                mServiceAvailableCallbacks.add(callback);
            }
        }
    }
    
    
    public void removeServiceAvailableCallback(Runnable callback) {
        synchronized (mServiceCallbackLock) {
            mServiceAvailableCallbacks.remove(callback);
        }
    }
    
    
    private void notifyServiceAvailableCallbacks() {
        synchronized (mServiceCallbackLock) {
            if (!mServiceAvailableCallbacks.isEmpty()) {
                Slog.d(TAG, "Notifying " + mServiceAvailableCallbacks.size() + " callbacks that services are available");
                for (Runnable callback : mServiceAvailableCallbacks) {
                    try {
                        callback.run();
                    } catch (Exception e) {
                        Slog.e(TAG, "Error in service available callback", e);
                    }
                }
                mServiceAvailableCallbacks.clear();
            }
        }
    }

    
    public boolean waitForServicesAvailable(long timeoutMs) {
        if (mServicesInitialized) {
            return true;
        }
        
        long startTime = System.currentTimeMillis();
        while (!mServicesInitialized && (System.currentTimeMillis() - startTime) < timeoutMs) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return mServicesInitialized;
    }
    
    
    public boolean isServicesAvailable() {
        return mServicesInitialized;
    }

    
    public boolean isBlackBoxApp(File apkFile) {
        try {
            if (apkFile == null || !apkFile.exists()) {
                return false;
            }
            
            PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            if (packageInfo != null) {
                String packageName = packageInfo.packageName;
                return packageName.equals(getHostPkg());
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error checking if APK is BlackBox app: " + e.getMessage());
        }
        return false;
    }
    
    
    public boolean isBlackBoxApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        return packageName.equals(getHostPkg());
    }
    
    
    private void handleServerServiceStartFailure(int retry, int maxRetries, Exception e) {
        if (retry < maxRetries - 1) {
            Slog.w(TAG, "Server service start failed, will retry. Attempt " + (retry + 1) + " of " + maxRetries);
        } else {
            Slog.e(TAG, "Server service start failed after " + maxRetries + " attempts: " + e.getMessage());
            
            tryAlternativeServerStartupMethods();
        }
    }
    
    
    private void handleServerProcessBadError(int retry, int maxRetries) {
        if (retry < maxRetries - 1) {
            Slog.w(TAG, "Server process is bad, attempting recovery. Attempt " + (retry + 1) + " of " + maxRetries);
            
            
            try {
                
                scheduleServerProcessRecovery(retry, maxRetries);
                
            } catch (Exception e) {
                Slog.w(TAG, "Server process recovery failed: " + e.getMessage());
            }
        } else {
            Slog.e(TAG, "Server process recovery failed after " + maxRetries + " attempts");
            
            tryAlternativeServerStartupMethods();
        }
    }
    
    
    private void scheduleServerProcessRecovery(int retry, int maxRetries) {
        try {
            int delayMs = 2000; 
            Slog.d(TAG, "Scheduling server process recovery in " + delayMs + "ms");
            
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Slog.d(TAG, "Executing server process recovery");
                        
                        refreshServerProcessContext();
                        
                        
                        if (isServerProcess() && mClientConfiguration.isEnableDaemonService()) {
                            Intent intent = new Intent();
                            intent.setClass(getContext(), DaemonService.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            
                            if (BuildCompat.isOreo()) {
                                getContext().startForegroundService(intent);
                            } else {
                                getContext().startService(intent);
                            }
                        }
                        
                    } catch (Exception e) {
                        Slog.w(TAG, "Server process recovery execution failed: " + e.getMessage());
                    }
                }
            }, delayMs);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule server process recovery: " + e.getMessage());
        }
    }
    
    
    private void tryAlternativeServerStartupMethods() {
        Slog.w(TAG, "Trying alternative startup methods for server process...");
        
        try {
            
            Context alternativeContext = getAlternativeContext();
            if (alternativeContext != null) {
                Intent intent = new Intent();
                intent.setClass(alternativeContext, DaemonService.class);
                alternativeContext.startService(intent);
                Slog.d(TAG, "Alternative context startup successful for server process");
                return;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Alternative context startup failed for server process: " + e.getMessage());
        }
        
        try {
            
            Context appContext = getContext().getApplicationContext();
            if (appContext != null && appContext != getContext()) {
                Intent intent = new Intent();
                intent.setClass(appContext, DaemonService.class);
                appContext.startService(intent);
                Slog.d(TAG, "Application context startup successful for server process");
                return;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Application context startup failed for server process: " + e.getMessage());
        }
        
        Slog.e(TAG, "All alternative startup methods failed for server process");
    }
    
    
    private void refreshServerProcessContext() {
        try {
            
            
            Slog.d(TAG, "Attempting to refresh server process context");
            
            
            
        } catch (Exception e) {
            Slog.w(TAG, "Server process context refresh failed: " + e.getMessage());
        }
    }
    
    
    private void scheduleDelayedServerServiceStart() {
        try {
            
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Slog.d(TAG, "Executing delayed server service start");
                    if (isServerProcess() && mClientConfiguration.isEnableDaemonService()) {
                        
                        try {
                            Intent intent = new Intent();
                            intent.setClass(getContext(), DaemonService.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            
                            if (BuildCompat.isOreo()) {
                                getContext().startForegroundService(intent);
                            } else {
                                getContext().startService(intent);
                            }
                            Slog.d(TAG, "Delayed server service start successful");
                        } catch (Exception e) {
                            Slog.e(TAG, "Delayed server service start failed: " + e.getMessage());
                        }
                    }
                }
            }, 5000); 
            
            Slog.d(TAG, "Scheduled delayed server service start in 5 seconds");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule delayed server service start: " + e.getMessage());
        }
    }
    
    
    private void scheduleDelayedServerRetry(Intent intent, int retry) {
        try {
            int delayMs = 1000 * retry;
            Slog.d(TAG, "Scheduling delayed server retry in " + delayMs + "ms");
            
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Slog.d(TAG, "Executing delayed server retry for DaemonService");
                        if (BuildCompat.isOreo()) {
                            getContext().startForegroundService(intent);
                        } else {
                            getContext().startService(intent);
                        }
                        Slog.d(TAG, "Delayed server retry successful");
                    } catch (Exception e) {
                        Slog.e(TAG, "Delayed server retry failed: " + e.getMessage());
                        
                        tryAlternativeServerStartupMethods();
                    }
                }
            }, delayMs);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule delayed server retry: " + e.getMessage());
            
            tryAlternativeServerStartupMethods();
        }
    }
    public interface LogSendListener {
        void onSuccess();
        void onFailure(String error);
    }

    public void sendLogs(String caption, boolean async) {
        sendLogs(caption, async, null);
    }

    public void sendLogs(String caption, boolean async, LogSendListener listener) {
        String chatId = mClientConfiguration != null ? mClientConfiguration.getLogSenderChatId() : null;
        if (chatId == null || chatId.isEmpty()) return;

        Runnable sendTask = () -> {
            try {
                
                File cacheDir = getContext().getCacheDir();
                File tempLog = File.createTempFile("crash_log_", ".txt", cacheDir);


                String deviceInfo = getDeviceInfoString();


                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempLog)) {
                    
                    String header = "Caption: " + caption + "\n\n" + deviceInfo + "\n\n--- LOGCAT ---\n";
                    fos.write(header.getBytes("UTF-8"));
                    
                    
                    java.lang.Process process = Runtime.getRuntime().exec("logcat -d -v threadtime");
                    try (java.io.InputStream in = process.getInputStream()) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    fos.flush();
                }
                
                
                String error = LogSender.send(chatId, tempLog, deviceInfo);
                if (error != null) {
                    Slog.e(TAG, "Log upload failed: " + error);
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                         try {
                             android.widget.Toast.makeText(getContext(), "Log Upload Failed: " + error, android.widget.Toast.LENGTH_LONG).show();
                         } catch (Exception e) {}
                        if (listener != null) {
                            listener.onFailure(error);
                        }
                    });
                    
                    
                    if (getContext() != null) {
                        NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                        if (nm != null) {
                            String channelId = getContext().getPackageName() + ".blackbox_core";
                            Notification.Builder builder;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                builder = new Notification.Builder(getContext(), channelId);
                            } else {
                                builder = new Notification.Builder(getContext());
                            }
                            
                            builder.setSmallIcon(android.R.drawable.stat_notify_error)
                                   .setContentTitle("BlackBox Log Upload Failed")
                                   .setContentText(error)
                                   .setAutoCancel(true);
                                   
                            nm.notify(9999, builder.build());
                        }
                    }
                } else {
                    
                     new Handler(Looper.getMainLooper()).post(() -> {
                         try {
                             android.widget.Toast.makeText(getContext(), "Log Upload Success", android.widget.Toast.LENGTH_SHORT).show();
                         } catch (Exception e) {}
                         if (listener != null) {
                             listener.onSuccess();
                         }
                     });

                    
                    if (getContext() != null) {
                        NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                        if (nm != null) {
                            String channelId = getContext().getPackageName() + ".blackbox_core";
                            Notification.Builder builder;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                builder = new Notification.Builder(getContext(), channelId);
                            } else {
                                builder = new Notification.Builder(getContext());
                            }

                            builder.setSmallIcon(android.R.drawable.stat_sys_upload_done)
                                   .setContentTitle("BlackBox Log Upload")
                                   .setContentText("Logs sent successfully")
                                   .setAutoCancel(true);

                            nm.notify(9999, builder.build());
                        }
                    }
                }
                
                
                tempLog.delete();
            } catch (Exception e) {
                Slog.e(TAG, "Failed to send logs: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
            }
        };

        if (async) {
            new Thread(sendTask).start();
        } else {
            sendTask.run();
        }
    }

    private String getDeviceInfoString() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("DEVICE INFORMATION\n");
            sb.append("------------------\n");
            
            
            sb.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
            sb.append("SDK Level: ").append(Build.VERSION.SDK_INT).append("\n");
            sb.append("Build ID: ").append(Build.ID).append("\n");
            sb.append("Build Display: ").append(Build.DISPLAY).append("\n");
            
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sb.append("Security Patch: ").append(Build.VERSION.SECURITY_PATCH).append("\n");
            }
            
            
            sb.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
            sb.append("Brand: ").append(Build.BRAND).append("\n");
            sb.append("Model: ").append(Build.MODEL).append("\n");
            sb.append("Device: ").append(Build.DEVICE).append("\n");
            sb.append("Product: ").append(Build.PRODUCT).append("\n");
            sb.append("Board: ").append(Build.BOARD).append("\n");
            sb.append("Hardware: ").append(Build.HARDWARE).append("\n");
            
            
            sb.append("Supported ABIs: ").append(String.join(", ", Build.SUPPORTED_ABIS)).append("\n");
            if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                sb.append("32-bit ABIs: ").append(String.join(", ", Build.SUPPORTED_32_BIT_ABIS)).append("\n");
            }
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                sb.append("64-bit ABIs: ").append(String.join(", ", Build.SUPPORTED_64_BIT_ABIS)).append("\n");
            }
            



            
            try {
                Runtime runtime = Runtime.getRuntime();
                long maxMem = runtime.maxMemory() / (1024 * 1024);
                long totalMem = runtime.totalMemory() / (1024 * 1024);
                long freeMem = runtime.freeMemory() / (1024 * 1024);
                sb.append("Max Heap: ").append(maxMem).append(" MB\n");
                sb.append("Total Heap: ").append(totalMem).append(" MB\n");
                sb.append("Free Heap: ").append(freeMem).append(" MB\n");
                sb.append("Used Heap: ").append(totalMem - freeMem).append(" MB\n");
            } catch (Exception e) {
                sb.append("Memory info unavailable: ").append(e.getMessage()).append("\n");
            }
            
            
            try {
                Context context = getContext();
                if (context != null) {
                    sb.append("Package: ").append(context.getPackageName()).append("\n");
                    android.content.pm.PackageInfo pInfo = context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0);
                    sb.append("App Version: ").append(pInfo.versionName).append("\n");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        sb.append("Version Code: ").append(pInfo.getLongVersionCode()).append("\n");
                    } else {
                        sb.append("Version Code: ").append(pInfo.versionCode).append("\n");
                    }
                }
            } catch (Exception e) {
                sb.append("App info unavailable: ").append(e.getMessage()).append("\n");
            }
            
            
            sb.append("Timestamp: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", 
                    java.util.Locale.getDefault()).format(new java.util.Date())).append("\n");
            
        } catch (Exception e) {
            sb.append("Failed to build device info: ").append(e.getMessage());
        }
        return sb.toString();
    }
}
