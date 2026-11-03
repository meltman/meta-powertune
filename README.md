# meta-powertune

`meta-powertune` is the product Yocto layer for PowerTune appliance
integration on the CompuLab IOT-GATE-IMX8PLUS / NXP i.MX8MP. It owns image
composition and target integration around the PowerTuneDash application;
application UI logic remains in the separate PowerTuneDash Git repository.

The layer is consumed directly from the local Yocto `sources` tree. Local
layer edits can therefore be built before commit. Inspect the layer worktree
and stage only task-related files: this repository also contains unrelated
Falcon/U-Boot experimental work.

## Image and application

`recipes-core/images/powertune-image.bb` composes the product image on top of
the CompuLab/NXP image and includes the PowerTune Qt 5 runtime package group.
The package group brings in PowerTuneDash, the modem provisioning package,
Qt/QML runtime modules, fontconfig support, Liberation fonts, and logrotate.
The Qt runtime includes `qtbase-plugins`, which provides Qt image-format
plugins. GIF support is enabled through the Qtbase recipe's `gif`
`PACKAGECONFIG` feature. Device 0 validation confirms
`/usr/lib/plugins/imageformats/libqgif.so` is present and external animated
State GIF resources render and animate. External PNG loading is also
validated.

`recipes-powertune/powertunedash/powertunedash.bb` builds the application
using `qmake5`, installs `/usr/bin/powertunedash`, installs the service and
EGLFS KMS configuration, and installs the application logrotate rule. The
application source is fetched by BitBake from the local PowerTuneDash Git
repository. **Commit intended PowerTuneDash changes before building**: the
local Git fetch does not include uncommitted source changes.

The target Qt GIF decoder is Qtbase's GIF image-format plugin,
`/usr/lib/plugins/imageformats/libqgif.so`, built by enabling
`PACKAGECONFIG:append = " gif"` in
`recipes-qt/qt5/qtbase_git.bbappend`. It is packaged as part of
`qtbase-plugins`, already an explicit dependency of the PowerTune runtime
package group. Device 0 has validated external animated State GIF rendering
and animation; no Qt development packages or unrelated image-format plugins
are required for this support.

## NetworkManager

The PowerTune Qt runtime package group explicitly installs
`networkmanager-daemon` and `networkmanager-wifi`. Device 0 reports
NetworkManager 1.46.0 active, system D-Bus available, QtDBus 5.15.13
installed, and successful NetworkManager D-Bus introspection. The target has
Wi-Fi enabled; its discovered devices include Wi-Fi `wlan0`, Wi-Fi P2P
`p2p-dev-wlan0`, Ethernet `eth0`/`eth1`, GSM, and unmanaged CAN devices.
These are Device 0 observations, not interface names PowerTune may assume.

PowerTune's new Network settings implementation uses QtDBus directly for
NetworkManager discovery and connection requests; NetworkManager owns
connection-profile and credential persistence. The source change still
requires build and Device 0 validation of scans, credentials, connection
state, disconnection, and address reporting. The password field uses the
existing Qt virtual keyboard and masks typed input. CAN configuration remains
outside this networking implementation.

## Development regression resource fixtures

The PowerTuneDash recipe packages the regression fixture sources from
`recipes-powertune/powertunedash/files/test-assets/` under the immutable
`/usr/share/powertune/test-assets/` seed location. On target installation,
each fixture is copied only when its matching filename is absent from
`/var/lib/powertune/Logo/`; the recipe does not overwrite existing user files
or follow/replace an existing symlink. A persistent marker prevents later
package upgrades from reseeding files that users subsequently delete. Seeded
files are `root:gauges` mode `0664`; newly created PowerTune data and Logo
directories are `root:gauges` mode `2775`.

The Logo directory remains mutable application/user data, not an immutable
resource directory. Users may replace, delete, or add files there, and
PowerTune's existing resource picker continues to use that directory. The
fixtures are currently included in the PowerTune development image for
validation convenience; reconsider or remove them from a future production
image.

| Fixture | Regression use |
| --- | --- |
| `black-cat.png` | Static external PNG and Picture/Image resource loading |
| `state-off.png`, `state-on.png` | State Image creation, configuration, and OFF/ON switching |
| `state-off.gif`, `state-on.gif` | State GIF creation, configuration, switching, and animated GIF loading |
| `cars.gif` | Additional/general animated GIF decoder validation |

Together these assets support Save/reload, portable Export/Import, persistent
resource URL handling, and external image/GIF loading checks. Device 0 has
validated external PNG loading and animated external GIF rendering; the seeded
fixture set should still be exercised through the dashboard regression flows.

## Qt fonts and display

`recipes-qt/qt5/qtbase_git.bbappend` enables Qt's fontconfig feature.
Liberation fonts provide the current system fallback. PowerTune requests
Eurostile extensively and UserDash requests Lato, but neither family is
currently supplied by the product image; typography assets and licensing
remain pending.

The packaged `recipes-powertune/powertunedash/files/eglfs.json` config selects
`/dev/dri/card1` and sets `"hwcursor": false`. This is the validated Device 0
EGLFS/KMS selection; do not change it without target validation.

## Qt Multimedia and warning audio

Device 0 provides HDMI playback through ALSA card 0, device 0
(`i.MX HDMI i2s-hifi-0`, `/dev/snd/pcmC0D0p`). The target-specific
`recipes-qt/qt5/qtmultimedia_git.bbappend` disables Qt Multimedia's
`pulseaudio` `PACKAGECONFIG` feature and explicitly retains `alsa`. This
builds `QSoundEffect` with Qt's generic `QAudioOutput` path backed by the
packaged ALSA plugin. Device 0 raw playback on `hw:0,0` failed with default
S16_LE parameters; `plughw:0,0` playback succeeded and was audible. Leave
device selection and format negotiation/conversion to Qt/ALSA; PowerTune
must not hard-code either endpoint. PowerTune keeps the RPM,
coolant-temperature, knock, and boost warning sounds; a cached Qt
default-output and preferred-format check leaves them uninstantiated when no
usable default output is available. The cached check logs one `qInfo()`
startup diagnostic to the application log and journald: either that warning
audio is unavailable for the session, or the selected Qt output name and
preferred format details.
A PulseAudio server is not required for target warning sounds, and this
layer does not add or start PulseAudio or PipeWire for that purpose.

The Device 0 ALSA device is validated, but warning playback by the rebuilt
application still requires target runtime verification. Qt Multimedia and
the optional GStreamer media player/camera remain packaged; the optional
media player's backend behavior is outside this warning-audio configuration
change and is not established here.

## systemd service and identity

`powertunedash.service` starts:

```text
/usr/bin/powertunedash -platform eglfs
```

It currently runs as `root` with group `gauges`, keeps standard output and
error in journald, and asks systemd to create `LogsDirectory=powertune` with
mode `2775`. systemd also creates a private `/run/powertune` directory with
mode `0700` and sets `XDG_RUNTIME_DIR` to that path, so Qt does not fall back
to `/tmp/runtime-root`. systemd assigns the runtime directory to the configured
service user; this remains straightforward when the service later moves to
`User=gauges`. Moving to `User=gauges` is future work. The app recipe creates
the system `gauges` account, configures its home as `/var/lib/powertune`, and
adds `dialout` and `video` supplementary groups.

## Modem provisioning

The `powertune-modem-provision` package owns Quectel-specific provisioning
outside PowerTuneDash. Its oneshot service runs before ModemManager and
applies profile-driven persistent modem settings, including the EC25 GNSS
`autogps` configuration. Device 0 uses `/dev/ttyUSB1` for generic NMEA data;
the modem's AT interface is discovered independently and must not be
hard-coded to a tty number.

## Logs and rotation

PowerTune's application log is
`/var/log/powertune/powertunedash.log`; raw NMEA logs are
`/var/log/powertune/YYYYMMDD.nmea`. The Qt application logger supplements
journald and does not replace systemd lifecycle logging.

The image includes `logrotate`. The `powertunedash` rule rotates only the
application log at 1 MiB, keeps three rotations, compresses old logs, and
uses `missingok`, `notifempty`, `copytruncate`, and `su root gauges`. It does
not govern NMEA retention.

## Compositor and containers

Weston remains available in the image, but its service auto-enable is disabled
for the direct EGLFS product UI. The dashboard service conflicts with
`weston.service` and `weston.socket`. Docker and containerd service
auto-enable are also disabled. Do not remove packages or alter this policy
without confirming current product requirements.

## U-Boot environment and recovery

`recipes-bsp/u-boot/files/fw_env.config` configures the validated Linux U-Boot
environment geometry:

```text
/dev/mmcblk2boot0 0x3F0000 0x4000
```

The image includes `libubootenv-bin`, `u-boot-compulab-env`, `cl-deploy`, and
`cl-uboot`, preserving Linux-side environment inspection and CompuLab
deployment/recovery tools. `u-boot-compulab-env` is the sole package owner of
`/etc/fw_env.config`; the PowerTune layer supplies the validated
`/dev/mmcblk2boot0 0x3F0000 0x4000` contents through that package.

The fast-eMMC boot patch and Falcon DDR/SPL/image experiments remain parked
and are not referenced by the validated appliance U-Boot configuration.
Device 0's maintenance environment retains `bootdelay=2` and
`bootlist=usb_ul sd_ul emmc_ul`. Keep the experimental patch files and
Falcon-specific recipes separate from the appliance commit.

Keep the environment inspection and recovery capabilities available when
changing bootloader packaging or image composition.

## Build and validation workflow

PowerTuneDash source is consumed from Git and must be committed before the
BitBake build can include it. In contrast, `meta-powertune` is a directly
consumed local layer, so local recipe changes can be built before commit.

USB live boot is the normal image-validation workflow. eMMC flashing is an
intentional milestone deployment. Validate target hardware behavior on
Device 0; a successful build alone does not establish display, modem, boot,
or recovery behavior.

This layer contains separate Falcon and U-Boot experimental files/branches.
They are not automatically part of the production baseline. Do not blindly
stage experimental patches or unrelated BSP work while making application or
image changes.
