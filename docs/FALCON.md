# PowerTune Falcon Boot - GOLDEN Device1

Last hardware validation: 2026-11-17

This document is the source of truth for the currently validated Falcon boot
implementation on the CompuLab IOT-GATE-IMX8PLUS / i.MX8MP platform.
The Stealth Falcon identities below describe the current Device1 GOLDEN
baseline. Earlier validated artifacts and installation evidence are explicitly
marked historical and are not interchangeable with it.

Do not reconstruct the Falcon installation procedure from memory or from old
chat history. Use the artifacts, hashes, and procedure recorded here.

## Validated Hardware

The final Falcon installation and boot path documented below was validated on:

- Device1
- CompuLab IOT-GATE-IMX8PLUS
- i.MX8MP
- 4 GiB Kingston DDR4-3200
- eMMC boot device: Linux `/dev/mmcblk2`
- eMMC boot0: Linux `/dev/mmcblk2boot0`

Device1 is the new GOLDEN boot/Linux/PowerTune baseline and has an AX210 but
currently no cellular modem. Device0 remains the golden-old reference appliance
and cellular/GPS runtime oracle with a Quectel EC25-A; do not casually disturb
it to repeat already-proven modem observations.

## GOLDEN Device1 acceptance - 2026-11-17

Cold-power acceptance passed Stealth Falcon, Linux
`6.6.52-1.0-g72608893689e`, automatic PowerTune startup with working gauges,
cold-boot CAN configuration, deferred AX210 firmware/PNVM loading, USB host,
hub and HID, AX210 Bluetooth USB enumeration, and zero failed systemd units.
The native split-module policy is part of this baseline.

Device1 observed USB2514 `0424:2514`, Dell Universal Receiver `413c:301c`,
and AX210 Bluetooth `8087:0032`; physical keyboard operation was validated
during Falcon USB work. `canC` was present/down, `canE` present/up, `eth0`
present, `eth1` present/up, and `wlan0` present.

## GOLDEN source and artifact identities

### U-Boot

Stealth Falcon U-Boot commit:

    1138825790f4712aeadef93f2b73835e2000e153

The serial banner identifies this build as:

    U-Boot SPL 2023.04-1.0-d4+fslc+g1138825790f+p0
    (Nov 11 2026 - 15:55:04 +0000)

### meta-powertune

`2d5b1ae5dfd6856531a2c84fd4f94265fd628684` pins the reproducible
Stealth Falcon identity. The appliance module policy is committed at
`ad8153a118f66345bae7fe51b9012de96fbe9bf1`; the CAN cold-boot race repair
is at `6704575a7a82050c02555a89efe648a5b104376e`.

Relevant validated history includes:

    3105fd8  u-boot: update Falcon for UUU USB boot
    56b072f  falcon-boot: accept UUU-capable SPL
    ef64720a2f7738418edbea886cc4005bf34dc8aa

The exact current Git history should always be checked before making new
Falcon changes.

### Reproducible Stealth Falcon artifacts

U-Boot commit:

    1138825790f4712aeadef93f2b73835e2000e153

The reproducible committed-source build has these artifact identities:

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| Raw Falcon SPL | 185453 bytes | `a1a5b55f8e378d8670e242fafa8edc417820998374f1cfe1250ae0c4dea016bc` |
| SPL+DDR | 246340 bytes | `33182ed9743f704a1e7822bee1b3dbcbc34b9c1931856f578fc7d3ee740cc64c` |
| boot0 `flash.bin_iot-gate-imx8plus_falcon_d4` | 255072 bytes | `e91cbf902f963eac246639e94cb08af1c4f7e749157f39845cf5b56f0244710e` |

Earlier quiet-candidate hashes were generated before the USB changes were
committed. U-Boot embeds its source Git identity in the version string, so
committing the changes altered the artifact bytes. Two forced rebuilds from
clean commit `1138825790f4712aeadef93f2b73835e2000e153`, with the pinned
`SOURCE_DATE_EPOCH=1794412504`, produced identical SPL and SPL+DDR artifacts
byte-for-byte. These committed-source hashes supersede the pre-commit hashes;
the change is not ongoing timestamp nondeterminism.

SPL nodtb is 176576 of 180224 bytes (`0x2c000`), leaving only **3648 bytes** of OCRAM
margin. Further SPL growth requires explicit review.

The Falcon boot recipe pins the raw SPL hash, and the U-Boot recipe pins the
above source commit. The resulting Stealth Falcon boot and appliance passed
Device1 cold-power acceptance on 2026-11-17. Historical hashes below are not
the current artifacts. Hardware reality remains authoritative.

### GOLDEN Linux and FIT

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| Active FAT `Image.itb` | 42221568 bytes | `b6aa45627d4098fb4d952aafe025f8432e4410cb692b9430fcf4c04204a8e964` |
| Matched build `Image` | not recorded | `376cf35dc0c13c57b826631e370fc8346d0aaecfd1c60f4bfed3affd446825db` |
| Matched build rootfs tar.zst | not recorded | `6187f1636fecd62bc32c3e73573ed2c31105a39c766c3fa2c36360ed2b92ead5` |
| Device1 FAT rollback `Image.itb.pre-g726` | not recorded | `c0d58844f7f9dbf82e1586f2684eeaeac5dc3e8cbc0d82ea97f8075fe5a10963` |

The rollback copy is at `/run/media/boot-mmcblk2p1/Image.itb.pre-g726`;
its presence is an observation, not an automatic rollback mechanism.

### USB root cause and validation

Falcon initially reached Linux without a working downstream USB path: a
logical GPIO VBUS assertion alone did not establish electrically working
VBUS. CompuLab bypassed LDO4 initialization under `CONFIG_SPL_OS_BOOT`.
F11 observed PCA9450 `LDO4CTRL=0x01` and `LOADSW_CTRL=0x85` under Falcon;
normal U-Boot used `LDO4CTRL=0x8a` and the same `LOADSW_CTRL=0x85`.
Production Falcon restores CompuLab's normal `0x8a` 1.8 V LDO4 configuration.

The required sequence is USB clocks, HSIOMIX power-up, HSIOMIX handshake
request, USB1 PHY power-up, HSIO USB clock module enable, USB2 PHY power-up,
and SAI1_MCLK muxing to GPIO4_IO20 for host VBUS assertion. The handshake
request is part of the validated sequence, but its acknowledgement is not
explicitly polled or verified; do not casually change this sequence. F12
validated the combined sequence on
Device1: Linux enumerated xHCI, the USB2514 hub, CH340, Dell HID receiver,
and AX210 Bluetooth; a physical keyboard worked.

Stealth Falcon removes F8-F12 success diagnostics while retaining
concise failure diagnostics. `FALCON_MEM` remains independently maintained
FIT/SPL heap-safety instrumentation. The later cold-power acceptance validates
the committed quiet build, separately from the earlier F12 experiment.

## Historical 2026-11-13 Falcon artifacts

These are retained for provenance and are not the current GOLDEN artifacts.
The historical U-Boot source was `69acc3b3b3c37882c7449294dd57b324e8457cae`.

### Falcon SPL

Validated clean SPL size:

    184493 bytes

SHA-256:

    de2fe2776a3f327d7cbbc8b49a8e18db8d46a71a281cc4f85cef3f28807b7844

### Historical Falcon container

Filename:

    flash.bin_iot-gate-imx8plus_falcon_d4

Size:

    254048 bytes

SHA-256:

    00e7d3839abd3d016cd482d9a9984ff586854ffb832972e036fed60d1b296b62

This was the persistent Falcon bootloader container installed into eMMC boot0
for the 2026-11-13 validation, not the current Stealth Falcon container.

Do not substitute an older diagnostic or experimental `flash.bin` merely
because the filename is the same. Verify the hash.

### Full U-Boot / UUU Bootstrap Artifact

Filename:

    imx-boot-iot-gate-imx8plus-sd.bin-flash_evk

Validated 2026-11-13 SHA-256:

    7ac47fa7a052d1bacb5be61f5bf48814d5219665cb3c9846e8131d66b7a7cf06

This artifact is NOT the persistent Falcon bootloader.

Its purpose in the Falcon installation procedure is to provide a temporary
full U-Boot environment over the i.MX8MP USB/ROM boot path so that UUU can use
Fastboot to install the separate Falcon container.

## Artifact Ownership

Falcon boot uses artifacts stored in two different physical locations.

### eMMC boot0

Contains the Falcon bootloader container:

    flash.bin_iot-gate-imx8plus_falcon_d4

Validated Linux device:

    /dev/mmcblk2boot0

The validated Falcon container starts at byte offset 0 of boot0.

### FAT Boot Partition

The WIC boot partition contains the artifacts consumed by Falcon after SPL
starts, including the Falcon FIT and related boot files.

The WIC packaging of these files does NOT imply that the WIC has installed the
Falcon SPL/container into eMMC boot0.

These ownership domains must remain conceptually separate:

    eMMC boot0
        -> Falcon SPL/container

    FAT boot partition
        -> Image.itb and related Falcon payload artifacts

This distinction is critical when building, flashing, diagnosing, or
reproducing the system.

On Device1 the chain and mounts are:

```text
i.MX8MP ROM
  -> /dev/mmcblk2boot0: Stealth Falcon boot container
  -> /dev/mmcblk2p1: FAT, mounted at /run/media/boot-mmcblk2p1
     -> active /run/media/boot-mmcblk2p1/Image.itb
     -> BL31 / OP-TEE / Linux / control FDT
  -> /dev/mmcblk2p2: ext4, mounted at /
     -> rootfs, modules, firmware, services and PowerTune
```

`/boot/Image.itb` on the ext4 rootfs is **not** the active Falcon FIT.
The image build can stage it there for Wic; changing it on a running device
does not update the FAT copy. Verify the destination before any update.

## Validated UUU Installation Procedure

Stealth Falcon installation was validated using TWO artifacts:

1. Full EVK/full-U-Boot image used only as the ROM/USB bootstrap.
2. Production Falcon container written persistently to eMMC boot0.

Create the following UUU script on the host running UUU:

    uuu_version 1.2.39

    # Full CompuLab/NXP image is used ONLY as the temporary ROM/SDPS bootstrap.
    SDPS: boot -f ./imx-boot-iot-gate-imx8plus-sd.bin-flash_evk

    # Once full U-Boot/Fastboot is alive, target the eMMC.
    FB: ucmd setenv fastboot_dev mmc
    FB: ucmd setenv mmcdev ${emmc_dev}
    FB: ucmd mmc dev ${emmc_dev}

    # Persist the Falcon container to eMMC boot0.
    FB: flash bootloader ./flash.bin_iot-gate-imx8plus_falcon_d4

    # Select boot0 exactly as the built-in/proven UUU eMMC flow does.
    FB: ucmd if env exists emmc_ack; then ; else setenv emmc_ack 0; fi;
    FB: ucmd mmc partconf ${emmc_dev} ${emmc_ack} 1 0

    FB: Done

The validated execution command is:

    sudo uuu -d -v falcon-emmc.uuu

The device must first be placed into the appropriate PROG/ROM USB boot mode.

## Why Two Bootloader Artifacts Are Required

The production Falcon SPL intentionally detects USB/ROM boot and enters the
full U-Boot path rather than attempting the production Falcon Linux path.

During installation:

    i.MX8MP ROM / PROG mode
            |
            v
    SDPS loads full EVK imx-boot image over USB
            |
            v
    Full U-Boot starts
            |
            v
    USB boot detection enters Fastboot
            |
            v
    UUU selects eMMC
            |
            v
    FB: flash bootloader
            |
            v
    Falcon flash.bin written to eMMC boot0
            |
            v
    mmc partconf selects boot0
            |
            v
    Reset
            |
            v
    Normal production Falcon boot

The full U-Boot image is therefore an installation/recovery tool. It is not
the production Falcon bootloader left running from eMMC.

## Historical 2026-11-13 UUU write

During the earlier Device1 installation, UUU downloaded exactly:

    254048 bytes

U-Boot then reported:

    writing to partition 'bootloader'
    Initializing 'bootloader'
    switch to partitions #1, OK
    mmc2(part 1) is current device
    Writing 'bootloader'

    MMC write: dev # 2, block # 0, count 497 ... 497 blocks written: OK
    Writing 'bootloader' DONE!

This proves that the validated installation targeted the eMMC boot partition
and wrote the expected 254048-byte Falcon container.

The temporary full-U-Boot bootstrap also initially attempted MMC 0 before the
UUU commands selected the correct eMMC device, MMC 2. Do not interpret the
initial MMC 0 diagnostic messages as a failed installation if the subsequent
commands explicitly select MMC 2 and the final bootloader write succeeds.

## Historical 2026-11-13 normal boot

After that historical installation and reset, Device1 produced:

    U-Boot SPL 2023.04-1.0-d4+fslc+g69acc3b3b3c+p0
    ...
    Normal Boot
    Trying to boot from MMC2
    FALCON_MEM: start arena=524288 reserved=94208 free_chunks=91424 unreserved=430080
    ...
    NOTICE: BL31: v2.10.0

No full U-Boot prompt/banner appeared after Falcon SPL.

The validated production path is therefore:

    i.MX8MP ROM
        ->
    Falcon SPL
        ->
    MMC2 / FAT boot partition / Falcon FIT
        ->
    BL31
        ->
    Linux

## Historical 2026-11-13 physical boot0 verification

After the successful normal boot, the installed Falcon container was verified
directly from Device1:

    dd if=/dev/mmcblk2boot0 bs=1 count=254048 status=none | sha256sum

Result:

    00e7d3839abd3d016cd482d9a9984ff586854ffb832972e036fed60d1b296b62  -

This matched the historical Falcon artifact, not the Stealth Falcon hash.

As of 2026-11-13, the chain has therefore been validated end to end:

    source
      -> build
      -> deploy artifact
      -> Mac UUU artifact
      -> UUU/Fastboot write
      -> physical eMMC boot0 contents
      -> successful production Falcon boot

## Recovery

The i.MX8MP ROM/UUU path remains the lowest-level recovery mechanism.

A broken Falcon image must not remove the ability to place the device into
PROG mode and bootstrap full U-Boot using:

    imx-boot-iot-gate-imx8plus-sd.bin-flash_evk

This recovery property is intentional and must be preserved by future Falcon
changes.

Normal Linux/update architecture must not replace or depend exclusively on
this mechanism. UUU/ROM is the unbrick/recovery path beneath the higher-level
HIVE update architecture.

## Wi-Fi Finding

An earlier Falcon boot reached Linux with Wi-Fi unavailable.

That failure was ultimately associated with a stale/mismatched rootfs/kernel
module state rather than the Falcon boot mechanism itself.

A subsequent correctly matched image booted through Falcon with Wi-Fi working.

Do not reopen the Falcon implementation solely because Wi-Fi is absent until
kernel, modules, rootfs, firmware, and image provenance have first been
verified.

## Current Status

VALIDATED on Device1:

- Production Falcon U-Boot source state.
- Clean SPL artifact.
- Clean Falcon `flash.bin`.
- Falcon files packaged into the WIC FAT boot partition.
- Two-artifact UUU installation procedure.
- USB/ROM bootstrap into full U-Boot.
- Fastboot installation of Falcon into eMMC boot0.
- eMMC boot0 selection.
- Normal Falcon boot from MMC2.
- Direct SHA-256 verification of physical boot0 contents for the historical
  2026-11-13 container as documented above.
- Linux boot through Falcon.
- Wi-Fi functional with the correctly matched Linux/rootfs state.
- Stealth Falcon cold-power boot and automatic PowerTune startup to gauges.
- CAN startup, AX210 deferred firmware and PNVM loading, USB/HID and Bluetooth
  enumeration, and zero failed systemd units on the accepted cold boot.
- ROM/UUU recovery path retained.

NOT YET PROVEN:

- That Falcon materially improves total power-to-PowerTune time enough to
  justify further Falcon optimization.
- Final Linux/userspace boot optimization.
- Final production update mechanism.
- Eventual A/B rootfs health-check/rollback implementation.

## Next Work

Do not immediately optimize Falcon further.

The next performance work should move upward into the Linux boot path:

1. Establish repeatable power-on timing measurements.
2. Measure Falcon/SPL-to-kernel handoff.
3. Measure kernel initialization.
4. Use systemd-analyze and journal timestamps for userspace.
5. Measure PowerTune service/application startup separately.
6. Identify actual critical-path delays before changing services or kernel
   configuration.
7. Compare the resulting power-to-PowerTune time against the previous
   full-U-Boot baseline.

The objective is measurable vehicle-appliance startup improvement, not merely
making U-Boot benchmarks look impressive.

## Rule for Future Changes

Before treating a replacement artifact involved in Falcon boot as known-good:

1. Identify its source commit.
2. Build it reproducibly.
3. Record its size.
4. Record its SHA-256.
5. Know which physical storage domain owns it.
6. Verify the destination after writing it.
7. Pass cold-power hardware checks and reach usable PowerTune gauges.
8. Audit failures and only then record it as validated in this document.
   Candidate build evidence may be recorded earlier if clearly identified as
   unvalidated.

Hardware reality is authoritative.
