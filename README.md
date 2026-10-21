# dumbtv

> **Grid edition:** This fork displays launcher apps in a responsive multi-row
> grid: 5 columns at the default tile size, 6 when small, and 4 when large.
> It has been tested on Android TV 14 at 1080p UI resolution.

## Grid edition — aktueller Stand

Dieser Fork erweitert [humblemuzzu/dumbtv](https://github.com/humblemuzzu/dumbtv)
um einen mehrzeiligen, per Fernbedienung bedienbaren Launcher. Die ursprünglichen
TV-Werkzeuge bleiben enthalten; **für den Launcher allein ist kein Debloating nötig**.

Aktuelle Veröffentlichung: **1.8** (App-Version `1.8-grid.10`, Android 9 oder neuer).
[APK herunterladen](https://github.com/projectreturn/dumbtv/releases/tag/launcher-v1.8)
· [Vollständiges Änderungsprotokoll](launcher/CHANGELOG.md)
· [Launcher-Dokumentation / English guide](launcher/README.md)

- App-Grid mit 4, 5 oder 6 Spalten je nach Kachelgröße.
- Feste Uhrzeit und Datum; das Grid scrollt darunter mit weichem Übergang.
- Geschmeidiges Scrollen und sanft eingeblendeter Auswahlrahmen an jeder Kachel.
- Links/rechts wechselt am Zeilenende zur nächsten bzw. vorherigen Zeile.
- Letzte App-/Eingangsauswahl wird bei Rückkehr wiederhergestellt.
- Zahnrad für Systemeinstellungen, Pinsel für Gestaltung und Launcher-Optionen.
- Verschieben von Apps in alle vier Richtungen; Umbenennen und Ausblenden.
- Einstellbare TV-Taste und optionaler Fernbedienungsdienst für systemweit
  empfangene Zahnrad-/TV-Tastensignale, soweit das Gerät sie weiterleitet.
- Lokale Hintergrundbilder aus internem Speicher oder USB, mit Abdunklung und
  Bildwechsel; keine Online-Bildquelle und kein Internetzugriff.
- Deutsch, Englisch oder Systemsprache, unabhängig von Androids Spracheinstellung.

### Kurzbedienung

Von der obersten App-Reihe nach oben gehen: **Pinsel = Gestaltung**, **Zahnrad =
Systemeinstellungen**. OK startet eine App; langes OK oder die Menütaste öffnet
das Kachelmenü. Zurück auf dem Startbildschirm wählt ausdrücklich die erste Kachel.

**Bilder:** Eigene JPG/JPEG-, PNG- oder WebP-Dateien z. B. nach `Pictures` kopieren.
Dann **Pinsel → Bilderordner auswählen → Interner Speicher → Pictures → Auswählen**.
Den angefragten Lesezugriff erlauben. Bilder müssen direkt im gewählten Ordner
liegen; Unterordner werden nicht durchsucht. **Bild abdunkeln**, **Hintergrundwechsel**
und **Nächstes Hintergrundbild** steuern die Darstellung. Der Launcher verändert
keine Bilddateien. Googles Ambient-Bilder werden nicht automatisch übernommen;
Fotos sind nicht im APK oder Repository enthalten.

**Sprache:** **Pinsel → Sprache → Systemsprache / Deutsch / English**. Systemsprache
verwendet Deutsch bei deutscher Gerätesprache, sonst Englisch. App-Namen und
Androids Systemeinstellungen bleiben unverändert.

**Fernbedienung:** Unter **Pinsel → TV-Taste: App auswählen** die Ziel-App festlegen.
Falls Android die Sondertasten vor dem Launcher abfängt, den optionalen Dienst
**dumbtv Fernbedienung** über den entsprechenden Gestaltungseintrag in den
Bedienungshilfen aktivieren. Er liest keine Bildschirminhalte und lässt sich dort
jederzeit abschalten. Die tatsächlich gelieferten Tastencodes sind geräteabhängig.

**Datenschutz und Leistung:** Rund 61 KiB APK, Java und native Android-Ansichten,
keine zusätzlichen Bibliotheken, Werbung, Konten, Telemetrie oder Update-Abfragen.
Der Bilderordner benötigt optionalen Lesezugriff; keine Schreib- oder
All-Dateien-Berechtigung. Die unten genannten historischen Leistungswerte des
ursprünglichen Launchers sind keine neuen Messungen der Grid edition.

**Update:** APK aus diesem Fork als Update installieren, ohne die bestehende App
zu deinstallieren oder ihre Daten zu löschen. Android akzeptiert Updates nur mit
demselben Signaturschlüssel. Eigene Builds können daher nicht ohne Weiteres die
veröffentlichte APK ersetzen.

## Original project and TV tools

Your TV should do what you paid for: your apps, your HDMI inputs, and nothing
else. No ads from the TV itself, no watch tracking, no update you did not ask
for. dumbtv applies that over ADB from another machine — **no root**, nothing
written to `/system`, every step reversible with one script.

The same idea, told to whichever TV will listen:

| TV | Debloat | Own home screen | On-device DNS filter |
|---|---|---|---|
| **Fire OS** (Fire TV sticks, Fire TV Edition TVs) | yes | yes | yes |
| **Android TV / Google TV** (Sony, TCL, Philips, Hisense, Xiaomi, Shield) | yes | yes, by disabling the stock launcher | yes |
| **Samsung, LG, Roku, VIDAA, Vizio, Panasonic, Philips Saphi** | no shell, no path | no | no — block it at the router, or boot the TV straight into HDMI |

Tested end to end on a Fire TV Edition television running Fire OS 7.7.1.7
(Android 9). Fire OS 6 and 8 differ in small ways; Android TV needs a different
package list and a different launcher trick. Fire OS 6 (Android 7.1, e.g. the 2018
Fire TV Stick 4K) uses its own lists in `data/fireos6/`, picked up from the device,
and the launcher holds HOME there with a small foreground service instead of the
accessibility hook Amazon refuses on 6.7. The ones that cannot be opened, and the
honest reason for each, are in `docs/OTHER-TVS.md` — along with what we are
working on to reach them.

**What it will not do:** replace the operating system, unlock a bootloader, or
survive a factory reset. Every current TV has a signed boot chain and a verified
system partition, so there is no honest way around it. Pretending otherwise is
how people brick their TVs. See `docs/WHY-NOT-REPLACE-OS.md`.

## Add your TV to the catalog

At the end of a session — or any time something surprised you — record it:

```sh
./scripts/contribute.sh
```

It reads the model, OS build and kernel from the TV, then asks only what a robot
cannot know: how developer mode was enabled, where the IP address was shown,
which dialogs blocked automation and what dismissed each one. It writes
`catalog/devices/<id>.yaml`, validates it, and prints the `git` / `gh` commands
for a pull request. No git? `.github/ISSUE_TEMPLATE/device-report.yml` asks the
same questions in a browser.

That file is the part this repo cannot generate: per-model knowledge. Code is
cheap; someone's evening is not.

## It asks what you want to keep

Nothing is decided for you:

```sh
./scripts/configure.sh
```

It goes through the apps this repo would remove (`data/fireos/user-apps.txt`,
with readable names from `data/apps.txt`) and asks, one by one, whether to remove
each. Answer "no" and that app is never touched. It also asks whether you watch
Prime Video — Prime stays installed either way, but if you say yes its video CDN
is left out of the blocklist so playback keeps working — and whether you want the
launcher replaced and the DNS filter installed.

The answers go into `dumbtv.conf` (git-ignored). Re-run it any time to change
your mind; the scripts read that file, so nothing has to be edited by hand.

Ads, telemetry, experiments and Alexa are removed regardless. This step is only
about apps a person might actually watch.

## Reversibility

`scripts/restore.sh` puts back package state, app-ops, network rules and every
setting the pass touched — using the snapshot `preflight.sh` took first, so the
values are the ones this TV actually had, not generic defaults. The snapshot is
taken once: run `preflight.sh` before the first pass, and a later run cannot
overwrite the originals.

Two things are not reversible:

1. **Apps that live in `/data`.** The streaming apps the pass uninstalls (JioHotstar,
   Zee5, JioCinema, SonyLIV, Discovery+, Aaj Tak, Twitch, FastScreen, MX Player)
   have their APKs deleted; they come back from the store, not from a script. The
   keep-list in `configure.sh` exists so you only lose what you agreed to lose.
2. **Anything uninstalled after the fact.** `restore.sh` restores the lists in
   `data/`, so an app removed by hand is still gone.

Everything else — the ~65 disabled packages, the launcher, the DNS filter, the
settings — is undoable, and a factory reset also undoes it (including this repo's
work).

## What it changes

| Layer | Result |
|---|---|
| Packages | ~65 disabled: ACR (screen fingerprinting), ad-ID service, sponsored rows, watch-time analytics, A/B experiment engines, Alexa/mic app, promo screensaver, OTA helpers. Ad-funded streaming apps uninstalled. YouTube and VLC kept. |
| Protected packages | The handful Amazon refuses to disable get their background execution and background data removed instead. |
| Settings | Ad tracking limited, interest-based ads off, app/device usage reporting off, wake word and voice off, promo autoplay off. |
| DNS | personalDNSfilter runs on the TV: Amazon telemetry, Alexa endpoints and the OTA servers answer `127.0.0.1`. YouTube and the app store still resolve. |
| Launcher | [dumbtv home](launcher/README.md), this repo's own launcher, replaces Amazon's home screen — no promo tiles, no sponsored rows. Grid edition: about 61 KiB, open source, no internet permission; optional read access for local pictures. |

Every step is reversible with `scripts/restore.sh`.

## The launcher

`launcher/` is a home screen written for this repo: your apps, your HDMI inputs,
a clock, and local wallpapers. Plain Java with no libraries, and **no internet
permission**. The Grid edition declares read access for its optional picture-folder
browser. Legacy Fire OS 6 Keeper permissions are restricted to SDK 27 and below;
the current release requires Android 9 or newer. The original launcher measured
about 0.2 s to HOME on its Fire OS test device; this is not a Grid edition benchmark.

It replaced Projectivy, which this repo used to install: Projectivy is closed
source and asks for the advertising ID, ad attribution, install referrer,
internet and package install/delete — far more than a home screen needs.

Just the launcher, without the rest of the pass:

```sh
adb connect <tv-ip>:5555
./launcher/install.sh            # downloads the release APK, checks it, installs it
```

No Java or Android SDK needed: the APK comes from the
[GitHub release](https://github.com/projectreturn/dumbtv/releases/latest), and its
SHA-256 is pinned in `launcher/install.sh`. `--build` builds it from source instead.
Details, settings and how the HOME takeover works: `launcher/README.md`.

## Requirements

- A machine with `adb` (macOS, Linux, Windows via WSL/Git Bash).
- The TV on the same network, with **Developer Options → ADB debugging** on
  (Settings → My Fire TV → About → click the device name 7×, then back to
  My Fire TV → Developer Options).
- ~10 minutes. The scripts reboot the TV twice.

## Quickstart

```sh
adb connect <tv-ip>:5555
adb devices                      # must show "device", not "unauthorized"

./scripts/preflight.sh           # read-only: device facts + what would change
./scripts/configure.sh           # asks what to keep, writes dumbtv.conf
./scripts/all.sh                 # the full pass, asks before touching anything
./scripts/verify.sh              # proof: blocked vs still-working domains
```

If anything looks wrong later, `scripts/restore.sh` puts it back.

## Run one piece at a time

| Script | Does | Reboot needed |
|---|---|---|
| `preflight.sh` | Prints device facts, snapshots the current settings, shows the plan. Changes nothing else. | no |
| `discover.sh` | Read-only inventory for a TV with no verified package list: flags likely ads/analytics packages and writes a commented block for `data/<platform>/packages.txt`. | no |
| `configure.sh` | Asks what to keep and writes `dumbtv.conf`. | no |
| `debloat.sh` | Uninstalls/disables packages, applies privacy settings. | yes |
| `lockdown.sh` | App-ops + data-saver rules for protected packages. | no |
| `dnsfilter.sh` | Installs and configures the on-device DNS filter. | yes |
| `youtube.sh` | **Opt-in, never run by `all.sh` unless you answered yes.** Replaces the YouTube app with SmartTube (no ad code) and takes it out of the home screen. Disables the stock app; `--remove` deletes it. | no |
| `launcher.sh` | Installs dumbtv home and takes over the HOME button. | no |
| `verify.sh` | Checks what is blocked and what still works. | no |
| `restore.sh` | Undoes packages, app-ops, network rules and settings. | yes |
| `all.sh` | Runs the above in order. | yes |

Every script takes `--dry-run` to print its commands without running them, and
`-y` to skip confirmation.

## Safety rules the scripts enforce

- A hard-coded **never-disable** list (launcher, settings, TV input/routing,
  device control, Bluetooth pairing, system UI). Disabling those is how a TV ends
  up with a black screen and no working remote.
- Nothing writes to `/system`. No `adb remount`, no `pm uninstall` without
  `--user 0`, no hosts-file editing — Fire OS verifies `/system` with dm-verity
  and modifying it is the documented brick path.
- The DNS filter only runs if the app's VPN consent was pre-granted, and it is
  verified after a reboot before the script reports success.

## Documentation

- `launcher/README.md` — the launcher: install, remote controls, settings file, HOME takeover
- `launcher/CHANGELOG.md` — Grid edition feature history, fixes and verification scope
- `docs/HOW-IT-WORKS.md` — the five mechanisms and why each is needed
- `docs/GOTCHAS.md` — everything that went wrong while building this
- `docs/OTHER-TVS.md` — which TVs can be opened, which cannot, and what is being worked on
- `docs/YOUTUBE.md` — why YouTube ads cannot be blocked at the network, and what to replace the app with
- `docs/WHY-NOT-REPLACE-OS.md` — the bootloader/verity wall, and the root exploit
- `docs/FAQ.md` — factory reset, updates, Prime Video, "is this illegal" (it isn't)
- `catalog/` — per-platform menu paths, prompts and quirks, machine-readable

## For AI agents

`AGENTS.md` is the machine-readable contract: preconditions, the user interview,
exact ordering, guardrails, verification and recovery. Point an agent at this
repository and it can run the whole thing without improvising.

`llms.txt` is the short index for agents that look for one, and
`catalog/devices/` holds the part that isn't automatable — developer-mode menu
path, where the IP is shown, which dialogs block a bot — one YAML per platform or
model. Adding a TV to the catalog is how this gets better for the next person.

There is also a skill file at `skills/firetv-debloat/SKILL.md` for agents that
load skills.

## Credits and licences

Scripts, docs and the launcher: MIT (`LICENSE`). Device catalog: CC BY-SA 4.0
(`catalog/LICENSE`).

Installed but not bundled: [personalDNSfilter](https://github.com/IngoZenz/personaldnsfilter)
(GPL), downloaded from its upstream release at run time. The launcher APK is
built from `launcher/` and published on this repo's releases. The Amazon tracker list
is fetched from [HaGeZi DNS blocklists](https://github.com/hagezi/dns-blocklists)
at run time; nothing is vendored, so the blocklist stays current.

Nothing in this repository is Amazon code.
