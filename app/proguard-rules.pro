# Add project specific ProGuard rules here.

# Keep Room entities and DAOs
-keep class com.belinze.lifeos.data.db.entity.** { *; }
-keep class com.belinze.lifeos.data.db.dao.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep SMS parser classes (referenced by name in WorkManager)
-keep class com.lifeos.sms.** { *; }

# Keep DataStore proto/preferences classes
-keep class androidx.datastore.** { *; }

# OpenCSV
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**
