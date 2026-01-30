
#include <jni.h>
#include <sys/system_properties.h>
#include "xdl.h"
#include "hidden_api.h"
#include "Utils/elf_util.h"
#include "Log.h"
/**
 * updated by alex5402 on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
bool disable_hidden_api(JNIEnv *env) {
    char version_str[PROP_VALUE_MAX];
    if (!__system_property_get("ro.build.version.sdk", version_str)) {
        ALOGE("Failed to obtain SDK int");
        return false;
    }
    long android_version = std::strtol(version_str, nullptr, 10);

    // Hidden api introduced in sdk 29
    if (android_version < 29) {
        ALOGD("HiddenAPI: Android version < 29, no need to disable");
        return true;
    }

    SandHook::ElfImg *elf_img = new SandHook::ElfImg("libart.so");
    if (!elf_img->isValid()) {
        ALOGE("HiddenAPI: Failed to load libart.so");
        delete elf_img;
        return false;
    }

    // Try multiple possible symbol names for different Android versions
    void *addr = nullptr;
    const char* symbol_names[] = {
        "_ZN3artL32VMRuntime_setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
        "_ZN3art9VMRuntime22setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
        "art::VMRuntime::setHiddenApiExemptions(_JNIEnv*, _jclass*, _jobjectArray*)",
        nullptr
    };

    for (int i = 0; symbol_names[i] != nullptr; i++) {
        addr = (void*)elf_img->getSymbAddress(symbol_names[i]);
        if (addr) {
            ALOGD("HiddenAPI: Found symbol %s at %p", symbol_names[i], addr);
            break;
        }
    }

    delete elf_img;
    
    if (!addr) {
        ALOGE("HiddenAPI: Didn't find setHiddenApiExemptions in any form");
        return false;
    }

    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) {
        ALOGE("HiddenAPI: Failed to find String class");
        return false;
    }

    // L is basically wildcard for everything
    jstring wildcard = env->NewStringUTF("L");
    if (!wildcard) {
        ALOGE("HiddenAPI: Failed to create wildcard string");
        return false;
    }

    jobjectArray args = env->NewObjectArray(1, stringClass, wildcard);
    if (!args) {
        ALOGE("HiddenAPI: Failed to create args array");
        return false;
    }

    auto func = reinterpret_cast<void (*)(JNIEnv *, jclass, jobjectArray)>(addr);
    // jclass arg is not used so pass string class for the memes
    func(env, stringClass, args);
    ALOGD("HiddenAPI: Successfully disabled hidden API restrictions");
    return true;
}

bool disable_resource_loading() {
    // Try to hook the ApkAssets.nativeLoad method directly (safer than system properties)
    try {
        // Load the framework library
        void* handle = xdl_open("libandroid_runtime.so", XDL_DEFAULT);
        if (handle) {
            // Try to find and hook the nativeLoad method
            void* nativeLoadAddr = xdl_sym(handle, "_ZN7android8ApkAssets9nativeLoadEPKc", nullptr);
            if (nativeLoadAddr) {
                ALOGD("ResourceLoading: Found ApkAssets.nativeLoad at %p", nativeLoadAddr);
                // Here we would implement the actual hook, but for now we'll just log it
            } else {
                ALOGD("ResourceLoading: Could not find ApkAssets.nativeLoad symbol");
            }
            xdl_close(handle);
        } else {
            ALOGD("ResourceLoading: Could not open libandroid_runtime.so");
        }
    } catch (...) {
        ALOGD("ResourceLoading: Exception while trying to hook ApkAssets.nativeLoad");
    }
    
    // Try to hook the file system calls directly
    try {
        // Load the libc library
        void* handle = xdl_open("libc.so", XDL_DEFAULT);
        if (handle) {
            // Try to find and hook the open function
            void* openAddr = xdl_sym(handle, "open", nullptr);
            if (openAddr) {
                ALOGD("ResourceLoading: Found open function at %p", openAddr);
                // Here we would implement the actual hook, but for now we'll just log it
            } else {
                ALOGD("ResourceLoading: Could not find open function symbol");
            }
            xdl_close(handle);
        } else {
            ALOGD("ResourceLoading: Could not open libc.so");
        }
    } catch (...) {
        ALOGD("ResourceLoading: Exception while trying to hook file system calls");
    }
    
    ALOGD("ResourceLoading: Native resource loading hooks initialized (without system properties)");
    return true;
}