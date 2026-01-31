# Release Notes - NewBlackbox

## Version: Latest Build (2026-01-31)

### Bug Fixes

#### Android 10 Black Screen Fix
**Problem:** Apps would show a black screen and timeout on Android 10 (API 29) due to initialization failures.

**Root Cause:** 
- `BRAttributionSource.getRealClass()` returns `null` on Android < 31 (AttributionSource was added in Android S)
- `SystemProviderStub.invoke()` crashed calling `.getName()` on null class
- `ClassInvocationStub.injectHook()` crashed when `getWho()` returned null for non-existent services

**Solution:**
- Added null checks in `SystemProviderStub.java` before accessing `BRAttributionSource.getRealClass()`
- Added null check in `ClassInvocationStub.java` to skip hooks when services don't exist

**Files Changed:**
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/service/context/providers/SystemProviderStub.java`
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/hook/ClassInvocationStub.java`

---

### Removed Features

#### Xposed Framework Support Removed
- Removed `BXposedManagerService` and related AIDL interfaces
- Removed "Install Xposed Module" UI and Settings entries
- Removed Xposed enablement flags in `BlackBoxCore` and `ClientConfiguration`
- Cleaned up `AppSystemEnv` and repositories to remove Xposed package checks

---

### 🔧 Stability Improvements

#### Anti-Detection Native Hook Stability
**Problem:** Native file system hooks caused crashes due to infinite recursion.

**Solution:**
- Removed `LOGD` calls from critical native hooks (`my_fopen`, `my_open`, `my_stat`, etc.)
- Fixed syntax errors in hook implementations
- Hooks now silently return `ENOENT` for blocked paths

---

### Compatibility

| Android Version | Status |
|-----------------|--------|
| Android 10 (Q)  | ✅ Fixed |
| Android 11 (R)  | ✅ Supported |
| Android 12 (S)  | ✅ Supported |
| Android 13 (T)  | ✅ Supported |
| Android 14 (U)  | ✅ Supported |
| Android 15+     | ⚠️ Requires testing |
