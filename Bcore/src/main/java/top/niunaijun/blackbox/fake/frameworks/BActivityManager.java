package top.niunaijun.blackbox.fake.frameworks;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;

import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.system.ServiceManager;
import top.niunaijun.blackbox.core.system.am.IBActivityManagerService;
import top.niunaijun.blackbox.entity.AppConfig;
import top.niunaijun.blackbox.entity.UnbindRecord;
import top.niunaijun.blackbox.entity.am.PendingResultData;
import top.niunaijun.blackbox.entity.am.RunningAppProcessInfo;
import top.niunaijun.blackbox.entity.am.RunningServiceInfo;
import top.niunaijun.blackbox.utils.Slog;

/**
 * updated by alex5402 on 4/14/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class BActivityManager extends BlackManager<IBActivityManagerService> {
    private static final String TAG = "BActivityManager";
    private static final BActivityManager sActivityManager = new BActivityManager();

    public static BActivityManager get() {
        return sActivityManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.ACTIVITY_MANAGER;
    }

    public AppConfig initProcess(String packageName, String processName, int userId) {
        int retryCount = 0;
        final int maxRetries = 3;
        
        while (retryCount < maxRetries) {
            try {
                IBActivityManagerService service = getService();
                if (service != null) {
                    AppConfig result = service.initProcess(packageName, processName, userId);
                    if (result != null) {
                        return result;
                    } else {
                        Slog.w(TAG, "initProcess returned null for package: " + packageName + ", process: " + processName + ", retry " + (retryCount + 1) + "/" + maxRetries);
                    }
                } else {
                    Slog.w(TAG, "ActivityManager service is null for initProcess, retry " + (retryCount + 1) + "/" + maxRetries);
                }
            } catch (DeadObjectException e) {
                Slog.w(TAG, "ActivityManager service died during initProcess, clearing cache and retrying " + (retryCount + 1) + "/" + maxRetries, e);
                clearServiceCache();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException in initProcess", e);
                break;
            } catch (Exception e) {
                Slog.e(TAG, "Unexpected error in initProcess", e);
                break;
            }
            retryCount++;
        }
        
        Slog.e(TAG, "Failed to initProcess after " + maxRetries + " retries for package: " + packageName + ", process: " + processName);
        return null;
    }

    public void restartProcess(String packageName, String processName, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.restartProcess(packageName, processName, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startActivity(Intent intent, int userId) {
        int retryCount = 0;
        final int maxRetries = 3;
        
        while (retryCount < maxRetries) {
            try {
                IBActivityManagerService service = getService();
                if (service != null) {
                    service.startActivity(intent, userId);
                    return; // Success, exit
                } else {
                    Slog.w(TAG, "ActivityManager service is null, retry " + (retryCount + 1) + "/" + maxRetries);
                    // Wait a bit longer for service to become available
                    try {
                        Thread.sleep(200 * (retryCount + 1)); // Progressive delay
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (DeadObjectException e) {
                Slog.w(TAG, "ActivityManager service died, clearing cache and retrying " + (retryCount + 1) + "/" + maxRetries);
                clearServiceCache(); // Clear the dead service
                try {
                    Thread.sleep(100); // Brief pause before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException in startActivity", e);
                break; // Don't retry for other RemoteExceptions
            } catch (Exception e) {
                Slog.e(TAG, "Unexpected error in startActivity", e);
                break;
            }
            retryCount++;
        }
        
        Slog.e(TAG, "Failed to start activity after " + maxRetries + " retries");
    }

    public int startActivityAms(int userId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, Bundle options) {
        int retryCount = 0;
        final int maxRetries = 3;
        
        while (retryCount < maxRetries) {
            try {
                IBActivityManagerService service = getService();
                if (service != null) {
                    return service.startActivityAms(userId, intent, resolvedType, resultTo, resultWho, requestCode, flags, options);
                } else {
                    Slog.w(TAG, "ActivityManager service is null, retry " + (retryCount + 1) + "/" + maxRetries);
                }
            } catch (DeadObjectException e) {
                Slog.w(TAG, "ActivityManager service died, clearing cache and retrying " + (retryCount + 1) + "/" + maxRetries);
                clearServiceCache();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException in startActivityAms", e);
                break;
            } catch (Exception e) {
                Slog.e(TAG, "Unexpected error in startActivityAms", e);
                break;
            }
            retryCount++;
        }
        
        Slog.e(TAG, "Failed to start activity AMS after " + maxRetries + " retries");
        return -1;
    }

    public int startActivities(int userId, Intent[] intent, String[] resolvedType, IBinder resultTo, Bundle options) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                return service.startActivities(userId, intent, resolvedType, resultTo, options);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public ComponentName startService(Intent intent, String resolvedType, boolean requireForeground, int userId) {
        int retryCount = 0;
        final int maxRetries = 3;
        
        while (retryCount < maxRetries) {
            try {
                IBActivityManagerService service = getService();
                if (service != null) {
                    return service.startService(intent, resolvedType, requireForeground, userId);
                } else {
                    Slog.w(TAG, "ActivityManager service is null, retry " + (retryCount + 1) + "/" + maxRetries);
                }
            } catch (DeadObjectException e) {
                Slog.w(TAG, "ActivityManager service died, clearing cache and retrying " + (retryCount + 1) + "/" + maxRetries);
                clearServiceCache();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException in startService", e);
                break;
            } catch (Exception e) {
                Slog.e(TAG, "Unexpected error in startService", e);
                break;
            }
            retryCount++;
        }
        
        Slog.e(TAG, "Failed to start service after " + maxRetries + " retries");
        return null;
    }

    public int stopService(Intent intent, String resolvedType, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                return service.stopService(intent, resolvedType, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Intent bindService(Intent service, IBinder binder, String resolvedType, int userId) {
        try {
            IBActivityManagerService serviceManager = getService();
            if (serviceManager != null) {
                return serviceManager.bindService(service, binder, resolvedType, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void unbindService(IBinder binder, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.unbindService(binder, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopServiceToken(ComponentName componentName, IBinder token, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.stopServiceToken(componentName, token, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onStartCommand(Intent proxyIntent, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.onStartCommand(proxyIntent, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public UnbindRecord onServiceUnbind(Intent proxyIntent, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                return service.onServiceUnbind(proxyIntent, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onServiceDestroy(Intent proxyIntent, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.onServiceDestroy(proxyIntent, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public IBinder acquireContentProviderClient(ProviderInfo providerInfo) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                return service.acquireContentProviderClient(providerInfo);
            } else {
                Slog.w(TAG, "ActivityManager service is null for acquireContentProviderClient");
            }
        } catch (DeadObjectException e) {
            Slog.w(TAG, "ActivityManager service died during acquireContentProviderClient, clearing cache", e);
            clearServiceCache();
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException in acquireContentProviderClient", e);
        } catch (Exception e) {
            Slog.e(TAG, "Unexpected error in acquireContentProviderClient", e);
        }
        return null;
    }

    public Intent sendBroadcast(Intent intent, String resolvedType, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                return service.sendBroadcast(intent, resolvedType, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public IBinder peekService(Intent intent, String resolvedType, int userId) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                return service.peekService(intent, resolvedType, userId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onActivityCreated(int taskId, IBinder token, IBinder activityRecord) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.onActivityCreated(taskId, token, activityRecord);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onActivityResumed(IBinder token) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.onActivityResumed(token);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onActivityDestroyed(IBinder token) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.onActivityDestroyed(token);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onFinishActivity(IBinder token) {
        try {
            IBActivityManagerService service = getService();
            if (service != null) {
                service.onFinishActivity(token);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public RunningAppProcessInfo getRunningAppProcesses(String callerPackage, int userId) throws RemoteException {
        try {
            return getService().getRunningAppProcesses(callerPackage, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public RunningServiceInfo getRunningServices(String callerPackage, int userId) throws RemoteException {
        try {
            return getService().getRunningServices(callerPackage, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void scheduleBroadcastReceiver(Intent intent, PendingResultData pendingResultData, int userId) throws RemoteException {
        getService().scheduleBroadcastReceiver(intent, pendingResultData, userId);
    }

    public void finishBroadcast(PendingResultData data) {
        try {
            getService().finishBroadcast(data);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getCallingPackage(IBinder token, int userId) {
        try {
            return getService().getCallingPackage(token, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ComponentName getCallingActivity(IBinder token, int userId) {
        try {
            return getService().getCallingActivity(token, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getIntentSender(IBinder target, String packageName, int uid) {
        try {
            getService().getIntentSender(target, packageName, uid, BActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getPackageForIntentSender(IBinder target) {
        try {
            return getService().getPackageForIntentSender(target, BActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getUidForIntentSender(IBinder target) {
        try {
            return getService().getUidForIntentSender(target, BActivityThread.getUserId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
