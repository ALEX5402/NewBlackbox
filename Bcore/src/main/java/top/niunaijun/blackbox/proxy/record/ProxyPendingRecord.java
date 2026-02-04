package top.niunaijun.blackbox.proxy.record;

import android.content.Intent;


public class ProxyPendingRecord {
    public int mUserId;
    public Intent mTarget;

    public ProxyPendingRecord(Intent target, int userId) {
        mUserId = userId;
        mTarget = target;
    }

    public static void saveStub(Intent shadow, Intent target, int userId) {
        shadow.putExtra("_B_|_P_user_id_", userId);
        shadow.putExtra("_B_|_P_target_", target);
    }

    public static ProxyPendingRecord create(Intent intent) {
        int userId = intent.getIntExtra("_B_|_P_user_id_", 0);
        Intent target = intent.getParcelableExtra("_B_|_P_target_");
        return new ProxyPendingRecord(target, userId);
    }

    @Override
    public String toString() {
        return "ProxyPendingActivityRecord{" +
                "mUserId=" + mUserId +
                ", mTarget=" + mTarget +
                '}';
    }
}
