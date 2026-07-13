package top.niunaijun.blackbox.app;

import android.app.job.JobInfo;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * Isolated JobScheduler for Virtual Environment to prevent background tasks from leaking to the Host OS.
 */
public class VirtualJobScheduler {
    private static final String TAG = "VirtualJobScheduler";
    private static VirtualJobScheduler sInstance;
    private final Map<Integer, JobInfo> mVirtualJobs = new HashMap<>();

    private VirtualJobScheduler() {}

    public static synchronized VirtualJobScheduler get() {
        if (sInstance == null) {
            sInstance = new VirtualJobScheduler();
        }
        return sInstance;
    }

    public int schedule(JobInfo job) {
        if (job == null) return 0;
        Log.i(TAG, "Virtual scheduling job: " + job.getId() + " for " + job.getService().getPackageName());
        mVirtualJobs.put(job.getId(), job);
        return 1; // RESULT_SUCCESS
    }

    public void cancel(int jobId) {
        Log.i(TAG, "Virtual canceling job: " + jobId);
        mVirtualJobs.remove(jobId);
    }

    public void cancelAll(String packageName) {
        Log.i(TAG, "Virtual canceling all jobs for: " + packageName);
        mVirtualJobs.entrySet().removeIf(entry ->
            entry.getValue().getService().getPackageName().equals(packageName)
        );
    }
}
