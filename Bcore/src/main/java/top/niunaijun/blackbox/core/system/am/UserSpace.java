package top.niunaijun.blackbox.core.system.am;

import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;


public class UserSpace {
    public final ActiveServices mActiveServices = new ActiveServices();
    public final ActivityStack mStack = new ActivityStack();
    public final Map<IBinder, PendingIntentRecord> mIntentSenderRecords = new HashMap<>();
}
