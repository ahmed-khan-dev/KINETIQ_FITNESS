# R8 Proguard Rules for Kinetiq App Obfuscation & Security

# Room Keep Rules
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.kinetiq.data.local.entity.** { *; }
-keepclassmembers class com.example.kinetiq.data.local.entity.** { *; }

# ViewBinding & Navigation Keep Rules
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}

# Preserve Custom Views & ViewModels
-keep class com.example.kinetiq.ui.** { *; }
-keep class com.example.kinetiq.ui.home.CircularCalorieRingView { *; }
-keep class com.example.kinetiq.ui.progress.WeightTrajectoryView { *; }
-keep class com.example.kinetiq.ui.progress.StrengthTrajectoryView { *; }
