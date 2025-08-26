#include <jni.h>
#include <string>
#include <cmath>
#include <vector>
#include <algorithm>

/**
 * 返回C++字符串的JNI方法
 */
extern "C" jstring
Java_com_combo_plugin_sample_example_jni_NativeLib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

/**
 * 计算两个整数相加的JNI方法
 */
extern "C" jint
Java_com_combo_plugin_sample_example_jni_NativeLib_addNumbers(
        JNIEnv *env,
        jobject /* this */,
        jint a,
        jint b) {
    return a + b;
}

/**
 * 计算平方根的JNI方法
 */
extern "C" jdouble
Java_com_combo_plugin_sample_example_jni_NativeLib_calculateSquareRoot(
        JNIEnv *env,
        jobject,
        jdouble number) {
    return sqrt(number);
}

/**
 * 处理字符串数组并返回连接结果的JNI方法
 */
extern "C" jstring
Java_com_combo_plugin_sample_example_jni_NativeLib_processStringArray(
        JNIEnv *env,
        jobject,
        jobjectArray stringArray) {

    jsize length = env->GetArrayLength(stringArray);
    std::string result = "Processed strings: ";

    for (jsize i = 0; i < length; i++) {
        jstring jstr = (jstring) env->GetObjectArrayElement(stringArray, i);
        const char *str = env->GetStringUTFChars(jstr, nullptr);

        if (i > 0) result += ", ";
        result += str;

        env->ReleaseStringUTFChars(jstr, str);
        env->DeleteLocalRef(jstr);
    }

    return env->NewStringUTF(result.c_str());
}

/**
 * 获取系统信息的JNI方法
 */
extern "C" jstring
Java_com_combo_plugin_sample_example_jni_NativeLib_getSystemInfo(
        JNIEnv *env,
        jobject) {

    std::string info = "Native Library Info:\n";
    info += "- Compiler: ";
#ifdef __clang__
    info += "Clang ";
    info += __clang_version__;
#elif defined(__GNUC__)
    info += "GCC ";
    info += __VERSION__;
#else
    info += "Unknown";
#endif

    info += "\n- Architecture: ";
#ifdef __aarch64__
    info += "ARM64";
#elif defined(__arm__)
    info += "ARM32";
#elif defined(__x86_64__)
    info += "x86_64";
#elif defined(__i386__)
    info += "x86";
#else
    info += "Unknown";
#endif

    info += "\n- C++ Standard: ";
    info += std::to_string(__cplusplus);

    return env->NewStringUTF(info.c_str());
}
