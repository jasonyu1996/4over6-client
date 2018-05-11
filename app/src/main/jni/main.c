#include <sys/socket.h>
#include <stdio.h>
#include "cn_edu_tsinghua_vpn4over6_VPNBackend.h"

JNIEXPORT jstring JNICALL Java_cn_edu_tsinghua_vpn4over6_VPNBackend_startThread
  (JNIEnv * env, jobject obj){

    return (*env)->NewStringUTF(env, "Hello world from JNI.");
}

