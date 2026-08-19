# PowerVault Health Pro - ProGuard rules
-keep class com.powervault.health.pro.data.db.entity.** { *; }
-keepattributes *Annotation*
-keep class androidx.room.** { *; }
-dontwarn kotlinx.coroutines.**
