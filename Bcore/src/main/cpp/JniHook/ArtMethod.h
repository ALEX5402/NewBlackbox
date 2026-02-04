


#include <stdio.h>

#ifndef ARTHOOK_ART_METHOD_H
#define ARTHOOK_ART_METHOD_H

#define __ANDROID_API_R__ 30
#define __ANDROID_API_Q__ 29
#define __ANDROID_API_P__ 28

static constexpr uint32_t kAccPublic =       0x0001;  
static constexpr uint32_t kAccPrivate =      0x0002;  
static constexpr uint32_t kAccProtected =    0x0004;  
static constexpr uint32_t kAccStatic =       0x0008;  
static constexpr uint32_t kAccFinal =        0x0010;  
static constexpr uint32_t kAccSynchronized = 0x0020;  
static constexpr uint32_t kAccSuper =        0x0020;  
static constexpr uint32_t kAccVolatile =     0x0040;  
static constexpr uint32_t kAccBridge =       0x0040;  
static constexpr uint32_t kAccTransient =    0x0080;  
static constexpr uint32_t kAccVarargs =      0x0080;  
static constexpr uint32_t kAccNative =       0x0100;  
static constexpr uint32_t kAccInterface =    0x0200;  
static constexpr uint32_t kAccAbstract =     0x0400;  
static constexpr uint32_t kAccStrict =       0x0800;  
static constexpr uint32_t kAccSynthetic =    0x1000;  
static constexpr uint32_t kAccAnnotation =   0x2000;  
static constexpr uint32_t kAccEnum =         0x4000;  

static constexpr uint32_t kAccPublicApi =             0x10000000;  
static constexpr uint32_t kAccCorePlatformApi =       0x20000000;  




static constexpr uint32_t kAccFastNative =            0x00080000;  
static constexpr uint32_t kAccCriticalNative =        0x00100000;  

static constexpr uint32_t kAccNterpInvokeFastPathFlag     = 0x00200000;  

#endif 
