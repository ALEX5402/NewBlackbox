package top.niunaijun.blackbox.fake.frameworks;

import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.Reflector;


public abstract class BlackManager<Service extends IInterface> {
    public static final String TAG = "BlackManager";

    private Service mService;
    private final AtomicBoolean mServiceCreationFailed = new AtomicBoolean(false);
    private long mLastRetryTime = 0;
    private long mLastServiceCreationTime = 0;
    private static final long RETRY_TIMEOUT_MS = 2000; 
    private static final long MIN_SERVICE_CREATION_INTERVAL_MS = 50; 
    
    
    private static final AtomicInteger globalServiceFailureCount = new AtomicInteger(0);
    private static final long GLOBAL_FAILURE_RESET_INTERVAL_MS = 30000; 
    private static long lastGlobalFailureReset = 0;

    protected abstract String getServiceName();

    public Service getService() {
        
        if (mServiceCreationFailed.get()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastRetryTime < RETRY_TIMEOUT_MS) {
                Log.d(TAG, "Skipping service creation for " + getServiceName() + " due to recent failure");
                return null;
            }
            
            mServiceCreationFailed.set(false);
        }

        if (mService != null && mService.asBinder().pingBinder() && mService.asBinder().isBinderAlive()) {
            return mService;
        }

        
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastServiceCreationTime < MIN_SERVICE_CREATION_INTERVAL_MS) {
            Log.d(TAG, "Rate limiting service creation for " + getServiceName());
            return mService; 
        }

        try {
            IBinder binder = BlackBoxCore.get().getService(getServiceName());
            if (binder == null) {
                Log.w(TAG, "Failed to get binder for service: " + getServiceName());
                markServiceCreationFailed();
                return null;
            }
            
            
            if (!binder.isBinderAlive()) {
                Log.w(TAG, "Binder is not alive for service: " + getServiceName());
                markServiceCreationFailed();
                return null;
            }
            
            String stubClassName = getTClass().getName() + "$Stub";
            Log.d(TAG, "Creating service for: " + stubClassName);
            
            mService = Reflector.on(stubClassName).method("asInterface", IBinder.class)
                    .call(binder);
            
            if (mService != null) {
                
                try {
                    if (!mService.asBinder().isBinderAlive()) {
                        Log.w(TAG, "Service binder is not alive after creation: " + getServiceName());
                        mService = null;
                        markServiceCreationFailed();
                        return null;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error checking service binder health: " + getServiceName(), e);
                    mService = null;
                    markServiceCreationFailed();
                    return null;
                }
                
                final Service serviceRef = mService; 
                try {
                    serviceRef.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            try {
                                if (serviceRef != null && serviceRef.asBinder() != null) {
                                    serviceRef.asBinder().unlinkToDeath(this, 0);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error unlinking death recipient for " + getServiceName(), e);
                            }
                            mService = null;
                            Log.w(TAG, "Service died: " + getServiceName());
                        }
                    }, 0);
                } catch (Exception e) {
                    Log.w(TAG, "Error linking death recipient for " + getServiceName(), e);
                    
                }
                
                Log.d(TAG, "Successfully created service: " + getServiceName());
                mServiceCreationFailed.set(false); 
                mLastServiceCreationTime = currentTime; 
            } else {
                Log.w(TAG, "Failed to create service instance for: " + getServiceName());
                markServiceCreationFailed();
            }
            
            return mService;
        } catch (Throwable e) {
            Log.e(TAG, "Error creating service for " + getServiceName(), e);
            markServiceCreationFailed();
            return null;
        }
    }

    private void markServiceCreationFailed() {
        mServiceCreationFailed.set(true);
        mLastRetryTime = System.currentTimeMillis();
    }
    
    
    public void clearServiceCache() {
        mService = null;
        Log.d(TAG, "Cleared service cache for " + getServiceName());
    }
    
    
    public boolean isServiceHealthy() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.asBinder().pingBinder() && mService.asBinder().isBinderAlive();
        } catch (Exception e) {
            Log.w(TAG, "Service health check failed for " + getServiceName(), e);
            return false;
        }
    }

    private Class<Service> getTClass() {
        return (Class<Service>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
