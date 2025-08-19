package android.content.pm;

import android.util.ArraySet;

/**
 * updated by alex5402 on 2021/5/7.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class PackageUserState {

    public boolean stopped;
    public boolean notLaunched;
    public boolean installed;
    public boolean hidden; // Is the app restricted by owner / admin
    public int enabled;
    public boolean blockUninstall;

    public String lastDisableAppCaller;

    public ArraySet<String> disabledComponents;
    public ArraySet<String> enabledComponents;

    public int domainVerificationStatus;
    public int appLinkGeneration;

    public PackageUserState() {
        throw new RuntimeException("Stub!");
    }

    public PackageUserState(final PackageUserState o) {
        throw new RuntimeException("Stub!");
    }

}
