# Infinite Recursion Fix for PackageManager Hook

## Problem Analysis

The app was crashing due to an **infinite recursion loop** in the `findActualApkPath` method:

```
at top.niunaijun.blackbox.fake.frameworks.BPackageManager.findActualApkPath(BPackageManager.java:696)
at top.niunaijun.blackbox.fake.frameworks.BPackageManager.createFallbackApplicationInfo(BPackageManager.java:667)
at top.niunaijun.blackbox.fake.frameworks.BPackageManager.getApplicationInfo(BPackageManager.java:304)
at top.niunaijun.blackbox.fake.service.IPackageManagerProxy$GetApplicationInfo.hook(IPackageManagerProxy.java:259)
```

**Root Cause:** The `findActualApkPath` method was calling `BlackBoxCore.getContext().getPackageManager().getApplicationInfo()`, which triggered the same hook again, creating an endless loop.

## The Recursion Cycle

1. **BPackageManager.getApplicationInfo()** is called
2. **IPackageManagerProxy$GetApplicationInfo.hook()** intercepts the call
3. **findActualApkPath()** is called to find real APK path
4. **findActualApkPath()** calls `PackageManager.getApplicationInfo()`
5. **IPackageManagerProxy$GetApplicationInfo.hook()** intercepts again
6. **findActualApkPath()** is called again
7. **Infinite loop continues** until stack overflow

## Issues Identified

1. **Hook Recursion**: The PackageManager is hooked, so calling it from within the hook causes recursion
2. **No Recursion Protection**: No mechanism to prevent infinite loops
3. **Fallback Dependency**: The fallback mechanism depended on the hooked PackageManager
4. **Stack Overflow**: The infinite recursion eventually causes stack overflow and crash

## Fixes Implemented

### 1. Recursion Protection Flag (`BPackageManager.java`)

**Added a static flag to prevent recursion:**

```java
private static volatile boolean sIsFindingApkPath = false; // Flag to prevent recursion
```

**Enhanced `findActualApkPath` method with recursion protection:**

```java
private String findActualApkPath(String packageName) {
    if (sIsFindingApkPath) {
        Log.w(TAG, "findActualApkPath called recursively, returning null to prevent infinite loop.");
        return null;
    }
    sIsFindingApkPath = true;
    try {
        // Skip PackageManager call to prevent infinite recursion
        // The PackageManager is hooked and would cause infinite loops
        Log.d(TAG, "Skipping PackageManager call to prevent recursion for " + packageName);
        
        // Try common paths
        String[] commonPaths = {
            "/data/app/" + packageName + "-1/base.apk",
            "/data/app/" + packageName + "-2/base.apk",
            "/data/app/" + packageName + "/base.apk",
            "/system/app/" + packageName + ".apk",
            "/system/priv-app/" + packageName + ".apk"
        };
        
        for (String path : commonPaths) {
            if (new File(path).exists()) {
                Log.d(TAG, "Found existing APK at: " + path);
                return path;
            }
        }
        
        Log.w(TAG, "No existing APK found for " + packageName + ", using fallback path");
        return null;
    } finally {
        sIsFindingApkPath = false; // Reset flag
    }
}
```

### 2. Removed PackageManager Dependency

**Eliminated the problematic PackageManager call:**

```java
// REMOVED: This was causing infinite recursion
// ApplicationInfo realInfo = BlackBoxCore.getContext().getPackageManager()
//         .getApplicationInfo(packageName, 0);

// REPLACED WITH: Direct file system checking
String[] commonPaths = {
    "/data/app/" + packageName + "-1/base.apk",
    "/data/app/" + packageName + "-2/base.apk",
    "/data/app/" + packageName + "/base.apk",
    "/system/app/" + packageName + ".apk",
    "/system/priv-app/" + packageName + ".apk"
};
```

### 3. Enhanced Fallback Mechanism

**Improved the fallback to work without PackageManager:**

```java
private ApplicationInfo createFallbackApplicationInfo(String packageName, int flags, int userId) {
    Log.w(TAG, "Creating fallback ApplicationInfo for " + packageName);
    ApplicationInfo info = new ApplicationInfo();
    info.packageName = packageName;
    info.flags = flags;
    info.uid = 0; // Placeholder
    
    // Use more realistic paths that are less likely to cause issues
    String apkPath = findActualApkPath(packageName);
    if (apkPath != null) {
        info.sourceDir = apkPath;
        info.publicSourceDir = apkPath;
    } else {
        // Use data app path instead of system app path
        info.sourceDir = "/data/app/" + packageName + "-1/base.apk";
        info.publicSourceDir = "/data/app/" + packageName + "-1/base.apk";
    }
    
    info.dataDir = "/data/data/" + packageName;
    info.nativeLibraryDir = "/data/app-lib/" + packageName;
    info.metaData = new Bundle();
    info.splitNames = new String[]{};
    
    // Set some basic flags to make it look more realistic
    info.flags |= ApplicationInfo.FLAG_ALLOW_BACKUP;
    info.flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
    
    return info;
}
```

## Key Changes Made

### 1. **Recursion Protection**
- Added `sIsFindingApkPath` flag to detect and prevent recursion
- Flag is set at the start and reset in finally block
- Early return if recursion is detected

### 2. **Eliminated PackageManager Dependency**
- Removed calls to `PackageManager.getApplicationInfo()`
- Replaced with direct file system path checking
- Avoids triggering the hook again

### 3. **File System Based Path Discovery**
- Check common APK installation paths directly
- Use `File.exists()` to verify APK presence
- Fallback to realistic paths when APK not found

### 4. **Enhanced Logging**
- Added detailed logging for recursion detection
- Clear messages about skipping PackageManager calls
- Better debugging information

## Expected Results

After implementing these fixes, the app should:

✅ **No more infinite recursion** crashes  
✅ **Stable fallback mechanism** without PackageManager dependency  
✅ **File system based APK discovery** for real paths  
✅ **Graceful degradation** when APK files are not found  
✅ **Proper error handling** without stack overflow  
✅ **Detailed logging** for debugging path issues  

## Testing Recommendations

1. **Test app startup** on various devices
2. **Monitor logcat** for recursion detection messages
3. **Test with missing APKs** to verify fallback paths
4. **Test with different APK locations** (data, system, priv-app)
5. **Verify no stack overflow** errors
6. **Test PackageManager operations** to ensure hooks work correctly

## Additional Notes

- The fix maintains backward compatibility
- File system checking is more reliable than PackageManager calls in this context
- Recursion protection prevents similar issues in the future
- Enhanced logging helps with debugging and monitoring
- The fallback mechanism is now completely independent of PackageManager hooks

## Build Instructions

1. Clean and rebuild the project in Android Studio
2. Test app startup on the target device
3. Monitor logcat for any recursion-related messages
4. Verify that the infinite loop is resolved

The comprehensive recursion protection and PackageManager independence implemented should resolve the infinite recursion crashes and allow the app to start successfully.
