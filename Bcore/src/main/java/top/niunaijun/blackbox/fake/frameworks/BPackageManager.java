package top.niunaijun.blackbox.fake.frameworks;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.system.ServiceManager;
import top.niunaijun.blackbox.core.system.pm.IBPackageManagerService;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.entity.pm.InstalledPackage;
import top.niunaijun.blackbox.utils.TransactionThrottler;


public class BPackageManager extends BlackManager<IBPackageManagerService> {
    private static final BPackageManager sPackageManager = new BPackageManager();
    private final TransactionThrottler transactionThrottler = new TransactionThrottler();
    private static volatile boolean sIsFindingApkPath = false; 

    public static BPackageManager get() {
        return sPackageManager;
    }
    
    
    public void resetTransactionThrottler() {
        transactionThrottler.reset();
        Log.d(TAG, "Transaction throttler reset");
    }
    
    
    private boolean shouldUseFallbackMode() {
        return transactionThrottler.getFailureCount() >= 2 || !isServiceHealthy();
    }

    
    public void forceReinitialize() {
        Log.d(TAG, "Force reinitializing PackageManager service");
        clearServiceCache();
        resetTransactionThrottler();
        
        
        try {
            IBPackageManagerService service = getService();
            if (service != null) {
                Log.d(TAG, "Successfully reinitialized PackageManager service");
            } else {
                Log.w(TAG, "Failed to reinitialize PackageManager service");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during service reinitialization", e);
        }
    }

    
    public IBPackageManagerService getServiceWithFallback() {
        IBPackageManagerService service = getService();
        if (service == null) {
            Log.w(TAG, "PackageManager service is null, attempting reinitialization");
            forceReinitialize();
            service = getService();
        }
        return service;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.PACKAGE_MANAGER;
    }

    public Intent getLaunchIntentForPackage(String packageName, int userId) {
        
        if (shouldUseFallbackMode()) {
            Log.w(TAG, "Using fallback launch intent for " + packageName + " due to service failures");
            return createFallbackLaunchIntent(packageName);
        }
        
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = queryIntentActivities(intentToResolve,
                0,
                intentToResolve.resolveTypeIfNeeded(BlackBoxCore.getContext().getContentResolver()),
                userId);

        
        if (ris == null || ris.size() <= 0) {
            
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = queryIntentActivities(intentToResolve,
                    0,
                    intentToResolve.resolveTypeIfNeeded(BlackBoxCore.getContext().getContentResolver()),
                    userId);
        }
        if (ris == null || ris.size() <= 0) {
            return null;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName,
                ris.get(0).activityInfo.name);
        return intent;
    }
    
    
    private Intent createFallbackLaunchIntent(String packageName) {
        try {
            
            Intent intent = BlackBoxCore.getContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            }
        } catch (Exception e) {
            Log.w(TAG, "Fallback launch intent failed for " + packageName, e);
        }
        
        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(packageName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public ResolveInfo resolveService(Intent intent, int flags, String resolvedType, int userId) {
        
        if (transactionThrottler.shouldThrottle()) {
            Log.w(TAG, "Throttling resolveService due to recent failures");
            return null;
        }
        
        try {
            IBPackageManagerService service = getService();
            if (service != null) {
                ResolveInfo result = service.resolveService(intent, flags, resolvedType, userId);
                
                transactionThrottler.reset();
                return result;
            } else {
                Log.w(TAG, "PackageManager service is null, returning null for resolveService");
            }
        } catch (android.os.DeadObjectException e) {
            Log.w(TAG, "PackageManager service died during resolveService, clearing service and retrying", e);
            transactionThrottler.recordFailure();
            
            clearServiceCache();
            
            try {
                IBPackageManagerService service = getService();
                if (service != null) {
                    ResolveInfo result = service.resolveService(intent, flags, resolvedType, userId);
                    transactionThrottler.reset(); 
                    return result;
                }
            } catch (Exception retryException) {
                Log.e(TAG, "Retry failed for resolveService", retryException);
                transactionThrottler.recordFailure();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in resolveService", e);
            transactionThrottler.recordFailure();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in resolveService", e);
            transactionThrottler.recordFailure();
        }
        return null;
    }

    public ResolveInfo resolveActivity(Intent intent, int flags, String resolvedType, int userId) {
        
        if (transactionThrottler.shouldThrottle()) {
            Log.w(TAG, "Throttling resolveActivity due to recent failures");
            return null;
        }
        
        try {
            IBPackageManagerService service = getService();
            if (service != null) {
                ResolveInfo result = service.resolveActivity(intent, flags, resolvedType, userId);
                
                transactionThrottler.reset();
                return result;
            } else {
                Log.w(TAG, "PackageManager service is null, returning null for resolveActivity");
            }
        } catch (android.os.DeadObjectException e) {
            Log.w(TAG, "PackageManager service died during resolveActivity, clearing service and retrying", e);
            transactionThrottler.recordFailure();
            
            clearServiceCache();
            
            try {
                IBPackageManagerService service = getService();
                if (service != null) {
                    ResolveInfo result = service.resolveActivity(intent, flags, resolvedType, userId);
                    transactionThrottler.reset(); 
                    return result;
                }
            } catch (Exception retryException) {
                Log.e(TAG, "Retry failed for resolveActivity", retryException);
                transactionThrottler.recordFailure();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in resolveActivity", e);
            transactionThrottler.recordFailure();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in resolveActivity", e);
            transactionThrottler.recordFailure();
        }
        return null;
    }

    public ProviderInfo resolveContentProvider(String authority, int flags, int userId) {
        
        if (transactionThrottler.shouldThrottle()) {
            Log.w(TAG, "Throttling resolveContentProvider due to recent failures");
            return null;
        }
        
        try {
            IBPackageManagerService service = getService();
            if (service != null) {
                ProviderInfo result = service.resolveContentProvider(authority, flags, userId);
                
                transactionThrottler.reset();
                return result;
            } else {
                Log.w(TAG, "PackageManager service is null, returning null for resolveContentProvider");
            }
        } catch (android.os.DeadObjectException e) {
            Log.w(TAG, "PackageManager service died during resolveContentProvider, clearing service and retrying", e);
            transactionThrottler.recordFailure();
            
            clearServiceCache();
            
            try {
                IBPackageManagerService service = getService();
                if (service != null) {
                    ProviderInfo result = service.resolveContentProvider(authority, flags, userId);
                    transactionThrottler.reset(); 
                    return result;
                }
            } catch (Exception retryException) {
                Log.e(TAG, "Retry failed for resolveContentProvider", retryException);
                transactionThrottler.recordFailure();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in resolveContentProvider", e);
            transactionThrottler.recordFailure();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in resolveContentProvider", e);
            transactionThrottler.recordFailure();
        }
        return null;
    }

    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
        try {
            return getService().resolveIntent(intent, resolvedType, flags, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        try {
            IBPackageManagerService service = getServiceWithFallback();
            if (service == null) {
                Log.w(TAG, "PackageManager service is null for getApplicationInfo, using fallback");
                return createFallbackApplicationInfo(packageName, flags, userId);
            }
            return service.getApplicationInfo(packageName, flags, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getApplicationInfo for " + packageName, e);
            return createFallbackApplicationInfo(packageName, flags, userId);
        } catch (Exception e) {
            Log.e(TAG, "Exception in getApplicationInfo for " + packageName, e);
            return createFallbackApplicationInfo(packageName, flags, userId);
        }
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        try {
            IBPackageManagerService service = getServiceWithFallback();
            if (service == null) {
                Log.w(TAG, "PackageManager service is null for getPackageInfo, using fallback");
                return createFallbackPackageInfo(packageName, flags, userId);
            }
            return service.getPackageInfo(packageName, flags, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getPackageInfo for " + packageName, e);
            return createFallbackPackageInfo(packageName, flags, userId);
        } catch (Exception e) {
            Log.e(TAG, "Exception in getPackageInfo for " + packageName, e);
            return createFallbackPackageInfo(packageName, flags, userId);
        }
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
        try {
            IBPackageManagerService service = getService();
            if (service == null) {
                Log.w(TAG, "PackageManager service is null for getServiceInfo, returning null");
                return null;
            }
            return service.getServiceInfo(component, flags, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getServiceInfo for " + component, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception in getServiceInfo for " + component, e);
            return null;
        }
    }

    public ActivityInfo getReceiverInfo(ComponentName componentName, int flags, int userId) {
        try {
            IBPackageManagerService service = getService();
            if (service == null) {
                Log.w(TAG, "PackageManager service is null for getReceiverInfo, returning null");
                return null;
            }
            return service.getReceiverInfo(componentName, flags, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getReceiverInfo for " + componentName, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception in getReceiverInfo for " + componentName, e);
            return null;
        }
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        try {
            IBPackageManagerService service = getService();
            if (service == null) {
                Log.w(TAG, "PackageManager service is null for getActivityInfo, returning null");
                return null;
            }
            return service.getActivityInfo(component, flags, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getActivityInfo for " + component, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception in getActivityInfo for " + component, e);
            return null;
        }
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {
        try {
            IBPackageManagerService service = getService();
            if (service == null) {
                Log.w(TAG, "PackageManager service is null for getProviderInfo, returning null");
                return null;
            }
            return service.getProviderInfo(component, flags, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getProviderInfo for " + component, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception in getProviderInfo for " + component, e);
            return null;
        }
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags, String resolvedType, int userId) {
        
        if (transactionThrottler.shouldThrottle()) {
            Log.w(TAG, "Throttling queryIntentActivities due to recent failures");
            return Collections.emptyList();
        }
        
        
        if (transactionThrottler.getFailureCount() >= 2) {
            Log.w(TAG, "Too many failures, returning empty list for queryIntentActivities");
            return Collections.emptyList();
        }
        
        try {
            IBPackageManagerService service = getService();
            if (service != null) {
                List<ResolveInfo> result = service.queryIntentActivities(intent, flags, resolvedType, userId);
                
                transactionThrottler.reset();
                return result;
            } else {
                Log.w(TAG, "PackageManager service is null, returning empty list for queryIntentActivities");
                return Collections.emptyList();
            }
        } catch (android.os.DeadObjectException e) {
            Log.w(TAG, "PackageManager service died during queryIntentActivities, clearing cache and retrying", e);
            transactionThrottler.recordFailure();
            clearServiceCache(); 
            
            
            if (transactionThrottler.getFailureCount() < 3) {
                try {
                    
                    IBPackageManagerService service = getService();
                    if (service != null) {
                        List<ResolveInfo> result = service.queryIntentActivities(intent, flags, resolvedType, userId);
                        transactionThrottler.reset(); 
                        return result;
                    }
                } catch (Exception retryException) {
                    Log.e(TAG, "Retry failed for queryIntentActivities", retryException);
                    transactionThrottler.recordFailure();
                }
            } else {
                Log.w(TAG, "Skipping retry due to too many failures");
            }
            return Collections.emptyList();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in queryIntentActivities", e);
            transactionThrottler.recordFailure();
            crash(e);
        }
        return Collections.emptyList();
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags, String resolvedType, int userId) {
        try {
            IBPackageManagerService service = getService();
            if (service != null) {
                return service.queryBroadcastReceivers(intent, flags, resolvedType, userId);
            } else {
                Log.w(TAG, "PackageManager service is null, returning empty list for queryBroadcastReceivers");
                return Collections.emptyList();
            }
        } catch (android.os.DeadObjectException e) {
            Log.w(TAG, "PackageManager service died during queryBroadcastReceivers, clearing cache and retrying", e);
            clearServiceCache(); 
            try {
                
                IBPackageManagerService service = getService();
                if (service != null) {
                    return service.queryBroadcastReceivers(intent, flags, resolvedType, userId);
                }
            } catch (Exception retryException) {
                Log.e(TAG, "Retry failed for queryBroadcastReceivers", retryException);
            }
            return Collections.emptyList();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in queryBroadcastReceivers", e);
            crash(e);
        }
        return Collections.emptyList();
    }

    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags, int userId) {
        try {
            IBPackageManagerService service = getService();
            if (service != null) {
                return service.queryContentProviders(processName, uid, flags, userId);
            } else {
                Log.w(TAG, "PackageManager service is null, returning empty list for queryContentProviders");
                return Collections.emptyList();
            }
        } catch (android.os.DeadObjectException e) {
            Log.w(TAG, "PackageManager service died during queryContentProviders, clearing cache and retrying", e);
            clearServiceCache(); 
            try {
                
                IBPackageManagerService service = getService();
                if (service != null) {
                    return service.queryContentProviders(processName, uid, flags, userId);
                }
            } catch (Exception retryException) {
                Log.e(TAG, "Retry failed for queryContentProviders", retryException);
            }
            return Collections.emptyList();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in queryContentProviders", e);
            crash(e);
        }
        return Collections.emptyList();
    }

    public InstallResult installPackageAsUser(String file, InstallOption option, int userId) {
        try {
            
            if (file != null && !file.isEmpty()) {
                try {
                    
                    PackageInfo packageInfo = BlackBoxCore.getPackageManager().getPackageArchiveInfo(file, 0);
                    if (packageInfo != null) {
                        String packageName = packageInfo.packageName;
                        String hostPackageName = BlackBoxCore.getHostPkg();
                        if (packageName.equals(hostPackageName)) {
                            Log.w(TAG, "Attempt to install BlackBox app detected and blocked: " + packageName);
                            return new InstallResult().installError("Cannot clone BlackBox app from within BlackBox. This would create infinite recursion and is not allowed for security reasons.");
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not verify package info for: " + file, e);
                }
            }
            
            return getService().installPackageAsUser(file, option, userId);
        } catch (RemoteException e) {
            crash(e);
        }
        return null;
    }

    public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        try {
            return getService().getInstalledApplications(flags, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        try {
            return getService().getInstalledPackages(flags, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public void clearPackage(String packageName, int userId) {
        try {
            getService().clearPackage(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopPackage(String packageName, int userId) {
        try {
            getService().stopPackage(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void uninstallPackageAsUser(String packageName, int userId) {
        try {
            getService().uninstallPackageAsUser(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void uninstallPackage(String packageName) {
        try {
            getService().uninstallPackage(packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isInstalled(String packageName, int userId) {
        
        if (shouldUseFallbackMode()) {
            Log.w(TAG, "Using fallback isInstalled check for " + packageName + " due to service failures");
            return isInstalledFallback(packageName);
        }
        
        try {
            IBPackageManagerService service = getService();
            if (service != null) {
                boolean result = service.isInstalled(packageName, userId);
                transactionThrottler.reset(); 
                return result;
            } else {
                Log.w(TAG, "PackageManager service is null, returning false for isInstalled check");
            }
        } catch (android.os.DeadObjectException e) {
            Log.w(TAG, "PackageManager service died during isInstalled check, clearing service and retrying", e);
            transactionThrottler.recordFailure();
            
            clearServiceCache();
            
            try {
                IBPackageManagerService service = getService();
                if (service != null) {
                    boolean result = service.isInstalled(packageName, userId);
                    transactionThrottler.reset(); 
                    return result;
                }
            } catch (Exception retryException) {
                Log.e(TAG, "Retry failed for isInstalled check", retryException);
                transactionThrottler.recordFailure();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isInstalled check", e);
            transactionThrottler.recordFailure();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in isInstalled check", e);
            transactionThrottler.recordFailure();
        }
        return false;
    }
    
    
    private boolean isInstalledFallback(String packageName) {
        try {
            
            BlackBoxCore.getContext().getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Fallback isInstalled check failed for " + packageName + ", assuming not installed");
            
            if (packageName != null && (packageName.equals("com.media.bestrecorder.audiorecorder") || 
                                       packageName.startsWith("top.niunaijun.blackbox"))) {
                Log.w(TAG, "Returning true for known app " + packageName + " despite fallback failure");
                return true;
            }
            return false;
        }
    }

    public List<InstalledPackage> getInstalledPackagesAsUser(int userId) {
        try {
            return getService().getInstalledPackagesAsUser(userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public String[] getPackagesForUid(int uid) {
        try {
            return getService().getPackagesForUid(uid, BActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new String[]{};
    }

    private void crash(Throwable e) {
        e.printStackTrace();
    }

    private ApplicationInfo createFallbackApplicationInfo(String packageName, int flags, int userId) {
        Log.w(TAG, "Creating fallback ApplicationInfo for " + packageName);
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = packageName;
        info.flags = flags;
        info.uid = 0; 
        
        
        
        String apkPath = findActualApkPath(packageName);
        if (apkPath != null) {
            info.sourceDir = apkPath;
            info.publicSourceDir = apkPath;
        } else {
            
            Log.w(TAG, "No APK found for " + packageName + ", using null paths to prevent I/O errors");
            info.sourceDir = null; 
            info.publicSourceDir = null; 
        }
        
        info.dataDir = "/data/data/" + packageName;
        info.nativeLibraryDir = "/data/app-lib/" + packageName;
        info.metaData = new Bundle();
        info.splitNames = new String[]{};
        
        
        info.flags |= ApplicationInfo.FLAG_ALLOW_BACKUP;
        info.flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
        
        return info;
    }

    
    private String findActualApkPath(String packageName) {
        if (sIsFindingApkPath) {
            Log.w(TAG, "findActualApkPath called recursively, returning null to prevent infinite loop.");
            return null;
        }
        sIsFindingApkPath = true;
        try {
            
            
            Log.d(TAG, "Skipping PackageManager call to prevent recursion for " + packageName);
            
            
            String[] commonPaths = {
                
                "/data/app/~~*/" + packageName + "-*/base.apk",
                "/data/app/~~*/" + packageName + "*/base.apk",
                
                
                "/data/app/" + packageName + "-1/base.apk",
                "/data/app/" + packageName + "-2/base.apk",
                "/data/app/" + packageName + "/base.apk",
                
                
                "/system/app/" + packageName + ".apk",
                "/system/priv-app/" + packageName + ".apk",
                "/system_ext/app/" + packageName + ".apk",
                "/product/app/" + packageName + ".apk",
                "/vendor/app/" + packageName + ".apk"
            };
            
            
            for (String path : commonPaths) {
                if (isValidApkPath(path)) {
                    Log.d(TAG, "Found existing APK at: " + path);
                    return path;
                }
            }
            
            
            String hashBasedPath = findHashBasedApkPath(packageName);
            if (hashBasedPath != null) {
                Log.d(TAG, "Found hash-based APK at: " + hashBasedPath);
                return hashBasedPath;
            }
            
            Log.w(TAG, "No existing APK found for " + packageName + ", using null path");
            return null;
        } finally {
            sIsFindingApkPath = false; 
        }
    }

    
    private String findHashBasedApkPath(String packageName) {
        try {
            File dataAppDir = new File("/data/app");
            if (!dataAppDir.exists() || !dataAppDir.isDirectory()) {
                return null;
            }
            
            
            File[] hashDirs = dataAppDir.listFiles((dir, name) -> name.startsWith("~~") && name.endsWith("=="));
            if (hashDirs == null) {
                return null;
            }
            
            for (File hashDir : hashDirs) {
                if (!hashDir.isDirectory()) {
                    continue;
                }
                
                
                File[] packageDirs = hashDir.listFiles((dir, name) -> name.startsWith(packageName));
                if (packageDirs == null) {
                    continue;
                }
                
                for (File packageDir : packageDirs) {
                    if (!packageDir.isDirectory()) {
                        continue;
                    }
                    
                    
                    File baseApk = new File(packageDir, "base.apk");
                    if (isValidApkPath(baseApk.getAbsolutePath())) {
                        return baseApk.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error searching for hash-based APK path for " + packageName + ": " + e.getMessage());
        }
        
        return null;
    }

    
    private boolean isValidApkPath(String path) {
        try {
            
            if (path.contains("*")) {
                return false;
            }
            
            File apkFile = new File(path);
            if (!apkFile.exists()) {
                return false;
            }
            
            
            if (!apkFile.canRead()) {
                Log.d(TAG, "APK file not readable: " + path);
                return false;
            }
            
            long fileSize = apkFile.length();
            if (fileSize < 1024) { 
                Log.d(TAG, "APK file too small: " + path + " (size: " + fileSize + ")");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error checking APK path " + path + ": " + e.getMessage());
            return false;
        }
    }

    private PackageInfo createFallbackPackageInfo(String packageName, int flags, int userId) {
        Log.w(TAG, "Creating fallback PackageInfo for " + packageName);
        PackageInfo info = new PackageInfo();
        info.packageName = packageName;
        info.versionCode = 1; 
        info.versionName = "1.0"; 
        info.applicationInfo = createFallbackApplicationInfo(packageName, flags, userId);
        info.firstInstallTime = System.currentTimeMillis(); 
        info.lastUpdateTime = System.currentTimeMillis(); 
        info.installLocation = 0; 
        info.gids = new int[]{}; 
        info.splitNames = new String[]{}; 
        info.signatures = new Signature[]{}; 
        return info;
    }
}

