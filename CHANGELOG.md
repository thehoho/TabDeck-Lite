# Changelog

All notable TapDeck Lite changes are recorded here.

## 1.0.12 — 2026-09-02

- Fixed the Page 1/Page 2 selector's internal clipping so the full rounded top edges and touch targets remain visible.

## 1.0.11 — 2026-09-02

- Added clearer vertical spacing between the Keys-page description and the Page 1/Page 2 selector so both page buttons remain fully visible and easy to press.


## 1.0.10 — 2026-09-01

- Expanded the local deck from 20 to 40 keys across two separate 20-key pages.
- Added Page 1 and Page 2 editors with independent saved-key counts.
- When Page 2 is used, the Enter utility key automatically becomes a one-tap P1/P2 switch.
- Added an optional, default-off horizontal swipe setting that keeps Enter visible while switching pages.
- Preserved all existing Page 1 commands when upgrading from earlier 20-key releases.
- Kept flexible 1–5-column layouts and configured-only filtering independent on each page.
## 1.0.9 — 2026-09-01

- Added a saved keyboard-layout control to the companion app's **Keys** tab.
- Users can show all 20 slots or only configured keys and choose from 1 to 5 keys per row.
- Small decks can use large single-row buttons, while longer layouts scroll inside the existing keyboard height.
- Kept the keyboard free of layout-editing gestures and preserved the default compact 5 × 4 layout.

## 1.0.8 — 2026-08-26

- Made saved and cleared keys redraw immediately without reopening or refreshing the app.
- Kept all sequential key edits in the current in-memory deck before persisting them locally.
- Avoided duplicate redraws when local storage reports the configuration already shown on screen.

## 1.0.7 — 2026-08-24

- Moved drag-and-drop reordering from the keyboard into the companion app's **Keys** tab.
- Swapping now exchanges only the two selected slots and redraws immediately on drop.
- Added animated drag and drop-target feedback, plus edge scrolling for long-distance swaps.
- Added status-bar-aware top spacing to the companion app.

## 1.0.6 — 2026-08-24

- Swapped the Settings and Backspace utility keys so an accidental center tap performs harmless backspace instead of opening the companion app.

## 1.0.5 — 2026-08-20

- Added long-press drag-and-drop reordering directly on the keyboard.
- Moving a key shifts the intervening slots while preserving every label, message, and send mode.
- Changed the downloadable artifact to a non-debuggable release APK named `TapDeck-Lite-1.0.5.apk`.
- Preserved the existing GitHub-distribution signing identity for in-place updates.

## 1.0.4 — 2026-08-19

- Added a Setup-tab preference to turn keyboard vibration on or off.
- Made key vibration off by default, including for existing users upgrading from earlier versions.
- Applied the preference to command keys and the full utility row.

## 1.0.3 — 2026-08-19

- Increased the keyboard's bottom safe area from 20 dp to a 36 dp fallback.
- Added navigation-bar inset detection with an additional 10 dp clearance.
- Kept the compact 20-key grid while moving the utility row clear of OEM keyboard controls.

## 1.0.2 — 2026-08-19

- Clarified the app's purpose around “One tap. One command.”
- Expanded the privacy policy to explain command storage and third-party processing.
- Restored public Android verification after the GitHub account billing lock was resolved.

## 1.0.1 — 2026-08-15

- Finalized the compact 5 × 4 grid containing all 20 phrase keys.
- Added the fifth utility row with ABC, Backspace, Settings, keyboard picker, and Enter.
- Added dedicated bottom clearance above Android gesture and navigation controls.
- Preserved one-tap Discord submission with a short composer delay.
- Added Setup, Keys, and Privacy tabs to the companion app.
- Confirmed zero requested Android permissions and disabled Android backup.

## 1.0.0 — 2026-08-14

- First complete TapDeck Lite build.
- Added 20 configurable local phrase keys with Insert only and Insert + send actions.
