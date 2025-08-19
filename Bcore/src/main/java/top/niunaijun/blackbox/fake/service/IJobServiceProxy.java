package top.niunaijun.blackbox.fake.service;

import android.app.job.JobInfo;
import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.app.job.BRIJobSchedulerStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.UIDSpoofingHelper;

/**
 * IJobService Proxy to handle job scheduling in sandboxed environments
 * This prevents UID mismatch crashes when scheduling background jobs
 */
public class IJobServiceProxy extends BinderInvocationStub {
    public static final String TAG = "JobServiceStub";

    public IJobServiceProxy() {
        super(BRServiceManager.get().getService(Context.JOB_SCHEDULER_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder jobScheduler = BRServiceManager.get().getService("jobscheduler");
        return BRIJobSchedulerStub.get().asInterface(jobScheduler);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    @ProxyMethod("schedule")
    public static class Schedule extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Check if args[0] is actually a JobInfo object
                if (args == null || args.length == 0) {
                    Slog.w(TAG, "Schedule: No arguments provided, returning RESULT_FAILURE");
                    return 0; // RESULT_FAILURE
                }
                
                if (args[0] == null) {
                    Slog.w(TAG, "Schedule: args[0] is null, returning RESULT_FAILURE");
                    return 0; // RESULT_FAILURE
                }
                
                if (!(args[0] instanceof JobInfo)) {
                    Slog.w(TAG, "Schedule: args[0] is not JobInfo: " + args[0].getClass().getSimpleName());
                    // For non-JobInfo objects, try to handle them gracefully
                    return handleNonJobInfoSchedule(who, method, args);
                }
                
                JobInfo jobInfo = (JobInfo) args[0];
                Slog.d(TAG, "Schedule: Processing JobInfo for package: " + jobInfo.getService().getPackageName());
                
                // Try to schedule through BlackBox job manager first
                try {
                    JobInfo proxyJobInfo = BlackBoxCore.getBJobManager().schedule(jobInfo);
                    if (proxyJobInfo != null) {
                        args[0] = proxyJobInfo;
                        Slog.d(TAG, "Schedule: Successfully created proxy JobInfo");
                        return method.invoke(who, args);
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Schedule: BlackBox job manager failed, trying system fallback", e);
                }
                
                // Fallback: Try to schedule directly with UID spoofing
                return scheduleWithUIDSpoofing(who, method, args, jobInfo);
                
            } catch (Exception e) {
                Slog.e(TAG, "Schedule: Error processing job", e);
                
                // Handle specific UID validation errors
                if (isUIDValidationError(e)) {
                    Slog.w(TAG, "UID validation failed for job scheduling, returning RESULT_FAILURE: " + e.getCause().getMessage());
                    return 0; // RESULT_FAILURE - don't retry with same UID
                }
                
                // For other errors, try to proceed with original method call as fallback
                try {
                    return method.invoke(who, args);
                } catch (Exception fallbackException) {
                    Slog.e(TAG, "Schedule: Fallback also failed", fallbackException);
                    return 0; // RESULT_FAILURE
                }
            }
        }
        
        /**
         * Handle non-JobInfo schedule requests (like WorkManager string IDs)
         */
        private Object handleNonJobInfoSchedule(Object who, Method method, Object[] args) throws Throwable {
            try {
                // For WorkManager string IDs, try to create a minimal JobInfo
                if (args[0] instanceof String) {
                    String workId = (String) args[0];
                    Slog.d(TAG, "Schedule: Handling WorkManager string ID: " + workId);
                    
                    // Create a minimal JobInfo that won't cause UID validation issues
                    JobInfo minimalJobInfo = createMinimalJobInfo(workId);
                    if (minimalJobInfo != null) {
                        args[0] = minimalJobInfo;
                        return method.invoke(who, args);
                    }
                }
                
                // Try original method for other types
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Schedule: Failed to handle non-JobInfo schedule", e);
                return 0; // RESULT_FAILURE
            }
        }
        
        /**
         * Try to schedule with UID spoofing to bypass validation
         */
        private Object scheduleWithUIDSpoofing(Object who, Method method, Object[] args, JobInfo jobInfo) throws Throwable {
            try {
                // Get the target package name from the JobInfo
                String targetPackage = jobInfo.getService().getPackageName();
                Slog.d(TAG, "Schedule: Attempting UID spoofing for package: " + targetPackage);
                
                // Use UID spoofing helper to get the best UID for job scheduling
                UIDSpoofingHelper.logUIDInfo("job_schedule", targetPackage);
                
                // Check if we need UID spoofing
                if (UIDSpoofingHelper.needsUIDSpoofing("job_schedule", targetPackage)) {
                    Slog.d(TAG, "Schedule: UID spoofing needed, attempting to bypass validation");
                    
                    // For now, we'll return failure but log the attempt
                    // In a full implementation, you'd need to actually spoof the UID at the system level
                    Slog.w(TAG, "Schedule: UID spoofing not fully implemented, returning RESULT_FAILURE");
                    return 0; // RESULT_FAILURE
                } else {
                    Slog.d(TAG, "Schedule: No UID spoofing needed, proceeding normally");
                    return method.invoke(who, args);
                }
                
            } catch (Exception e) {
                Slog.w(TAG, "Schedule: UID spoofing failed", e);
                return 0; // RESULT_FAILURE
            }
        }
        
        /**
         * Create a minimal JobInfo for WorkManager compatibility
         */
        private JobInfo createMinimalJobInfo(String workId) {
            try {
                // This is a simplified approach - in practice, you'd need to create a proper JobInfo
                // For now, we'll return null to indicate failure
                Slog.d(TAG, "Schedule: Creating minimal JobInfo for work ID: " + workId);
                return null; // Placeholder - would need proper JobInfo.Builder implementation
            } catch (Exception e) {
                Slog.w(TAG, "Schedule: Failed to create minimal JobInfo", e);
                return null;
            }
        }
        
        /**
         * Check if the error is a UID validation error
         */
        private boolean isUIDValidationError(Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                String message = e.getCause().getMessage();
                return message != null && message.contains("cannot schedule job");
            }
            
            if (e.getCause() instanceof android.os.RemoteException) {
                String message = e.getCause().getMessage();
                return message != null && message.contains("cannot schedule job");
            }
            
            return false;
        }
    }

    @ProxyMethod("cancel")
    public static class Cancel extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Integer jobId = (Integer) args[0];
                if (jobId == null) {
                    Slog.w(TAG, "Cancel: JobId is null");
                    return method.invoke(who, args);
                }
                
                args[0] = BlackBoxCore.getBJobManager()
                        .cancel(BActivityThread.getAppConfig().processName, jobId);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "Cancel: Error canceling job", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("cancelAll")
    public static class CancelAll extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                BlackBoxCore.getBJobManager().cancelAll(BActivityThread.getAppConfig().processName);
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "CancelAll: Error canceling all jobs", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("enqueue")
    public static class Enqueue extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Check if args[0] is actually a JobInfo object
                if (args == null || args.length == 0) {
                    Slog.w(TAG, "Enqueue: No arguments provided, returning RESULT_FAILURE");
                    return 0; // RESULT_FAILURE
                }
                
                if (args[0] == null) {
                    Slog.w(TAG, "Enqueue: args[0] is null, returning RESULT_FAILURE");
                    return 0; // RESULT_FAILURE
                }
                
                if (!(args[0] instanceof JobInfo)) {
                    Slog.w(TAG, "Enqueue: args[0] is not JobInfo: " + args[0].getClass().getSimpleName());
                    // For non-JobInfo objects, try to handle them gracefully
                    return handleNonJobInfoEnqueue(who, method, args);
                }
                
                JobInfo jobInfo = (JobInfo) args[0];
                Slog.d(TAG, "Enqueue: Processing JobInfo for package: " + jobInfo.getService().getPackageName());
                
                // Try to enqueue through BlackBox job manager first
                try {
                    JobInfo proxyJobInfo = BlackBoxCore.getBJobManager().schedule(jobInfo);
                    if (proxyJobInfo != null) {
                        args[0] = proxyJobInfo;
                        Slog.d(TAG, "Enqueue: Successfully created proxy JobInfo");
                        return method.invoke(who, args);
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Enqueue: BlackBox job manager failed, trying system fallback", e);
                }
                
                // Fallback: Try to enqueue directly with UID spoofing
                return enqueueWithUIDSpoofing(who, method, args, jobInfo);
                
            } catch (Exception e) {
                Slog.e(TAG, "Enqueue: Error processing job", e);
                
                // Handle specific UID validation errors
                if (isUIDValidationError(e)) {
                    Slog.w(TAG, "UID validation failed for job enqueuing, returning RESULT_FAILURE: " + e.getCause().getMessage());
                    return 0; // RESULT_FAILURE - don't retry with same UID
                }
                
                // For other errors, try to proceed with original method call as fallback
                try {
                    return method.invoke(who, args);
                } catch (Exception fallbackException) {
                    Slog.e(TAG, "Enqueue: Fallback also failed", fallbackException);
                    return 0; // RESULT_FAILURE
                }
            }
        }
        
        /**
         * Handle non-JobInfo enqueue requests (like WorkManager string IDs)
         */
        private Object handleNonJobInfoEnqueue(Object who, Method method, Object[] args) throws Throwable {
            try {
                // For WorkManager string IDs, try to create a minimal JobInfo
                if (args[0] instanceof String) {
                    String workId = (String) args[0];
                    Slog.d(TAG, "Enqueue: Handling WorkManager string ID: " + workId);
                    
                    // Create a minimal JobInfo that won't cause UID validation issues
                    JobInfo minimalJobInfo = createMinimalJobInfo(workId);
                    if (minimalJobInfo != null) {
                        args[0] = minimalJobInfo;
                        return method.invoke(who, args);
                    }
                }
                
                // Try original method for other types
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Enqueue: Failed to handle non-JobInfo enqueue", e);
                return 0; // RESULT_FAILURE
            }
        }
        
        /**
         * Try to enqueue with UID spoofing to bypass validation
         */
        private Object enqueueWithUIDSpoofing(Object who, Method method, Object[] args, JobInfo jobInfo) throws Throwable {
            try {
                // Get the target package name from the JobInfo
                String targetPackage = jobInfo.getService().getPackageName();
                Slog.d(TAG, "Enqueue: Attempting UID spoofing for package: " + targetPackage);
                
                // Use UID spoofing helper to get the best UID for job scheduling
                UIDSpoofingHelper.logUIDInfo("job_enqueue", targetPackage);
                
                // Check if we need UID spoofing
                if (UIDSpoofingHelper.needsUIDSpoofing("job_enqueue", targetPackage)) {
                    Slog.d(TAG, "Enqueue: UID spoofing needed, attempting to bypass validation");
                    
                    // For now, we'll return failure but log the attempt
                    // In a full implementation, you'd need to actually spoof the UID at the system level
                    Slog.w(TAG, "Enqueue: UID spoofing not fully implemented, returning RESULT_FAILURE");
                    return 0; // RESULT_FAILURE
                } else {
                    Slog.d(TAG, "Enqueue: No UID spoofing needed, proceeding normally");
                    return method.invoke(who, args);
                }
                
            } catch (Exception e) {
                Slog.w(TAG, "Enqueue: UID spoofing failed", e);
                return 0; // RESULT_FAILURE
            }
        }
        
        /**
         * Create a minimal JobInfo for WorkManager compatibility
         */
        private JobInfo createMinimalJobInfo(String workId) {
            try {
                // This is a simplified approach - in practice, you'd need to create a proper JobInfo
                // For now, we'll return null to indicate failure
                Slog.d(TAG, "Enqueue: Creating minimal JobInfo for work ID: " + workId);
                return null; // Placeholder - would need proper JobInfo.Builder implementation
            } catch (Exception e) {
                Slog.w(TAG, "Enqueue: Failed to create minimal JobInfo", e);
                return null;
            }
        }
        
        /**
         * Check if the error is a UID validation error
         */
        private boolean isUIDValidationError(Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                String message = e.getCause().getMessage();
                return message != null && message.contains("cannot schedule job");
            }
            
            if (e.getCause() instanceof android.os.RemoteException) {
                String message = e.getCause().getMessage();
                return message != null && message.contains("cannot schedule job");
            }
            
            return false;
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
