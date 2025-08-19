package top.niunaijun.blackbox.core.system.am;

import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

/**
 * updated by alex5402 on 4/25/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class UserSpace {
    public final ActiveServices mActiveServices = new ActiveServices();
    public final ActivityStack mStack = new ActivityStack();
    public final Map<IBinder, PendingIntentRecord> mIntentSenderRecords = new HashMap<>();
}
