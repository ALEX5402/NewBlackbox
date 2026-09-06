package top.niunaijun.blackbox.fake.service;

import android.os.Process;

import top.niunaijun.blackbox.BlackBoxCore;


public final class BinderCallingUid {
    public static final String EXTRA_CALLING_BUID = "_B_|_CallingBUid";
    public static final String EXTRA_CALLING_PKG = "_B_|_CallingPkg";

    private static final ThreadLocal<Integer> sThreadUid = new ThreadLocal<>();
    private static final ThreadLocal<String> sThreadPkg = new ThreadLocal<>();

    private BinderCallingUid() {
    }

    public static void set(int uid) {
        set(uid, null);
    }

    public static void set(int uid, String packageName) {
        if (isVirtualAppUid(uid)) {
            sThreadUid.set(uid);
            if (packageName != null) {
                sThreadPkg.set(packageName);
            }
        }
    }

    public static int get() {
        Integer threadUid = sThreadUid.get();
        if (isVirtualAppUid(threadUid)) {
            return threadUid;
        }
        return -1;
    }

    public static String getPackage() {
        return sThreadPkg.get();
    }

    public static void clear() {
        sThreadUid.remove();
        sThreadPkg.remove();
    }

    public static boolean isVirtualAppUid(Integer uid) {
        return uid != null
                && uid > Process.FIRST_APPLICATION_UID
                && uid < Process.LAST_APPLICATION_UID
                && uid != BlackBoxCore.getHostUid();
    }
}
