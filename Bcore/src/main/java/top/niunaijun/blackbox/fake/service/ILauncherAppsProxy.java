package top.niunaijun.blackbox.fake.service;

import android.content.Context;

import java.lang.reflect.Method;

import black.android.content.pm.BRILauncherAppsStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.utils.MethodParameterUtils;

/**
 * updated by alex5402 on 4/13/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class ILauncherAppsProxy extends BinderInvocationStub {

    public ILauncherAppsProxy() {
        super(BRServiceManager.get().getService(Context.LAUNCHER_APPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRILauncherAppsStub.get().asInterface(BRServiceManager.get().getService(Context.LAUNCHER_APPS_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.LAUNCHER_APPS_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    protected void onBindMethod() {
        super.onBindMethod();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        // todo shouldHideFromSuggestions
        return super.invoke(proxy, method, args);
    }

}
