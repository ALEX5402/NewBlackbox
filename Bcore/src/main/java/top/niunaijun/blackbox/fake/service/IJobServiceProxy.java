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
                if (args == null || args.length == 0 || args[0] == null) {
                    return 0; 
                }
                
                if (!(args[0] instanceof JobInfo)) {
                    return handleNonJobInfoSchedule(who, method, args);
                }
                
                JobInfo jobInfo = (JobInfo) args[0];
                
                // Phase 3 Fix: Redirect to VirtualJobScheduler to isolate background tasks from host System OS
                return top.niunaijun.blackbox.app.VirtualJobScheduler.get().schedule(jobInfo);
                
            } catch (Exception e) {
                Slog.e(TAG, "Schedule: Error processing job", e);
                return 0;
            }
        }
        
        
        private Object handleNonJobInfoSchedule(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                if (args[0] instanceof String) {
                    String workId = (String) args[0];
                    Slog.d(TAG, "Schedule: Handling WorkManager string ID: " + workId);
                    
                    
                    JobInfo minimalJobInfo = createMinimalJobInfo(workId);
                    if (minimalJobInfo != null) {
                        args[0] = minimalJobInfo;
                        return method.invoke(who, args);
                    }
                }
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Schedule: Failed to handle non-JobInfo schedule", e);
                return 0; 
            }
        }
        
        
        private Object scheduleWithUIDSpoofing(Object who, Method method, Object[] args, JobInfo jobInfo) throws Throwable {
            try {
                
                String targetPackage = jobInfo.getService().getPackageName();
                Slog.d(TAG, "Schedule: Attempting UID spoofing for package: " + targetPackage);
                
                
                UIDSpoofingHelper.logUIDInfo("job_schedule", targetPackage);
                
                
                if (UIDSpoofingHelper.needsUIDSpoofing("job_schedule", targetPackage)) {
                    Slog.d(TAG, "Schedule: UID spoofing needed, attempting to bypass validation");
                    
                    
                    
                    Slog.w(TAG, "Schedule: UID spoofing not fully implemented, returning RESULT_FAILURE");
                    return 0; 
                } else {
                    Slog.d(TAG, "Schedule: No UID spoofing needed, proceeding normally");
                    return method.invoke(who, args);
                }
                
            } catch (Exception e) {
                Slog.w(TAG, "Schedule: UID spoofing failed", e);
                return 0; 
            }
        }
        
        
        private JobInfo createMinimalJobInfo(String workId) {
            try {
                
                
                Slog.d(TAG, "Schedule: Creating minimal JobInfo for work ID: " + workId);
                return null; 
            } catch (Exception e) {
                Slog.w(TAG, "Schedule: Failed to create minimal JobInfo", e);
                return null;
            }
        }
        
        
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
                if (jobId != null) {
                    top.niunaijun.blackbox.app.VirtualJobScheduler.get().cancel(jobId);
                }
                return 0;
            } catch (Exception e) {
                return 0;
            }
        }
    }

    @ProxyMethod("cancelAll")
    public static class CancelAll extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                top.niunaijun.blackbox.app.VirtualJobScheduler.get().cancelAll(top.niunaijun.blackbox.app.BActivityThread.getAppConfig().processName);
                return 0;
            } catch (Exception e) {
                return 0;
            }
        }
    }

    @ProxyMethod("enqueue")
    public static class Enqueue extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args == null || args.length == 0 || args[0] == null) {
                    return 0; 
                }
                
                if (!(args[0] instanceof JobInfo)) {
                    return handleNonJobInfoEnqueue(who, method, args);
                }
                
                JobInfo jobInfo = (JobInfo) args[0];
                
                // Phase 3 Fix: Redirect to VirtualJobScheduler
                return top.niunaijun.blackbox.app.VirtualJobScheduler.get().schedule(jobInfo);
                
            } catch (Exception e) {
                Slog.e(TAG, "Enqueue: Error processing job", e);
                return 0;
            }
        }
        
        
        private Object handleNonJobInfoEnqueue(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                if (args[0] instanceof String) {
                    String workId = (String) args[0];
                    Slog.d(TAG, "Enqueue: Handling WorkManager string ID: " + workId);
                    
                    
                    JobInfo minimalJobInfo = createMinimalJobInfo(workId);
                    if (minimalJobInfo != null) {
                        args[0] = minimalJobInfo;
                        return method.invoke(who, args);
                    }
                }
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Enqueue: Failed to handle non-JobInfo enqueue", e);
                return 0; 
            }
        }
        
        
        private Object enqueueWithUIDSpoofing(Object who, Method method, Object[] args, JobInfo jobInfo) throws Throwable {
            try {
                
                String targetPackage = jobInfo.getService().getPackageName();
                Slog.d(TAG, "Enqueue: Attempting UID spoofing for package: " + targetPackage);
                
                
                UIDSpoofingHelper.logUIDInfo("job_enqueue", targetPackage);
                
                
                if (UIDSpoofingHelper.needsUIDSpoofing("job_enqueue", targetPackage)) {
                    Slog.d(TAG, "Enqueue: UID spoofing needed, attempting to bypass validation");
                    
                    
                    
                    Slog.w(TAG, "Enqueue: UID spoofing not fully implemented, returning RESULT_FAILURE");
                    return 0; 
                } else {
                    Slog.d(TAG, "Enqueue: No UID spoofing needed, proceeding normally");
                    return method.invoke(who, args);
                }
                
            } catch (Exception e) {
                Slog.w(TAG, "Enqueue: UID spoofing failed", e);
                return 0; 
            }
        }
        
        
        private JobInfo createMinimalJobInfo(String workId) {
            try {
                
                
                Slog.d(TAG, "Enqueue: Creating minimal JobInfo for work ID: " + workId);
                return null; 
            } catch (Exception e) {
                Slog.w(TAG, "Enqueue: Failed to create minimal JobInfo", e);
                return null;
            }
        }
        
        
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
