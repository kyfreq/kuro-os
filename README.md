# Kuro OS

A Y2K home screen (launcher) for Android: a silver head-unit music widget, Aizawa and Mic sprite widgets you can pet, a notepad, a pixel cat, a Start menu and an All Programs drawer.

## Install

1. Open **Releases** (right side of the repo page) and download `KuroOS.apk` from the newest build.
2. Open the file and install it. Android asks once to allow installs from your browser or Files app.
3. Settings → Apps → Choose default apps → Home app → **Kuro OS**.
4. On the home screen, tap the music display and turn on notification access for **Kuro OS**, so it can show and control Spotify.
5. Settings → Apps → Kuro OS → Battery → **Unrestricted**, so Samsung doesn't close it.

To go back to the normal home screen: Settings → Apps → Choose default apps → Home app → One UI Home.

## Updating

Every push to `main` builds a new APK on GitHub and adds it to Releases. Install it over the old one; your sprites, notes and bond levels stay.

## Where things are

- `app/src/main/assets/index.html` is the whole home screen (HTML, CSS, JS). Character lines are in the `LINES` object.
- `app/src/main/java/com/kyfreq/kuroos/Bridge.java` connects the page to the phone: apps, music, battery, vibration.
