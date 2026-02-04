package top.niunaijun.blackbox.utils;

public class HackAppUtils {

    
    public static void enableQQLogOutput(String packageName, ClassLoader classLoader) {
        if ("com.tencent.mobileqq".equals(packageName)) {
            try {
                Reflector.on("com.tencent.qphone.base.util.QLog", true, classLoader)
                        .field("UIN_REPORTLOG_LEVEL")
                        .set(100);
            } catch (Exception e) {
                e.printStackTrace();
                
            }
        }
    }
}
