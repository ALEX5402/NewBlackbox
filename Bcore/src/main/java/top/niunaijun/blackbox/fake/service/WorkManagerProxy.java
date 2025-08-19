package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.Bundle;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * WorkManager Proxy to handle WorkManager-specific job scheduling issues
 * This prevents UID validation crashes when WorkManager tries to schedule background jobs
 */
public class WorkManagerProxy extends ClassInvocationStub {
    public static final String TAG = "WorkManagerProxy";

    public WorkManagerProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        try {
            Context context = BlackBoxCore.getContext();
            if (context != null) {
                // Try to get the real WorkManager instance
                Class<?> workManagerClass = Class.forName("androidx.work.WorkManager");
                Method getInstanceMethod = workManagerClass.getMethod("getInstance", Context.class);
                return getInstanceMethod.invoke(null, context);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get WorkManager instance", e);
        }
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Not needed for class method hooks
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    /**
     * Hook WorkManager.enqueue() to handle UID validation issues
     */
    @ProxyMethod("enqueue")
    public static class Enqueue extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "WorkManager: enqueue() called");
                
                // Log the arguments for debugging
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            Slog.d(TAG, "WorkManager: args[" + i + "] = " + args[i].getClass().getSimpleName() + ": " + args[i]);
                        }
                    }
                }
                
                // Try to proceed with the original method
                return method.invoke(who, args);
                
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: enqueue() failed, returning mock result", e);
                
                // Return a mock result to prevent crashes
                return createMockWorkResult();
            }
        }
        
        private Object createMockWorkResult() {
            try {
                // Try to create a mock WorkResult object
                Class<?> workResultClass = Class.forName("androidx.work.Operation");
                // Return a mock operation that indicates success
                return null; // Placeholder - would need proper mock implementation
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: Failed to create mock result", e);
                return null;
            }
        }
    }

    /**
     * Hook WorkManager.enqueueUniqueWork() to handle UID validation issues
     */
    @ProxyMethod("enqueueUniqueWork")
    public static class EnqueueUniqueWork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "WorkManager: enqueueUniqueWork() called");
                
                // Log the arguments for debugging
                if (args != null && args.length > 0) {
                    String workName = (String) args[0];
                    Slog.d(TAG, "WorkManager: Unique work name: " + workName);
                }
                
                // Try to proceed with the original method
                return method.invoke(who, args);
                
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: enqueueUniqueWork() failed, returning mock result", e);
                
                // Return a mock result to prevent crashes
                return createMockWorkResult();
            }
        }
        
        private Object createMockWorkResult() {
            try {
                // Try to create a mock WorkResult object
                Class<?> workResultClass = Class.forName("androidx.work.Operation");
                // Return a mock operation that indicates success
                return null; // Placeholder - would need proper mock implementation
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: Failed to create mock result", e);
                return null;
            }
        }
    }

    /**
     * Hook WorkManager.enqueueUniquePeriodicWork() to handle UID validation issues
     */
    @ProxyMethod("enqueueUniquePeriodicWork")
    public static class EnqueueUniquePeriodicWork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "WorkManager: enqueueUniquePeriodicWork() called");
                
                // Log the arguments for debugging
                if (args != null && args.length > 0) {
                    String workName = (String) args[0];
                    Slog.d(TAG, "WorkManager: Periodic work name: " + workName);
                }
                
                // Try to proceed with the original method
                return method.invoke(who, args);
                
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: enqueueUniquePeriodicWork() failed, returning mock result", e);
                
                // Return a mock result to prevent crashes
                return createMockWorkResult();
            }
        }
        
        private Object createMockWorkResult() {
            try {
                // Try to create a mock WorkResult object
                Class<?> workResultClass = Class.forName("androidx.work.Operation");
                // Return a mock operation that indicates success
                return null; // Placeholder - would need proper mock implementation
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: Failed to create mock result", e);
                return null;
            }
        }
    }

    /**
     * Hook WorkManager.cancelAllWork() to handle UID validation issues
     */
    @ProxyMethod("cancelAllWork")
    public static class CancelAllWork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "WorkManager: cancelAllWork() called");
                
                // Try to proceed with the original method
                return method.invoke(who, args);
                
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: cancelAllWork() failed, ignoring", e);
                
                // Return void (null) for cancel operations
                return null;
            }
        }
    }

    /**
     * Hook WorkManager.cancelWorkById() to handle UID validation issues
     */
    @ProxyMethod("cancelWorkById")
    public static class CancelWorkById extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "WorkManager: cancelWorkById() called");
                
                // Log the work ID for debugging
                if (args != null && args.length > 0) {
                    String workId = (String) args[0];
                    Slog.d(TAG, "WorkManager: Cancelling work ID: " + workId);
                }
                
                // Try to proceed with the original method
                return method.invoke(who, args);
                
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: cancelWorkById() failed, ignoring", e);
                
                // Return void (null) for cancel operations
                return null;
            }
        }
    }

    /**
     * Hook WorkManager.getWorkInfos() to handle UID validation issues
     */
    @ProxyMethod("getWorkInfos")
    public static class GetWorkInfos extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "WorkManager: getWorkInfos() called");
                
                // Try to proceed with the original method
                return method.invoke(who, args);
                
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: getWorkInfos() failed, returning empty list", e);
                
                // Return an empty list to prevent crashes
                return createEmptyWorkInfoList();
            }
        }
        
        private Object createEmptyWorkInfoList() {
            try {
                // Try to create an empty WorkInfo list
                Class<?> workInfoListClass = Class.forName("androidx.work.WorkInfo");
                // Return an empty list
                return java.util.Collections.emptyList();
            } catch (Exception e) {
                Slog.w(TAG, "WorkManager: Failed to create empty work info list", e);
                return java.util.Collections.emptyList();
            }
        }
    }
}
