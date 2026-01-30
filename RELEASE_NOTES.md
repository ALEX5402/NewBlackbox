# Release Notes - NewBlackbox

## Version: Latest Build (2026-01-30)

### Major Fixes

#### 1. Fixed Instagram and App Crashes on Android 14+

**Problem:** Apps like Instagram were crashing with `SecurityException` due to package/UID verification failures in `SensitiveContentProtectionManagerService`.

**Solution:**
- Created `ISensitiveContentProtectionManagerProxy` to hook the Android 14+ sensitive content protection service
- Intercepts `setSensitiveContentProtection()` calls and replaces guest package names with host package name
- Properly implements `getWho()` using reflection to obtain the original service interface
- Includes safety checks to only hook when service exists (Android 14+)

**Files Modified:**
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/service/ISensitiveContentProtectionManagerProxy.java` (NEW)
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/hook/HookManager.java`

**Impact:** Instagram and other apps now work correctly on Android 14+ devices

---

#### 2. Fixed ContentProvider Call Method Crashes

**Problem:** Apps were crashing when ContentProviders' `call()` method had `AttributionSource` objects nested deep within `Bundle` arguments.

**Solution:**
- Created `AttributionSourceUtils` utility class with recursive Bundle traversal
- Handles deeply nested `AttributionSource` objects in Bundles and Parcelables
- Correctly fixes UID to use `getHostUid()` instead of `getBUid()`

**Files Modified:**
- `Bcore/src/main/java/top/niunaijun/blackbox/utils/AttributionSourceUtils.java` (NEW)
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/service/context/providers/ContentProviderStub.java`
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/service/context/providers/SystemProviderStub.java`

**Impact:** Apps using ContentProvider for deep links, inter-app communication, and settings no longer crash

---

#### 3. Fixed SettingsProvider "Invalid Method" Errors

**Problem:** System SettingsProvider was receiving the package name instead of the actual method name (e.g., "GET_global"), causing "Invalid method" errors and crashes.

**Solution:**
- Updated `SystemProviderStub` and `ContentProviderStub` to NOT replace String arguments for `call()` methods
- String arguments in `call()` are method names like "GET_global", "PUT_system", etc., not package names
- Only replaces package names for query/insert/update/delete operations where appropriate

**Files Modified:**
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/service/context/providers/SystemProviderStub.java`
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/service/context/providers/ContentProviderStub.java`

**Impact:** Settings-related operations and system preferences now work correctly

---

#### 4. Improved ContentProvider Error Handling

**Problem:** ContentProvider `call()` methods were returning `null` on errors, causing `NullPointerException` crashes in apps expecting valid Bundle responses.

**Solution:**
- Updated `getSafeDefaultValue()` in `ContentProviderStub` to return empty `Bundle()` instead of `null` for `call()` method
- Prevents NPE crashes in apps that don't null-check ContentProvider responses

**Files Modified:**
- `Bcore/src/main/java/top/niunaijun/blackbox/fake/service/context/providers/ContentProviderStub.java`

**Impact:** Apps handle ContentProvider errors gracefully without crashes

---

### Technical Improvements

#### AttributionSource Handling
- Implemented comprehensive recursive traversal of Bundle and Parcelable structures
- Handles nested AttributionSource objects at any depth
- Properly clones Bundles and Parcelables to avoid modifying shared state
- Includes safety checks for null values and class loading errors

#### Hook Architecture
- Improved `BinderInvocationStub` usage patterns
- Added proper `getWho()` implementations using reflection
- Enhanced `isBadEnv()` checks to detect and recover from hook failures
- Added service existence checks before attempting to hook Android version-specific services

---

### Known Limitations

#### Browser Compatibility
**GeckoView/Chromium-based browsers (e.g., Nira Browser):**
- These browsers use native rendering engines (libxul.so for Gecko, Chromium for Chrome-based)
- Native crashes occur during engine initialization due to:
  - Multi-process architecture conflicts
  - Seccomp syscall filtering incompatibilities
  - Low-level shared memory/IPC issues
  
**Recommendation:** Use WebView-based browsers instead:
- Via Browser
- Privacy Browser  
- JQuarks Browser
- Simple Browser

These use Android's built-in WebView and work correctly in virtualized environments.

---

### Testing

**Tested Apps:**
- Instagram - Now works on Android 14+
- Apps using SettingsProvider
- Apps using ContentProvider for IPC
- GeckoView/Chromium-based browsers (native engine limitation)

**Test Environment:**
- Android 14 (API 34)
- Device: Various devices with Android 12+

---

### Future Improvements

1. **Native Engine Support:** Investigate native layer modifications to support browser engines
2. **Additional Android 14 APIs:** Monitor for new system services requiring hooks
3. **Performance:** Optimize recursive Bundle traversal for large data structures
4. **Logging:** Add debug mode to trace ContentProvider and service hook operations

---

### Contributors

- Fixed by: AI Assistant (Antigravity)
- Tested by: alex5402
- Date: 2026-01-30

---

### Upgrade Instructions

1. Clean build:
   ```bash
   ./gradlew clean assembleDebug
   ```

2. Install the updated APK on your device

3. Test with previously crashing apps (e.g., Instagram on Android 14+)

---

### Bug Reports

If you encounter issues:
1. Capture full logcat: `adb logcat > crash.log`
2. Note the Android version and device model
3. Identify which app is crashing
4. Check for `SecurityException`, `NullPointerException`, or other errors

---

## Summary

This release primarily focuses on **Android 14 compatibility** and **ContentProvider crash fixes**. Apps that previously crashed due to package/UID verification, AttributionSource handling, or SettingsProvider issues should now work correctly.

**Breaking Changes:** None

**Minimum Android Version:** Unchanged (Android 5.0+)

**Recommended Android Version:** Android 12+ for best compatibility
