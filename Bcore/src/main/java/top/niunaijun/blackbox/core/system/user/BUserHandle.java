

package top.niunaijun.blackbox.core.system.user;

import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;


public final class BUserHandle implements Parcelable {
    

    
    public static final int PER_USER_RANGE = 100000;

    
    public static final int USER_ALL = -1;

    
    public static final BUserHandle ALL = new BUserHandle(USER_ALL);

    
    public static final int USER_CURRENT = -2;

    
    public static final BUserHandle CURRENT = new BUserHandle(USER_CURRENT);

    
    public static final int USER_CURRENT_OR_SELF = -3;




    
    public static final BUserHandle CURRENT_OR_SELF = new BUserHandle(USER_CURRENT_OR_SELF);

    
    public static final int USER_NULL = -10000;

    
    @Deprecated
    public static final int USER_OWNER = 0;

    
    @Deprecated
    public static final BUserHandle OWNER = new BUserHandle(USER_OWNER);

    
    public static final int USER_SYSTEM = 0;

    
    public static final int USER_SERIAL_SYSTEM = 0;

    
    public static final BUserHandle SYSTEM = new BUserHandle(USER_SYSTEM);

    
    public static final boolean MU_ENABLED = true;

    
    public static final int ERR_GID = -1;
    
    public static final int AID_ROOT = 0;
    
    public static final int AID_APP_START = android.os.Process.FIRST_APPLICATION_UID;
    
    public static final int AID_APP_END = android.os.Process.LAST_APPLICATION_UID;
    
    public static final int AID_SHARED_GID_START = 50000;
    
    public static final int AID_CACHE_GID_START = 20000;

    final int mHandle;

    
    public static boolean isSameUser(int uid1, int uid2) {
        return getUserId(uid1) == getUserId(uid2);
    }

    
    public static boolean isSameApp(int uid1, int uid2) {
        return getAppId(uid1) == getAppId(uid2);
    }

    
    public static boolean isApp(int uid) {
        if (uid > 0) {
            final int appId = getAppId(uid);
            return appId >= Process.FIRST_APPLICATION_UID && appId <= Process.LAST_APPLICATION_UID;
        } else {
            return false;
        }
    }

    
    public static boolean isCore(int uid) {
        if (uid >= 0) {
            final int appId = getAppId(uid);
            return appId < Process.FIRST_APPLICATION_UID;
        } else {
            return false;
        }
    }

    
    public static BUserHandle getUserHandleForUid(int uid) {
        return of(getUserId(uid));
    }

    
    public static int getUserId(int uid) {
        if (MU_ENABLED) {
            return uid / PER_USER_RANGE;
        } else {
            return BUserHandle.USER_SYSTEM;
        }
    }

    
    public static int getCallingUserId() {
        return getUserId(Binder.getCallingUid());
    }

    
    public static int getCallingAppId() {
        return getAppId(Binder.getCallingUid());
    }

    
    public static BUserHandle of(int userId) {
        return userId == USER_SYSTEM ? SYSTEM : new BUserHandle(userId);
    }

    
    public static int getUid(int userId, int appId) {
        if (MU_ENABLED) {
            return userId * PER_USER_RANGE + (appId % PER_USER_RANGE);
        } else {
            return appId;
        }
    }

    
    public static int getAppId(int uid) {
        return uid % PER_USER_RANGE;
    }

    
    public static int getUserGid(int userId) {
        return getUid(userId, 9997 );
    }

    
    public static int getSharedAppGid(int uid) {
        return getSharedAppGid(getUserId(uid), getAppId(uid));
    }

    
    public static int getSharedAppGid(int userId, int appId) {
        if (appId >= AID_APP_START && appId <= AID_APP_END) {
            return (appId - AID_APP_START) + AID_SHARED_GID_START;
        } else if (appId >= AID_ROOT && appId <= AID_APP_START) {
            return appId;
        } else {
            return -1;
        }
    }

    









    
    public static int getCacheAppGid(int uid) {
        return getCacheAppGid(getUserId(uid), getAppId(uid));
    }

    
    public static int getCacheAppGid(int userId, int appId) {
        if (appId >= AID_APP_START && appId <= AID_APP_END) {
            return getUid(userId, (appId - AID_APP_START) + AID_CACHE_GID_START);
        } else {
            return -1;
        }
    }

    
    public static int parseUserArg(String arg) {
        int userId;
        if ("all".equals(arg)) {
            userId = BUserHandle.USER_ALL;
        } else if ("current".equals(arg) || "cur".equals(arg)) {
            userId = BUserHandle.USER_CURRENT;
        } else {
            try {
                userId = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad user number: " + arg);
            }
        }
        return userId;
    }

    
    public static int myUserId() {
        return getUserId(Process.myUid());
    }

    
    @Deprecated
    public boolean isOwner() {
        return this.equals(OWNER);
    }

    
    public boolean isSystem() {
        return this.equals(SYSTEM);
    }

    
    public BUserHandle(int h) {
        mHandle = h;
    }

    
    public int getIdentifier() {
        return mHandle;
    }

    @Override
    public String toString() {
        return "UserHandle{" + mHandle + "}";
    }

    @Override
    public boolean equals(Object obj) {
        try {
            if (obj != null) {
                BUserHandle other = (BUserHandle) obj;
                return mHandle == other.mHandle;
            }
        } catch (ClassCastException e) {
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mHandle;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mHandle);
    }

    
    public static void writeToParcel(BUserHandle h, Parcel out) {
        if (h != null) {
            h.writeToParcel(out, 0);
        } else {
            out.writeInt(USER_NULL);
        }
    }

    
    public static BUserHandle readFromParcel(Parcel in) {
        int h = in.readInt();
        return h != USER_NULL ? new BUserHandle(h) : null;
    }

    public static final Parcelable.Creator<BUserHandle> CREATOR
            = new Creator<BUserHandle>() {
        public BUserHandle createFromParcel(Parcel in) {
            return new BUserHandle(in);
        }

        public BUserHandle[] newArray(int size) {
            return new BUserHandle[size];
        }
    };

    
    public BUserHandle(Parcel in) {
        mHandle = in.readInt();
    }
}
