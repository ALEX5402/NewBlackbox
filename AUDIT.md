# BlackBox (NewBlackbox) — Full Audit

**Repo:** https://github.com/ALEX5402/NewBlackbox
**Audited commit:** `89b5983` (main, 2026-07-19)
**Local clone:** `/root/BLACKBOX`
**Audit date:** 2026-09-08

---

## 0. TL;DR

| Question | Answer |
|---|---|
| Bisa build sendiri? | **Ya.** Fully reproducible from source. Needs JDK 21 + Android SDK 35 + NDK 29. |
| Aman di-install? | **Prebuilt dari release: TIDAK disarankan.** Build sendiri: ya, setelah telemetry dibuang. |
| Ada malware? | Tidak ditemukan stealer/keylogger/C2. |
| Red flag utama? | **Crash telemetry default-ON** mengirim full `logcat` + device info ke server pihak ketiga. |
| Kualitas kode? | Fungsional, tapi 0 test, 301 exported components, god classes, silent catch. |

**Rekomendasi:** Build APK sendiri dari source dengan patch telemetry. Jangan pakai APK dari release/Telegram channel.

---

## 1. Apa Ini

Android app-virtualization engine (fork lineage: VirtualApp + VirtualAPK + BlackBox).
Menjalankan APK di sandbox userspace tanpa install — inti dari "app cloning".

- **410 ★ / 230 forks / 58 commits** — single-maintainer (alex5402) + 1 kontributor
- **License:** Apache-2.0
- **Size:** ~23 MB, 586 Java + 50 Kotlin + 41 native C/C++
- **Module:** `app` (UI, 53 files) · `Bcore` (engine, 501 files) · `black-reflection` (19) · `compiler` (6)
- **Build:** Gradle 8.14.5, AGP 8.13.2, Kotlin 1.9.23, JDK 21
- **Target:** `compileSdk 35`, `targetSdk 28`, `minSdk 21`

### Fitur (dari README + source)

| Fitur | Status |
|---|---|
| Virtual app cloning (multi-instance) | ✅ Core |
| Sandboxed process isolation | ✅ Core |
| No root required (userspace) | ✅ |
| Multi-arch (ARM64 / ARMv7 / x86) | ✅ |
| Device spoofing (IMEI, model, brand) | ✅ `DeviceIdProxy`, `UIDSpoofingHelper` |
| Fake GPS location | ✅ `BLocationManagerService` + overlay |
| Hide root | ✅ `AntiDetection.cpp` |
| GMS / Google Play support | ⚠️ Partial — issue #57, #37 masih terbuka |
| WebView support | ⚠️ Sering rusak — issue #34, #32, #27, #46 |
| VPN network mode | ✅ (opsi baru, default OFF) |
| Disable FLAG_SECURE (screenshot apps) | ✅ |

---

## 2. Temuan Keamanan

### 🔴 CRITICAL-1 — Crash telemetry default-ON ke server pihak ketiga

**File:** `Bcore/src/main/java/top/niunaijun/blackbox/app/configuration/ClientConfiguration.java:39`

```java
public String getLogSenderChatId() {
    return "-1003719573856";   // hardcoded Telegram channel ID
}
```

**Alur:**
1. `SimpleCrashFix.java` memasang `Thread.UncaughtExceptionHandler` global
2. Setiap crash (null-context, GMS, WebView, attribution, social-media, **dan fatal apa pun**) memanggil `BlackBoxCore.sendLogs(...)`
3. `sendLogs()` menjalankan `Runtime.getRuntime().exec("logcat -d -v threadtime")` — **dump seluruh logcat**
4. Di-upload ke `https://logs-sender-api.vercel.app/api/<chatId>/upload` → diteruskan ke Telegram channel `-1003719573856`

**Data yang dikirim** (`getDeviceInfoString()`):
- Model, brand, manufacturer, device, product, board, hardware
- Android version, SDK level, security patch, Build ID/Display
- Semua ABI, heap memory, package name, app version, version code, timestamp
- **Plus seluruh isi logcat** — bisa berisi package name app lain, path, detail auth flow, token di log

**Kenapa berbahaya:** endpoint `*.vercel.app` dikuasai penuh oleh developer (bisa berubah kapan saja tanpa update app), dan ini aktif secara default tanpa opt-in eksplisit.

**Fix:**
```java
public String getLogSenderChatId() {
    return "";  // disable by default; make it user-configured
}
```
Dan ubah `SimpleCrashFix` supaya auto-report jadi **opt-in** (setting eksplisit), bukan default.

### 🔴 CRITICAL-2 — Cleartext traffic diizinkan global

**File:** `app/src/main/res/xml/network_security_config.xml`
```xml
<base-config cleartextTrafficPermitted="true" />
```

Semua HTTP plaintext diizinkan, host maupun app dalam sandbox. Terkonfirmasi juga di issue #41 (belum difix).

**Fix:** set `cleartextTrafficPermitted="false"` + exception khusus hanya untuk host yang benar-benar butuh.

### 🟠 HIGH-1 — `android:allowBackup="true"`

**File:** `app/src/main/AndroidManifest.xml`
Sandbox metadata (daftar app ter-clone, path virtual) bisa ikut ke backup cloud/adb. Terkonfirmasi issue #41.

**Fix:** `android:allowBackup="false"` + `android:fullBackupContent="false"`.

### 🟠 HIGH-2 — 301 komponen di-export tanpa pembatasan caller

**File:** `Bcore/src/main/AndroidManifest.xml` — **301** `android:exported="true"`

Proxy activity/service/broadcast/provider internal di-export, padahal hanya mengonsumsi extra privat (`_B_|_target_`, `_Black_|_init_process_`). App pihak ketiga bisa memanggilnya langsung → proxy dispatch tak sah, DoS, crash via payload malformed.

Terkonfirmasi issue #39 (belum difix).

**Fix:** tandai `android:exported="false"` untuk komponen internal; hanya pertahankan `exported` untuk service yang butuh system binding; tambah validasi caller/null di entry point.

### 🟠 HIGH-3 — Command injection di `FileUtils`

**File:** `Bcore/src/main/java/top/niunaijun/blackbox/utils/FileUtils.java:106,118`

```java
Runtime.getRuntime().exec(cmd + cmode + " " + path).waitFor();      // chmod
Runtime.getRuntime().exec("ln -s " + oldPath + " " + newPath).waitFor();  // symlink
```

Path di-concat ke string shell. Path dengan metachar (`;`, `&&`, spasi, backtick) → inkonsistensi eksekusi hingga command-injection pada fallback path. Terkonfirmasi issue #42 (belum difix).

**Fix:** pakai bentuk array:
```java
Runtime.getRuntime().exec(new String[]{"chmod", cmode, path}).waitFor();
Runtime.getRuntime().exec(new String[]{"ln", "-s", oldPath, newPath}).waitFor();
```

### 🟡 MEDIUM-1 — God classes

| File | LOC |
|---|---|
| `BlackBoxCore.java` | 2,267 |
| `BAccountManagerService.java` | 1,807 |
| `IConnectivityManagerProxy.java` | 1,522 |
| `BActivityThread.java` | 1,343 |
| `IActivityManagerProxy.java` | 905 |

`BlackBoxCore` memegang config, service registry, crash handling, log sender, device-info, dan lifecycle — semuanya dalam satu file. Sulit di-review, sulit dites.

### 🟡 MEDIUM-2 — Silent error handling

- 3 catch block sepenuhnya kosong
- ~16 catch yang hanya berisi komentar/lanjut diam-diam
- Crash handling sangat agresif: **semua** `UncaughtException` dicegat dan tidak diteruskan ke handler default → crash nyata tersembunyi, debugging sulit, dan bisa menyembunyikan kegagalan keamanan

### 🟡 MEDIUM-3 — Dependency usang

```gradle
androidx.preference:preference-ktx:1.1.1        // 2021
androidx.lifecycle:*:2.3.1                      // 2021
androidx.recyclerview:recyclerview:1.2.1        // 2021
androidx.work:work-runtime:2.7.1                // 2022
org.osmdroid:osmdroid-android:6.1.11            // 2022
```

Bukan kerentanan kritis, tapi ketinggalan 3–4 tahun patch.

### 🟡 MEDIUM-4 — Release ditandatangani debug key

**File:** `app/build.gradle`
```gradle
release {
    signingConfig signingConfigs.debug    // debug key untuk build release
    minifyEnabled true
}
```

Debug keystore bersifat publik & shared — APK bisa diverifikasi ulang, ditandatangani ulang, dan didistribusikan sebagai "BlackBox resmi" oleh siapa pun.

**Fix:** generate release keystore sendiri, simpan di luar repo.

### 🟢 LOW

- `//` komentar berlebihan & penomoran file berantakan (README menyebut `Docs.md` yang ada, tapi `work_notes.md` di README tidak ada)
- Dokumentasi 12 file `.md` fix-note di root tanpa indeks
- Tidak ada `SECURITY.md`, tidak ada cara lapor kerentanan
- ID chat Telegram developer ter-hardcode juga di dokumen publik (bisa di-spam)

---

## 3. Yang Sudah Bagus

- **Reproducible build** — source lengkap, Gradle wrapper, CI workflow ada
- **Tidak ada kredensial hardcoded** — scan API key/token/secret bersih
- **Tidak ada stealer/keylogger/C2** — tidak ada exfiltrasi kontak, SMS, kamera, atau clipboard
- **Anti-detection transparan di source** — daftar blocked path/paket bisa dibaca, bukan binary blob
- **Struktur modul jelas** — engine (Bcore) dipisah dari UI (app)
- **Security issues #39–#42 difiling rapi** oleh auditor lain (ketemu 4 issue yang sama dengan audit ini)
- **Issue tracker aktif** — 18 closed / 22 open, maintainer responsif

---

## 4. Peluang Improvement (USEFUL side)

### 🔐 Keamanan & Privacy (prioritas)

| # | Item | Effort | Dampak |
|---|------|--------|--------|
| 1 | Matikan log upload by default; jadikan opt-in dengan dialog consent | 30 mnt | 🔴 Tinggi |
| 2 | Perbaiki command injection `FileUtils` (issue #42) | 15 mnt | 🔴 Tinggi |
| 3 | `allowBackup=false` + cleartext=false (issue #41) | 15 mnt | 🔴 Tinggi |
| 4 | Non-export komponen proxy internal (issue #39) | 2–3 jam | 🟠 Sedang |
| 5 | Timeout + retry + validasi destination pada LogSender | 30 mnt | 🟠 Sedang |
| 6 | Buat release keystore sendiri, sign release build | 20 mnt | 🟠 Sedang |
| 7 | Tambah `SECURITY.md` + koordinat pelaporan | 10 mnt | 🟢 Rendah |

### 🛠️ Kualitas Kode

| # | Item | Effort | Dampak |
|---|------|--------|--------|
| 8 | **Tambah test** — 0 test saat ini. Mulai dari `FileUtils`, `Reflector`, `BPackage`, `UIDSpoofingHelper` (pure logic) | 3-4 jam | 🔴 Tinggi |
| 9 | Ganti catch kosong dengan logging; jangan telan semua UncaughtException | 1 jam | 🟠 Sedang |
| 10 | Pecah `BlackBoxCore` (2,267 LOC) → `ConfigManager`, `ServiceRegistry`, `CrashReporter`, `DeviceInfoProvider` | 3-4 jam | 🟠 Sedang |
| 11 | Ganti `Runtime.exec(String)` yang lain dengan array form | 1 jam | 🟠 Sedang |
| 12 | Update dependency 2021/2022 → versi stabil terbaru | 1 jam | 🟡 Rendah |
| 13 | Rapikan CI: pisah job build dari job distribute ke Telegram (sekarang tercampur, perlu secrets untuk build) | 1 jam | 🟡 Rendah |

### ✨ Fitur / UX (dari 22 open issues)

| # | Permintaan | Frekuensi | Effort |
|---|-----------|-----------|--------|
| 14 | **Perbaiki WebView** — issue #34, #32, #27, #46, #35. Ini keluhan #1 user. `WebViewProxy.getWho()` return null → semua hook WebView mati | 🔴 Paling sering | 2-4 jam |
| 15 | **Perbaiki GMS sign-in** — #57 (layar login akun Google tidak terbuka), #37 (Play Store crash `CHANGE_CONFIGURATION`) | 🔴 Sering | 3-4 jam |
| 16 | **Fake location tidak berfungsi** — #33, #26 | 🔴 Sering | 2 jam |
| 17 | **Izin tidak bisa diperoleh** di dalam sandbox — #31 | 🟠 Sedang | 2 jam |
| 18 | **Akses file app clone** — #55, #58 (sub-user) | 🟠 Sedang | 2 jam |
| 19 | **LSPosed framework support** — #38 (feature request) | 🟡 Niches | Besar |
| 20 | **Loopback isolation** (`unshare`, `bwrap`) — #29 | 🟡 Niches | Besar |
| 21 | Tambah locale UI — saat ini hanya `values`, `values-zh-rCN`, `values-zh-rTW`. Tidak ada `ko`, `en` eksplisit (default en), `ru`, `ar` (pengguna #28 berbahasa Arab) | 🟢 | 2-3 jam |
| 22 | Batasi `QUERY_ALL_PACKAGES` — Google Play akan menolak; pakai `<queries>` spesifik | 🟢 | 1 jam |

---

## 5. Rencana Build Sendiri (Recommended Path)

### Prasyarat (belum ada di box ini)

```bash
# JDK 21
apt install -y openjdk-21-jdk

# Android cmdline-tools
mkdir -p /opt/android-sdk/cmdline-tools && cd /opt/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-*_latest.zip
unzip -q commandlinetools-linux-*.zip && mv cmdline-tools latest

export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# SDK + NDK (NDK 29.0.13846066 sesuai build.gradle)
sdkmanager "platforms;android-35" "build-tools;35.0.0" \
           "ndk;29.0.13846066" "platform-tools"
yes | sdkmanager --licenses
```

**Estimasi:** ~3 GB download, build 10–20 menit di 2 vCPU.

### Patch wajib sebelum build

1. `ClientConfiguration.getLogSenderChatId()` → `return "";`
2. `SimpleCrashFix` → hapus/comment panggilan auto `sendLogs` (atau gate di belakang setting)
3. `network_security_config.xml` → `cleartextTrafficPermitted="false"`
4. Manifest app → `android:allowBackup="false"`
5. `FileUtils` → ganti 2 `exec(String)` ke array form

### Build

```bash
cd /root/BLACKBOX
./gradlew assembleRelease
# → app/build/outputs/apk/release/BlackBox_4.0.0_release.apk
```

**Catatan:** release build sekarang pakai debug key. Untuk sideload pribadi ini oke, tapi kalau mau aman dari repackaging, generate release keystore sendiri dan update `signingConfigs.release`.

---

## 6. Verdict

**BlackBox adalah tool dual-use yang legitimate** — virtualisasi app, bukan malware. Tapi **default-nya tidak privacy-respecting**: crash telemetry otomatis mengirim logcat + info device ke server developer.

**Jangan install APK dari release atau channel Telegram developer.** Build sendiri dari source setelah patch 5 item di §5, dengan release keystore sendiri.

Untuk improvement paling berdampak: **fix WebView + GMS sign-in** (keluhan terbanyak), lalu **4 security issue yang sudah difiling** (#39–#42) yang belum tersentuh.
