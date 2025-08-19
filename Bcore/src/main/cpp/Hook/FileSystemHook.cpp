//
// Created for blocking problematic resource files
//

#include "FileSystemHook.h"
#include "Log.h"
#include "xdl.h"
#include <sys/stat.h>
#include <fcntl.h>
#include <stdarg.h>
#include <cstring>
#include <errno.h>

// Original function pointers
static int (*orig_open)(const char *pathname, int flags, ...) = nullptr;
static int (*orig_open64)(const char *pathname, int flags, ...) = nullptr;

// Hook for open system call
int new_open(const char *pathname, int flags, ...) {
    // Block problematic resource paths
    if (pathname != nullptr) {
        if (strstr(pathname, "resource-cache") || 
            strstr(pathname, "@idmap") || 
            strstr(pathname, ".frro") ||
            strstr(pathname, "systemui") ||
            strstr(pathname, "data@resource-cache@")) {
            ALOGD("FileSystemHook: Blocking problematic file access: %s", pathname);
            errno = ENOENT; // No such file or directory
            return -1;
        }
    }
    
    // For non-problematic paths, proceed normally
    va_list args;
    va_start(args, flags);
    mode_t mode = va_arg(args, mode_t);
    va_end(args);
    
    return orig_open(pathname, flags, mode);
}

// Hook for open64 system call
int new_open64(const char *pathname, int flags, ...) {
    // Block problematic resource paths
    if (pathname != nullptr) {
        if (strstr(pathname, "resource-cache") || 
            strstr(pathname, "@idmap") || 
            strstr(pathname, ".frro") ||
            strstr(pathname, "systemui") ||
            strstr(pathname, "data@resource-cache@")) {
            ALOGD("FileSystemHook: Blocking problematic file access (64): %s", pathname);
            errno = ENOENT; // No such file or directory
            return -1;
        }
    }
    
    // For non-problematic paths, proceed normally
    va_list args;
    va_start(args, flags);
    mode_t mode = va_arg(args, mode_t);
    va_end(args);
    
    return orig_open64(pathname, flags, mode);
}

void FileSystemHook::init() {
    ALOGD("FileSystemHook: Initializing file system hooks");
    
    // Load libc.so
    void* handle = xdl_open("libc.so", XDL_DEFAULT);
    if (!handle) {
        ALOGE("FileSystemHook: Failed to open libc.so");
        return;
    }
    
    // Hook open function
    orig_open = (int (*)(const char*, int, ...))xdl_sym(handle, "open", nullptr);
    if (orig_open) {
        // Note: In a real implementation, you would use a proper hooking library
        // like PLT hook or similar to replace the function pointer
        ALOGD("FileSystemHook: Found open function at %p", orig_open);
    } else {
        ALOGE("FileSystemHook: Failed to find open function");
    }
    
    // Hook open64 function
    orig_open64 = (int (*)(const char*, int, ...))xdl_sym(handle, "open64", nullptr);
    if (orig_open64) {
        ALOGD("FileSystemHook: Found open64 function at %p", orig_open64);
    } else {
        ALOGE("FileSystemHook: Failed to find open64 function");
    }
    
    xdl_close(handle);
}
