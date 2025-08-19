package top.niunaijun.blackbox.utils.compat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import black.android.os.BRBundle;

public class BundleCompat {
    public static IBinder getBinder(Bundle bundle, String key) {
        if (Build.VERSION.SDK_INT >= 18) {
            return bundle.getBinder(key);
        } else {
            return BRBundle.get(bundle).getIBinder(key);
        }
    }

    public static void putBinder(Bundle bundle, String key, IBinder value) {
        if (Build.VERSION.SDK_INT >= 18) {
            bundle.putBinder(key, value);
        } else {
            BRBundle.get(bundle).putIBinder(key, value);
        }
    }

    public static void putBinder(Intent intent, String key, IBinder value) {
        Bundle bundle = new Bundle();
        putBinder(bundle, "binder", value);
        intent.putExtra(key, bundle);
    }

    public static IBinder getBinder(Intent intent, String key) {
        Bundle bundle = intent.getBundleExtra(key);
        if (bundle != null) {
            return getBinder(bundle, "binder");
        }
        return null;
    }

//    public static void clearParcelledData(Bundle bundle) {
//        Parcel obtain = Parcel.obtain();
//        obtain.writeInt(0);
//        obtain.setDataPosition(0);
//        Parcel parcel;
//        if (BaseBundle.TYPE != null) {
//            parcel = BaseBundle.mParcelledData.get(bundle);
//            if (parcel != null) {
//                parcel.recycle();
//            }
//            BaseBundle.mParcelledData.set(bundle, obtain);
//        } else if (BundleICS.TYPE != null) {
//            parcel = BundleICS.mParcelledData.get(bundle);
//            if (parcel != null) {
//                parcel.recycle();
//            }
//            BundleICS.mParcelledData.set(bundle, obtain);
//        }
//    }
}
