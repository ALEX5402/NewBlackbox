//
// updated by alex5402 on 4/25/21.
//

#include "BinderHook.h"
#include <IO.h>
#include <BoxCore.h>
#include "UnixFileSystemHook.h"
#import "JniHook/JniHook.h"



HOOK_JNI(jint, getCallingUid, JNIEnv *env, jobject obj) {
    int orig = orig_getCallingUid(env, obj);
    return BoxCore::getCallingUid(env, orig);
}


void BinderHook::init(JNIEnv *env) {
    const char *clazz = "android/os/Binder";
    JniHook::HookJniFun(env, clazz, "getCallingUid", "()I", (void *) new_getCallingUid,
                        (void **) (&orig_getCallingUid), true);
}