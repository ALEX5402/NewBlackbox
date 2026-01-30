# Release Notes - NewBlackbox

## Version: Latest Build (2026-01-30)

### Major Changes

#### 1. Removal of Xposed Framework Support
**Summary:** The Xposed module loading and management features have been completely removed from BlackBox to streamline the core and remove legacy dependencies.
**Details:**
-   Removed `BXposedManagerService` and related AIDL interfaces.
-   Removed "Install Xposed Module" UI and Settings entries.
-   Removed Xposed enablement flags in `BlackBoxCore` and `ClientConfiguration`.
-   Cleaned up `AppSystemEnv` and repositories to remove Xposed package checks.
-   Removed `FLAG_XPOSED` from `InstallOption` and `USER_XPOSED` from `BUserHandle`.

#### 2. Anti-Detection Stability Fix
**Problem:** The native file system hooks in `AntiDetection.cpp` were causing application crashes due to infinite recursion (Hook calls logging -> Logging calls `open` -> `open` hook calls Logging).
**Solution:**
-   Removed all `LOGD` calls from critical native hooks (`my_fopen`, `my_open`, `my_stat`, etc.).
-   Fixed syntax errors in hook implementations.
-   Hooks now silently return `ENOENT` for blocked paths without triggering recursion.

### Technical Details

**Refactoring:**
-   Deleted `Bcore/src/main/java/top/niunaijun/blackbox/fake/frameworks/BXposedManager.java`
-   Deleted `app/src/main/java/top/niunaijun/blackboxa/view/xp` (Xposed UI)
-   Fixed compilation errors in `BlackBoxCore.java` and `AppSystemEnv.java` resulting from the removal.

**Known Limitations:**
-   **Native Signature Spoofing:** While `AntiDetection` hooks are stable, fully spoofing the APK signature at the native level (for apps that read the APK file directly) is not fully implemented. The current hooks hide blocked files but do not redirect file access to the original APK. Implementing this requires **PLT Hooking** (e.g., via xHook) and knowledge of the original APK path.