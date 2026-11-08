# Fin Tracker

Fin Tracker is a lightweight, offline-first Android app for planning monthly income across budget buckets and seeing how much to keep in each bank. It uses a local profile without an account or sign-in.

## Features

- Add multiple monthly income sources and assign each one to a bank.
- Set bucket target percentages and add planned categories with amounts and bank assignments.
- Review planned amounts by bucket and the total amount to keep in each bank.
- Manage banks, with deletion prevented while a bank is assigned to an income source or category.
- Store budget data in a local SQLite database on the device.
- Migrate existing budget data when the local database schema changes.
- Keep the screen responsive on different phone widths.

The first launch creates a generic sample budget so the app is ready to explore. Replace the sample names and amounts with your own.

## Privacy and security

- The app declares no Android permissions, including the Internet permission. It does not access mobile or Wi-Fi data, and has no remote database, account sign-in, analytics, or cloud service.
- Budget information is stored in the app's private on-device SQLite database. The database is not encrypted by the app. Someone who can unlock the device and open the app can view its contents; use your device's screen lock to protect access.
- Android cloud backup and device-transfer backup are disabled for the app. Uninstalling or clearing app storage removes the budget; the app does not currently provide export or restore.
- Build tools download Android and Gradle dependencies during development. This build-time network use does not add network access to the installed app.

## Development process

This project has been developed entirely through vibe coding with ChatGPT Codex. Requirements and product decisions come from the project owner; implementation has been created and iterated with Codex.

## Build and test

### Prerequisites

- Android Studio with its bundled Java runtime.
- Android SDK Platform 36.
- A Windows, macOS, or Linux development environment. Commands below are for Windows PowerShell.

Open the project folder in Android Studio and let Gradle sync. Or run from PowerShell:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

If Gradle cannot find Java, set `JAVA_HOME` to the `jbr` directory inside your Android Studio installation. Install Android SDK Platform 36 from Android Studio's SDK Manager if Gradle reports that the platform is missing.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To install it on a connected device with USB debugging enabled:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

- `app/src/main/java/com/fintracker/app/domain/budget/` — budget models, allocation, and money calculations.
- `app/src/main/java/com/fintracker/app/data/` — local SQLite database and repository.
- `app/src/main/java/com/fintracker/app/presentation/budget/` — UI commands, controller, and Android Views screen.
- `app/src/test/` — JVM unit tests for budget calculations and allocation.
