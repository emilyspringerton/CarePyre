/* carepyre_sip_jni.c -- Phase 1 proof for SIP_PHONE_ANDROID_NORTHSTAR.md: the real
 * PARENA-C -> shared-library -> JNI link, exercised end to end with one real function
 * (PARENA's own sip/message.prn build-request) before any Android scaffolding exists.
 *
 * Real, minimal JNI shim: takes 6 Java Strings + 1 int (build-request's own real argument
 * shape, an Arena is a purely internal PARENA concept the JNI boundary never exposes),
 * creates a real PARENA Arena for the call's own lifetime, invokes the real generated
 * build_request C function, copies the result into a jstring, and tears the arena down --
 * the JNI-facing half of the "PARENA decides, host owns the platform" split
 * SIP_PHONE_ANDROID_NORTHSTAR.md names.
 */
#include "parena_runtime.h"
#include <jni.h>
#include <string.h>

/* Declared in sip_message_gen.c (PARENA's own generated C, not hand-written here). */
char *build_request(char *method, char *request_uri, char *from_uri, char *to_uri,
                     char *call_id, int cseq_num, char *via_host, Arena *dest);

JNIEXPORT jstring JNICALL
Java_org_carepyre_sip_SipNative_buildRequest(
    JNIEnv *env, jclass clazz,
    jstring jMethod, jstring jRequestUri, jstring jFromUri, jstring jToUri,
    jstring jCallId, jint jCseqNum, jstring jViaHost)
{
    (void)clazz;
    const char *method      = (*env)->GetStringUTFChars(env, jMethod, NULL);
    const char *requestUri  = (*env)->GetStringUTFChars(env, jRequestUri, NULL);
    const char *fromUri     = (*env)->GetStringUTFChars(env, jFromUri, NULL);
    const char *toUri       = (*env)->GetStringUTFChars(env, jToUri, NULL);
    const char *callId      = (*env)->GetStringUTFChars(env, jCallId, NULL);
    const char *viaHost     = (*env)->GetStringUTFChars(env, jViaHost, NULL);

    Arena arena;
    arena_init(&arena);

    char *result = build_request((char *)method, (char *)requestUri, (char *)fromUri,
                                  (char *)toUri, (char *)callId, (int)jCseqNum,
                                  (char *)viaHost, &arena);

    jstring jResult = (*env)->NewStringUTF(env, result);

    arena_free_all(&arena);
    (*env)->ReleaseStringUTFChars(env, jMethod, method);
    (*env)->ReleaseStringUTFChars(env, jRequestUri, requestUri);
    (*env)->ReleaseStringUTFChars(env, jFromUri, fromUri);
    (*env)->ReleaseStringUTFChars(env, jToUri, toUri);
    (*env)->ReleaseStringUTFChars(env, jCallId, callId);
    (*env)->ReleaseStringUTFChars(env, jViaHost, viaHost);

    return jResult;
}
