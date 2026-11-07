# Nabbit for Android

Reminders that stick, for ADHD brains. Three taps to make one, alerts you can't
miss, and a little win every time you get it done. Always free.

The iPhone version is in the App Store; this is the Android version, in progress.

## Run it in the Android Emulator

1. Install [Android Studio](https://developer.android.com/studio).
2. **File → New → Project from Version Control**, paste
   `https://github.com/CrankyNerd24/nabbit-android`, and clone.
3. Let Gradle sync finish. If Android Studio offers to install a missing SDK
   (Android 17 / API 37), say yes.
4. **Tools → Device Manager → Create virtual device**: pick a Pixel, a system
   image with **Google Play** or **Google APIs** (API 36 or newer), and finish.
5. Pick the device in the toolbar and press **Run** (the green triangle).

### Testing alerts in the emulator

- On first open, turn on everything in **Let Nabbit ring on time**.
- Make a reminder with **Pick day & time** for two minutes from now, then lock
  the emulator (the power button in its side toolbar) and wait.
- To jump ahead, open the emulator's **Settings → System → Date & time**, turn
  off automatic time, and move the clock forward. Nabbit reschedules when the
  clock changes.
- Speech in the emulator uses your Mac's microphone: in the emulator's
  **Extended controls (…) → Microphone**, turn on "Virtual microphone uses
  host audio input".

### Widgets

Press and hold an empty spot on the emulator's home screen, tap **Widgets**,
find **Nabbit**, and drag **Next up** or **Quick add** onto the home screen.
Press and hold a widget, then drag its edges to make it wider for the medium
layout.

## Build from the command line

```
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```
