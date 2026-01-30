package top.niunaijun.blackbox.fake;

import top.niunaijun.jnihook.ReflectCore;

/**
 * updated by alex5402 on 2021/5/7.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
public class FakeCore {
    public static void init() {
        ReflectCore.set(android.app.ActivityThread.class);
    }
}
