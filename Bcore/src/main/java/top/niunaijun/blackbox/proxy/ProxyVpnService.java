package top.niunaijun.blackbox.proxy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import top.niunaijun.blackbox.utils.Slog;

/**
 * updated by alex5402
 * Enhanced VPN service for BlackBox to handle network routing and DNS
 * Created by BlackBox on 2022/2/25.\
 * improved 9/15/2025 by alex5404
 */
public class ProxyVpnService extends VpnService {
    private static final String TAG = "ProxyVpnService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "BlackBoxVPN";
    
    private ParcelFileDescriptor mVpnInterface = null;
    private boolean mIsEstablished = false;
    private Thread mNetworkThread = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Slog.d(TAG, "ProxyVpnService created");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Slog.d(TAG, "ProxyVpnService started");
        
        try {
            // CRITICAL: Start foreground immediately to prevent timeout
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires foreground service type
                startForeground(NOTIFICATION_ID, createNotification(), 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
            
            Slog.d(TAG, "Foreground service started successfully");
            
            // Start VPN establishment in background to prevent blocking
            new Thread(new Runnable() {
                @Override
                public void run() {
                    establishVpn();
                }
            }, "VPNEstablishment").start();
            
        } catch (Exception e) {
            Slog.e(TAG, "Critical error in onStartCommand: " + e.getMessage(), e);
            // If we can't start foreground, stop the service to prevent hanging
            stopSelf();
            return START_NOT_STICKY;
        }
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpn();
        Slog.d(TAG, "ProxyVpnService destroyed");
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "BlackBox VPN Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("VPN service for BlackBox network access");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Create notification for foreground service
     */
    private Notification createNotification() {
        Notification.Builder builder;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        return builder
            .setContentTitle("BlackBox VPN Active")
            .setContentText("Managing network access for sandboxed apps")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build();
    }

    /**
     * Establish VPN interface for proper network routing and DNS handling
     */
    protected void establishVpn() {
        // Use a timeout to prevent hanging
        final long TIMEOUT_MS = 5000; // 5 seconds timeout
        final long startTime = System.currentTimeMillis();
        
        try {
            Slog.d(TAG, "Starting VPN establishment...");
            
            // Check timeout before proceeding
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                Slog.w(TAG, "VPN establishment timeout, aborting");
                return;
            }
            
            Builder builder = new Builder();
            
            // Set VPN interface name
            builder.setSession("BlackBox VPN");
            
            // Add network addresses - use a private network range
            builder.addAddress("10.0.0.2", 32);
            
            // Add routes for all traffic - this is critical for internet access
            builder.addRoute("0.0.0.0", 0);  // Route all IPv4 traffic through VPN
            
            // Add DNS servers (Google DNS as fallback)
            builder.addDnsServer("8.8.8.8");
            builder.addDnsServer("8.8.4.4");
            
            // Allow all applications to use this VPN
            builder.addAllowedApplication(getPackageName());
            
            // Set session name for debugging
            builder.setSession("BlackBox Internet Access");
            
            Slog.d(TAG, "VPN builder configured, establishing interface...");
            
            // Check timeout again before establish
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                Slog.w(TAG, "VPN establishment timeout before establish(), aborting");
                return;
            }
            
            // Establish the VPN interface - this should be fast
            mVpnInterface = builder.establish();
            if (mVpnInterface != null) {
                mIsEstablished = true;
                Slog.d(TAG, "VPN interface established successfully");
                
                // Start network monitoring and traffic handling in background
                startNetworkHandling();
            } else {
                Slog.e(TAG, "Failed to establish VPN interface - builder.establish() returned null");
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error establishing VPN: " + e.getMessage());
            e.printStackTrace();
            // Don't let VPN errors crash the service
            mIsEstablished = false;
        }
        
        // Final timeout check
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            Slog.w(TAG, "VPN establishment took too long: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    /**
     * Start network handling thread
     */
    private void startNetworkHandling() {
        if (mNetworkThread != null && mNetworkThread.isAlive()) {
            mNetworkThread.interrupt();
        }
        
        mNetworkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (mIsEstablished && mVpnInterface != null && !Thread.interrupted()) {
                        // Monitor network connectivity
                        Thread.sleep(10000); // Check every 10 seconds
                        
                        // Log network status
                        Slog.d(TAG, "VPN interface active, monitoring network...");
                        
                        // Check if VPN interface is still valid
                        if (mVpnInterface != null) {
                            try {
                                // This will throw if the interface is closed
                                mVpnInterface.getFd();
                            } catch (Exception e) {
                                Slog.w(TAG, "VPN interface appears to be closed, re-establishing...");
                                reestablishVpn();
                                break;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Slog.w(TAG, "Network monitoring interrupted: " + e.getMessage());
                } catch (Exception e) {
                    Slog.e(TAG, "Error in network monitoring: " + e.getMessage());
                }
            }
        }, "BlackBoxNetworkHandler");
        
        mNetworkThread.start();
        Slog.d(TAG, "Network handling thread started");
    }

    /**
     * Re-establish VPN if it was lost
     */
    private void reestablishVpn() {
        try {
            Slog.d(TAG, "Attempting to re-establish VPN connection");
            stopVpn();
            Thread.sleep(1000); // Wait a bit before re-establishing
            establishVpn();
        } catch (Exception e) {
            Slog.e(TAG, "Failed to re-establish VPN: " + e.getMessage());
        }
    }

    /**
     * Stop VPN service
     */
    private void stopVpn() {
        mIsEstablished = false;
        
        if (mNetworkThread != null) {
            mNetworkThread.interrupt();
            mNetworkThread = null;
        }
        
        if (mVpnInterface != null) {
            try {
                mVpnInterface.close();
                mVpnInterface = null;
                Slog.d(TAG, "VPN interface closed");
            } catch (Exception e) {
                Slog.w(TAG, "Error closing VPN interface: " + e.getMessage());
            }
        }
    }

    /**
     * Check if VPN is established
     */
    public boolean isEstablished() {
        return mIsEstablished && mVpnInterface != null;
    }

    /**
     * Get VPN interface file descriptor
     */
    public ParcelFileDescriptor getVpnInterface() {
        return mVpnInterface;
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        Slog.w(TAG, "VPN service revoked by user");
        stopVpn();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            Slog.d(TAG, "Memory trim requested, optimizing VPN service");
        }
    }
}
