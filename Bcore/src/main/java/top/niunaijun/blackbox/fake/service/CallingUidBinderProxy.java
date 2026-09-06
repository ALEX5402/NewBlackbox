package top.niunaijun.blackbox.fake.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;


public class CallingUidBinderProxy extends Binder {
    private final IBinder mBase;
    private final int mCallingUid;
    private final String mCallingPkg;

    public CallingUidBinderProxy(IBinder base, int callingUid, String callingPkg) {
        mBase = base;
        mCallingUid = callingUid;
        mCallingPkg = callingPkg;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        BinderCallingUid.set(mCallingUid, mCallingPkg);
        Parcel newData = Parcel.obtain();
        try {
            newData.appendFrom(data, 0, data.dataSize());
            newData.setDataPosition(0);
            return mBase.transact(code, newData, reply, flags);
        } finally {
            newData.recycle();
            BinderCallingUid.clear();
        }
    }

    @Nullable
    @Override
    public String getInterfaceDescriptor() {
        try {
            return mBase.getInterfaceDescriptor();
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public boolean pingBinder() {
        return mBase.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        return mBase.isBinderAlive();
    }

    @Nullable
    @Override
    public IInterface queryLocalInterface(@NonNull String descriptor) {
        return null;
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) {
        try {
            mBase.dump(fd, args);
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) {
        try {
            mBase.dumpAsync(fd, args);
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) {
        try {
            mBase.linkToDeath(recipient, flags);
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return mBase.unlinkToDeath(recipient, flags);
    }
}
