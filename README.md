# Lumex — Exponometer

Bare-bones native Android scaffold (Kotlin + Jetpack Compose Material3).

Package: `com.lumex.app` · minSdk 26 · target/compile 34

## Prereqs (not yet installed on this machine)
- JDK 17 (you have Temurin 25 — AGP 8.x needs 17)
- Android SDK + platform-tools (`adb`, `sdkmanager`)
- Android Studio (recommended, bundles the above), or cmdline-tools + Gradle

## Next steps once SDK is installed
1. Point Gradle at SDK: `cp local.properties.example local.properties` and edit `sdk.dir`
2. Generate full wrapper: `gradle wrapper` (replaces stub `gradlew`)
3. Build: `./gradlew assembleDebug`
4. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
