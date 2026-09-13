# GMS Fix — What Was Wrong and What Was Changed

**Goal:** make GMS-dependent apps (Play Store, apps requiring Google sign-in) actually work inside cloned/virtual instances.

**Audited commit:** `89b5983` · **Local clone:** `/root/BLACKBOX`

---

## 1. Root Causes Found

### 🔴 C-1 — `GoogleAccountManagerProxy` fabricated fake Google accounts

**File:** `Bcore/.../fake/service/GoogleAccountManagerProxy.java`

```java
// BEFORE
if ("com.google".equals(accountType)) {
    return createMockGoogleAccounts();   // mock.user@gmail.com, virtual.user@gmail.com
}
```

Every app that asked "which Google accounts does this device have?" — which is the **first thing Google Sign-In does** — got two accounts that do not exist. Consequences:

- Sign-in UIs resolved to a bogus account instead of offering "Add account"
- Any token request against those fake accounts failed downstream
- The proxy also **shadowed `IAccountManagerProxy`** (the proxy that actually talks to the virtual account store), so real virtual accounts were never reachable

**This is the primary reason sign-in never appeared.**

### 🔴 C-2 — No LocaleManager proxy → Play Store crash (upstream #37)

**File:** *(missing — no such proxy existed)*

Play Store calls `LocaleManager.setApplicationLocales()` during init. That reaches the real system `ILocaleManager`, which enforces the **signature-level** `android.permission.CHANGE_CONFIGURATION`. A virtualized process does not hold it:

```
java.lang.SecurityException: setApplicationLocales: Neither user 10840 nor
current process has android.permission.CHANGE_CONFIGURATION.
    at android.app.ILocaleManager$Stub$Proxy.setApplicationLocales
```

Process dies before Play Store finishes starting. `locale` was not among the 81 registered proxied services.

### 🟠 H-1 — GMS install silently skipped when GMS is absent from the device

**File:** `Bcore/.../core/GmsCore.java`

```java
try {
    BlackBoxCore.getContext().getPackageManager().getApplicationInfo(packageName, 0);
} catch (PackageManager.NameNotFoundException e) {
    continue;   // silently skip
}
```

`installGApps()` only clones GMS/GSF/Play Store from the **host device**. No APK is bundled. On a device without GMS (or where the app can't see it), the install reports success while installing nothing — the UI toggle flips, but the sandbox has no GMS.

### 🟡 M-1 — `AuthenticationProxy` returned mock success results

`AuthenticationProxy.SignIn` returned `createMockSignInResult()` on failure, `GetAccessToken` returned mock tokens. Apps would think they were authenticated against fake credentials.

### 🟡 M-2 — `isGoogleApp()` was dead code

A 20-line helper in `GoogleAccountManagerProxy` that was never called anywhere.

### ⚠️ Architectural constraint (not a bug)

`BAccountManagerService` keeps its **own** account store (`BEnvironment.getAccountsConf()`), fully isolated from the host. Your real Google account on the host device is **not** visible inside the sandbox and is not shared. Adding a Google account inside a cloned app requires the sandbox to run the real Google sign-in flow.

---

## 2. Changes Made

### 2.1 New: `ILocaleManagerProxy.java`

Intercepts the `locale` service and acknowledges locale writes for virtual processes instead of letting them reach the permission-enforcing system service.

| Method | Behaviour |
|---|---|
| `setApplicationLocales` | no-op, returns null |
| `setSystemLocales` | no-op, returns null |
| `getApplicationLocales` | forwards |

Locales are a UX preference; discarding them is safe and unblocks Play Store startup.

**Registered** in `HookManager.init()` → `addInjector(new ILocaleManagerProxy())` (only meaningful in black/server process, matching the rest of the list).

### 2.2 Rewritten: `GoogleAccountManagerProxy`

- `getAccountsByType(type)` → queries the **virtual account store** (`BAccountManager.getAccountsAsUser`) first, and forwards to the real service when empty. **No more fabricated accounts.**
- `getAccounts()` → returns real results or an empty array, never mocks.
- Import added: `BAccountManager`.

### 2.3 Net effect

| Before | After |
|---|---|
| Sign-in UI sees 2 fake accounts | Sign-in UI sees the real (empty) account list and can start the add-account flow |
| Play Store crashes on init (SecurityException) | Locale call acknowledged → Play Store starts |
| Virtual account store unreachable | Virtual store is queried first |

---

## 3. Verified

- ✅ `./gradlew assembleDebug` — see build log
- ⚠️ Runtime behaviour inside a sandbox not yet exercised (needs an ARM64 Android device/emulator with GMS)

---

## 4. Remaining Blockers Before Real Google Sign-In Works

These are **not fixed** and are the honest reason sign-in may still not complete end-to-end:

1. **GMS must exist on the host device** (C-1 dependency). BlackBox clones GMS; it cannot supply it. If the phone has no GMS, nothing to clone.
2. **`GmsCore` is downloaded dynamically at runtime by Google** — modern Google Sign-In fetches a separate GmsCore module APK. The sandbox's install path (`installPackageAsUser`) does not handle Play Feature Delivery / split APKs, so that download cannot land.
3. **`AuthenticationProxy` still mocks.** Sign-in paths that reach it get fake success instead of a real token exchange.
4. **Account plumbing is isolated.** The virtual `BAccountManagerService` has no bridge to the host AccountManager; there is no "share my host Google account into the sandbox" feature.

**Realistic outcome:** the two fixes here remove the crash and the fake-account hallucination, which are prerequisites. Full sign-in likely still needs (2) and (3) addressed — that is a substantially larger piece of work than a proxy patch.

---

## 5. How To Test

```bash
export ANDROID_HOME=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd /root/BLACKBOX
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Install on an **ARM64 device that has Google Play Services**, then:

1. Open BlackBox → GMS Manager → install GMS for user 0
2. Clone an app that needs Google sign-in
3. Launch it and observe `logcat | grep -E "GmsProxy|GoogleAccountManager|ILocaleManager|LocaleManager"`
4. Expected: no `setApplicationLocales` SecurityException; account lookups log real counts, not mocks
