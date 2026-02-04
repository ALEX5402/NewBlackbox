package top.niunaijun.blackbox.fake;

import top.niunaijun.jnihook.ReflectCore;


public class FakeCore {
    public static void init() {
        ReflectCore.set(android.app.ActivityThread.class);
    }
}
