# ===== kotlinx.serialization =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.stargaze.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.stargaze.ai.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.stargaze.ai.** {
    *** Companion;
    *** INSTANCE;
}

# ===== Retrofit / OkHttp =====
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# ===== Hilt / Dagger =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembernames class * { @javax.inject.* <init>(...); }

# ===== Keep model classes used by serialization =====
-keep class com.stargaze.ai.network.** { *; }

# ===== Kotlin metadata =====
-keep class kotlin.Metadata { *; }

# ===== MediaPipe Tasks GenAI (on-device Gemma) =====
# Keep the public LLM inference API and its generated protobuf/AutoValue support.
-keep class com.google.mediapipe.tasks.genai.** { *; }
-keep class com.google.mediapipe.tasks.core.** { *; }
# These are compile-time/annotation-only references MediaPipe carries; safe to ignore at runtime.
-dontwarn com.google.auto.value.**
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.model.**
# We use text-only LLM inference; MediaPipe's optional vision/image classes are not bundled.
-dontwarn com.google.mediapipe.framework.image.**
# Protobuf lite generated message classes must keep their structure for reflection-based parsing.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
