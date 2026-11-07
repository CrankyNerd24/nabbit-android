# Nabbit for Android — project brief

The Android version of **Nabbit**, the ADHD reminder app ("Remind me in Nabbit to call the dentist"). The iPhone app lives in the private repo `CrankyNerd24/Nabbit`; its `CLAUDE.md` is the source of truth for the product: the capture flow, alerts, nudge copy, celebrations, themes, tones, and copy rules. Match it, adapted to Android. Where this file and that one differ, this one wins for Android.

## Core principles (same as iPhone)

- Every extra step is a reason to quit. Never add a field, screen, or confirmation that isn't essential.
- No "Save" buttons. The last tap saves.
- No guilt. No streaks that can break, no "you forgot", no "late", no "again". Say "slipped".
- Never use the word "nag" anywhere — code comments, UI, or copy. Repeat alerts are called **nudges**.
- Always free: no ads, no subscription, no paywall. The website promises it.

## Tech

- Kotlin, Jetpack Compose (Material 3), single `app` module. AGP 9 with built-in Kotlin, Gradle wrapper, version catalog in `gradle/libs.versions.toml`.
- `minSdk` 31 (Android 12), `targetSdk` 36, `compileSdk` 37 (the newest Compose libraries need it).
- Package and application ID `com.mynabbit.nabbit` (permanent once on Google Play).
- Local-only: reminders and things are one JSON file (`Store`, kotlinx.serialization); settings are SharedPreferences (`Prefs`). No account, no backend, no analytics.
- Alarms: `AlarmManager`. One alarm per open reminder for its next step (the first alert, then nudges 1 to 3); `AlertReceiver` posts it and schedules the next. `setAlarmClock` for the first alert in Alarm style, `setExactAndAllowWhileIdle` for nudges, inexact if exact alarms aren't allowed. `RescheduleReceiver` puts alarms back after a restart, a clock or time zone change, or an update.
- Two notification channels (`Notifier`): **Alarms** (alarm-volume sound so silent mode doesn't stop it, `FLAG_INSISTENT` so it keeps ringing, full-screen intent, category alarm so Do Not Disturb's alarm exception lets it through) and **Nudges** (follows sound, vibrate, and DND). Every notification has Done / 15 min / Tonight (`ActionReceiver`). Each step times out when the next is due; the last stays.
- Permissions Android needs to ring on time: notifications, exact alarms ("Alarms & reminders"), and full-screen alerts (Android 14+). `Permissions` checks them; the alerts intro and Settings > Ring on time show a checklist with buttons.
- Speech: Android's `SpeechRecognizer`, offline preferred, en-US. `VoiceParser` is a port of the iPhone's; `VoiceParserTest` covers it.

## Layout (`app/src/main/java/com/mynabbit/nabbit`)

- `model/`: `Verb` (eight built-ins plus `Verb.custom`, stored as "custom:Word"), `Reminder`, `Thing`, `ReminderAction`, and `Timing.kt` (`NudgeTiming`, `WhenPreset`, `Times` for formatting and "tonight at 7:00 PM").
- `data/`: `Store` (the JSON store, the one save path, `onChange` reschedules alarms), `Prefs` (every setting as a StateFlow), `VerbStore` (added, ordered, and hidden verbs), `Reminders.perform` (Done / 15 min / Tonight from anywhere).
- `copy/Copy.kt`: `Tone` and every tone-dependent line, ported word for word from iPhone. Add new user-facing lines there in every tone. Salty and Sweary aren't offered until the age check (Google Play Age Signals) is built; `Tone.current` reads a saved one as Plain.
- `alerts/`: `Scheduler`, `Notifier`, the receivers, and `AlertCenter` (which reminder the full-screen alert shows; set when an alert fires, a notification is tapped, or the app comes forward with something due).
- `celebration/`: sizing and rotating lines (`Celebration`, `CelebrationLines`), the confetti overlay (a color flash when animations are off), and `Haptics`.
- `voice/`: `SpeechListener`, `VoiceParser`.
- `ui/`: `NabbitRoot` (simple screen stack: Home, Thing, When, Settings; banners, celebrations, and the alert overlay), `HomeScreen`, `CaptureScreens`, `AlertScreen`, `SettingsScreen`, `IntroScreens`, `Components` (`StickyNote`, `NoteGrid`, `TaskAndTime`, `BannerView`), `Theme` (the five themes and WCAG text color).
- Pick day & time (`WhenStep`) matches the watch: a row of days (today while times remain, then two weeks, the next Morning's day picked) and half-hour time notes from 6 AM to 10 PM scrolled to the Morning time; tapping a time saves. Another time… opens a `TimeInput` for an exact minute. No date or clock dialogs: they cost too many taps.
- `MainActivity` shows over the lock screen only while an alert is up, and reschedules plus checks for due reminders on every resume.

## Accessibility

Every press-and-hold menu item is also a TalkBack custom action. Banners are live regions. At large font scales the note grid drops to one column and rows and banners stack. Motion falls back to a color flash when animations are off. A Done button under 3:1 against the alert color gets an outline.

## Version 1 scope

Three taps and the mic, the full-screen alarm with Done / 15 min / Tonight, nudges, Today / Coming up / Wins today, hot keys, Settings (theme, Plain and Sassy tones, default times, alert style, nudge gap, ring-on-time checklist), themes, celebrations, and the two first-open cards.

Next: widgets (Next up, Quick add), the to-do list board, the tip jar and review prompt (Play Billing, Play In-App Review), Salty and Sweary with Age Signals, the bunny icon. Later or never: Wear OS, voice-assistant integration.

## Building and checks

- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` before every push. Lint should stay at 0 errors.
- Test alarms on a real phone when possible; emulators handle them, but battery savers on Samsung, Xiaomi, and others can delay them.

## Changelog

`CHANGELOG.md` follows Semantic Versioning, newest first, with `## [Unreleased]` at the top. Every commit that changes the app adds one bullet there: plain words for someone using the app, no trailing period, wrapped at about 72 characters, a blank line between bullets. Releases bump `versionName` and `versionCode` in `app/build.gradle.kts` in a commit "Bump version to X.Y.Z", then "Release X.Y.Z in the changelog".
