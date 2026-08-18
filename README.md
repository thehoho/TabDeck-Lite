# TapDeck Lite

[![Android verification](https://github.com/thehoho/TabDeck-Lite/actions/workflows/android.yml/badge.svg)](https://github.com/thehoho/TabDeck-Lite/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/github/v/release/thehoho/TabDeck-Lite)](https://github.com/thehoho/TabDeck-Lite/releases/latest)
[![Permissions: none](https://img.shields.io/badge/Android_permissions-none-70E1B5)](app/src/main/AndroidManifest.xml)

## One tap. One command.

TapDeck Lite is a compact Android shortcut keyboard for command-driven Discord chats and bots such as OwO. Save up to 20 commands, label them clearly, then tap one key to insert or immediately send it.

Whether a command is short or long, every saved key keeps it consistent and ready. TapDeck Lite makes repetitive command entry faster, easier, and less error-prone.

## Download

Download the APK from the [latest GitHub Release](https://github.com/thehoho/TabDeck-Lite/releases/latest). The release includes a SHA-256 checksum so the downloaded file can be verified.

> Android shows an unknown-source warning for apps installed outside Google Play and a separate privacy warning when any third-party keyboard is enabled. These are normal platform warnings. TapDeck Lite requests zero Android permissions and has no Internet capability.

The GitHub APK retains the signing identity used by existing testers so it installs as an update for them. It is published as a debug-signed community build.

## How it works

1. Configure up to 20 command keys inside TapDeck Lite.
2. Choose **Insert only** or **Insert + send** for each key.
3. Open Discord and focus the message composer.
4. Tap one key. TapDeck Lite handles only that selected command.
5. Use **ABC** to return immediately to your normal typing keyboard.

A key can hold a short command or a longer phrase up to the app's 4,000-character limit.


## Why the source is public

TapDeck Lite is a keyboard, so trust matters. This repository makes its privacy boundary directly inspectable:

- The [manifest](app/src/main/AndroidManifest.xml) requests no permissions.
- The only runtime libraries are the Kotlin standard library and JetBrains annotations; there are no advertising, analytics, network, account, or social SDKs.
- Commands stay in app-private local preferences.
- Android cloud backup is disabled.
- There is no clipboard access or Accessibility Service.
- Public verification runs the tests, Android lint, and APK build from this source.
- The [privacy policy](PRIVACY-POLICY.md), [security policy](SECURITY.md), source code, and release checksum are public.

## Features

- One deck of exactly 20 configurable command keys.
- Per-key **Insert only** or **Insert + send** behavior.
- One tap inserts or sends the selected command.
- Discord-tested send sequence: commit the command, wait 250 ms, then issue raw Enter.
- Compact 5 × 4 command grid.
- Fifth utility row with **ABC**, Backspace, Settings, Android keyboard picker, and Enter.
- Setup, Keys, and Privacy tabs in the companion app.
- Empty first-run deck with no bundled commands.
- Dynamic clearance above Android gesture and navigation controls.
- Completely offline with no ads, analytics, accounts, tracking, or upgrade prompts.

## Install and set up

1. Download the APK from [Releases](https://github.com/thehoho/TabDeck-Lite/releases/latest).
2. Allow installation from the browser or file manager Android identifies.
3. Open TapDeck Lite and use the **Setup** tab.
4. Tap **Enable keyboard** and enable TapDeck Lite in Android settings.
5. Tap **Choose keyboard** and select TapDeck Lite.
6. Open **Keys** and configure any of the 20 keys.
7. Open Discord, focus its composer, and tap a configured key.

## Compatibility

**Insert + send** is optimized for Discord and applications where raw Enter sends the message. Some applications treat Enter as a new line. In those apps, use **Insert only** and press the app's Send button.

TapDeck Lite is an independent application. It is not affiliated with, endorsed by, sponsored by, or an official product of Discord, OwO, WhatsApp, Meta, Google, or Apple.

## Build from source

Requirements: JDK 17 and Android SDK 36.

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

macOS/Linux:

```bash
chmod +x gradlew
./gradlew testDebugUnitTest lintDebug assembleDebug
```


## Support

- [Report a bug](https://github.com/thehoho/TabDeck-Lite/issues)
- [Report a vulnerability privately](https://github.com/thehoho/TabDeck-Lite/security/advisories/new)
- [Read the hosted privacy policy](https://thehoho.github.io/TabDeck-Lite/privacy)

## License

The source is public for inspection and security review. Copyright is retained; see [LICENSE](LICENSE). No permission to copy, modify, redistribute, sell, or rebrand the app is granted without written approval.
