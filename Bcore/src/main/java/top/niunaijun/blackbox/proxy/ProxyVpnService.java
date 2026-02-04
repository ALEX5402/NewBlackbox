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
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                
                startForeground(NOTIFICATION_ID, createNotification(), 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
            
            Slog.d(TAG, "Foreground service started successfully");
            
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    establishVpn();
                }
            }, "VPNEstablishment").start();
            
        } catch (Exception e) {
            Slog.e(TAG, "Critical error in onStartCommand: " + e.getMessage(), e);
            
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

    
    protected void establishVpn() {
        
        final long TIMEOUT_MS = 5000; 
        final long startTime = System.currentTimeMillis();
        
        try {
            Slog.d(TAG, "Starting VPN establishment...");
            
            
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                Slog.w(TAG, "VPN establishment timeout, aborting");
                return;
            }
            
            Builder builder = new Builder();
            
            
            builder.setSession("BlackBox VPN");
            
            
            builder.addAddress("10.0.0.2", 32);
            
            
            builder.addRoute("0.0.0.0", 0);  
            
            
            builder.addDnsServer("8.8.8.8");
            builder.addDnsServer("8.8.4.4");
            
            
            builder.addAllowedApplication(getPackageName());
            
            
            builder.setSession("BlackBox Internet Access");
            
            Slog.d(TAG, "VPN builder configured, establishing interface...");
            
            
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                Slog.w(TAG, "VPN establishment timeout before establish(), aborting");
                return;
            }
            
            
            mVpnInterface = builder.establish();
            if (mVpnInterface != null) {
                mIsEstablished = true;
                Slog.d(TAG, "VPN interface established successfully");
                
                
                startNetworkHandling();
            } else {
                Slog.e(TAG, "Failed to establish VPN interface - builder.establish() returned null");
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error establishing VPN: " + e.getMessage());
            e.printStackTrace();
            
            mIsEstablished = false;
        }
        
        
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            Slog.w(TAG, "VPN establishment took too long: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    
    private void startNetworkHandling() {
        if (mNetworkThread != null && mNetworkThread.isAlive()) {
            mNetworkThread.interrupt();
        }
        
        mNetworkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (mIsEstablished && mVpnInterface != null && !Thread.interrupted()) {
                        
                        Thread.sleep(10000); 
                        
                        
                        Slog.d(TAG, "VPN interface active, monitoring network...");
                        
                        
                        if (mVpnInterface != null) {
                            try {
                                
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

    
    private void reestablishVpn() {
        try {
            Slog.d(TAG, "Attempting to re-establish VPN connection");
            stopVpn();
            Thread.sleep(1000); 
            establishVpn();
        } catch (Exception e) {
            Slog.e(TAG, "Failed to re-establish VPN: " + e.getMessage());
        }
    }

    
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

    
    public boolean isEstablished() {
        return mIsEstablished && mVpnInterface != null;
    }

    
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
