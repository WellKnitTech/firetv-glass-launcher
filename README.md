# Fire TV Glass

**Your Fire TV, minus the ads and the tracking.**

Glass is a small, fast home screen for the Fire TV Stick, plus scripts that switch off
Amazon's telemetry, ad and preloaded packages over Wi-Fi. No root, nothing to sideload by
hand, and no new apps beyond the launcher.

[![Stock Fire TV home on the left, Glass on the right](docs/before-after.jpg)](docs/before-after.jpg)

**Everything is reversible.** Packages are disabled with `pm disable-user`, never uninstalled.
`./restore.sh` turns everything back on and gives Home back to Amazon's launcher.

## Quick start

You need a Mac or Linux computer on the same Wi-Fi as your Fire TV. To build the launcher you
also need [Android Studio](https://developer.android.com/studio) (install it and open it once)
or another JDK 17+ with the Android SDK.

### Prerequisites

| | macOS | Linux |
|---|---|---|
| Shell | Terminal.app | any terminal |
| Python 3 | [python.org](https://www.python.org/downloads/) or `brew install python` | `sudo apt install python3 python3-venv` (Debian/Ubuntu) or `sudo dnf install python3` (Fedora) |
| Android Studio / SDK | default SDK: `~/Library/Android/sdk` | default SDK: `~/Android/Sdk` (also checks `/usr/lib/android-sdk`, `/opt/android-sdk`) |
| Java (for `./install.sh`) | bundled with Android Studio, or any JDK 17+ | same; scripts also look for common Studio JBR paths under `/opt`, `/snap`, Flatpak, and Toolbox |

If `ANDROID_HOME` or `ANDROID_SDK_ROOT` is set, that SDK location is used.

### 1. On the TV

1. Go to **Settings > My Fire TV > About** and click your device's name **7 times**.
   You'll see "You are now a developer".
2. Go back to **Settings > My Fire TV > Developer Options** and turn on **ADB debugging**.

### 2. On your computer

1. [Download this project as a ZIP](https://github.com/ivantrj/firetv-glass-launcher/archive/refs/heads/main.zip)
   and unzip it.
2. Open a terminal, `cd` into the unzipped folder. On macOS you can type `cd ` (with a space),
   drag the folder into the Terminal window, and press Enter.
3. Run the setup and follow what it says:

   ```bash
   ./setup.sh
   ```

   It asks for your TV's IP address and tells you where to find it on the TV. When the TV asks
   **Allow USB debugging?**, tick **Always allow** and choose **OK**.

   If setup fails creating `.venv` on Debian/Ubuntu, install `python3-venv` and run it again:
   `sudo apt install python3-venv`.

### 3. Clean up the TV and install Glass

```bash
./debloat.sh
```

Switches off Amazon's ads, tracking and preinstalled extras.

```bash
./install.sh
```

Installs Glass and makes it your home screen. That's it.

If your TV later gets a new IP address, run `./setup.sh` again.

## What's in the repo

| Path | What it is |
|---|---|
| `setup.sh` | One-time setup: installs the TV helper, asks for your TV's IP, checks the connection |
| `debloat.sh` | Disables everything in `debloat/disable-list.txt` (safe to re-run) |
| `restore.sh` | Re-enables one package, or everything, and gives Home back to Amazon |
| `install.sh` | Builds Glass, installs it, makes it the home screen |
| `configure.sh` | Changes dock apps, hidden apps or the wallpaper without rebuilding |
| `debloat/disable-list.txt` | Every package that gets disabled, by category, with notes |
| `launcher/` | Glass source (Java, Android Studio / Gradle project) |
| `tools/tv` | The ADB client all scripts use (see [Why not `adb`](#why-not-adb)) |

## What gets disabled

Full list with comments in [`debloat/disable-list.txt`](debloat/disable-list.txt). Edit it
before running `./debloat.sh` if you want to keep something.

- **Telemetry:** Minerva metrics, device logger, telemetry emitter, wireless metrics,
  perf collection, ACR (content recognition).
- **Content and ads:** recommendations, the sponsored screensaver (`kso.blackbird`),
  ad-ID service, shopping, miniTV, Ambient Experience, sneak-peek previews.
- **Background:** sharing proxy, Kindle connect, phone sign-in companion,
  print and pro-tips stubs.
- **Preloaded:** tutorials, Amazon Kids, game hub, website launcher.
- **Personal picks** (remove the lines if you use them): HBO Max, Live TV, Amazon Music and
  Photos, phone casting (Fling/DIAL), Miracast, the forced updater.

Fire OS refuses to disable 8 "protected" packages (crash/log managers, `device.metrics`,
the usage-stats proxy, ad deeplinking, csapp and the core OTA service). They're marked in the
list. Updates can still arrive, and a Fire OS update may switch things back on. Run
`./debloat.sh` again if that happens.

## The launcher

[![Glass app grid](docs/grid.png)](docs/grid.png)

- **Top bar:** clock, greeting, Inputs, Wi-Fi and Settings.
- **Dock:** your four favourite apps. The big title and the background glow follow the focused app.
- **Grid:** every other app, alphabetical, 4 columns. Press Down from the dock.
- **D-pad:** Up and Down always move exactly one row, and the ends of a row never jump elsewhere.
- **Back** returns to the dock. **Home** while already on Glass does the same.
- **Menu** (☰) on a tile opens that app's system page (force stop, clear cache, uninstall).
- Remembers the last focused tile when you come back from an app.
- Native Java + RecyclerView: about a 4 MB APK and 57 MB of RAM.

### Customise

All of these take effect immediately, no rebuild:

```bash
./configure.sh dock com.netflix.ninja com.stremio.one org.smarttube.beta com.spotify.tv.android
```

```bash
./configure.sh hide org.videolan.vlc com.nordvpn.android
```

```bash
./configure.sh wallpaper ~/Pictures/mountains.jpg   # scaled and dimmed; "none" for black
```

```bash
./configure.sh apps    # list installed package names
```

`dock reset` and `hide reset` go back to the defaults in
`launcher/app/src/main/res/values/config.xml`. Edit that file and run `./install.sh` to change
the built-in defaults.

## Undo

One package:

```bash
./restore.sh com.amazon.tv.livetv
```

Everything, plus Home back to Amazon's launcher and Glass's Home-button service switched off:

```bash
./restore.sh
```

## How it works, and gotchas

### Home on Fire OS 8

Fire OS protects its launcher: `pm disable-user` is refused, and `cmd package set-home-activity`
is accepted but ignored. So Glass includes a small accessibility service ("Glass Home button",
switched on by `install.sh`). Whenever the Amazon launcher comes to the front (Home button,
boot, an app exiting), it brings Glass straight back. It only listens to window events from
the Amazon launcher's package; there's no polling.

Fire OS 8 side effects:

- The Amazon home screen flashes for about 0.4 s before Glass appears.
- Fire OS doesn't pass remote key presses to accessibility services, so Home can't be caught earlier.
- You can't reach the Amazon home screen while it's on. Turn it off with `./restore.sh`, or in
  **Settings > Accessibility**.

### Home on Fire OS 6 (Android 7.1 / API 25)

Some Fire OS 6 builds block third-party accessibility services and ignore the home-activity
setting. On API 25, Glass instead watches Android's Usage Stats foreground events and opens
itself when Amazon Home becomes foreground. `install.sh` grants the `GET_USAGE_STATS` app-op;
this lets Glass read app-usage events, though it only checks the foreground package locally.
Amazon Home may appear briefly, and Fire OS can delay the redirect for several seconds. The
watcher starts when Glass opens and after boot. `restore.sh` stops it and resets the app-op.

Verify the redirect on the connected TV with:

```bash
./tools/test-home-redirect.sh
```

The check waits up to eight seconds for older Fire OS builds to hand off Home.

### Why not `adb`

On macOS the stock `adb` binary (and Xcode's Python) can get "No route to host" because of
Local Network privacy, even with port 5555 open. `tools/tv` uses the `adb-shell` Python package
instead (works the same on Linux), and pushes files as base64 chunks through the shell with an
md5 check. If plain `adb` works for you on either OS, the scripts' commands work the same way
with `adb shell`.

```bash
tools/tv shell 'pm list packages -d'
```

```bash
tools/tv key DPAD_DOWN DPAD_CENTER
```

```bash
tools/tv screencap shot.png
```

### One `pm` command per call

Chaining `pm` commands in one shell call (`pm disable-user a; pm disable-user b`) can silently
stop after the first, so every script runs them one at a time.

## Tested on

| Device | Fire OS | Chipset |
|---|---|---|
| Fire TV Stick 4K (AFTKRT) | Fire OS 8 (Android 11, API 30) | MediaTek |

Other Fire OS 8 sticks should work. If you try one, open an issue or PR with what happened.

Host scripts are written for **macOS and Linux** (POSIX `sh`).

## Related

Built in the spirit of [CleanYourTV](https://github.com/Gathin23/cleanyourtv), which does the
same for Android TV and Google TV sets.

## License

MIT, see [LICENSE](LICENSE). The **Outfit** font is © The Outfit Project Authors, SIL Open
Font License 1.1 ([`OFL.txt`](launcher/app/src/main/assets/fonts/OFL.txt)).
