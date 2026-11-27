-dontwarn org.slf4j.**

-keep class com.powersync.** { *; }

-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
