#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_vrtheater_NativeBridge_hello(JNIEnv* env, jobject /*thiz*/) {
    const char* msg = "Native OK";
    return env->NewStringUTF(msg);
}