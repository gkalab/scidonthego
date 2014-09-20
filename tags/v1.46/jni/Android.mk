LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := jni
LOCAL_SRC_FILES := jniscid.cpp
LOCAL_LDLIBS    := -llog

LOCAL_CFLAGS    := -mandroid \
	-DTARGET_OS=android -D__ANDROID__ \
	-isystem $(SYSROOT)/usr/include

LOCAL_STATIC_LIBRARIES := scid

include $(BUILD_SHARED_LIBRARY)

include jni/scid/Android.mk
