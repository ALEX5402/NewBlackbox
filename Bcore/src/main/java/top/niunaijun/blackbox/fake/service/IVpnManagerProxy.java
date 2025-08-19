package top.niunaijun.blackbox.fake.service;

import black.android.net.BRIVpnManagerStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.ScanClass;

/**
 * updated by alex5402 on 4/12/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
@ScanClass(VpnCommonProxy.class)
public class IVpnManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IVpnManagerProxy";
    public static final String VPN_MANAGEMENT_SERVICE = "vpn_management";

    public IVpnManagerProxy() {
        super(BRServiceManager.get().getService(VPN_MANAGEMENT_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIVpnManagerStub.get().asInterface(BRServiceManager.get().getService(VPN_MANAGEMENT_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(VPN_MANAGEMENT_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
