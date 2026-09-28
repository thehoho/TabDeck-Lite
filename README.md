# TapDeck Lite

[![Android verification](https://github.com/thehoho/TapDeck-Lite/actions/workflows/android.yml/badge.svg)](https://github.com/thehoho/TapDeck-Lite/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/github/v/release/thehoho/TapDeck-Lite)](https://github.com/thehoho/TapDeck-Lite/releases/latest)
[![Permissions: none](https://img.shields.io/badge/Android_permissions-none-70E1B5)](app/src/main/AndroidManifest.xml)

## One tap. One command.

TapDeck Lite is a compact Android shortcut keyboard for command-driven Discord chats and bots such as OwO. Save up to 40 commands across two pages, label them clearly, then tap one key to insert or immediately send it.

Whether a command is short or long, every saved key keeps it consistent and ready. TapDeck Lite makes repetitive command entry faster, easier, and less error-prone.

## Download

Download the APK from the [latest GitHub Release](https://github.com/thehoho/TapDeck-Lite/releases/latest). The release includes a SHA-256 checksum so the downloaded file can be verified.

> Android shows an unknown-source warning for apps installed outside Google Play and a separate privacy warning when any third-party keyboard is enabled. These are normal platform warnings. TapDeck Lite requests zero Android permissions and has no Internet capability.

The GitHub APK is a non-debuggable release build. It retains the established GitHub-distribution signing identity so it installs as an update for existing users.

## How it works

1. Configure up to 40 command keys across Page 1 and Page 2 inside TapDeck Lite.
2. Choose **Insert only** or **Insert + send** for each key.
3. Open Discord and focus the message composer.
4. Tap one key. TapDeck Lite handles only that selected command.
5. To reorder the deck, open **Keys**, then long press a configured card and drag it onto another slot to swap them.
6. Use **ABC** to return immediately to your normal typing keyboard.

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

- Two pages of 20 configurable command keys, for 40 saved commands total.
- Per-key **Insert only** or **Insert + send** behavior.
- One tap inserts or sends the selected command.
- Discord-tested send sequence: commit the command, wait 250 ms, then issue raw Enter.
- Compact 5 × 4 command grid.
- Optional saved layouts per page: show all slots or configured keys only, with 1–5 keys per row.
- Automatic P1/P2 utility key when Page 2 contains a command.
- Optional horizontal page swiping, off by default, which preserves the Enter utility key.
- Immediate two-slot drag-and-drop swapping in the companion app's **Keys** tab.
- Command keyboard stays focused on one-tap use, with no accidental drag mode.
- Fifth utility row with **ABC**, Settings, Backspace, Android keyboard picker, and either Enter or the P1/P2 switch.
- Setup, Keys, and Privacy tabs in the companion app.
- Empty first-run deck with no bundled commands.
- Optional key vibration, off by default.
- Dynamic clearance above Android gesture and navigation controls.
- Status-bar-aware spacing at the top of the companion app.
- Completely offline with no ads, analytics, accounts, tracking, or upgrade prompts.

## Install and set up

1. Download the APK from [Releases](https://github.com/thehoho/TapDeck-Lite/releases/latest).
2. Allow installation from the browser or file manager Android identifies.
3. Open TapDeck Lite and use the **Setup** tab.
4. Tap **Enable keyboard** and enable TapDeck Lite in Android settings.
5. Tap **Choose keyboard** and select TapDeck Lite.
6. Open **Keys**, choose Page 1 or Page 2, and configure any of the 40 keys.
7. Open Discord, focus its composer, and tap a configured key.

## Compatibility

**Insert + send** is optimized for Discord and applications where raw Enter sends the message. Some applications treat Enter as a new line. In those apps, use **Insert only** and press the app's Send button.

TapDeck Lite is an independent application. It is not affiliated with, endorsed by, sponsored by, or an official product of Discord, OwO, WhatsApp, Meta, Google, or Apple.

## Build from source

Requirements: JDK 17 and Android SDK 36.

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintRelease assembleRelease
```

macOS/Linux:

```bash
chmod +x gradlew
./gradlew testDebugUnitTest lintRelease assembleRelease
```


## Support

- [Report a bug](https://github.com/thehoho/TapDeck-Lite/issues)
- [Report a vulnerability privately](https://github.com/thehoho/TapDeck-Lite/security/advisories/new)
- [Read the hosted privacy policy](https://thehoho.github.io/TapDeck-Lite/privacy)

## License

The source is public for inspection and security review. Copyright is retained; see [LICENSE](LICENSE). No permission to copy, modify, redistribute, sell, or rebrand the app is granted without written approval.
