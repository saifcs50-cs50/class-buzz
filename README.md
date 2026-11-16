# Class Buzz

The calendar sync built into my Huawei Watch 3 didn't show my Google Calendar, even with sync turned on, so I wrote
this Android app. It downloads the calendar by itself and buzzes your wrist (or phone) 5 minutes before each class.

The Watch 3 runs HarmonyOS 3, which runs Android 10 apps, and Class Buzz uses only standard Android APIs with no
dependencies besides the Android SDK. This is a personal project and it isn't polished. It is not affiliated with,
endorsed by or sponsored by Huawei or Google; "Huawei", "Watch 3" and "Google Calendar" are trademarks of their owners
and are used here only to say what the app works with.

## Downloading the calendar

The app downloads your calendar's iCal link (`.ics`) over HTTPS every hour, and when you open it if the last
download is over 10 minutes old. A bad, partial or non-calendar download (such as a Wi-Fi login page) never replaces
the saved schedule. After 6 hours without a download it warns you with a notification, once per stale stretch and
only between 07:00 and 21:00.

It looks 7 days ahead and skips all-day events, cancelled events and titles containing "holiday" or "cancel". It
keeps only your classes: events with no batch tag, plus events whose tag matches a batch name you configure (see
[Choose your batches](#choose-your-batches)).

## The buzz

Each class gets one alarm-clock alarm, 5 minutes before it starts, and buzzes once. A class already inside that
window that hasn't buzzed yet buzzes right away. The buzz is an alarm-class vibration plus a high-importance
alarm-category notification with a full-screen countdown ring (minutes left, subject colour, topic, teacher and
times). The main screen shows the next class as the ring, the rest of the day below it, and when the calendar was
last updated.

I started with plain exact alarms. Off the charger the watch held them until the screen woke, and a test buzz came
93 s late. Alarm-clock alarms are the one kind power saving has to deliver on time, and with them the buzz came 0 s
late, through Do Not Disturb.

Alarms are put back after a restart or an app update. Some Huawei firmware never delivers the boot broadcast, so the
app also checks the watch's boot count and re-arms everything the next time any part of it runs (opening the app is
enough).

## Calendar title format

Class Buzz reads event titles written like this (fields separated by `;`):

```
ABC;Chem-Ionic Equilibrium;Batch-Batch A
```

That's teacher initials (2 to 4 capital letters, several separated by `/` or `;`), subject and topic, then the batch
tag. It recognises Chemistry, Physics and Maths (`Chem`/`Phy`/`Math` prefixes, each with its own colour); anything
else shows as a plain class. Titles that don't fit are kept as plain text. `ClassTitleTest` lists the title shapes it
handles.

## Limitations

- Recurring events are not expanded. The parser reads each `VEVENT` once and ignores `RRULE`, so a repeating event
  appears only on its first date. That's fine for calendars where each class is its own event.
- Only Google Calendar iCal links are accepted: `https://calendar.google.com/calendar/ical/...ics`.
- If the calendar has no `X-WR-TIMEZONE`, times without a zone are read as `Asia/Kolkata`.
- There is no settings screen. The calendar link is set over `adb`, and the batch names are a constant in the code.
- Subjects and the title format are hard-coded for one kind of coaching-class calendar.
- I've only tested it on a Huawei Watch 3 and with JVM unit tests. It hasn't been run on other devices or on Wear OS.

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

Set `ALLOWED_BATCHES` in [`Config.kt`](app/src/main/java/com/saif/classbuzz/Config.kt) to the batch names that appear
after `Batch-` in your event titles, then build. The defaults (`Batch A`, `Batch B`) are placeholders. Matching ignores
case and punctuation and checks whether the event's tag contains one of your names.

## Install and set the calendar link

Install the APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Sideloading to a Huawei Watch 3 needs
developer options and `adb` over Wi-Fi turned on; the exact steps depend on the firmware, so check Huawei's
documentation.

Then send your calendar's iCal address to the app:

```bash
adb shell am start -n com.saif.classbuzz/.ui.MainActivity --es calendar_url "<your iCal address ending in .ics>"
```

Treat that address like a password: Google's "secret address in iCal format" lets anyone who has it read the
calendar. Class Buzz keeps it only in its private app storage and logs just the last 12 characters. Never put it in a
commit, an issue or a screenshot.

Useful commands for debug builds:

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

The app has no accounts or analytics, and it only talks to the calendar address you give it.

## Tech

Kotlin, plain Android views (no Compose or third-party libraries), `AlarmManager` alarm-clock alarms, `JobScheduler`
for the download, a small hand-written iCal parser, and JUnit 4 for the tests. The pure logic (title parsing, event
selection, alarm planning, refresh rules, text formatting) has no Android classes, so it runs on the JVM.

## Licence

MIT, see [LICENSE](LICENSE). The Gradle wrapper (`gradlew`, `gradle/wrapper/`) is generated by Gradle and is
Apache-2.0 licensed.
