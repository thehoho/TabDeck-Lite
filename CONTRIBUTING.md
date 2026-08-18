# Contributing

Bug reports and focused pull requests are welcome.

## Before opening a pull request

1. Create a branch from `main`.
2. Keep changes focused on TapDeck Lite's keyboard, setup, privacy, or compatibility.
3. Add or update tests for behavioral changes.
4. Run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

5. Explain user impact and phone-testing performed in the pull request.

Network access, analytics, advertising, accounts, tracking, clipboard reads, Accessibility Service use, or new runtime permissions are outside TapDeck Lite's privacy boundary and should not be introduced without prior public discussion.
