#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include "Dobby/dobby.h"
#include "xdl.h"

#define LOG_TAG "AntiDetection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
/**
 * created by alex5402 on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
struct SpoofedProp {
    const char* key;
    const char* value;
};


static int (*orig_system_property_get)(const char *name, char *value) = nullptr;

// =============================================================================
// FILE SYSTEM HIDING
// =============================================================================
/**
 * updated by alex5402 on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
static const char* blocked_files[] = {
    // Root detection files
    "/system/xbin/su",
    "/system/bin/su",
    "/sbin/su",
    "/system/app/Superuser.apk",
    "/system/app/SuperSU.apk",
    "/system/etc/init.d/99SuperSUDaemon",
    "/system/xbin/daemonsu",
    "/system/xbin/sugote",
    "/system/bin/sugote-mksh",
    "/system/xbin/sugote-mksh",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/data/local/tmp/su",
    "/system/bin/magisk",
    "/system/xbin/magisk",
    "/sbin/magisk",
    "/data/adb/magisk",
    
    // Virtual environment detection
    "/data/virtual",
    "/data/data/com.benny.openlauncher",
    "/data/data/io.va.exposed",
    "/data/data/com.lody.virtual",
    "/data/data/com.excelliance.dualaid",
    "/data/data/com.lbe.parallel",
    "/data/data/com.dual.dualspace",
    "/data/data/com.ludashi.superboost",
    "/data/data/top.niunaijun.blackboxa",
    "/blackbox",
    "/virtual",
    
    // Emulator detection files
    "/dev/vboxguest",
    "/dev/vboxuser",
    "/dev/qemu_pipe",
    "/dev/goldfish_pipe",
    "/dev/socket/qemud",
    "/dev/socket/baseband_genyd",
    "/dev/socket/genyd",
    "/system/lib/libc_malloc_debug_qemu.so",
    "/sys/qemu_trace",
    "/system/bin/qemu-props",
    "/system/bin/nox-prop",
    "/sys/module/goldfish_audio",
    "/sys/module/goldfish_sync",
    "/proc/tty/drivers/goldfish",
    "/dev/goldfish_events",
    "/system/lib/libdroid4x.so",
    "/system/bin/windroyed",
    "/system/lib/libnoxspeedup.so",
    "/system/lib/libmemu.so",
    "/system/lib/libbluelog.so",
    
    // Xposed detection
    "/system/xposed.prop",
    "/system/framework/XposedBridge.jar",
    "/data/data/de.robv.android.xposed.installer",
    "/data/data/org.meowcat.edxposed.manager",
    "/data/data/top.canyie.dreamland.manager",
    
    nullptr
};

static const char* blocked_packages[] = {
    "com.noshufou.android.su",
    "com.noshufou.android.su.elite", 
    "eu.chainfire.supersu",
    "com.koushikdutta.superuser",
    "com.thirdparty.superuser",
    "com.yellowes.su",
    "com.koushikdutta.rommanager",
    "com.koushikdutta.rommanager.license",
    "com.dimonvideo.luckypatcher",
    "com.chelpus.lackypatch",
    "com.ramdroid.appquarantine",
    "com.ramdroid.appquarantinepro",
    "com.devadvance.rootcloak",
    "com.devadvance.rootcloakplus",
    "de.robv.android.xposed.installer",
    "com.saurik.substrate",
    "com.zachspong.temprootremovejb",
    "com.amphoras.hidemyroot",
    "com.amphoras.hidemyrootadfree",
    "com.formyhm.hiderootPremium",
    "com.formyhm.hideroot",
    "me.phh.superuser",
    "eu.chainfire.supersu.pro",
    "com.kingouser.com",
    "com.topjohnwu.magisk",
    "com.lody.virtual",
    "io.va.exposed",
    "com.benny.openlauncher",
    nullptr
};

static bool is_blocked_file(const char* path) {
    if (!path) return false;
    for (int i = 0; blocked_files[i]; ++i) {
        if (strstr(path, blocked_files[i])) {
            return true;
        }
    }
    return false;
}

static bool is_blocked_package(const char* path) {
    if (!path) return false;
    for (int i = 0; blocked_packages[i]; ++i) {
        if (strstr(path, blocked_packages[i])) {
            return true;
        }
    }
    return false;
}

// Hook function pointers
static int (*orig_access)(const char *pathname, int mode) = nullptr;
static int (*orig_stat)(const char *pathname, struct stat *buf) = nullptr;
static int (*orig_lstat)(const char *pathname, struct stat *buf) = nullptr;
static FILE* (*orig_fopen)(const char *pathname, const char *mode) = nullptr;
static int (*orig_open)(const char *pathname, int flags, ...) = nullptr;
static ssize_t (*orig_readlink)(const char *pathname, char *buf, size_t bufsiz) = nullptr;
static DIR* (*orig_opendir)(const char *name) = nullptr;

// Hook implementations
// Safe whitelist for network and critical paths
static bool is_safe_path(const char* path) {
    if (!path) return false;
    if (strstr(path, "/proc/net/")) return true;
    if (strstr(path, "/dev/socket/")) return true;
    return false;
}

static int my_access(const char *pathname, int mode) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        LOGD("[file-hide] access blocked: %s", pathname);
        errno = ENOENT;
        return -1;
    }
    return orig_access ? orig_access(pathname, mode) : -1;
}

static int my_stat(const char *pathname, struct stat *buf) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        LOGD("[file-hide] stat blocked: %s", pathname);
        errno = ENOENT;
        return -1;
    }
    return orig_stat ? orig_stat(pathname, buf) : -1;
}

static int my_lstat(const char *pathname, struct stat *buf) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        LOGD("[file-hide] lstat blocked: %s", pathname);
        errno = ENOENT;
        return -1;
    }
    return orig_lstat ? orig_lstat(pathname, buf) : -1;
}

static FILE* my_fopen(const char *pathname, const char *mode) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        LOGD("[file-hide] fopen blocked: %s", pathname);
        errno = ENOENT;
        return nullptr;
    }
    return orig_fopen ? orig_fopen(pathname, mode) : nullptr;
}

static int my_open(const char *pathname, int flags, ...) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        LOGD("[file-hide] open blocked: %s", pathname);
        errno = ENOENT;
        return -1;
    }
    if (orig_open) {
        if (flags & O_CREAT) {
            va_list args;
            va_start(args, flags);
            mode_t mode = va_arg(args, mode_t);
            va_end(args);
            return orig_open(pathname, flags, mode);
        } else {
            return orig_open(pathname, flags);
        }
    }
    return -1;
}

static ssize_t my_readlink(const char *pathname, char *buf, size_t bufsiz) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        LOGD("[file-hide] readlink blocked: %s", pathname);
        errno = ENOENT;
        return -1;
    }
    return orig_readlink ? orig_readlink(pathname, buf, bufsiz) : -1;
}

static DIR* my_opendir(const char *name) {
    if (name && !is_safe_path(name) && (is_blocked_file(name) || is_blocked_package(name))) {
        LOGD("[file-hide] opendir blocked: %s", name);
        errno = ENOENT;
        return nullptr;
    }
    return orig_opendir ? orig_opendir(name) : nullptr;
}


static void install_file_hooks() {
    void* handle = xdl_open("libc.so", XDL_DEFAULT);
    if (!handle) {
        LOGD("xdl_open failed for libc.so");
        return;
    }
    
    // Hook file access functions
    void* fopen_addr = xdl_dsym(handle, "fopen", nullptr);
    if (fopen_addr) DobbyHook(fopen_addr, (void*)my_fopen, (void**)&orig_fopen);

    void* lstat_addr = xdl_dsym(handle, "lstat", nullptr);
    if (lstat_addr) DobbyHook(lstat_addr, (void*)my_lstat, (void**)&orig_lstat);

    // void* access_addr = xdl_dsym(handle, "access", nullptr); 
    // if (access_addr) DobbyHook(access_addr, (void*)my_access, (void**)&orig_access);

    // void* stat_addr = xdl_dsym(handle, "stat", nullptr); 
    // if (stat_addr) DobbyHook(stat_addr, (void*)my_stat, (void**)&orig_stat);

    // void* open_addr = xdl_dsym(handle, "open", nullptr);  
    // if (open_addr) DobbyHook(open_addr, (void*)my_open, (void**)&orig_open);

    // void* readlink_addr = xdl_dsym(handle, "readlink", nullptr);  
    // if (readlink_addr) DobbyHook(readlink_addr, (void*)my_readlink, (void**)&orig_readlink);

    // void* opendir_addr = xdl_dsym(handle, "opendir", nullptr); 
    // if (opendir_addr) DobbyHook(opendir_addr, (void*)my_opendir, (void**)&orig_opendir);

    xdl_close(handle);
    LOGD("File system hooks installed");
}

// Main installer function - call this once at startup
__attribute__((constructor)) void install_antidetection_hooks() {
    LOGD("Installing anti-detection hooks...");
    install_file_hooks(); 
    LOGD("Anti-detection hooks installation complete");
}
