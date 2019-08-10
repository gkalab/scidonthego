LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -llog -latomic
LOCAL_MODULE    := jni
LOCAL_SRC_FILES := jniscid.cpp

LOCAL_STATIC_LIBRARIES := scid

include $(BUILD_SHARED_LIBRARY)

include jni/scid/Android.mk
