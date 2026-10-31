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

`recipes-powertune/powertunedash/powertunedash.bb` builds the application
using `qmake5`, installs `/usr/bin/powertunedash`, installs the service and
EGLFS KMS configuration, and installs the application logrotate rule. The
application source is fetched by BitBake from the local PowerTuneDash Git
repository. **Commit intended PowerTuneDash changes before building**: the
local Git fetch does not include uncommitted source changes.

## Qt fonts and display

`recipes-qt/qt5/qtbase_git.bbappend` enables Qt's fontconfig feature.
Liberation fonts provide the current system fallback. PowerTune requests
Eurostile extensively and UserDash requests Lato, but neither family is
currently supplied by the product image; typography assets and licensing
remain pending.

The packaged `recipes-powertune/powertunedash/files/eglfs.json` config selects
`/dev/dri/card1` and sets `"hwcursor": false`. This is the validated Device 0
EGLFS/KMS selection; do not change it without target validation.

## systemd service and identity

`powertunedash.service` starts:

```text
/usr/bin/powertunedash -platform eglfs
```

It currently runs as `root` with group `gauges`, keeps standard output and
error in journald, and asks systemd to create `LogsDirectory=powertune` with
mode `2775`. Moving to `User=gauges` is future work. The app recipe creates
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
deployment/recovery tools. Keep these capabilities available when changing
bootloader packaging or image composition.

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
