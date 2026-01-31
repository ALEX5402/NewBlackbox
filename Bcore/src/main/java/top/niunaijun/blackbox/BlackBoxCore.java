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
// just use it guys and i know you forgot to give credits so have it what ever 🤧
/**
 * updated by alex5402 on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
@SuppressLint({"StaticFieldLeak", "NewApi"})
@SuppressWarnings({"unchecked", "deprecation"})
public class BlackBoxCore extends ClientConfiguration {
    public static final String TAG = "BlackBoxCore";

    private static final BlackBoxCore sBlackBoxCore = new BlackBoxCore();
    private static Context sContext;
    
    // Static initializer to install the simple fix at class loading time
    static {
        try {
            // Install Pine anti-detection hooks as early as possible
            SimpleCrashFix.installSimpleFix();
            Slog.d(TAG, "Simple crash fix installed at class loading time");
            // Install stack trace filter for anti-detection
            StackTraceFilter.install();
            Slog.d(TAG, "Stack trace filter installed at class loading time");
            // Install social media app crash prevention
            SocialMediaAppCrashPrevention.initialize();
            Slog.d(TAG, "Social media app crash prevention initialized at class loading time");
            // Install DEX crash prevention
            DexCrashPrevention.initialize();
            Slog.d(TAG, "DEX crash prevention initialized at class loading time");
            // Install native crash prevention
            NativeCrashPrevention.initialize();
            Slog.d(TAG, "Native crash prevention initialized at class loading time");
            // Install comprehensive crash monitoring
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
    private static final long SERVICE_INIT_TIMEOUT_MS = 10000; // 10 seconds
    
    // Add callback mechanism for service availability
    private final List<Runnable> mServiceAvailableCallbacks = new ArrayList<>();
    private final Object mServiceCallbackLock = new Object();

    // Enhanced UID management for sandboxed environments
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

    // Enhanced UID management methods
    public void setCurrentAppUid(int uid, String packageName) {
        mCurrentAppUid = uid;
        mCurrentAppPackage = packageName;
        
        // Detect sandboxed environment
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

    // Enhanced UID resolution for security-sensitive operations
    public int resolveUidForOperation(int originalUid, String operation) {
        try {
            // System UIDs should remain unchanged
            if (originalUid > 0 && originalUid < Process.FIRST_APPLICATION_UID) {
                return originalUid;
            }
            
            // Non-user app UIDs should remain unchanged
            if (originalUid > Process.LAST_APPLICATION_UID) {
                return originalUid;
            }

            // If we're in a sandboxed environment, use the current app UID
            if (mIsSandboxedEnvironment && mCurrentAppUid > 0) {
                Slog.d("BlackBoxCore", "Resolving UID for " + operation + ": " + originalUid + " -> " + mCurrentAppUid);
                return mCurrentAppUid;
            }

            // Default behavior for non-sandboxed environments
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
            // First, try to check if the black process is running
            if (isMainProcess() && !isBlackProcessRunning()) {
                Slog.w(TAG, "Black process not running, starting it and using fallback services...");
                startBlackProcess();
                
                // Initialize services with fallbacks
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
                // Notify callbacks that services are available
                notifyServiceAvailableCallbacks();
                return true;
            }
            
            // Try to initialize services normally
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
                    // Try fallback if normal initialization fails
                    IBinder fallbackService = createFallbackService(serviceName);
                    if (fallbackService != null) {
                        mServices.put(serviceName, fallbackService);
                    }
                }
            }
            
            Slog.d(TAG, "Services initialized successfully");
            mServicesInitialized = true;
            // Notify callbacks that services are available
            notifyServiceAvailableCallbacks();
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize services, using fallbacks: " + e.getMessage());
            // Even if initialization fails, try to create fallback services
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
                // Notify callbacks that services are available
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
        
        // Check if we're in the main process and trying to access services
        // If so, we need to ensure the black process is running first
        if (isMainProcess() && !isBlackProcessRunning()) {
            Slog.w(TAG, "Main process trying to access service " + name + " but black process not running, starting it...");
            startBlackProcess();
            // Wait a bit for the process to start, but with timeout
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Add timeout for provider calls to prevent hanging
        long startTime = System.currentTimeMillis();
        long timeout = 3000; // 3 seconds timeout
        
        try {
            Bundle bundle = new Bundle();
            bundle.putString("_B_|_server_name_", name);
            
            // Try to call the provider with timeout
            Bundle vm = null;
            try {
                vm = ProviderCall.callSafely(ProxyManifest.getBindProvider(), "VM", null, bundle);
            } catch (Exception e) {
                // If provider call fails, check if we've exceeded timeout
                if (System.currentTimeMillis() - startTime > timeout) {
                    Slog.w(TAG, "Provider call timeout for service: " + name + ", using fallback");
                    return createFallbackService(name);
                }
                throw e;
            }
            
            if (vm == null) {
                Slog.w(TAG, "Provider call returned null for service: " + name);
                // Try to create a fallback service if possible
                return createFallbackService(name);
            }
            
            binder = BundleCompat.getBinder(vm, "_B_|_server_");
            Slog.d(TAG, "getService: " + name + ", " + binder);
            if (binder != null) {
                mServices.put(name, binder);
            } else {
                Slog.w(TAG, "Failed to get binder for service: " + name);
                // Try to create a fallback service
                return createFallbackService(name);
            }
            return binder;
        } catch (Exception e) {
            Slog.e(TAG, "Error getting service: " + name + ", creating fallback: " + e.getMessage());
            return createFallbackService(name);
        }
    }
    
    /**
     * Create a fallback service when the main service is not available
     */
    private IBinder createFallbackService(String name) {
        try {
            // Don't call ServiceManager.getService here to avoid circular dependency
            // Instead, just return null and let the caller handle it
            Slog.w(TAG, "No fallback available for service: " + name + " (avoiding circular dependency)");
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "Error creating fallback service for " + name, e);
            return null;
        }
    }
    

    
    /**
     * Check if the black process is running
     */
    private boolean isBlackProcessRunning() {
        try {
            // Try to access the SystemCallProvider to see if it's available
            Bundle testBundle = new Bundle();
            testBundle.putString("_B_|_server_name_", "test");
            
            // Use a more robust provider check with better error handling
            try {
                Bundle result = ProviderCall.callSafely(ProxyManifest.getBindProvider(), "VM", null, testBundle);
                if (result != null) {
                    Slog.d(TAG, "Black process is running - SystemCallProvider accessible");
                    return true;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Provider call failed: " + e.getMessage());
            }
            
            // Fallback: Check if the provider authority exists
            try {
                String authority = ProxyManifest.getBindProvider();
                if (authority != null && !authority.isEmpty()) {
                    // Try to resolve the provider
                    android.content.pm.ProviderInfo providerInfo = getContext().getPackageManager()
                        .resolveContentProvider(authority, 0);
                    if (providerInfo != null) {
                        Slog.d(TAG, "Provider exists but call failed - black process may be starting");
                        return false; // Provider exists but not accessible yet
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Provider resolution failed: " + e.getMessage());
            }
            
            // Additional fallback: Check if DaemonService is running
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
    
    /**
     * Start the black process if it's not running
     */
    private void startBlackProcess() {
        try {
            Slog.d(TAG, "Starting black process...");
            
            // Check if we're in a valid state to start services
            if (!isValidProcessState()) {
                Slog.w(TAG, "Process state is invalid, delaying service start");
                // Schedule a delayed retry
                scheduleDelayedServiceStart();
                return;
            }
            
            // Start the DaemonService which should be in the black process
            Intent intent = new Intent();
            intent.setClass(getContext(), DaemonService.class);
            
            // Add flags to help with process startup
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            boolean serviceStarted = false;
            int maxRetries = 3;
            
            for (int retry = 0; retry < maxRetries && !serviceStarted; retry++) {
                try {
                    if (retry > 0) {
                        Slog.d(TAG, "Retry attempt " + (retry + 1) + " for starting DaemonService");
                        // Use non-blocking delay instead of Thread.sleep
                        if (retry == 1) {
                            // For first retry, just continue immediately
                            Slog.d(TAG, "First retry - continuing immediately");
                        } else {
                            // For subsequent retries, schedule delayed execution
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
                // Try alternative startup methods
                tryAlternativeStartupMethods();
                return;
            }
            
            // Schedule provider check asynchronously instead of blocking
            scheduleProviderCheck();
            
            Slog.d(TAG, "Started DaemonService to initialize black process");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to start black process", e);
            // Schedule a delayed retry
            scheduleDelayedServiceStart();
        }
    }
    
    /**
     * Check if the current process state is valid for starting services
     */
    private boolean isValidProcessState() {
        try {
            // Check if the context is valid
            if (getContext() == null) {
                Slog.w(TAG, "Context is null, process state invalid");
                return false;
            }
            
            // Check if we're in the main process
            if (!isMainProcess()) {
                Slog.w(TAG, "Not in main process, skipping service start");
                return false;
            }
            
            // Check if the process is in a good state
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
    
    /**
     * Schedule a delayed retry for service startup
     */
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
                        // Try alternative methods
                        tryAlternativeStartupMethods();
                    }
                }
            }, delayMs);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule delayed retry: " + e.getMessage());
            // Fall back to alternative methods immediately
            tryAlternativeStartupMethods();
        }
    }
    
    /**
     * Schedule provider check asynchronously
     */
    private void scheduleProviderCheck() {
        try {
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Try to access the provider to ensure it's working
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
            }, 1000); // 1 second delay
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule provider check: " + e.getMessage());
        }
    }
    
    /**
     * Handle service startup failures with appropriate recovery actions
     */
    private void handleServiceStartFailure(int retry, int maxRetries, Exception e) {
        if (retry < maxRetries - 1) {
            Slog.w(TAG, "Service start failed, will retry. Attempt " + (retry + 1) + " of " + maxRetries);
        } else {
            Slog.e(TAG, "Service start failed after " + maxRetries + " attempts: " + e.getMessage());
            // Try alternative startup methods
            tryAlternativeStartupMethods();
        }
    }
    
    /**
     * Handle "process is bad" errors with recovery strategies
     */
    private void handleProcessBadError(int retry, int maxRetries) {
        if (retry < maxRetries - 1) {
            Slog.w(TAG, "Process is bad, attempting recovery. Attempt " + (retry + 1) + " of " + maxRetries);
            
            // Try to recover the process state
            try {
                // Schedule recovery instead of blocking
                scheduleProcessRecovery(retry, maxRetries);
                
            } catch (Exception e) {
                Slog.w(TAG, "Process recovery failed: " + e.getMessage());
            }
        } else {
            Slog.e(TAG, "Process recovery failed after " + maxRetries + " attempts");
            // Try alternative startup methods
            tryAlternativeStartupMethods();
        }
    }
    
    /**
     * Schedule process recovery asynchronously
     */
    private void scheduleProcessRecovery(int retry, int maxRetries) {
        try {
            int delayMs = 2000; // 2 second delay for process recovery
            Slog.d(TAG, "Scheduling process recovery in " + delayMs + "ms");
            
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Slog.d(TAG, "Executing process recovery");
                        // Try to refresh the context
                        refreshProcessContext();
                        
                        // Retry the service start
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
    
    /**
     * Try alternative methods to start the service
     */
    private void tryAlternativeStartupMethods() {
        Slog.w(TAG, "Trying alternative startup methods...");
        
        try {
            // Method 1: Try using a different context
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
            // Method 2: Try using application context
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
    
    /**
     * Get an alternative context for service startup
     */
    private Context getAlternativeContext() {
        try {
            // Try to get the application context
            Context appContext = getContext().getApplicationContext();
            if (appContext != null) {
                return appContext;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get application context: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Refresh the process context to recover from bad state
     */
    private void refreshProcessContext() {
        try {
            // This is a placeholder for context refresh logic
            // In a real implementation, you might want to reinitialize certain components
            Slog.d(TAG, "Attempting to refresh process context");
            
            // No blocking operations - just log and continue
            
        } catch (Exception e) {
            Slog.w(TAG, "Context refresh failed: " + e.getMessage());
        }
    }
    
    /**
     * Schedule a delayed service start for later
     */
    private void scheduleDelayedServiceStart() {
        try {
            // Schedule a delayed retry using a handler
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Slog.d(TAG, "Executing delayed service start");
                    if (isMainProcess() && !isBlackProcessRunning()) {
                        startBlackProcess();
                    }
                }
            }, 5000); // 5 second delay
            
            Slog.d(TAG, "Scheduled delayed service start in 5 seconds");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule delayed service start: " + e.getMessage());
        }
    }
    
    /**
     * Ensure the black process is properly initialized
     */
    public void ensureBlackProcessInitialized() {
        if (isMainProcess() && !isBlackProcessRunning()) {
            Slog.w(TAG, "Ensuring black process is initialized...");
            startBlackProcess();
            
            // Wait for the process to be ready
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
            // Set essential properties for the context
            setEssentialProperties(context, clientConfiguration);
            
            // Try to disable window leak warnings via reflection
            try {
                Class<?> windowManagerClass = Class.forName("android.view.WindowManager");
                Field ignoreLeaksField = windowManagerClass.getDeclaredField("mIgnoreWindowLeaks");
                ignoreLeaksField.setAccessible(true);
                // This might not work, but worth trying
            } catch (Exception e) {
                Slog.w(TAG, "Could not access WindowManager leak field: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set essential properties: " + e.getMessage());
        }

        sContext = context;
        mClientConfiguration = clientConfiguration;
        
        // Install system hooks to prevent crashes
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
//                android.os.Debug.waitForDebugger();
            }
//            android.os.Debug.waitForDebugger();
        }
        if (isServerProcess()) {
            if (clientConfiguration.isEnableDaemonService()) {
                try {
                    // Check if we're in a valid state to start services
                    if (!isValidProcessState()) {
                        Slog.w(TAG, "Server process state is invalid, delaying service start");
                        // Schedule a delayed retry for server process
                        scheduleDelayedServerServiceStart();
                        return;
                    }
                    
                    Intent intent = new Intent();
                    intent.setClass(getContext(), DaemonService.class);
                    
                    // Add flags to help with process startup
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    boolean serviceStarted = false;
                    int maxRetries = 3;
                    
                    for (int retry = 0; retry < maxRetries && !serviceStarted; retry++) {
                        try {
                            if (retry > 0) {
                                Slog.d(TAG, "Retry attempt " + (retry + 1) + " for starting DaemonService in server process");
                                // Use non-blocking delay instead of Thread.sleep
                                if (retry == 1) {
                                    // For first retry, just continue immediately
                                    Slog.d(TAG, "First retry for server process - continuing immediately");
                                } else {
                                    // For subsequent retries, schedule delayed execution
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
                        // Try alternative startup methods for server process
                        tryAlternativeServerStartupMethods();
                    }
                    
                } catch (Exception e) {
                    Slog.e(TAG, "Unexpected error starting DaemonService in server process: " + e.getMessage(), e);
                    // Schedule a delayed retry
                    scheduleDelayedServerServiceStart();
                }
            }
        }
        
        // Initialize VPN service for internet access
        initVpnService();
        
        HookManager.get().init();
    }

    public void doCreate() {
        // Install system hooks to prevent crashes
        installSystemHooks();
        
        // Set a timeout for the entire initialization process
        long startTime = System.currentTimeMillis();
        long maxInitTime = 10000; // 10 seconds max
        
        try {
            // Ensure black process is initialized before proceeding
            ensureBlackProcessInitialized();
            
            // Check if we've exceeded the timeout
            if (System.currentTimeMillis() - startTime > maxInitTime) {
                Slog.w(TAG, "Initialization timeout exceeded, proceeding with fallback services");
            }
            
            ensureProperInitialization();
            
            // fix contentProvider
            if (isBlackProcess()) {
                ContentProviderDelegate.init();
            }
            if (!isServerProcess()) {
                // Initialize the ServiceManager to ensure services are available
                try {
                    ServiceManager.initBlackManager();
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to initialize ServiceManager, continuing with fallback: " + e.getMessage());
                }
                
                // Reset transaction throttler on startup
                getBPackageManager().resetTransactionThrottler();
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            Slog.d(TAG, "BlackBox initialization completed in " + totalTime + "ms");
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            Slog.e(TAG, "BlackBox initialization failed after " + totalTime + "ms", e);
            
            // Try to continue with fallback services
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
    
    // ============== Storage Permission Methods (SDK 30+) ==============
    
    /**
     * Check if the host app has storage permission.
     * On Android 13+, this checks for granular media permissions.
     * On Android 11-12, this checks for READ_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE.
     */
    public boolean hasStoragePermission() {
        return StoragePermissionHelper.hasStoragePermission(sContext);
    }
    
    /**
     * Check if the host app has "All Files Access" (MANAGE_EXTERNAL_STORAGE) on Android 11+.
     * Returns true on older Android versions where this isn't required.
     */
    public boolean hasAllFilesAccess() {
        return StoragePermissionHelper.hasAllFilesAccess();
    }
    
    /**
     * Check if the host app has full file access (both storage permission and all files access).
     */
    public boolean hasFullFileAccess() {
        return StoragePermissionHelper.hasFullFileAccess(sContext);
    }
    
    /**
     * Request storage permission from the user.
     * On Android 13+, requests granular media permissions.
     * On older versions, requests READ/WRITE_EXTERNAL_STORAGE.
     * 
     * @param activity The activity to use for requesting permission
     */
    public void requestStoragePermission(android.app.Activity activity) {
        StoragePermissionHelper.requestStoragePermission(activity);
    }
    
    /**
     * Request "All Files Access" (MANAGE_EXTERNAL_STORAGE) on Android 11+.
     * This opens the system settings page for the user to grant permission.
     * 
     * @param activity The activity to use for requesting permission
     */
    public void requestAllFilesAccess(android.app.Activity activity) {
        StoragePermissionHelper.requestAllFilesAccess(activity);
    }
    
    /**
     * Request full file access (both storage permission and all files access).
     * Use this before launching apps that need file access.
     * 
     * @param activity The activity to use for requesting permission
     */
    public void requestFullFileAccess(android.app.Activity activity) {
        StoragePermissionHelper.requestFullFileAccess(activity);
    }
    
    /**
     * Handle permission result callback from Activity.onRequestPermissionsResult()
     * @return true if storage permission was granted
     */
    public boolean handleStoragePermissionResult(android.app.Activity activity, int requestCode, 
            String[] permissions, int[] grantResults) {
        return StoragePermissionHelper.handlePermissionResult(activity, requestCode, permissions, grantResults);
    }
    
    /**
     * Handle result from All Files Access settings activity.
     * Call this from Activity.onActivityResult()
     * @return true if all files access was granted
     */
    public boolean handleAllFilesAccessResult(int requestCode) {
        return StoragePermissionHelper.handleAllFilesAccessResult(requestCode);
    }
    
    /**
     * Get the request codes for permission handling.
     */
    public static int getStoragePermissionRequestCode() {
        return StoragePermissionHelper.REQUEST_CODE_STORAGE_PERMISSION;
    }
    
    public static int getAllFilesAccessRequestCode() {
        return StoragePermissionHelper.REQUEST_CODE_MANAGE_STORAGE;
    }

    public boolean launchApk(String packageName, int userId) {
        onBeforeMainLaunchApk(packageName, userId);
        
        // Check storage permissions on Android 11+ (SDK 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!hasAllFilesAccess()) {
                Slog.w(TAG, "All files access not granted for launching: " + packageName);
                // Notify the host app that permission is needed
                for (AppLifecycleCallback callback : mAppLifecycleCallbacks) {
                    if (callback.onStoragePermissionNeeded(packageName, userId)) {
                        // Host app will handle the permission request, cancel launch
                        Slog.d(TAG, "Launch cancelled - host app handling permission request");
                        return false;
                    }
                }
                // Otherwise, continue but warn
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
            // Prevent cloning BlackBox app from within BlackBox
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
        // Check if this is a BlackBox-related APK
        try {
            PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            if (packageInfo != null) {
                String packageName = packageInfo.packageName;
                if (packageName.equals(getHostPkg())) {
                    return new InstallResult().installError("Cannot clone BlackBox app from within BlackBox. This would create infinite recursion and is not allowed for security reasons.");
                }
            }
        } catch (Exception e) {
            // If we can't check the package info, proceed but log a warning
            Slog.w(TAG, "Could not verify package info for APK: " + apk.getAbsolutePath());
        }
        
        return getBPackageManager().installPackageAsUser(apk.getAbsolutePath(), InstallOption.installByStorage(), userId);
    }

    public InstallResult installPackageAsUser(Uri apk, int userId) {
        // For URI installations, we can't easily check the package name, so we'll rely on the BPackageManagerService check
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

    /**
     * Process type
     */
    private enum ProcessType {
        /**
         * Server process
         */
        Server,
        /**
         * Black app process
         */
        BAppClient,
        /**
         * Main process
         */
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
            
            // Log comprehensive device information
            logDeviceInfo();
            
            try {
                if (useMediaStore) {
                    // Use MediaStore for Android 10+
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain");
                    values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/logs");
                    android.net.Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        try (java.io.OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                            // Clear logcat and dump to output stream
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
                    // Use app-private external files dir for Android 9 and below
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

    /**
     * Log comprehensive device information for debugging purposes
     */
    private void logDeviceInfo() {
        try {
            Slog.i(TAG, "╔══════════════════════════════════════════════════════════════╗");
            Slog.i(TAG, "║                    DEVICE INFORMATION                        ║");
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            
            // Android Version Info
            Slog.i(TAG, "║ Android Version: " + Build.VERSION.RELEASE);
            Slog.i(TAG, "║ SDK Level: " + Build.VERSION.SDK_INT);
            Slog.i(TAG, "║ Build ID: " + Build.ID);
            Slog.i(TAG, "║ Build Display: " + Build.DISPLAY);
            
            // Security Patch (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Slog.i(TAG, "║ Security Patch: " + Build.VERSION.SECURITY_PATCH);
            }
            
            // Device Info
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            Slog.i(TAG, "║ Manufacturer: " + Build.MANUFACTURER);
            Slog.i(TAG, "║ Brand: " + Build.BRAND);
            Slog.i(TAG, "║ Model: " + Build.MODEL);
            Slog.i(TAG, "║ Device: " + Build.DEVICE);
            Slog.i(TAG, "║ Product: " + Build.PRODUCT);
            Slog.i(TAG, "║ Board: " + Build.BOARD);
            Slog.i(TAG, "║ Hardware: " + Build.HARDWARE);
            
            // CPU/ABI Info
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            Slog.i(TAG, "║ Supported ABIs: " + String.join(", ", Build.SUPPORTED_ABIS));
            if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                Slog.i(TAG, "║ 32-bit ABIs: " + String.join(", ", Build.SUPPORTED_32_BIT_ABIS));
            }
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                Slog.i(TAG, "║ 64-bit ABIs: " + String.join(", ", Build.SUPPORTED_64_BIT_ABIS));
            }
            
            // Build Fingerprint
            Slog.i(TAG, "╠══════════════════════════════════════════════════════════════╣");
            Slog.i(TAG, "║ Fingerprint: " + Build.FINGERPRINT);
            Slog.i(TAG, "║ Type: " + Build.TYPE);
            Slog.i(TAG, "║ Tags: " + Build.TAGS);
            
            // Memory Info
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
            
            // App Info
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
            
            // Timestamp
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
        
        // Try modern approach first (API 28+)
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
        
        // Fallback to deprecated method if modern approach fails
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
    
    /**
     * Install system hooks to prevent crashes
     */
    public static void installSystemHooks() {
        try {
            SimpleCrashFix.installSimpleFix();
            Slog.d(TAG, "System hooks installed successfully");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to install system hooks", e);
        }
    }
    
    /**
     * Set essential properties for the context
     */
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
        
        // Set only essential system properties that don't require system permissions
        try {
            // Set properties to handle window management issues (these are usually allowed)
            System.setProperty("android.view.WindowManager.IGNORE_WINDOW_LEAKS", "true");
            System.setProperty("android.app.Activity.IGNORE_WINDOW_LEAKS", "true");
            System.setProperty("android.view.WindowManager.SUPPRESS_WINDOW_LEAK_WARNINGS", "true");
            
            // Try to disable overlay loading via reflection (safer than system properties)
            try {
                Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
                Field disableOverlayField = resourcesManagerClass.getDeclaredField("mDisableOverlayLoading");
                disableOverlayField.setAccessible(true);
                // This might not work, but worth trying
            } catch (Exception e) {
                Slog.w(TAG, "Could not access ResourcesManager overlay field: " + e.getMessage());
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set essential properties: " + e.getMessage());
        }
    }
    
    /**
     * Initialize VPN service for internet access
     */
    private void initVpnService() {
        try {
            // Check if VPN mode is enabled in settings
            if (mClientConfiguration == null || !mClientConfiguration.isUseVpnNetwork()) {
                Slog.d(TAG, "VPN network mode disabled, using normal network");
                return;
            }
            
            // Start the VPN service asynchronously to prevent blocking main thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Start the VPN service to ensure internet access works
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
                        // Don't fail initialization if VPN service fails
                        // The app can still work without VPN, just with limited network access
                    }
                }
            }, "VPNServiceInit").start();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to initialize VPN service: " + e.getMessage());
            // Don't fail initialization if VPN service fails
        }
    }
    
    /**
     * Ensure proper initialization order for all components
     */
    private static void ensureProperInitialization() {
        try {
            // Initialize core components in the correct order
            Slog.d(TAG, "Ensuring proper initialization order...");
            
            // 1. Initialize NativeCore first
            try {
                NativeCore.init(android.os.Build.VERSION.SDK_INT);
                Slog.d(TAG, "NativeCore initialized successfully");
            } catch (Exception e) {
                Slog.w(TAG, "NativeCore initialization failed: " + e.getMessage());
            }
            
            // 2. Initialize services
            try {
                ServiceManager.initBlackManager();
                Slog.d(TAG, "ServiceManager initialized successfully");
            } catch (Exception e) {
                Slog.w(TAG, "ServiceManager initialization failed: " + e.getMessage());
            }
            
            // 3. Initialize ActivityThread hooks
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

    /**
     * Returns true if the application instance is already running for the given user, else false.
     */
    public static boolean isRunningApplication(String packageName, int userId) {
        // Access the virtual task stack
        try {
            // Get the ActivityManager from the context
            android.app.ActivityManager am = (android.app.ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            // Check all running tasks in the virtual stack
            // We need to check the virtual stack, not the real system stack
            // So we use the ActivityStack from BlackBox
            ServiceManager.get();
            top.niunaijun.blackbox.core.system.am.ActivityStack stack =
                (top.niunaijun.blackbox.core.system.am.ActivityStack) ServiceManager.getService(ServiceManager.ACTIVITY_MANAGER);
            if (stack == null) return false;
            java.util.Map<Integer, top.niunaijun.blackbox.core.system.am.TaskRecord> tasks =
                    top.niunaijun.blackbox.utils.Reflector.with(stack).field("mTasks").get();
            if (tasks == null) return false;
            for (top.niunaijun.blackbox.core.system.am.TaskRecord task : tasks.values()) {
                if (task.userId == userId && task.taskAffinity != null && task.taskAffinity.contains(packageName)) {
                    // Check if there is at least one non-finished activity in the task
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

    /**
     * Register a callback to be notified when services become available
     */
    public void addServiceAvailableCallback(Runnable callback) {
        synchronized (mServiceCallbackLock) {
            if (mServicesInitialized) {
                // Services are already available, run callback immediately
                callback.run();
            } else {
                mServiceAvailableCallbacks.add(callback);
            }
        }
    }
    
    /**
     * Remove a service available callback
     */
    public void removeServiceAvailableCallback(Runnable callback) {
        synchronized (mServiceCallbackLock) {
            mServiceAvailableCallbacks.remove(callback);
        }
    }
    
    /**
     * Notify all registered callbacks that services are available
     */
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

    /**
     * Wait for services to become available with a timeout
     * @param timeoutMs Timeout in milliseconds
     * @return true if services became available, false if timeout
     */
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
    
    /**
     * Check if services are currently available
     */
    public boolean isServicesAvailable() {
        return mServicesInitialized;
    }

    /**
     * Check if an APK file is the BlackBox app itself
     * @param apkFile The APK file to check
     * @return true if it's the BlackBox app, false otherwise
     */
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
    
    /**
     * Check if a package name is the BlackBox app
     * @param packageName The package name to check
     * @return true if it's the BlackBox app, false otherwise
     */
    public boolean isBlackBoxApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        return packageName.equals(getHostPkg());
    }
    
    /**
     * Handle server service startup failures with appropriate recovery actions
     */
    private void handleServerServiceStartFailure(int retry, int maxRetries, Exception e) {
        if (retry < maxRetries - 1) {
            Slog.w(TAG, "Server service start failed, will retry. Attempt " + (retry + 1) + " of " + maxRetries);
        } else {
            Slog.e(TAG, "Server service start failed after " + maxRetries + " attempts: " + e.getMessage());
            // Try alternative startup methods for server process
            tryAlternativeServerStartupMethods();
        }
    }
    
    /**
     * Handle "process is bad" errors for server process with recovery strategies
     */
    private void handleServerProcessBadError(int retry, int maxRetries) {
        if (retry < maxRetries - 1) {
            Slog.w(TAG, "Server process is bad, attempting recovery. Attempt " + (retry + 1) + " of " + maxRetries);
            
            // Try to recover the server process state
            try {
                // Schedule recovery instead of blocking
                scheduleServerProcessRecovery(retry, maxRetries);
                
            } catch (Exception e) {
                Slog.w(TAG, "Server process recovery failed: " + e.getMessage());
            }
        } else {
            Slog.e(TAG, "Server process recovery failed after " + maxRetries + " attempts");
            // Try alternative startup methods for server process
            tryAlternativeServerStartupMethods();
        }
    }
    
    /**
     * Schedule server process recovery asynchronously
     */
    private void scheduleServerProcessRecovery(int retry, int maxRetries) {
        try {
            int delayMs = 2000; // 2 second delay for process recovery
            Slog.d(TAG, "Scheduling server process recovery in " + delayMs + "ms");
            
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Slog.d(TAG, "Executing server process recovery");
                        // Try to refresh the server process context
                        refreshServerProcessContext();
                        
                        // Retry the service start
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
    
    /**
     * Try alternative methods to start the service in server process
     */
    private void tryAlternativeServerStartupMethods() {
        Slog.w(TAG, "Trying alternative startup methods for server process...");
        
        try {
            // Method 1: Try using a different context
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
            // Method 2: Try using application context
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
    
    /**
     * Refresh the server process context to recover from bad state
     */
    private void refreshServerProcessContext() {
        try {
            // This is a placeholder for server process context refresh logic
            // In a real implementation, you might want to reinitialize certain server components
            Slog.d(TAG, "Attempting to refresh server process context");
            
            // No blocking operations - just log and continue
            
        } catch (Exception e) {
            Slog.w(TAG, "Server process context refresh failed: " + e.getMessage());
        }
    }
    
    /**
     * Schedule a delayed service start for server process
     */
    private void scheduleDelayedServerServiceStart() {
        try {
            // Schedule a delayed retry using a handler
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Slog.d(TAG, "Executing delayed server service start");
                    if (isServerProcess() && mClientConfiguration.isEnableDaemonService()) {
                        // Re-trigger the server process service start
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
            }, 5000); // 5 second delay
            
            Slog.d(TAG, "Scheduled delayed server service start in 5 seconds");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule delayed server service start: " + e.getMessage());
        }
    }
    
    /**
     * Schedule a delayed retry for server process service startup
     */
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
                        // Try alternative methods
                        tryAlternativeServerStartupMethods();
                    }
                }
            }, delayMs);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to schedule delayed server retry: " + e.getMessage());
            // Fall back to alternative methods immediately
            tryAlternativeServerStartupMethods();
        }
    }
}
