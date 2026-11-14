# Class Buzz

An Android app that buzzes your wrist (or phone) a few minutes before each class on a Google Calendar.

It downloads a calendar feed (`.ics`) by itself, picks out your classes, and sets an alarm for 5 minutes before
each one. When the alarm goes off it vibrates, posts a full-screen notification and shows a countdown ring with the
subject, topic, teacher and time. It was built for a Huawei Watch 3 (HarmonyOS 3, which runs Android 10 apps), but it
uses only standard Android APIs and has no dependencies besides the Android SDK.

This is a personal project, not a polished product. It is **not affiliated with, endorsed by or sponsored by Huawei
or Google**; "Huawei", "Watch 3" and "Google Calendar" are trademarks of their owners and are used here only to say
what the app works with.

## What it does

- Downloads your calendar's iCal link over HTTPS every hour, and when you open the app if the last download is more
  than 10 minutes old. A bad, partial or non-calendar download (for example a Wi-Fi login page) never replaces the
  saved schedule.
- Looks 7 days ahead. Skips all-day events, cancelled events, and titles that contain "holiday" or "cancel".
- Keeps only your classes: events with no batch tag, plus events whose batch tag matches one of the batch names you
  configure (see [Choose your batches](#choose-your-batches)).
- Sets one alarm-clock alarm per class, 5 minutes before it starts. A class that is already inside that window and
  has not buzzed yet buzzes right away. Each class buzzes once.
- The buzz is an alarm-class vibration plus a high-importance alarm-category notification with a full-screen
  countdown ring (minutes left, subject colour, topic, teacher and times). On the Watch 3 it came through Do Not Disturb.
- The main screen shows the next class as the ring, the rest of that day below it, and when the calendar was last
  updated.
- Warns with a notification if the calendar has not downloaded for 6 hours, between 07:00 and 21:00, once per stale
  stretch.
- Puts the alarms back after a restart or an app update. Some Huawei firmware never delivers the boot broadcast, so the
  app also notices a new boot the next time anything in it runs (opening the app is enough).

## Calendar title format

Class Buzz reads event titles written like this (fields separated by `;`):

```
ABC;Chem-Ionic Equilibrium;Batch-Batch A
```

teacher initials (2 to 4 capital letters, several separated by `/` or `;`), then subject and topic, then the batch
tag. Subjects it recognises are Chemistry, Physics and Maths (`Chem`/`Phy`/`Math` prefixes, each with its own
colour); anything else is shown as a plain class. Titles that do not fit are kept as plain text, never dropped.
`ClassTitleTest` lists the title shapes it handles.

## Limitations

- **Recurring events are not expanded.** The parser reads each `VEVENT` once and ignores `RRULE`, so a repeating
  event appears only on its first date. It works for calendars where each class is its own event.
- Only Google Calendar iCal links are accepted: `https://calendar.google.com/calendar/ical/...ics`.
- If the calendar has no `X-WR-TIMEZONE`, times without a zone are read as `Asia/Kolkata`.
- There is no settings screen. The calendar link is set over `adb`, and the batch names are a constant in the code.
- Subjects and the title format are hard-coded for one kind of coaching-class calendar.
- Tested only on a Huawei Watch 3 and with JVM unit tests. It has not been run on other devices or Wear OS.

## Requirements

- JDK 17 or newer, and the Android SDK (platform 36). Set `ANDROID_HOME`, or put `sdk.dir=/path/to/sdk` in a
  `local.properties` file (git-ignored).
- An Android device or watch with Android 8.1 (API 27) or newer, and `adb` access.

## Build

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Run the unit tests (JVM only, no device needed) with:

```bash
./gradlew test
```

### Choose your batches

Edit `ALLOWED_BATCHES` in [`Config.kt`](app/src/main/java/com/saif/classbuzz/Config.kt) to the batch names that appear
after `Batch-` in your event titles, then build. The defaults (`Batch A`, `Batch B`) are placeholders. Matching ignores
case and punctuation and checks whether the event's tag contains one of your names.

## Install and set the calendar link

Install the APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Sideloading to a Huawei Watch 3 needs
developer options and `adb` over Wi-Fi turned on, and the exact steps depend on the firmware; check Huawei's documentation.

Then send your calendar's iCal address to the app:

```bash
adb shell am start -n com.saif.classbuzz/.ui.MainActivity --es calendar_url "<your iCal address ending in .ics>"
```

**Treat that address like a password.** Google's "secret address in iCal format" lets anyone who has it read the
calendar. Class Buzz keeps it only in its own private app storage and logs just the last 12 characters. Never put it
in a commit, an issue or a screenshot.

Handy commands (debug builds):

```bash
adb shell am start -n com.saif.classbuzz/.ui.MainActivity --es cmd refresh                  # download now
adb shell am start -n com.saif.classbuzz/.ui.MainActivity --es cmd testbuzz --ei seconds 30 # test buzz in 30 s
adb shell run-as com.saif.classbuzz cat shared_prefs/class_buzz_log.xml                     # read the event log
```

## Permissions

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Download the calendar feed |
| `VIBRATE` | The buzz |
| `USE_FULL_SCREEN_INTENT`, `POST_NOTIFICATIONS` | The full-screen countdown and the stale-calendar warning |
| `RECEIVE_BOOT_COMPLETED` | Put alarms back after a restart or app update |
| `ACCESS_NOTIFICATION_POLICY` | Used to log the Do Not Disturb state when a buzz fires |

The app has no accounts, no analytics and talks only to the calendar address you give it.

## Tech

Kotlin, plain Android views (no Compose, no third-party libraries), `AlarmManager` alarm-clock alarms, `JobScheduler`
for the download, and a small hand-written iCal parser. Unit tests use JUnit 4. The pure logic (title parsing, event
selection, alarm planning, refresh rules, text formatting) is free of Android classes so it runs on the JVM.

## Licence

MIT, see [LICENSE](LICENSE). The Gradle wrapper (`gradlew`, `gradle/wrapper/`) is generated by Gradle and is
Apache-2.0 licensed.
