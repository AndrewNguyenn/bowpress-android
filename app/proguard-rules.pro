# Add project-specific ProGuard rules here.
# See https://developer.android.com/build/shrink-code for reference.

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Kotlinx serialization keeps @Serializable classes — rules are bundled with the library.

# uCrop — launched via Intent so R8 can't trace the reference and strips the class.
-keep class com.yalantis.ucrop.UCropActivity
