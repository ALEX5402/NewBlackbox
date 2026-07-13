package top.niunaijun.blackbox.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import top.niunaijun.blackbox.BlackBoxCore;

public class ProcessFreezer {
    private static final String TAG = "ProcessFreezer";

    // 5 minutes timeout before killing background process to save memory
    private static final long FREEZE_TIMEOUT_MS = 5 * 60 * 1000;

    private static int sActiveActivityCount = 0;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    private static final Runnable sFreezeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Log.i(TAG, "Process idle timeout reached. Freezing/Killing stub process to save memory: " + Process.myPid());
                Process.killProcess(Process.myPid());
                System.exit(0);
            } catch (Exception e) {
                Log.w(TAG, "Failed to kill process", e);
            }
        }
    };

    public static synchronized void onActivityResumed() {
        sActiveActivityCount++;
        // Cancel freeze if any activity comes to foreground
        sHandler.removeCallbacks(sFreezeRunnable);
        Log.d(TAG, "Activity resumed. Active count: " + sActiveActivityCount + ". Freeze canceled.");
    }

    public static synchronized void onActivityPaused() {
        sActiveActivityCount--;
        if (sActiveActivityCount < 0) sActiveActivityCount = 0;

        // If all activities are paused/background, schedule freeze
        if (sActiveActivityCount == 0) {
            Log.d(TAG, "All activities in background. Scheduling process freeze in " + (FREEZE_TIMEOUT_MS / 1000) + "s.");
            sHandler.postDelayed(sFreezeRunnable, FREEZE_TIMEOUT_MS);
        }
    }
}
