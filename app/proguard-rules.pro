# Add project specific ProGuard rules here.

# Keep SMS parser classes (referenced by name in WorkManager)
-keep class com.lifeos.sms.** { *; }

# Keep DataStore proto/preferences classes
-keep class androidx.datastore.** { *; }
