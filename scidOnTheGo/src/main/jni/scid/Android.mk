LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := scid
LOCAL_SRC_FILES := \
    misc.cpp index.cpp date.cpp namebase.cpp \
    position.cpp game.cpp gfile.cpp matsig.cpp \
    bytebuf.cpp textbuf.cpp myassert.cpp stralloc.cpp \
    mfile.cpp dstring.cpp pgnparse.cpp stored.cpp \
    movelist.cpp

LOCAL_STATIC_LIBRARIES :=

include $(BUILD_STATIC_LIBRARY)
