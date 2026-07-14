package top.niunaijun.blackbox.fake.hook;

import android.os.Build;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import top.niunaijun.blackbox.utils.Md5Utils;
import top.niunaijun.blackbox.utils.Slog;

public class BuildProxy {
    private static final String TAG = "BuildProxy";

    public static void spoofBuild(int userId) {
        try {
            // Generate user-specific pseudorandom suffix to append to device serials
            String userHash = Md5Utils.md5("USER_PROFILE_" + userId).substring(0, 6).toUpperCase();

            // Setting a reliable but distinctly separated device footprint per virtual user
            setBuildField("SERIAL", "BB" + userHash + "X");
            setBuildField("MODEL", "BB_Virtual_User_" + userId);
            setBuildField("MANUFACTURER", "BlackBox");
            setBuildField("DEVICE", "VirtualSpace_" + userId);
            setBuildField("PRODUCT", "BlackBox_V" + userId);
            setBuildField("BRAND", "BlackBox");
            setBuildField("FINGERPRINT", "blackbox/VirtualSpace_" + userId + "/VirtualSpace_" + userId + ":13/TQ2A.230505.002/" + userHash + ":user/release-keys");

            Slog.d(TAG, "Successfully spoofed Build.prop for User ID: " + userId);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to spoof Build.prop", e);
        }
    }

    private static void setBuildField(String fieldName, String value) {
        try {
            Field field = Build.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            try {
                Field modifiersField = Field.class.getDeclaredField("accessFlags");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (Exception ignored) {
                // Not supported on newer ART versions natively
            }

            field.set(null, value);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to modify field: " + fieldName);
        }
    }
}
