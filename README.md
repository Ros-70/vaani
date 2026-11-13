# Vaani

An offline, voice-first Android keyboard. Dictation (speech-to-text, fully
on-device) is the primary way to type; a full QWERTY keyboard is there as a
fallback for anything dictation doesn't handle well. English, Hindi, and
Hinglish (the two mixed) are all supported. Once it's turned on, Vaani
replaces (or sits alongside) your regular keyboard inside WhatsApp, Gmail,
Notes, and every other app — same as switching keyboards usually works on
Android.

## What it needs, and why

- **Microphone** — only while you're dictating (held down, or tapped
  on/off, depending on a setting). Audio is turned into text on your phone
  and then discarded, unless you turn on "Keep recorded audio" in Settings.
- **Storage — only for the one file you pick** — when you import a speech
  model or a backup file, Android shows you its normal file picker and
  Vaani only ever touches the exact file you chose. It is never given
  access to your phone's storage in general.
- **Internet — never.** Vaani does not have, and cannot be given, internet
  access. It is incapable of sending anything — what you type, what you
  say, anything — anywhere. Speech models are large files you put on your
  phone yourself (see below); Vaani never fetches anything on its own.
- **No account, no sign-in, nothing phoned home.** Nothing is collected.

The in-app "Privacy" screen (Settings → 🔒 What this app can and cannot do)
restates all of this in the app itself, and every line there is something
you can double-check for yourself in Android's own Settings → Apps screen.

## Turning Vaani on

Android doesn't let any app switch itself on as your keyboard automatically
— that's a deliberate Android security rule, the same reason no app can
silently grant itself a permission. So the first time you open Vaani, it
walks you through two short steps:

1. **Turn Vaani on as a keyboard.** The app opens Android's own "Manage
   keyboards" screen for you; just switch the Vaani entry on and go back.
2. **Choose Vaani as the keyboard you're using right now.** The app shows
   you Android's own keyboard-switcher; pick Vaani from the list.

Vaani checks both of these automatically every time you come back to the
app, so you never have to tell it "I did it" — it just knows.

## Importing a speech model

Dictation needs one more thing: a speech model file. Vaani can't download
this itself (see "Internet — never" above), so you put it on your phone the
same way you'd put any file there — copying it over by USB, or saving it
from a file-sharing app or cloud-drive app you already have — and then
import it from inside Vaani:

1. Open Vaani → **Speech models**.
2. Tap **Import…** next to the model you want.
3. Pick the file you downloaded. Vaani copies it into its own private
   storage and checks that it's actually a model file before accepting it.

The QWERTY keyboard works perfectly well with no model imported at all —
only dictation needs one. The **Speech models** screen also has a "Where to
find a model" button that opens your own web browser to a page listing
models to download — that's your browser doing the downloading, not Vaani.

## Building it yourself

```
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/`. You'll need the
Android SDK installed (or let `./gradlew` guide you — see
`local.properties`), plus the Android NDK for the on-device speech engine
(whisper.cpp), which Gradle fetches automatically during the native build.

Run the test suite with:

```
./gradlew test        # unit tests — no device or emulator needed
./gradlew lintDebug    # static analysis
```

## Getting the latest build without building it yourself

Every push runs this repository's GitHub Actions workflow
(`.github/workflows/android.yml`). Once it finishes, open the latest run
under this repo's **Actions** tab and download the `vaani-debug-apk`
artifact — that's a ready-to-install APK you can copy to your phone and
open directly (you may need to allow "install from this source" once,
which Android will prompt you for).

## Project layout

- `app/` — the Android app: the keyboard itself (`ime/`), the companion
  screens (`ui/`), on-device data storage (`data/`), and the speech engine
  (`speech/`, plus the native code in `app/src/main/cpp/`).
- `textpipeline/` — the text clean-up logic (spoken punctuation, your
  vocabulary corrections, auto-capitalisation, spacing) as plain, portable
  Kotlin with no Android dependency at all, so it's easy to test on its own.
- `vaani-keyboard-v1_0.html` — the original interactive mock-up this app is
  built from. Kept in the repo as a design reference; it isn't part of the
  built app.
- `icon/` — the finalized app icon, in every size Android needs.

See `HANDOVER.md` for what's done, what's left, and anything worth knowing
before picking this project back up. `DECISIONS.md` and `BLOCKERS.md` (if
present) record judgment calls made along the way and anything that needed
a person to weigh in.
