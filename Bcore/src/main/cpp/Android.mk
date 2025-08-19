LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libdobby
LOCAL_SRC_FILES := Dobby/$(TARGET_ARCH_ABI)/libdobby.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := xdl
LOCAL_CXXFLAGS := -std=c++11 -fno-exceptions -fno-rtti
LOCAL_EXPORT_C_INCLUDES:=$(LOCAL_PATH)
LOCAL_SRC_FILES := xdl/xdl.c \
    xdl/xdl_iterate.c \
    xdl/xdl_linker.c \
    xdl/xdl_lzma.c \
    xdl/xdl_util.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_STATIC_LIBRARY)


include $(CLEAR_VARS)
# Collect all source files in the current directory
SRC1 := $(wildcard $(LOCAL_PATH)/*.cpp) $(wildcard $(LOCAL_PATH)/*.c)
# Collect all source files in Utils/
SRC2 := $(wildcard $(LOCAL_PATH)/Utils/*.cpp) $(wildcard $(LOCAL_PATH)/Utils/*.c)
# Collect all source files in Hook/
SRC3 := $(wildcard $(LOCAL_PATH)/Hook/*.cpp) $(wildcard $(LOCAL_PATH)/Hook/*.c)
# Collect all source files in JniHook/
SRC4 := $(wildcard $(LOCAL_PATH)/JniHook/*.cpp) $(wildcard $(LOCAL_PATH)/JniHook/*.c)

LOCAL_MODULE := blackbox
LOCAL_SRC_FILES := BoxCore.cpp \
hidden_api.cpp \
IO.cpp \
Utils/elf_util.cpp \
Hook/DexFileHook.cpp \
Hook/FileSystemHook.cpp \
Utils/VirtualSpoof.cpp \
Utils/HexDump.cpp \
Utils/AntiDetection.cpp \
Hook/VMClassLoaderHook.cpp \
Hook/UnixFileSystemHook.cpp \
Hook/BinderHook.cpp \
Hook/BaseHook.cpp \
JniHook/JniHook.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_CFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -std=c++17
LOCAL_CPPFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -Werror -fms-extensions
LOCAL_LDFLAGS += -Wl,--gc-sections,--strip-all
LOCAL_ARM_MODE := arm

LOCAL_CPP_FEATURES := exceptions
LOCAL_STATIC_LIBRARIES := libdobby xdl
LOCAL_LDLIBS := -llog -landroid -lz
include $(BUILD_SHARED_LIBRARY)
