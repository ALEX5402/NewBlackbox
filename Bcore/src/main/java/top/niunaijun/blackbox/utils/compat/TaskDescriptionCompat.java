package top.niunaijun.blackbox.utils.compat;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.DrawableUtils;

public class TaskDescriptionCompat {
    public static ActivityManager.TaskDescription fix(ActivityManager.TaskDescription td) {
        String label = td.getLabel();
        Bitmap icon = td.getIcon();

        if (label != null && icon != null)
            return td;

        label = getTaskDescriptionLabel(BlackBoxCore.getUserId(), getApplicationLabel());
        
        
        
        

        
        
        
        td = new ActivityManager.TaskDescription(label, null, td.getPrimaryColor());
        return td;
    }

    public static String getTaskDescriptionLabel(int userId, CharSequence label) {
        return String.format(Locale.CHINA, "[B%d]%s", userId, label);
    }

    private static CharSequence getApplicationLabel() {
        try {
            PackageManager pm = BlackBoxCore.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(BlackBoxCore.getAppPackageName(), 0));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static Drawable getApplicationIcon() {
        try {
            
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }
}
