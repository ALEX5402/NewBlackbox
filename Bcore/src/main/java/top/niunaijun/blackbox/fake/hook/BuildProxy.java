package top.niunaijun.blackbox.fake.hook;

import android.os.Build;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import top.niunaijun.blackbox.utils.Md5Utils;
import top.niunaijun.blackbox.utils.Slog;

public class BuildProxy {
    private static final String TAG = "BuildProxy";

    // Android 16 OTA profiles based on BP31.250610.009
    private static final DeviceProfile[] PROFILES = {
        new DeviceProfile("Pixel 6", "oriole", "Google", "oriole"),
        new DeviceProfile("Pixel 6 Pro", "raven", "Google", "raven"),
        new DeviceProfile("Pixel 7", "panther", "Google", "panther"),
        new DeviceProfile("Pixel 7 Pro", "cheetah", "Google", "cheetah"),
        new DeviceProfile("Pixel 7a", "lynx", "Google", "lynx"),
        new DeviceProfile("Pixel Fold", "felix", "Google", "felix"),
        new DeviceProfile("Pixel Tablet", "tangorpro", "Google", "tangorpro"),
        new DeviceProfile("Pixel 8", "shiba", "Google", "shiba"),
        new DeviceProfile("Pixel 8 Pro", "husky", "Google", "husky"),
        new DeviceProfile("Pixel 8a", "akita", "Google", "akita"),
        new DeviceProfile("Pixel 9", "tokay", "Google", "tokay"),
        new DeviceProfile("Pixel 9 Pro", "caiman", "Google", "caiman"),
        new DeviceProfile("Pixel 9 Pro XL", "komodo", "Google", "komodo"),
        new DeviceProfile("Pixel 9 Pro Fold", "comet", "Google", "comet"),
        new DeviceProfile("Pixel 9a", "tegu", "Google", "tegu")
    };

    private static class DeviceProfile {
        String model;
        String device;
        String manufacturer;
        String product;

        DeviceProfile(String model, String device, String manufacturer, String product) {
            this.model = model;
            this.device = device;
            this.manufacturer = manufacturer;
            this.product = product;
        }
    }

    public static void spoofBuild(int userId) {
        try {
            // Pick a profile deterministically
            DeviceProfile profile = PROFILES[userId % PROFILES.length];

            // Generate user-specific pseudorandom suffix to append to device serials
            String userHash = Md5Utils.md5("USER_PROFILE_" + userId).substring(0, 8).toUpperCase();

            String buildId = "BP31.250610.009";
            if ("tangorpro".equals(profile.product)) {
                buildId = "BP31.250610.009.A1";
            }

            String fingerprint = "google/" + profile.product + "/" + profile.device + ":16/" + buildId + "/" + userHash + ":user/release-keys";

            setBuildField("SERIAL", userHash);
            setBuildField("MODEL", profile.model);
            setBuildField("MANUFACTURER", profile.manufacturer);
            setBuildField("DEVICE", profile.device);
            setBuildField("PRODUCT", profile.product);
            setBuildField("BRAND", "google");
            setBuildField("HARDWARE", profile.device);
            setBuildField("BOARD", profile.device);
            setBuildField("ID", buildId);
            setBuildField("DISPLAY", buildId);
            setBuildField("FINGERPRINT", fingerprint);

            // Also attempt to set Build.VERSION.SDK_INT and RELEASE
            try {
                Field versionRelease = Build.VERSION.class.getField("RELEASE");
                setAccessibleAndFinal(versionRelease);
                versionRelease.set(null, "16");

                Field versionSdk = Build.VERSION.class.getField("SDK_INT");
                setAccessibleAndFinal(versionSdk);
                versionSdk.set(null, 36);
            } catch (Exception e) {
                Slog.w(TAG, "Failed to spoof Build.VERSION fields");
            }

            Slog.d(TAG, "Successfully spoofed Build.prop for User ID: " + userId + " as " + profile.model + " (Android 16)");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to spoof Build.prop", e);
        }
    }

    private static void setAccessibleAndFinal(Field field) {
        try {
            field.setAccessible(true);
            try {
                Field modifiersField = Field.class.getDeclaredField("accessFlags");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (Exception ignored) {
                // Not supported on newer ART versions natively
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void setBuildField(String fieldName, String value) {
        try {
            Field field = Build.class.getDeclaredField(fieldName);
            setAccessibleAndFinal(field);
            field.set(null, value);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to modify field: " + fieldName);
        }
    }
}