# Contributing to FreeDeck Launcher

Thanks for helping. Bug reports, fixes, translations and ideas are all welcome.

## Report a problem

Open an [issue](../../issues/new/choose) and include:

- your device (for example "Fire TV Stick 4K Max, Fire OS 8") and the FreeDeck version (Settings has it in the app details),
- what you did, what you expected, and what happened,
- a photo or screenshot if it is visual. `adb logcat -s AndroidRuntime` helps for crashes.

Please **do not post** your Wi-Fi name, IP address or location in screenshots.

## Build and test

```bash
./gradlew testDebugUnitTest lintRelease assembleDebug
```

You need JDK 17 and the Android SDK (API 35). See [Build from source](README.md#build-from-source).

## Send a change

1. Fork the repository and create a branch.
2. Keep the change focused, and match the style around it (plain Java, no new libraries without a good reason).
3. Add or update a unit test when you change logic in `data/`, `system/` or the `ui/` helpers that have tests.
4. Run the command above. CI runs the same checks on every pull request.
5. Describe what you changed and how you tested it. Screenshots help for visual changes.

### Translations

Strings live in `app/src/main/res/values/strings.xml`, and each language has a `values-xx/strings.xml`. A missing string
falls back to English, so a partial translation is welcome. Keep placeholders such as `%1$s` unchanged.

### Things worth knowing

- This app is for **Amazon Fire TV**. Fire OS has no "default home app" setting and no list of accessibility services, so
  the Home button works through an accessibility service that the app switches on itself. That needs
  `WRITE_SECURE_SETTINGS`, which is granted once with `adb` (or by the app, through the TV's own ADB prompt).
- Never commit a keystore, passwords or `local.properties`. They are in `.gitignore`.
- Released APKs go in `apks/vX.Y/` with a checksum (see "Making a release" in the README). Do not edit old ones.

## Make your own version

You are free to fork and change it under the [MIT licence](LICENSE). The README explains how to rename the app and use
your own signing key.
