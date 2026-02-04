package android.content.pm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PackageParser {

    public final static int PARSE_IS_SYSTEM = 1 << 0;
    public final static int PARSE_CHATTY = 1 << 1;
    public final static int PARSE_MUST_BE_APK = 1 << 2;
    public final static int PARSE_IGNORE_PROCESSES = 1 << 3;
    public final static int PARSE_FORWARD_LOCK = 1 << 4;
    public final static int PARSE_EXTERNAL_STORAGE = 1 << 5;
    public final static int PARSE_IS_SYSTEM_DIR = 1 << 6;
    public final static int PARSE_IS_PRIVILEGED = 1 << 7;
    public final static int PARSE_COLLECT_CERTIFICATES = 1 << 8;
    public final static int PARSE_TRUSTED_OVERLAY = 1 << 9;

    public static class NewPermissionInfo {
        public final String name;
        public final int sdkVersion;
        public final int fileVersion;

        public NewPermissionInfo(String name, int sdkVersion, int fileVersion) {
            throw new RuntimeException("Stub!");
        }
    }

    public static class SplitPermissionInfo {
        public final String rootPerm;
        public final String[] newPerms;
        public final int targetSdk;

        public SplitPermissionInfo(String rootPerm, String[] newPerms, int targetSdk) {
            throw new RuntimeException("Stub!");
        }
    }

    public static final PackageParser.NewPermissionInfo NEW_PERMISSIONS[] = new PackageParser.NewPermissionInfo[]{
            new PackageParser.NewPermissionInfo(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Build.VERSION_CODES.DONUT, 0),
            new PackageParser.NewPermissionInfo(android.Manifest.permission.READ_PHONE_STATE, android.os.Build.VERSION_CODES.DONUT, 0)
    };

    static class ParsePackageItemArgs {
        final Package owner;
        final String[] outError;
        final int nameRes;
        final int labelRes;
        final int iconRes;
        final int logoRes;
        final int bannerRes;

        String tag;
        TypedArray sa;

        ParsePackageItemArgs(final Package owner, final String[] outError, final int nameRes, final int labelRes, final int iconRes, final int logoRes, final int bannerRes) { throw new RuntimeException("Stub!");
        }
    }

    static class ParseComponentArgs extends ParsePackageItemArgs {
        final String[] sepProcesses;
        final int processRes;
        final int descriptionRes;
        final int enabledRes;
        int flags;

        ParseComponentArgs(final Package owner, final String[] outError, final int nameRes, final int labelRes, final int iconRes, final int logoRes, final int bannerRes, final String[] sepProcesses, final int processRes, final int descriptionRes, final int enabledRes) {
            super(owner, outError, nameRes, labelRes, iconRes, logoRes, bannerRes);
            throw new RuntimeException("Stub!");
        }
    }

    public static class PackageLite {
        public final String packageName;
        public final int versionCode;
        public final int installLocation;
        public final VerifierInfo[] verifiers;

        
        public final String[] splitNames;

        
        public final String codePath;

        
        public final String baseCodePath;
        
        public final String[] splitCodePaths;

        
        public final int baseRevisionCode;
        
        public final int[] splitRevisionCodes;

        public final boolean coreApp;
        public final boolean multiArch;
        public final boolean extractNativeLibs;

        public PackageLite(final String codePath, final ApkLite baseApk, final String[] splitNames, final String[] splitCodePaths, final int[] splitRevisionCodes) {
            throw new RuntimeException("Stub!");
        }

        public List<String> getAllCodePaths() {
            throw new RuntimeException("Stub!");
        }
    }

    public static class ApkLite {
        public final String codePath;
        public final String packageName;
        public final String splitName;
        public final int versionCode;
        public final int revisionCode;
        public final int installLocation;
        public final VerifierInfo[] verifiers;
        public final Signature[] signatures;
        public final boolean coreApp;
        public final boolean multiArch;
        public final boolean extractNativeLibs;

        public ApkLite(final String codePath, final String packageName, final String splitName, final int versionCode, final int revisionCode, final int installLocation, final List<VerifierInfo> verifiers, final Signature[] signatures, final boolean coreApp, final boolean multiArch, final boolean extractNativeLibs) {
            throw new RuntimeException("Stub!");
        }
    }

    
    public PackageParser() {
        throw new RuntimeException("Stub!");
    }

    public PackageParser(final String archiveSourcePath) {
        throw new RuntimeException("Stub!");
    }

    public void setSeparateProcesses(final String[] procs) {
        throw new RuntimeException("Stub!");
    }

    public void setOnlyCoreApps(final boolean onlyCoreApps) {
        throw new RuntimeException("Stub!");
    }

    public void setDisplayMetrics(final DisplayMetrics metrics) {
        throw new RuntimeException("Stub!");
    }

    public static final boolean isApkFile(final File file) {
        throw new RuntimeException("Stub!");
    }

    public static PackageInfo generatePackageInfo(final PackageParser.Package p, final int gids[], final int flags, final long firstInstallTime, final long lastUpdateTime, final Set<String> grantedPermissions, final PackageUserState state) {
        throw new RuntimeException("Stub!");
    }

    public static boolean isAvailable(final PackageUserState state) {
        throw new RuntimeException("Stub!");
    }

    public static PackageInfo generatePackageInfo(final PackageParser.Package p, final int gids[], final int flags, final long firstInstallTime, final long lastUpdateTime, final Set<String> grantedPermissions, final PackageUserState state, final int userId) {
        throw new RuntimeException("Stub!");
    }

    
    public static PackageLite parsePackageLite(final File packageFile, final int flags) throws PackageParserException {
        throw new RuntimeException("Stub!");
    }

    
    public Package parsePackage(final File packageFile, final int flags) throws PackageParserException {
        throw new RuntimeException("Stub!");
    }

    
    public Package parsePackage(final File sourceFile, final String destCodePath, final DisplayMetrics metrics, final int flags) {
        throw new RuntimeException("Stub!");
    }

    public void collectManifestDigest(final Package pkg) throws PackageParserException {
        throw new RuntimeException("Stub!");
    }

    public void collectCertificates(final Package pkg, final int flags) throws PackageParserException {
        throw new RuntimeException("Stub!");
    }

    
    public static ApkLite parseApkLite(final File apkFile, final int flags) throws PackageParserException {
        throw new RuntimeException("Stub!");
    }

    
    public final static class Package {

        public String packageName;

        
        public String[] splitNames;

        

        public String volumeUuid;

        
        public String codePath;

        
        public String baseCodePath;
        
        public String[] splitCodePaths;

        
        public int baseRevisionCode;
        
        public int[] splitRevisionCodes;

        
        public int[] splitFlags;

        
        public int[] splitPrivateFlags;

        public boolean baseHardwareAccelerated;

        
        public ApplicationInfo applicationInfo = new ApplicationInfo();

        public final ArrayList<Permission> permissions = new ArrayList<Permission>(0);
        public final ArrayList<PermissionGroup> permissionGroups = new ArrayList<PermissionGroup>(0);
        public final ArrayList<Activity> activities = new ArrayList<Activity>(0);
        public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
        public final ArrayList<Provider> providers = new ArrayList<Provider>(0);
        public final ArrayList<Service> services = new ArrayList<Service>(0);
        public final ArrayList<Instrumentation> instrumentation = new ArrayList<Instrumentation>(0);

        public final ArrayList<String> requestedPermissions = new ArrayList<String>();

        public ArrayList<String> protectedBroadcasts;

        public ArrayList<String> libraryNames = null;
        public ArrayList<String> usesLibraries = null;
        public ArrayList<String> usesOptionalLibraries = null;
        public String[] usesLibraryFiles = null;

        public ArrayList<ActivityIntentInfo> preferredActivityFilters = null;

        public ArrayList<String> mOriginalPackages = null;
        public String mRealPackage = null;
        public ArrayList<String> mAdoptPermissions = null;

        
        public Bundle mAppMetaData = null;

        
        public int mVersionCode;

        
        public String mVersionName;

        
        public String mSharedUserId;

        
        public int mSharedUserLabel;

        
        public Signature[] mSignatures;
        public SigningDetails mSigningDetails;
        public Certificate[][] mCertificates;

        
        
        public int mPreferredOrder = 0;

        


        
        public long mLastPackageUsageTimeInMills;

        
        
        
        
        

        
        public Object mExtras;

        
        public ArrayList<ConfigurationInfo> configPreferences = null;

        
        public ArrayList<FeatureInfo> reqFeatures = null;

        
        public ArrayList<FeatureGroupInfo> featureGroups = null;

        public int installLocation;

        public boolean coreApp;

        
        public boolean mRequiredForAllUsers;

        
        public String mRestrictedAccountType;

        
        public String mRequiredAccountType;

        
        public ManifestDigest manifestDigest;

        public String mOverlayTarget;
        public int mOverlayPriority;
        public boolean mTrustedOverlay;

        
        public ArraySet<PublicKey> mSigningKeys;
        public ArraySet<String> mUpgradeKeySets;
        public ArrayMap<String, ArraySet<PublicKey>> mKeySetMapping;

        
        public String cpuAbiOverride;

        public Package(String packageName) {
            throw new RuntimeException("Stub!");
        }

        public List<String> getAllCodePaths() {
            throw new RuntimeException("Stub!");
        }

        
        public List<String> getAllCodePathsExcludingResourceOnly() {
            throw new RuntimeException("Stub!");
        }

        public void setPackageName(final String newName) {
            throw new RuntimeException("Stub!");
        }

        public boolean hasComponentClassName(final String name) {
            throw new RuntimeException("Stub!");
        }

        public boolean isForwardLocked() {
            throw new RuntimeException("Stub!");
        }

        public boolean isSystemApp() {
            throw new RuntimeException("Stub!");
        }

        public boolean isPrivilegedApp() {
            throw new RuntimeException("Stub!");
        }

        public boolean isUpdatedSystemApp() {
            throw new RuntimeException("Stub!");
        }

        public boolean canHaveOatDir() {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    public static class Component<II extends IntentInfo> {
        public final Package owner;
        public final ArrayList<II> intents;
        public final String className;
        public Bundle metaData;

        ComponentName componentName;
        String componentShortName;

        public Component(final Package owner) {
            throw new RuntimeException("Stub!");
        }

        public Component(final ParsePackageItemArgs args, final PackageItemInfo outInfo) {
            throw new RuntimeException("Stub!");
        }

        public Component(final ParseComponentArgs args, final ComponentInfo outInfo) {
            throw new RuntimeException("Stub!");
        }

        public Component(final Component<II> clone) {
            throw new RuntimeException("Stub!");
        }

        public ComponentName getComponentName() {
            throw new RuntimeException("Stub!");
        }

        public void appendComponentShortName(final StringBuilder sb) {
            throw new RuntimeException("Stub!");
        }

        public void printComponentShortName(final PrintWriter pw) {
            throw new RuntimeException("Stub!");
        }

        public void setPackageName(final String packageName) {
            throw new RuntimeException("Stub!");
        }
    }

    public final static class Permission extends Component<IntentInfo> {
        public final PermissionInfo info;
        public boolean tree;
        public PermissionGroup group;

        public Permission(final Package owner) {
            super(owner);
            throw new RuntimeException("Stub!");
        }

        public Permission(final Package owner, final PermissionInfo info) {
            super(owner);
            throw new RuntimeException("Stub!");
        }

        @Override
        public void setPackageName(final String packageName) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    public final static class PermissionGroup extends Component<IntentInfo> {
        public final PermissionGroupInfo info;

        public PermissionGroup(final Package owner) {
            super(owner);
            throw new RuntimeException("Stub!");
        }

        public PermissionGroup(final Package owner, final PermissionGroupInfo info) {
            super(owner);
            throw new RuntimeException("Stub!");
        }

        @Override
        public void setPackageName(final String packageName) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    public final static class Activity extends Component<ActivityIntentInfo> {
        public final ActivityInfo info;

        public Activity(final ParseComponentArgs args, final ActivityInfo info) {
            super(args, info);
            throw new RuntimeException("Stub!");
        }

        @Override
        public void setPackageName(final String packageName) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    public final static class Service extends Component<ServiceIntentInfo> {
        public final ServiceInfo info;

        public Service(final ParseComponentArgs args, final ServiceInfo info) {
            super(args, info);
            throw new RuntimeException("Stub!");
        }

        @Override
        public void setPackageName(final String packageName) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    public final static class Provider extends Component<ProviderIntentInfo> {
        public final ProviderInfo info;
        public boolean syncable;

        public Provider(final ParseComponentArgs args, final ProviderInfo info) {
            super(args, info);
            throw new RuntimeException("Stub!");
        }

        public Provider(final Provider existingProvider) {
            super(existingProvider);
            throw new RuntimeException("Stub!");
        }

        @Override
        public void setPackageName(final String packageName) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    public final static class Instrumentation extends Component<IntentInfo> {
        public final InstrumentationInfo info;

        public Instrumentation(final ParsePackageItemArgs args, final InstrumentationInfo info) {
            super(args, info);
            throw new RuntimeException("Stub!");
        }

        @Override
        public void setPackageName(final String packageName) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    @SuppressLint("ParcelCreator")
    public static class IntentInfo extends IntentFilter {
        public boolean hasDefault;
        public int labelRes;
        public CharSequence nonLocalizedLabel;
        public int icon;
        public int logo;
        public int banner;
        public int preferred;
    }

    @SuppressLint("ParcelCreator")
    public final static class ActivityIntentInfo extends IntentInfo {
        public final Activity activity;

        public ActivityIntentInfo(final Activity activity) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    @SuppressLint("ParcelCreator")
    public final static class ServiceIntentInfo extends IntentInfo {
        public final Service service;

        public ServiceIntentInfo(final Service service) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    @SuppressLint("ParcelCreator")
    public static final class ProviderIntentInfo extends IntentInfo {
        public final Provider provider;

        public ProviderIntentInfo(final Provider provider) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    public static class PackageParserException extends Exception {

        public PackageParserException(int error, String detailMessage) {
            super(detailMessage);
            throw new RuntimeException("Stub!");
        }

        public PackageParserException(int error, String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
            throw new RuntimeException("Stub!");
        }

    }

    public static class SigningDetails {
        public static final SigningDetails UNKNOWN = null;
        public Signature[] signatures;
        public Signature[] pastSigningCertificates;
    }
}