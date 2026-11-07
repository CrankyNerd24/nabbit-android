# Google Play release notes

What to fill in on the Play Console for Nabbit for Android.

## Before the first upload

- [ ] Play Console developer account ($25 one-time)
- [ ] New personal accounts: a **closed test with at least 12 testers for 14 days in a row** before production
- [ ] App created with package `com.mynabbit.nabbit` (permanent)
- [ ] Upload key and Play App Signing set up; release builds signed (not set up in the repo yet)

## Tip jar (in-app products)

Three **one-time, consumable** products. Nothing unlocks; they show in Settings >
Keep Nabbit going, which hides if Play returns none (so they only appear in builds
installed from Play, including internal testing).

| Product ID | Name | Price |
|---|---|---|
| `tip_small` | Small tip | $1.99 |
| `tip_medium` | Medium tip | $4.99 |
| `tip_large` | Big tip | $9.99 |

- [ ] Payments profile set up (Console → Settings → Payments profile)
- [ ] The three products created and active (Monetize → In-app products)
- [ ] Testers added as license testers (Settings → License testing) to buy without being charged

## Permission declarations

- **Exact alarms** (`SCHEDULE_EXACT_ALARM`): Nabbit's core feature is reminders that
  ring at the minute they're due, with an alarm-style first alert. The person can turn
  it off in Android's "Alarms & reminders"; Nabbit then rings a few minutes late.
- **Full-screen intent** (`USE_FULL_SCREEN_INTENT`): the first alert is an alarm that
  fills the screen, even on the lock screen, with Done / 15 min / Tonight.

## Data safety

- No data collected or shared. No account, no analytics, no ads. Reminders stay on the
  phone. Purchases (tips) are handled by Google Play.
- Microphone: used only on the phone, while the mic button is listening, to turn
  speech into a reminder. Nothing is recorded or sent anywhere by Nabbit.

## Content rating

Plain and Sassy have no swearing. Salty and Sweary aren't offered on Android yet; when
they are (behind Play's Age Signals), answer the questionnaire for the profanity they
can show.
