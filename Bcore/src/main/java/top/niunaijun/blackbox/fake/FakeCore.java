package top.niunaijun.blackbox.fake;

import top.niunaijun.jnihook.ReflectCore;

/**
 * updated by alex5402 on 2021/5/7.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class FakeCore {
    public static void init() {
        ReflectCore.set(android.app.ActivityThread.class);
    }
}
