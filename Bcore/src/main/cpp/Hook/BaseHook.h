#ifndef VIRTUALM_BASEHOOK_H
#define VIRTUALM_BASEHOOK_H

#include <jni.h>
#include <Log.h>

class BaseHook {
public:
    static void init(JNIEnv *env);
};


#endif 
