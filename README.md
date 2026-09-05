# Lumex — Exponometer

Bare-bones native Android scaffold (Kotlin + Jetpack Compose Material3).

Package: `com.lumex.app` · minSdk 26 · target/compile 36 (Android 16) · AGP 8.10.1 · Kotlin 2.1.0 · Gradle 8.13

## Prereqs
- Android Studio + SDK (done) with **Android 16 (API 36)**: SDK Platform 36 + Build-Tools 36
- JDK 17 for Gradle (Studio JBR works; terminal `JAVA_HOME` pointing at Java 25 will fail — set `org.gradle.java.home` or `JAVA_HOME` to JDK 17)

## Next steps once SDK is installed
1. Point Gradle at SDK: `cp local.properties.example local.properties` and edit `sdk.dir`
2. Generate full wrapper: `gradle wrapper` (replaces stub `gradlew`)
3. Build: `./gradlew assembleDebug`
4. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
