package top.niunaijun.blackbox.fake.service;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.FileDescriptor;
import java.util.Map;
import java.util.WeakHashMap;

import black.android.app.BRIServiceConnectionO;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

/**
 * Connects clones to the location service of the real (host) Google Play Services instead of the virtual GMS.
 */
public final class GmsLocationRedirect {
    private static final String TAG = "GmsLocationRedirect";
    private static final String ACTION = "com.google.android.location.internal.GoogleLocationManagerService.START";
    private static final String GMS = "com.google.android.gms";
    private static final String BROKER = "com.google.android.gms.common.internal.IGmsServiceBroker";
    private static final int SAFE_PARCEL_LONG_HEADER = 0xFFFF0000;

    private static final Map<IBinder, Connection> sConnections = new WeakHashMap<>();

    private GmsLocationRedirect() {
    }

    public static boolean matches(Intent intent) {
        return intent != null && ACTION.equals(intent.getAction())
                && (intent.getPackage() == null || GMS.equals(intent.getPackage()));
    }

    public static synchronized IServiceConnection wrap(IServiceConnection base) {
        Connection connection = sConnections.get(base.asBinder());
        if (connection == null) {
            connection = new Connection(base);
            sConnections.put(base.asBinder(), connection);
        }
        return connection;
    }

    public static synchronized IServiceConnection getWrapper(IBinder base) {
        return base == null ? null : sConnections.get(base);
    }

    private static final class Connection extends IServiceConnection.Stub {
        private final IServiceConnection mBase;

        Connection(IServiceConnection base) {
            mBase = base;
        }

        @Override
        public void connected(ComponentName name, IBinder service) throws RemoteException {
            connected(name, service, false);
        }

        public void connected(ComponentName name, IBinder service, boolean dead) throws RemoteException {
            IBinder broker = service == null ? null : new BrokerBinder(service);
            if (BuildCompat.isOreo()) {
                BRIServiceConnectionO.get(mBase).connected(name, broker, dead);
            } else {
                mBase.connected(name, broker);
            }
        }
    }

    /** Forwards everything to the real broker, rewriting the clone package in getService. */
    private static final class BrokerBinder implements IBinder {
        private final IBinder mBase;

        BrokerBinder(IBinder base) {
            mBase = base;
        }

        @Override
        public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Parcel rewritten = null;
            try {
                rewritten = rewrite(data, BActivityThread.getAppPackageName(), BlackBoxCore.getHostPkg());
            } catch (Throwable e) {
                Slog.w(TAG, "cannot rewrite broker call " + code, e);
            }
            if (rewritten == null) {
                data.setDataPosition(0);
                return mBase.transact(code, data, reply, flags);
            }
            try {
                return mBase.transact(code, rewritten, reply, flags);
            } finally {
                rewritten.recycle();
            }
        }

        @Override
        public String getInterfaceDescriptor() throws RemoteException {
            return mBase.getInterfaceDescriptor();
        }

        @Override
        public boolean pingBinder() {
            return mBase.pingBinder();
        }

        @Override
        public boolean isBinderAlive() {
            return mBase.isBinderAlive();
        }

        @Override
        public IInterface queryLocalInterface(String descriptor) {
            return null;
        }

        @Override
        public void dump(FileDescriptor fd, String[] args) throws RemoteException {
            mBase.dump(fd, args);
        }

        @Override
        public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
            mBase.dumpAsync(fd, args);
        }

        @Override
        public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
            mBase.linkToDeath(recipient, flags);
        }

        @Override
        public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
            return mBase.unlinkToDeath(recipient, flags);
        }
    }

    /**
     * getService(IGmsCallbacks, GetServiceRequest): GetServiceRequest is a SafeParcelable. Its fields are copied as
     * they are, except the String equal to the clone package, which is written with the host package. Returns null
     * when there is nothing to change.
     */
    static Parcel rewrite(Parcel data, String clonePkg, String hostPkg) {
        if (clonePkg == null || clonePkg.equals(hostPkg)) return null;
        data.setDataPosition(0);
        data.enforceInterface(BROKER);
        data.readStrongBinder();
        if (data.readInt() == 0) return null;
        int objectStart = data.dataPosition();
        int objectHeader = data.readInt();
        if ((objectHeader & SAFE_PARCEL_LONG_HEADER) != SAFE_PARCEL_LONG_HEADER) return null;
        int objectSize = data.readInt();
        int fieldsStart = data.dataPosition();
        int objectEnd = fieldsStart + objectSize;
        if (objectSize < 0 || objectEnd > data.dataSize()) return null;

        Parcel out = Parcel.obtain();
        boolean changed = false;
        try {
            out.appendFrom(data, 0, objectStart);
            out.writeInt(objectHeader);
            int sizePos = out.dataPosition();
            out.writeInt(0);
            int outFieldsStart = out.dataPosition();

            data.setDataPosition(fieldsStart);
            while (data.dataPosition() < objectEnd) {
                int fieldStart = data.dataPosition();
                int header = data.readInt();
                boolean longHeader = (header & SAFE_PARCEL_LONG_HEADER) == SAFE_PARCEL_LONG_HEADER;
                int size = longHeader ? data.readInt() : header >>> 16;
                int payload = data.dataPosition();
                int fieldEnd = payload + size;
                if (size < 0 || fieldEnd > objectEnd) return null;
                boolean replaced = false;
                if (longHeader && size > 0) {
                    String value = null;
                    try {
                        value = data.readString();
                    } catch (Throwable ignored) {
                    }
                    if (clonePkg.equals(value) && data.dataPosition() == fieldEnd) {
                        out.writeInt(header);
                        int fieldSizePos = out.dataPosition();
                        out.writeInt(0);
                        int fieldPayload = out.dataPosition();
                        out.writeString(hostPkg);
                        int end = out.dataPosition();
                        out.setDataPosition(fieldSizePos);
                        out.writeInt(end - fieldPayload);
                        out.setDataPosition(end);
                        replaced = true;
                        changed = true;
                    }
                }
                if (!replaced) out.appendFrom(data, fieldStart, fieldEnd - fieldStart);
                data.setDataPosition(fieldEnd);
            }
            int end = out.dataPosition();
            out.setDataPosition(sizePos);
            out.writeInt(end - outFieldsStart);
            out.setDataPosition(end);
            if (data.dataSize() > objectEnd) out.appendFrom(data, objectEnd, data.dataSize() - objectEnd);
            out.setDataPosition(0);
            if (changed) {
                Slog.d(TAG, "getService: " + clonePkg + " -> " + hostPkg);
                Parcel result = out;
                out = null;
                return result;
            }
            return null;
        } finally {
            if (out != null) out.recycle();
        }
    }
}
