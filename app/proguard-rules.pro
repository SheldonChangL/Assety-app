# Preserve source file and line number attributes for crash diagnosis
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- SQLCipher (net.zetetic:sqlcipher-android) ----------
# The native JNI bridge and Room integration class must not be renamed or stripped
-keep class net.zetetic.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.**

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.TypeConverter
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-dontwarn androidx.room.paging.**

# ---------- CameraX ----------
# CameraX uses reflection internally for UseCaseConfigFactory and SessionProcessor
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---------- WorkManager ----------
# Worker subclasses are instantiated reflectively by WorkManager
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---------- ML Kit Text Recognition ----------
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_bundled.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# ---------- Hilt ----------
# Hilt generates its own consumer rules, but keep entry points explicitly
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.EntryPoint class * { *; }

# ---------- Kotlin Coroutines ----------
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---------- JSON / Backup ----------
# BackupSerializer uses org.json for serialization; keep the public API
-keep class org.json.** { *; }
