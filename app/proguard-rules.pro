# ProGuard rules for BunkerStock
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.bunkerstock.app.data.**$$serializer { *; }
-keepclassmembers class com.bunkerstock.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.bunkerstock.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
