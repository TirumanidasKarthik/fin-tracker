# Release and data upgrade policy

## Version numbers

- Use `MAJOR.MINOR.PATCH` for `versionName` and Git tags (`v0.2.0`, `v0.2.1`). Keep the app in `0.x` while it is still being tested; use `1.0.0` for the first stable release.
- Increment Android `versionCode` for every published APK. Never reuse or lower a published code. It is independent of the database version.
- Keep `applicationId` (`com.fintracker.app`) unchanged after release so Android recognizes later APKs as updates to the same app.
- Treat the SQLite schema version and backup-file format version as separate counters. Bump them only when their respective data formats change.

## Before each release

1. Finish the feature on a branch and open a pull request to `main`.
2. Require the Android CI workflow to pass its unit tests and debug build.
3. For any database change, add a forward migration and test it using a database created by the prior released app. Confirm that income sources, banks, buckets, categories, targets, amounts, and bank links survive.
4. For a backup-format change, preserve import support for previous backup versions or add an explicit backup-file migration. Do not change the meaning of an already released format version.
5. Update `versionName`, `versionCode`, and release notes after validation, merge to `main`, then tag the exact release commit as `vX.Y.Z`.

## Local database changes

The app currently uses `SQLiteOpenHelper` with schema version 3. When a feature changes the schema, increment `DATABASE_VERSION` and add the next forward step in `BudgetDatabase.onUpgrade`, such as `if (oldVersion < 4) { ... }`. Keep existing migration steps unchanged after they have shipped; correct a shipped migration by adding another step. Android runs upgrades in a transaction and rolls back a failed upgrade.

Features that only change UI or calculations do not require a database version bump. Never clear or recreate the user's tables as an upgrade shortcut. Test the oldest database version that the release promises to support, including direct upgrades across multiple versions.

## Signed APK on GitHub

1. Generate a dedicated Android release signing key and keep an offline backup. Do not commit the keystore or passwords; the repository `.gitignore` excludes common keystore files.
2. Keep that same key for every APK release. Android only accepts an in-place update when the application ID and signer match the installed app.
3. Build a signed **APK** for direct Android installation. A debug APK is for development; an AAB is primarily for app stores and is not a normal direct-install file.
4. Push a `vX.Y.Z` tag from the reviewed release commit. The `Android release` workflow checks that the tag matches `versionName`, runs unit tests, builds and verifies the signed APK, then attaches it to a GitHub Release with generated notes.
5. Download the APK from the published release, verify its signature, and install it over the previous signed release before announcing it. GitHub Releases supports binary assets; do not commit APKs into the source tree.

### Configure automated signing once

The repository workflow needs a dedicated release keystore, which is a long-lived app identity. Generate it locally and keep two secure offline copies before configuring automation. Never reuse the debug key or commit the keystore/passwords. The Gradle build reads signing settings only from environment variables, and release CI rejects a missing or invalid signature.

In the public GitHub repository:

1. Create a GitHub Actions environment named `android-release`. Add these as **environment secrets**, not repository variables: `ANDROID_RELEASE_KEYSTORE_BASE64`, `ANDROID_RELEASE_KEY_ALIAS`, `ANDROID_RELEASE_KEYSTORE_PASSWORD`, and `ANDROID_RELEASE_KEY_PASSWORD`. Store the base64 encoding of the keystore file as the first secret.
2. Restrict who can push `v*` tags with a tag ruleset, and protect `main` so release tags are created only from reviewed commits. Where available, require approval for deployments to the `android-release` environment.
3. Increment `versionCode` and update `versionName` in `app/build.gradle.kts` before creating the release tag. The workflow refuses a tag whose version does not equal `versionName`.
4. After pushing the tag, inspect the Actions run and the GitHub Release asset. Confirm the APK installs as an update and the app data remains. Never delete or replace a published tag to ship a correction; increment both the patch version and version code.

Until the release key has been generated, backed up, and configured as environment secrets, do not push a release tag: the workflow intentionally fails instead of publishing an unsigned APK.

For the first signed release, this project is currently installed on the development phone as a debug build. Its debug signing key differs from the production release key, so Android cannot normally update it in place. Before removing that debug installation, export an encrypted backup, install the first signed APK, and restore the backup. Future releases signed with the same release key can update in place and retain the database.

The app has Android cloud backup disabled. Uninstalling or clearing app storage deletes its local database; the encrypted export is the recovery path. Users choose the destination in the Android file picker. The app itself has no network permission, but a user-selected cloud document provider may sync a file.

## Cost and automation

GitHub Actions standard hosted runners are free for a public repository. The CI workflow runs tests and a debug build on pull requests and pushes to `main`. The tagged-release workflow signs and uploads the APK using environment-scoped secrets. This avoids committing credentials and keeps signing unavailable to normal pull request builds. GitHub Releases can host the source archive and APK without a paid app-store account.
