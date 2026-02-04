package top.niunaijun.blackbox.utils.compat;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import black.android.app.BRActivity;
import black.android.app.BRActivityManagerNative;
import black.android.app.BRIActivityManager;
import black.android.app.BRIActivityManagerL;
import black.android.app.BRIActivityManagerN;

public class ActivityManagerCompat {
	
	public static final int SERVICE_DONE_EXECUTING_ANON = 0;
	
	public static final int SERVICE_DONE_EXECUTING_START = 1;
	
	public static final int SERVICE_DONE_EXECUTING_STOP = 2;

























	
	public static final int INTENT_SENDER_BROADCAST = 1;

	
	public static final int INTENT_SENDER_ACTIVITY = 2;

	
	public static final int INTENT_SENDER_ACTIVITY_RESULT = 3;

	
	public static final int INTENT_SENDER_SERVICE = 4;

	
	public static final int USER_OP_SUCCESS = 0;

	public static final int START_FLAG_DEBUG = 1<<1;
	public static final int START_FLAG_TRACK_ALLOCATION = 1<<2;
	public static final int START_FLAG_NATIVE_DEBUGGING = 1<<3;

	public static boolean finishActivity(IBinder token, int code, Intent data) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return BRIActivityManagerN.get(BRActivityManagerNative.get().getDefault()).finishActivity(
					token, code, data, 0);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return BRIActivityManagerL.get(BRActivityManagerNative.get().getDefault()).finishActivity(
						token, code, data, false);
		}
		return false;
	}


    public static void setActivityOrientation(Activity activity, int orientation) {
        try {
            activity.setRequestedOrientation(orientation);
        } catch (Throwable e) {
            e.printStackTrace();
            
            Activity parent =  BRActivity.get(activity).mParent();
            while (true) {
				Activity tmp = BRActivity.get(parent).mParent();
				if (tmp != null) {
					parent = tmp;
				} else {
					break;
				}
			}
            IBinder token = BRActivity.get(parent).mToken();
            try {
				BRIActivityManager.get(BRActivityManagerNative.get().getDefault()).setRequestedOrientation(token, orientation);
            }catch (Throwable ex){
                ex.printStackTrace();
            }
        }
    }
}
