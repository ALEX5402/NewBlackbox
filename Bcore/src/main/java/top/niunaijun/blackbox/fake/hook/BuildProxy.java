package top.niunaijun.blackbox.fake.hook;

import android.os.Build;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import top.niunaijun.blackbox.utils.Slog;

public class BuildProxy {
    private static final String TAG = "BuildProxy";

    public static void spoofBuild(String packageName) {
        try {
            setBuildField("MODEL", "Pixel 6 Pro");
            setBuildField("MANUFACTURER", "Google");
            setBuildField("DEVICE", "raven");
            setBuildField("PRODUCT", "raven");
            setBuildField("BRAND", "google");
            setBuildField("FINGERPRINT", "google/raven/raven:13/TQ2A.230505.002/9891404:user/release-keys");

            Slog.d(TAG, "Successfully spoofed Build.prop for package: " + packageName);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to spoof Build.prop", e);
        }
    }

    private static void setBuildField(String fieldName, String value) throws Exception {
        Field field = Build.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        try {
            Field modifiersField = Field.class.getDeclaredField("accessFlags");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Exception ignored) {
            // Some Android versions do not have accessFlags field.
        }

        field.set(null, value);
    }
}
