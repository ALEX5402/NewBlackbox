package top.niunaijun.blackbox.fake.delegate;

import android.app.IBinderSession;
import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import black.android.app.BRIServiceConnectionO;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


public class ServiceConnectionDelegate extends IServiceConnection.Stub {
    private static final Map<IBinder, ServiceConnectionDelegate> sServiceConnectDelegate = new HashMap<>();
    private final IServiceConnection mConn;
    private final ComponentName mComponentName;

    private ServiceConnectionDelegate(IServiceConnection mConn, ComponentName targetComponent) {
        this.mConn = mConn;
        this.mComponentName = targetComponent;
    }

    public static ServiceConnectionDelegate getDelegate(IBinder iBinder) {
        return sServiceConnectDelegate.get(iBinder);
    }

    public static IServiceConnection createProxy(IServiceConnection base, Intent intent) {
        final IBinder iBinder = base.asBinder();
        ServiceConnectionDelegate delegate = sServiceConnectDelegate.get(iBinder);
        if (delegate == null) {
            try {
                iBinder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        sServiceConnectDelegate.remove(iBinder);
                        iBinder.unlinkToDeath(this, 0);
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            delegate = new ServiceConnectionDelegate(base, intent.getComponent());
            sServiceConnectDelegate.put(iBinder, delegate);
        }
        return delegate;
    }

    @Override
    public void connected(ComponentName name, IBinder service) throws RemoteException {
        connected(name, service, false);
    }

    public void connected(ComponentName name, IBinder service, boolean dead) throws RemoteException {
        ComponentName target = mComponentName != null ? mComponentName : name;
        if (BuildCompat.isOreo()) {
            try {
                BRIServiceConnectionO.get(mConn).connected(target, service, dead);
                return;
            } catch (Throwable ignored) {
            }
        }
        try {
            mConn.connected(target, service);
        } catch (Throwable ignored) {
        }
    }

    // Android 16 & 17+ (SDK 36, 37) compatibility method
    public void connected(ComponentName name, IBinder service, IBinderSession session, boolean dead) throws RemoteException {
        ComponentName target = mComponentName != null ? mComponentName : name;
        if (mConn != null) {
            try {
                Method method = mConn.getClass().getMethod("connected", ComponentName.class, IBinder.class, IBinderSession.class, boolean.class);
                method.invoke(mConn, target, service, session, dead);
                return;
            } catch (Throwable ignored) {
            }
        }
        connected(target, service, dead);
    }
}
