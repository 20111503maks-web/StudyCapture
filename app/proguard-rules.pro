# Tesseract4Android calls back into Java classes from native code.
-keep class com.googlecode.tesseract.android.** { *; }

# kotlinx.serialization: keep generated serializers for navigation routes and stored JSON.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.studycapture.** {
    *** Companion;
}
-keepclasseswithmembers class app.studycapture.** {
    kotlinx.serialization.KSerializer serializer(...);
}
