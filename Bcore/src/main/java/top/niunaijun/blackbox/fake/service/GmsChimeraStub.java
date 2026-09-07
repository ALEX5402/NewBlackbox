package top.niunaijun.blackbox.fake.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;


public class GmsChimeraStub extends Binder implements IInterface {
    public static final String DESCRIPTOR = "com.google.android.gms.common.internal.IGmsServiceBroker";

    public GmsChimeraStub() {
        attachInterface(this, DESCRIPTOR);
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == IBinder.INTERFACE_TRANSACTION) {
            if (reply != null) {
                reply.writeString(DESCRIPTOR);
            }
            return true;
        }
        if (reply != null) {
            reply.writeNoException();
        }
        return true;
    }
}
