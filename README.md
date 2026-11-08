# Fin Tracker

A lightweight native Android app for planning monthly income across spending, savings, investment, and goal buckets.

## Current features

- Multiple named monthly income sources with an automatically calculated total.
- Editable bucket target percentages and planned categories with account labels.
- Local SQLite storage with a schema migration that preserves existing budget data.
- No login, Internet permission, remote database, or cloud/device backup of app data.
- Responsive Android Views UI with the budget model, repository, controller, and screen kept in separate modules.

The app starts with a generic sample budget to demonstrate buckets and multiple income sources. Replace the sample names and amounts with your own budget on the device; no personal workbook values are included in the source.

## Build and test

Open this folder in Android Studio, or run these commands from PowerShell:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The project uses Android SDK API 36 and Android Studio's bundled Java runtime. Set `JAVA_HOME` to the Android Studio JBR if Gradle cannot find Java.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
