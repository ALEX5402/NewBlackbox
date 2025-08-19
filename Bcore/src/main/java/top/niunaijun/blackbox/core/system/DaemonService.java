package top.niunaijun.blackbox.core.system;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

/**
 * Enhanced DaemonService with proper foreground service support and error handling.
 * Fixed for Android 14+ compatibility and proper notification setup.
 */
public class DaemonService extends Service {
    public static final String TAG = "DaemonService";
    private static final int NOTIFY_ID = BlackBoxCore.getHostPkg().hashCode();
    private static final String CHANNEL_ID = "blackbox_daemon_channel";
    private static final String CHANNEL_NAME = "BlackBox Daemon Service";
    private static final String CHANNEL_DESCRIPTION = "Keeps BlackBox core services running";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DaemonService onCreate");
        
        // Create notification channel for Android 8.0+
        if (BuildCompat.isOreo()) {
            createNotificationChannel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "DaemonService onStartCommand");
        
        try {
            // Start the inner service
            Intent innerIntent = new Intent(this, DaemonInnerService.class);
            startService(innerIntent);
            
            // Start foreground service for Android 8.0+
            if (BuildCompat.isOreo()) {
                if (!startForegroundService()) {
                    Log.w(TAG, "Failed to start foreground service, falling back to regular service");
                    return START_STICKY;
                }
            }
            
            Log.d(TAG, "DaemonService started successfully");
            return START_STICKY;
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting DaemonService: " + e.getMessage(), e);
            // Return START_STICKY to allow the system to restart the service
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "DaemonService onDestroy");
        super.onDestroy();
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription(CHANNEL_DESCRIPTION);
                channel.setShowBadge(false);
                channel.setSound(null, null);
                channel.enableVibration(false);
                
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created successfully");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create notification channel: " + e.getMessage(), e);
        }
    }

    /**
     * Start foreground service with proper notification
     */
    private boolean startForegroundService() {
        try {
            Notification notification = createNotification();
            if (notification != null) {
                startForeground(NOTIFY_ID, notification);
                Log.d(TAG, "Foreground service started successfully");
                return true;
            } else {
                Log.e(TAG, "Failed to create notification");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create proper notification for foreground service
     */
    private Notification createNotification() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BlackBox Core")
                .setContentText("Core services are running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false);
            
            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create notification: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Inner service to handle notification cancellation
     */
    public static class DaemonInnerService extends Service {
        @Override
        public void onCreate() {
            Log.i(TAG, "DaemonInnerService -> onCreate");
            super.onCreate();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.i(TAG, "DaemonInnerService -> onStartCommand");
            
            try {
                // Cancel the notification from the main service
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.cancel(NOTIFY_ID);
                    Log.d(TAG, "Notification cancelled successfully");
                }
                
                // Stop this inner service
                stopSelf();
                return START_NOT_STICKY;
                
            } catch (Exception e) {
                Log.e(TAG, "Error in DaemonInnerService: " + e.getMessage(), e);
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "DaemonInnerService -> onDestroy");
            super.onDestroy();
        }
    }
}
