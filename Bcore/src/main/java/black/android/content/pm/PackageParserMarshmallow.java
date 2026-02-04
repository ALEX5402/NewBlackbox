package black.android.content.pm;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;

import java.io.File;

import top.niunaijun.blackreflection.annotation.BClassName;
import top.niunaijun.blackreflection.annotation.BConstructor;
import top.niunaijun.blackreflection.annotation.BMethod;

@BClassName("android.content.pm.PackageParser")
public interface PackageParserMarshmallow {
    @BConstructor
    PackageParser _new();
















    @BMethod
    void collectCertificates(Package p, int flags);

    @BMethod
    Package parsePackage(File File0, int int1);
}
