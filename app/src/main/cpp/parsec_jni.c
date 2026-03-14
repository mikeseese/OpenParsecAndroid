/**
 * JNI bridge between Kotlin ParsecSDKBridge and the native Parsec SDK.
 * Ported from iOS ParsecSDKBridge.swift native call layer.
 *
 * This implements the JNI methods declared as `external fun` in
 * ParsecSDKBridge.kt, delegating to the Parsec C SDK functions.
 *
 * Build requirements:
 * - Parsec SDK shared library (libparsec.so) in app/src/main/jniLibs/{abi}/
 * - Parsec SDK header in app/src/main/cpp/parsec-sdk/parsec.h
 *   (use the official SDK header for production builds)
 */

#include <jni.h>
#include <string.h>
#include <android/log.h>

#include "parsec-sdk/parsec.h"
#include "audio_android.h"

#define TAG "ParsecJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* JNI class path */
#define JNI_CLASS "com/aigch/openparsec/parsec/ParsecSDKBridge"

/* Cached method IDs for callbacks from native to Kotlin */
static jmethodID handleCursorEventMethodId = NULL;
static jmethodID handleUserDataEventMethodId = NULL;

/* Cached field IDs for getStatusEx */
static jfieldID hostWidthFieldId = NULL;
static jfieldID hostHeightFieldId = NULL;

/**
 * Parsec SDK log callback - forwards to Android logcat.
 */
static void parsec_log_callback(int32_t level, const char *msg, void *opaque)
{
    if (level == LOG_DEBUG) {
        LOGD("[Parsec] %s", msg);
    } else {
        LOGI("[Parsec] %s", msg);
    }
}

/* --- Initialization / Destruction --- */

JNIEXPORT jlong JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeInit(JNIEnv *env, jobject thiz)
{
    Parsec *parsec = NULL;

    ParsecSetLogCallback(parsec_log_callback, NULL);

    ParsecStatus status = ParsecInit(
        (PARSEC_VER_MAJOR << 16) | PARSEC_VER_MINOR,
        NULL, NULL, &parsec);

    if (status != PARSEC_OK || !parsec) {
        LOGE("ParsecInit failed with status %d", status);
        return 0;
    }

    /* Cache callback method IDs */
    jclass cls = (*env)->GetObjectClass(env, thiz);
    handleCursorEventMethodId = (*env)->GetMethodID(env, cls,
        "handleCursorEvent", "(ZZZIIIIII[B)V");
    handleUserDataEventMethodId = (*env)->GetMethodID(env, cls,
        "handleUserDataEvent", "(I[B)V");

    /* Cache field IDs for getStatusEx */
    hostWidthFieldId = (*env)->GetFieldID(env, cls, "hostWidth", "F");
    hostHeightFieldId = (*env)->GetFieldID(env, cls, "hostHeight", "F");

    (*env)->DeleteLocalRef(env, cls);

    if (!handleCursorEventMethodId || !handleUserDataEventMethodId) {
        LOGE("Failed to find callback method IDs");
        ParsecDestroy(parsec);
        return 0;
    }

    LOGD("ParsecInit succeeded, handle=%p", parsec);
    return (jlong)(intptr_t)parsec;
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeDestroy(JNIEnv *env, jobject thiz,
                                                                jlong parsec)
{
    if (parsec) {
        ParsecDestroy((Parsec *)(intptr_t)parsec);
        LOGD("ParsecDestroy called");
    }
}

JNIEXPORT jlong JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeAudioInit(JNIEnv *env, jobject thiz)
{
    struct audio *audio = NULL;
    audio_init(&audio);
    LOGD("Audio init, handle=%p", audio);
    return (jlong)(intptr_t)audio;
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeAudioDestroy(JNIEnv *env, jobject thiz,
                                                                     jlong audio)
{
    struct audio *ctx = (struct audio *)(intptr_t)audio;
    if (ctx) {
        audio_destroy(&ctx);
        LOGD("Audio destroyed");
    }
}

/* --- Connection --- */

JNIEXPORT jint JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeConnect(JNIEnv *env, jobject thiz,
                                                                jlong parsec,
                                                                jstring sessionId,
                                                                jstring peerId,
                                                                jint resX, jint resY,
                                                                jboolean decoderH265,
                                                                jboolean decoderCompat)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return PARSEC_ERR;

    const char *sid = (*env)->GetStringUTFChars(env, sessionId, NULL);
    const char *pid = (*env)->GetStringUTFChars(env, peerId, NULL);

    ParsecClientConfig cfg;
    memset(&cfg, 0, sizeof(cfg));

    /* Configure both video streams (matching iOS dual-stream config) */
    for (int i = 0; i < 2; i++) {
        cfg.video[i].decoderIndex = 1;
        cfg.video[i].resolutionX = resX;
        cfg.video[i].resolutionY = resY;
        cfg.video[i].decoderH265 = decoderH265;
        cfg.video[i].decoderCompatibility = decoderCompat;
    }

    cfg.mediaContainer = 0;
    cfg.protocol = 1;
    cfg.pngCursor = false;

    LOGD("Connecting to peer %s (res=%dx%d, h265=%d, compat=%d)",
         pid, resX, resY, decoderH265, decoderCompat);

    ParsecStatus status = ParsecClientConnect(p, &cfg, sid, pid);

    (*env)->ReleaseStringUTFChars(env, sessionId, sid);
    (*env)->ReleaseStringUTFChars(env, peerId, pid);

    return (jint)status;
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeDisconnect(JNIEnv *env, jobject thiz,
                                                                   jlong parsec, jlong audio)
{
    struct audio *actx = (struct audio *)(intptr_t)audio;
    if (actx) {
        audio_clear(&actx);
    }

    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (p) {
        ParsecClientDisconnect(p);
        LOGD("Disconnected");
    }
}

JNIEXPORT jint JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeGetStatus(JNIEnv *env, jobject thiz,
                                                                  jlong parsec)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return PARSEC_ERR;

    return (jint)ParsecClientGetStatus(p, NULL);
}

JNIEXPORT jint JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeGetStatusEx(JNIEnv *env, jobject thiz,
                                                                    jlong parsec)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return PARSEC_ERR;

    ParsecClientStatus status;
    memset(&status, 0, sizeof(status));
    ParsecStatus result = ParsecClientGetStatus(p, &status);

    /* Update host dimensions from decoder info (matching iOS getStatusEx) */
    if (hostWidthFieldId && hostHeightFieldId) {
        (*env)->SetFloatField(env, thiz, hostWidthFieldId, (jfloat)status.decoder[0].width);
        (*env)->SetFloatField(env, thiz, hostHeightFieldId, (jfloat)status.decoder[0].height);
    }

    return (jint)result;
}

/* --- Rendering --- */

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSetDimensions(JNIEnv *env, jobject thiz,
                                                                      jlong parsec,
                                                                      jint stream,
                                                                      jint width, jint height,
                                                                      jfloat scale)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (p) {
        ParsecClientSetDimensions(p, (uint8_t)stream, (uint32_t)width,
                                   (uint32_t)height, scale);
    }
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeRenderGLFrame(JNIEnv *env, jobject thiz,
                                                                      jlong parsec,
                                                                      jint stream,
                                                                      jint timeout)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (p) {
        ParsecClientGLRenderFrame(p, (uint8_t)stream, NULL, NULL, (uint32_t)timeout);
    }
}

/* --- Config --- */

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSetConfig(JNIEnv *env, jobject thiz,
                                                                  jlong parsec,
                                                                  jboolean decoderH265,
                                                                  jboolean decoderCompat,
                                                                  jint mediaContainer,
                                                                  jint protocol,
                                                                  jboolean pngCursor)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecClientConfig cfg;
    memset(&cfg, 0, sizeof(cfg));

    for (int i = 0; i < 2; i++) {
        cfg.video[i].decoderIndex = 1;
        cfg.video[i].resolutionX = 0;
        cfg.video[i].resolutionY = 0;
        cfg.video[i].decoderH265 = decoderH265;
        cfg.video[i].decoderCompatibility = decoderCompat;
    }

    cfg.mediaContainer = mediaContainer;
    cfg.protocol = protocol;
    cfg.pngCursor = pngCursor;

    ParsecClientSetConfig(p, &cfg);
    LOGD("Config applied (h265=%d, compat=%d)", decoderH265, decoderCompat);
}

/* --- Input Messages --- */

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSendMouseButton(JNIEnv *env, jobject thiz,
                                                                        jlong parsec,
                                                                        jint button,
                                                                        jboolean pressed)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecMessage msg;
    memset(&msg, 0, sizeof(msg));
    msg.type = MESSAGE_MOUSE_BUTTON;
    msg.mouseButton.button = (ParsecMouseButton)button;
    msg.mouseButton.pressed = pressed;
    ParsecClientSendMessage(p, &msg);
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSendMouseMotion(JNIEnv *env, jobject thiz,
                                                                        jlong parsec,
                                                                        jint x, jint y,
                                                                        jboolean relative)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecMessage msg;
    memset(&msg, 0, sizeof(msg));
    msg.type = MESSAGE_MOUSE_MOTION;
    msg.mouseMotion.x = x;
    msg.mouseMotion.y = y;
    msg.mouseMotion.relative = relative;
    ParsecClientSendMessage(p, &msg);
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSendMouseWheel(JNIEnv *env, jobject thiz,
                                                                       jlong parsec,
                                                                       jint x, jint y)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecMessage msg;
    memset(&msg, 0, sizeof(msg));
    msg.type = MESSAGE_MOUSE_WHEEL;
    msg.mouseWheel.x = x;
    msg.mouseWheel.y = y;
    ParsecClientSendMessage(p, &msg);
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSendKeyboard(JNIEnv *env, jobject thiz,
                                                                     jlong parsec,
                                                                     jint code,
                                                                     jboolean pressed)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecMessage msg;
    memset(&msg, 0, sizeof(msg));
    msg.type = MESSAGE_KEYBOARD;
    msg.keyboard.code = (ParsecKeycode)code;
    msg.keyboard.mod = MOD_NONE;
    msg.keyboard.pressed = pressed;
    ParsecClientSendMessage(p, &msg);
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSendGamepadButton(JNIEnv *env, jobject thiz,
                                                                          jlong parsec,
                                                                          jint controllerId,
                                                                          jint button,
                                                                          jboolean pressed)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecMessage msg;
    memset(&msg, 0, sizeof(msg));
    msg.type = MESSAGE_GAMEPAD_BUTTON;
    msg.gamepadButton.id = (uint32_t)controllerId;
    msg.gamepadButton.button = (ParsecGamepadButton)button;
    msg.gamepadButton.pressed = pressed;
    ParsecClientSendMessage(p, &msg);
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSendGamepadAxis(JNIEnv *env, jobject thiz,
                                                                        jlong parsec,
                                                                        jint controllerId,
                                                                        jint axis,
                                                                        jshort value)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecMessage msg;
    memset(&msg, 0, sizeof(msg));
    msg.type = MESSAGE_GAMEPAD_AXIS;
    msg.gamepadAxis.id = (uint32_t)controllerId;
    msg.gamepadAxis.axis = (ParsecGamepadAxis)axis;
    msg.gamepadAxis.value = value;
    ParsecClientSendMessage(p, &msg);
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSendGamepadUnplug(JNIEnv *env, jobject thiz,
                                                                          jlong parsec,
                                                                          jint controllerId)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecMessage msg;
    memset(&msg, 0, sizeof(msg));
    msg.type = MESSAGE_GAMEPAD_UNPLUG;
    msg.gamepadUnplug.id = (uint32_t)controllerId;
    ParsecClientSendMessage(p, &msg);
}

/* --- User Data --- */

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeSendUserData(JNIEnv *env, jobject thiz,
                                                                     jlong parsec,
                                                                     jint type,
                                                                     jbyteArray message)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    jsize len = (*env)->GetArrayLength(env, message);
    jbyte *data = (*env)->GetByteArrayElements(env, message, NULL);

    /* Null-terminate the message (matching iOS implementation) */
    char *nullTerminated = malloc(len + 1);
    if (nullTerminated) {
        memcpy(nullTerminated, data, len);
        nullTerminated[len] = '\0';
        ParsecClientSendUserData(p, (uint32_t)type, nullTerminated);
        free(nullTerminated);
    }

    (*env)->ReleaseByteArrayElements(env, message, data, JNI_ABORT);
}

/* --- Audio --- */

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativePollAudio(JNIEnv *env, jobject thiz,
                                                                  jlong parsec, jlong audio,
                                                                  jint timeout)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    struct audio *a = (struct audio *)(intptr_t)audio;
    if (p && a) {
        ParsecClientPollAudio(p, audio_cb, (uint32_t)timeout, a);
    }
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeAudioMute(JNIEnv *env, jobject thiz,
                                                                  jlong audio, jboolean muted)
{
    struct audio *a = (struct audio *)(intptr_t)audio;
    if (a) {
        audio_mute(muted, a);
    }
}

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativeAudioClear(JNIEnv *env, jobject thiz,
                                                                   jlong audio)
{
    struct audio *a = (struct audio *)(intptr_t)audio;
    if (a) {
        audio_clear(&a);
    }
}

/* --- Event Polling --- */

JNIEXPORT void JNICALL
Java_com_aigch_openparsec_parsec_ParsecSDKBridge_nativePollEvents(JNIEnv *env, jobject thiz,
                                                                   jlong parsec, jint timeout)
{
    Parsec *p = (Parsec *)(intptr_t)parsec;
    if (!p) return;

    ParsecClientEvent event;
    memset(&event, 0, sizeof(event));

    bool success = ParsecClientPollEvents(p, (uint32_t)timeout, &event);
    if (!success) return;

    if (event.type == CLIENT_EVENT_CURSOR && handleCursorEventMethodId) {
        jbyteArray imageData = NULL;

        if (event.cursor.cursor.imageUpdate) {
            void *buf = ParsecGetBuffer(p, event.cursor.key);
            if (buf) {
                imageData = (*env)->NewByteArray(env, event.cursor.cursor.size);
                (*env)->SetByteArrayRegion(env, imageData, 0,
                                            event.cursor.cursor.size,
                                            (const jbyte *)buf);
                ParsecFree(buf);
            }
        }

        (*env)->CallVoidMethod(env, thiz, handleCursorEventMethodId,
            (jboolean)event.cursor.cursor.hidden,
            (jboolean)event.cursor.cursor.relative,
            (jboolean)event.cursor.cursor.imageUpdate,
            (jint)event.cursor.cursor.width,
            (jint)event.cursor.cursor.height,
            (jint)event.cursor.cursor.hotX,
            (jint)event.cursor.cursor.hotY,
            (jint)event.cursor.cursor.positionX,
            (jint)event.cursor.cursor.positionY,
            imageData);

        if (imageData) {
            (*env)->DeleteLocalRef(env, imageData);
        }

    } else if (event.type == CLIENT_EVENT_USER_DATA && handleUserDataEventMethodId) {
        void *buf = ParsecGetBuffer(p, event.userData.key);
        if (buf) {
            size_t len = strlen((const char *)buf);
            jbyteArray data = (*env)->NewByteArray(env, (jsize)len);
            (*env)->SetByteArrayRegion(env, data, 0, (jsize)len,
                                        (const jbyte *)buf);
            (*env)->CallVoidMethod(env, thiz, handleUserDataEventMethodId,
                                    (jint)event.userData.id, data);
            (*env)->DeleteLocalRef(env, data);
            ParsecFree(buf);
        }
    }
}
