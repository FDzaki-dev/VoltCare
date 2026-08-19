# VoltCare - ProGuard rules
-keep class com.voltcare.app.data.db.entity.** { *; }
-keepattributes *Annotation*
-keep class androidx.room.** { *; }
-dontwarn kotlinx.coroutines.**
