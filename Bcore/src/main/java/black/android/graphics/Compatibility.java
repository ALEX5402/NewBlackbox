package black.android.graphics;

import top.niunaijun.blackreflection.annotation.BClassName;
import top.niunaijun.blackreflection.annotation.BStaticMethod;


@BClassName("android.graphics.Compatibility")
public interface Compatibility {
    @BStaticMethod
    void setTargetSdkVersion(int targetSdkVersion);
}
