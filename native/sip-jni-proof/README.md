# SIP JNI proof (Phase 1 of `docs/SIP_PHONE_ANDROID_NORTHSTAR.md`)

Real, minimal proof that PARENA's `stdlib/sip/message.prn` can be compiled to C, wrapped in a
shared library, and called from Java through a real JNI native method — the load-bearing link
the eventual CarePyre Android app needs (SIP-0001's own "java ffi escape hatch when needed"),
verified before any Android/Gradle scaffolding exists.

## Files

- `sip_message_gen.c` — real PARENA-generated C, produced by
  `parena build stdlib/string.prn stdlib/sip/message.prn -o sip_message_gen.c` in the `PARENA`
  repo. Regenerate from there if `message.prn` changes; do not hand-edit.
- `parena_runtime.h` / `parena_runtime.c` — copied verbatim from `PARENA/runtime/`. Regenerate
  the same way.
- `carepyre_sip_jni.c` — the real, hand-written JNI shim exposing
  `Java_org_carepyre_sip_SipNative_buildRequest`.
- `SipNative.java` — the real Java side: the native method declaration + a smoke test that
  builds a REGISTER request and asserts its shape.

## Build and run

Requires a real JDK with `jni.h` (this proof used `/home/fatbaby/EINHORN_SURVIVAL/jdk25`, no
install needed on this box):

```bash
export JDK=/path/to/a/real/jdk
gcc -std=c99 -Wall -Wextra -fPIC -shared \
  -I "$JDK/include" -I "$JDK/include/linux" \
  carepyre_sip_jni.c sip_message_gen.c parena_runtime.c \
  -o libcarepyre_sip.so -lm

"$JDK/bin/javac" -d classes SipNative.java
"$JDK/bin/java" -Djava.library.path=. -cp classes org.carepyre.sip.SipNative
```

Expect `PASS: real SIP REGISTER built by PARENA's build-request, through a real JNI call,
matches RFC 3261's own required shape`.

## Real, honest note on include order

`parena_runtime.h` must be included before `<jni.h>` in `carepyre_sip_jni.c` — it sets its own
feature-test macros, and including a JDK header first locks glibc into a narrower default first
(found live: `struct addrinfo`/`getaddrinfo` etc. failed to compile until the include order was
fixed to match every existing PARENA test file's own convention).
