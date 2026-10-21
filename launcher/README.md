# dumbtv home — Grid edition

A home screen for Fire TV and Android TV that does one job: show your apps and
your HDMI inputs. This fork displays apps in a multi-row grid instead of a
single horizontal row.

The published 1.8 Grid edition (`1.8-grid.10`, Android 9+) adds a per-card selection
ring with a gentle fade (160 ms), animated focus-driven scrolling (240 ms), German launcher menus and
vector-drawn brush/settings buttons. The redundant Apps heading is removed, and
the fixed clock/date header fades softly into the grid. Focus and app launching
remain immediate, including repeated remote presses. No extra libraries or network
permissions are added. See the [change history](CHANGELOG.md) for the complete
feature and verification summary, and the [German quick guide](../README.md#kurzbedienung)
for on-TV menu paths.
Left/right follows the full app order across row boundaries; the first and last
apps are stops rather than wrapping around the entire grid. Header icons can grow
on focus without their highlight being clipped by the header's padding.
The optional **dumbtv Fernbedienung** accessibility service maps Settings/Notification
to system settings and TV/Guide to the app chosen in **Gestaltung › TV-Taste: App
auswählen**. It must be explicitly enabled by the owner, filters only those shortcut
codes, and never retrieves screen content. Other key events pass through unchanged.
Disable it at any time in system accessibility settings, reachable from Gestaltung.
Move mode draws four direction arrows for apps (two for the horizontal inputs row).
Google Ambient Mode's online gallery is not an accessible wallpaper folder; no
network permission or online-image downloader is added to this launcher.
The last focused app/input is restored on return and after recreating the home
activity; its small private state is saved on pause, not on every remote press.
Back on the home screen still explicitly returns to the first tile. Removed or
hidden apps fall back safely to the first available tile.

**Gestaltung › Bilderordner auswählen** opens a read-only TV-friendly folder browser
for internal/USB storage, without requiring DocumentsUI or another app. It requests
`READ_EXTERNAL_STORAGE` with owner consent. Select a folder containing JPG/JPEG,
PNG or WebP files directly (subfolders are not scanned). Selection enables image
backgrounds; the existing dim and rotation settings still apply. **Standardordner**
restores the app-owned wallpapers folder. An absent/unplugged folder falls back to
the plain background; no image files are copied or modified by the browser.

**Gestaltung › Sprache** offers only system language, German and English. The system
option uses German on German-language devices and English as the fallback otherwise.
The preference is launcher-local: menus, dialogs and the date update on return;
Android's system language and app names are not changed. Translations are offline
and add no permissions or background services.

- **About 61 KiB APK.** Plain Java and Android views: no AndroidX, Kotlin, Compose, Play
  Services or other libraries.
- **No network.** It has no `INTERNET` permission, so Android blocks it from
  opening any network connection. The APK declares usage access, reorder-tasks
  and boot-completed with `android:maxSdkVersion="27"`: Fire OS 6 requests them
  in the legacy Keeper implementation described below, not on Fire OS 7/8.
  The published APK requires Android 9+. Check it yourself:
  `aapt2 dump permissions build/dumbtv-home.apk`.
- **Read-only local pictures.** Optional storage access supports the folder browser;
  there is no storage-write or all-files-management permission.
- **No ads, no "recommended" rows, no account, no analytics, no update checks.**

## Install (no Java, no SDK)

You need `adb`, Android 9 or newer, and a TV with ADB debugging on. On Fire TV:
Settings → My Fire TV → About → click the device name 7 times, then Developer
Options → ADB debugging.

```sh
adb connect <tv-ip>:5555         # accept "Allow USB debugging?" on the TV
./launcher/install.sh            # downloads the APK, checks its SHA-256, installs, takes over HOME
```

That is all. The APK comes from this repo's
[releases](https://github.com/projectreturn/dumbtv/releases/latest); the script
refuses to install it if the checksum differs from the one pinned in
`install.sh`. Prefer to install by hand? Download `dumbtv-home.apk` from the
release and `adb install` it — but HOME only switches over after `install.sh`
(or `scripts/launcher.sh`) has installed the HOME hook described below: the
accessibility service and alarm setting on Fire OS 7/8, the UsageStats Keeper on
Fire OS 6.

The current APK does not install on Fire OS 6 / Android 7.1; the Keeper section
below documents the retained legacy implementation, not current APK compatibility.
For Android TV / Google TV, use the device-specific guidance in
[OTHER-TVS.md](../docs/OTHER-TVS.md), not the Fire OS HOME-hook instructions.

Update an existing installation signed by this fork with the new APK; do not
uninstall it or clear its data merely to update. A differently signed custom
build cannot update the release installation.

Undo: `./launcher/install.sh --undo` gives HOME back to Amazon's launcher.

## Why not Projectivy

This repo used to install Projectivy. It was replaced because it is closed source
and asks for far more than a home screen needs: the advertising ID, ad
attribution, Play install referrer, internet, notification access and package
install/delete. Historical measurements of the original pre-Grid launcher on an
Onida Fire TV Edition (Fire OS 7.7, 1.4 GB RAM), **not benchmarks of this fork**:

| | dumbtv home | Projectivy 4.72 |
|---|---|---|
| APK | 45 KB | several MB |
| Permissions requested | 0 (Fire OS 7/8); usage access (Fire OS 6) | 20+, incl. advertising ID |
| RAM, home on screen | 57–72 MB PSS | 135 MB PSS |
| RAM, while an app plays | ~37 MB PSS | — |
| HOME → home screen | 0.16–0.25 s | 3–6 s, Amazon's home shows first |

## Build it yourself

Needs a JDK 17 and the Android SDK (`platforms;android-28`, `build-tools`). No
Gradle: `build.sh` calls aapt2, javac, d8, zipalign and apksigner directly.

```sh
brew install openjdk@17
brew install --cask android-commandlinetools
sdkmanager "platforms;android-28" "build-tools;34.0.0"
./launcher/build.sh              # → launcher/build/dumbtv-home.apk
./launcher/install.sh --build    # build, install, take over HOME
```

On Linux set `JAVA_HOME` and `ANDROID_HOME`; the defaults are Homebrew's paths.

`build.sh` creates `launcher/release.keystore` the first time it runs. Your build
is signed with your key, the release with the project's, and Android will not
update one with the other: `install.sh` says so and tells you to
`adb uninstall tv.dumbtv.home` first (back up `home.conf` before, see below).

## Using it

| Remote | Does |
|---|---|
| OK | open the app, or switch to the input |
| Hold OK, or ☰ Menu | Move, Rename, Hide, App info |
| Move mode in the inputs row | ◀ ▶ moves the tile; OK drops it |
| Move mode in the app grid | ◀ ▶ ▲ ▼ moves the tile |
| Up from the top row | Brush: Appearance / Gestaltung; gear: system Settings |
| Settings/Notification shortcut | system settings (device-dependent key codes) |
| TV/Guide shortcut | chosen app; opens the app chooser if none is assigned |
| Back on the home screen | jump to the first tile |
| Hold HOME | Fire OS quick menu (Amazon's, unchanged) |

**Options** cycles each setting with ◀ ▶ or OK: accent color, tile size, app names
(on focus / always / never), clock format, inputs row, background (black, graphite,
midnight, or an image), image dim and wallpaper rotation, and **Next wallpaper**
steps to the next image. It also lists which apps and inputs appear on
the home screen. Apps without a TV banner, like the Appstore or Downloads, start
hidden; tick them there to add them.

### Local pictures, step by step

1. Copy your own pictures to a folder on internal or attached USB storage, for
   example `Pictures`. No pictures are bundled or downloaded by the launcher.
2. Open the brush → **Choose picture folder** (**Bilderordner auswählen**).
3. Allow read access, choose the storage and folder, then **Select** (**Auswählen**).
   The folder must contain supported pictures directly; subfolders are not scanned.
4. Adjust **Image dim** and **Wallpaper rotation**, or use **Next wallpaper**.

The picker also works on TVs without a usable Android document picker. Storage
permission is broader than a single-folder grant; the launcher uses the selected
folder read-only. Removing a USB drive or losing the folder shows the plain
background instead. **Default folder** returns to the app-owned wallpapers folder.
Google Ambient Mode is separate: its online photo collection is not a shared
local folder and is not imported here. Use landscape pictures near your UI
resolution (e.g. 1920 × 1080) to avoid unnecessarily large source files.

### Language and remote shortcuts

The brush → **Language / Sprache** offers **System language / Systemsprache**,
**Deutsch**, and **English** only. System mode chooses German for German devices,
English otherwise; it does not change the language of Android or installed apps.

Choose a target in **TV button: choose app / TV-Taste: App auswählen**. To receive
shortcut keys outside the launcher, explicitly enable **dumbtv remote control /
dumbtv Fernbedienung** via the remote-service row in Appearance and Android
Accessibility. Disable it in the same place. This service is separate from the
legacy Fire OS HOME hook. It handles only Settings/Notification and TV/Guide key
codes; manufacturer-reserved keys may still not reach it. No screen contents are
retrieved and there is no network access.

## The settings file

Appearance, app order and shortcut preferences are stored in a plain text file:

```
/sdcard/Android/data/tv.dumbtv.home/files/home.conf
```

```
# white blue cyan green amber red pink violet, or #RRGGBB
accent=blue
# small medium large
tiles=medium
# focus always never
labels=focus
# auto 24h 12h
clock=auto
# on off
inputs=on
# black graphite midnight image
background=black
# 0 20 40 60 80, image background only
dim=40
# off visit hourly daily daytime (visit = every return to the home screen)
rotate=off
# system de en (launcher language only)
ui.language=system
# optional absolute path; omit to use the app-owned wallpapers folder
wallpaper.folder=/sdcard/Pictures
# optional package to open with the TV button
remote.tv.app=org.videolan.vlc
apps.order=com.amazon.firetv.youtube,org.videolan.vlc
apps.hide=org.videolan.vlc
apps.show=com.amazon.venezia
inputs.hide=com.droidlogic.tvinput/.services.AV1InputService/HW1
name.com.droidlogic.tvinput/.services.Hdmi2InputService/HW6=PS5
```

Comments go on their own line; anything after `=` is the value.

The last focused tile is stored separately in Android's private preferences when
the launcher pauses. It is restored on return or activity recreation; unavailable
apps fall back to the first tile. Copying `home.conf` does not copy this state.

Back it up with `adb pull`, restore it with `adb push`. Hand edits apply the next
time the home screen comes back. For the default app-owned folder, omit
`wallpaper.folder` (or use **Default folder** in the picker). Then push
`wallpaper.jpg` (or `.png`)
into the same folder and set `background=image`. For several, push them into a
`wallpapers/` subfolder instead (`.jpg`, `.png` or `.webp`, shown in name order) and
pick a rotation; while that folder has images, `wallpaper.jpg` is ignored.

```sh
adb shell mkdir -p /sdcard/Android/data/tv.dumbtv.home/files/wallpapers
adb push *.jpg /sdcard/Android/data/tv.dumbtv.home/files/wallpapers/
```

`rotate=daytime` matches wallpapers to the time of day: name a file with `morning` or `day`
(06:00–13:00), `afternoon` or `evening` (13:00–20:00), or `night` (the rest), for example
`morning-coast.jpg` or `night-city.jpg`. Several files for one period take turns on each
return home. With no file named that way, the period simply picks one image of the folder.

## How HOME works on Fire OS

Fire OS 7 has no working "default launcher" setting, and Amazon's launcher is a
protected package. Three things combine to make HOME land here, measured on this TV:

1. **The HOME key can't be intercepted.** Fire OS handles it before any accessibility
   key filter runs (D-pad keys reached the filter; HOME never did). So `Guard`, a
   small accessibility service, watches for Amazon's home window instead. It reads
   no screen content and sees only window changes.
2. **Android delays app launches for 5 s after a HOME press** (`APP_SWITCH_DELAY_TIME`).
   This is what makes other launchers flash Amazon's home for 3–6 s. A launch fired
   by AlarmManager comes from `system_server`, which is exempt from that delay.
   AlarmManager normally refuses to fire sooner than 5 s ahead, so `install.sh` sets
   `settings global alarm_manager_constants min_futurity=0`. `--undo` removes it.
3. **Amazon's home is covered before it draws.** `Guard` keeps an overlay window ready
   (no surface while idle, so it costs nothing during video). It shows the overlay
   the moment Amazon's home appears, and removes it once our window is on screen.

What you see on HOME: the app, then about 0.3 s of black, then this home screen.

### Fire OS 6

Fire OS 6.7 goes further: its `A11yAuthorizer` refuses third-party accessibility
services, so `Guard` is never bound and HOME cannot be taken over that way. The
Fire OS 6 build therefore uses `Keeper`, a small foreground service that needs no
accessibility:

1. `UsageStats` (`appops set tv.dumbtv.home GET_USAGE_STATS allow`, granted by
   `install.sh`) tells it when Amazon's launcher reaches the foreground.
2. `ActivityManager.moveTaskToFront` brings the dumbtv task back. This is not an
   activity launch, so it is not held back by the 5 s app-switch delay: measured
   under 0.25 s with a real remote key.
3. At boot there is no dumbtv task yet, so the first start uses an alarm-fired
   `PendingIntent`; after that, `moveTaskToFront` takes over.

`Options > Open Amazon's home screen` pauses the Keeper, so Amazon's home stays
until another app is opened.

## Files

| File | What it is |
|---|---|
| `src/.../Home.java` | the home screen: clock, Apps grid, Inputs row, tile menu, move mode |
| `src/.../Tile.java` | one tile, drawn in a single `onDraw`: banner or icon, focus ring |
| `src/.../Catalog.java` | which apps and inputs exist, and which are shown in what order |
| `src/.../Config.java` | the `home.conf` reader and writer |
| `src/.../Options.java` | the settings screen |
| `src/.../FolderPicker.java` | read-only internal/USB picture-folder browser |
| `src/.../Language.java` | offline German/English translations and locale selection |
| `src/.../HeaderButton.java` | vector-drawn brush and settings buttons |
| `src/.../RemoteKeys.java` | shared shortcut handling and chosen TV app |
| `src/.../RemoteService.java` | optional accessibility key filter; no screen retrieval |
| `src/.../Guard.java` | the Fire OS 7/8 HOME takeover described above |
| `src/.../Keeper.java` | the Fire OS 6 HOME takeover (UsageStats + moveTaskToFront) |
| `src/.../BootReceiver.java` | restarts the Keeper after a boot or an update |
| `build.sh` | aapt2 → javac → d8 → zipalign → apksigner |
| `install.sh` | download + checksum (or `--build`), install, HOME hook (Guard or Keeper), `--undo` |
| `version.env` | the only place `VERSION` and `APK_SHA256` live; both scripts read it |
| `release-check.sh` | proves version, built APK and GitHub release agree (`--local`, `--remote`, `--manifest`) |
| `CHANGELOG.md` | Grid edition history, privacy notes and verification scope |

## Releasing

1. Edit `version.env` (bump `VERSION`, `VERSION_CODE`, `VERSION_NAME`).
2. `./build.sh`, then put the new SHA-256 in `version.env`.
3. Upload `build/dumbtv-home.apk` to a release tagged `launcher-v<VERSION>`.
4. `./launcher/release-check.sh` — it fails if the tag, the APK and the hash
   disagree. CI runs `--manifest` on every push.
