# i.MX8Plus Falcon Boot Investigation


> **HISTORICAL INVESTIGATION - SUPERSEDED**
>
> This document records the investigation that led to the working Falcon
> implementation. Its status statements, source revisions, warnings, and
> proposed next experiments describe intermediate states and MUST NOT be
> treated as the current implementation plan.
>
> Falcon has since been successfully built, installed, and hardware-validated
> on Device1. The authoritative current state, artifact hashes, UUU installation
> procedure, recovery path, and next steps are documented in `FALCON.md`.
>
> Preserve this file as engineering history. Do not resume an experiment from
> this document without first reconciling it against `FALCON.md` and the
> current Git state.

## Status and scope

F1 successfully built `u-boot-compulab` with both the qualified `sd` and
vendor `falcon` configurations selected. Effective metadata confirmed
`UBOOT_CONFIG="sd falcon"` and
`UBOOT_CONFIG:iot-gate-imx8plus="sd falcon"`.

The reported sstate summary was 100% matched (172 wanted/local, 0 mirrors,
0 missed; 839 current; 2,360 tasks attempted, all succeeded). That summary
alone does not establish whether source compilation ran. In this workspace,
however, the current `do_compile` log (`log.do_compile.1810552`, timestamped
2026-11-10 16:16) contains `make` executions for both
`iot-gate-imx8plus_defconfig` and `iot-gate-imx8plus_falcon_defconfig`.
It contains 1,418 cross-compiler compile commands. The matching buildstats
record reports `do_compile` elapsed time of 20.40 seconds. The current compile
task stamp and signature both carry hash
`c9b6344237302bf8eb98a3b49e7c444fcf9fe1df0e246720ff4b4ed88bad7aad`;
the deploy task has current signature
`8919f3f17b2a42d480d4ccdea80bff5b890ef4361e9a042d26361f5c5a35d951`.
Thus these outputs are attributable to the current pinned build and are not
merely old files left in `tmp/deploy`. The compile log independently confirms
that compilation occurred, even though the sstate summary reports full
matching for the cacheable tasks.

No full image build, device deployment, flash, target access, or boot-policy
write was performed. The U-Boot recipe's BitBake deploy task produced the
listed local artifacts. The historical Falcon ATF, imx-mkimage, DDR,
fast-eMMC, and SPL-size patches remain untouched and disabled/unapplied.

## F1 source selection

`recipes-bsp/u-boot/u-boot-compulab_%.bbappend` selects:

- Repository: `https://github.com/meltman/u-boot-compulab`
- Branch: `imx8mp-falcon-4g`
- Revision: `facddf8faa629f07cffb2dae8b92d6f6a96e9583`
- `UBOOT_CONFIG:iot-gate-imx8plus = "sd falcon"`
- Falcon defconfig: `iot-gate-imx8plus_falcon_defconfig`

The branch, `powertune-known-good-baseline` tag, and the U-Boot worktree
`HEAD` resolve to that same revision. The local checkout at
`/home/cjjohnson/compulab-yocker/work/u-boot-compulab` also contains the
`pr-15` comparison reference. The append keeps the existing `fw_env.config`
installation. It does not add any of the historical patches.

## F1 validation attempt

The earlier analysis attempt in a different execution environment could not
run BitBake because its local toolchain was incomplete and its `bblayers.conf`
had a stale `meta-qt5` path. That path was temporarily corrected for the
attempt and restored exactly. This was an environment-specific limitation, not
a blocker in the actual Yocto container. No packages were installed here.

The current worktree has pre-existing unrelated changes and untracked
experimental files. They were not changed as part of this F1 work.

## Boot-chain intent

The qualified normal path, as currently understood, is:

```text
Boot ROM
  -> SPL / DDR initialization
  -> platform ATF/BL31 handoff as packaged for this board
  -> full U-Boot
  -> bsp_bootcmd / bootlist
  -> kernel and DTB
  -> systemd
  -> PowerTune
```

The CompuLab U-Boot recipe links the deployed ATF `bl31.bin` into each
configuration build and depends on ATF deployment. The exact final container
layout was not regenerated during F1.

The Falcon defconfig declares this intended path:

```text
Boot ROM
  -> SPL / DDR initialization
  -> SPL OS boot of Image.itb and iot-gate-imx8plus-falcon.dtb
  -> Linux / systemd
  -> PowerTune
```

Its source settings include `CONFIG_SPL_OS_BOOT=y`,
`CONFIG_SPL_LOAD_FIT=y`, `CONFIG_SYS_MMCSD_RAW_MODE_U_BOOT_SECTOR=0x300`,
`CONFIG_SPL_FS_LOAD_KERNEL_NAME="Image.itb"`,
`CONFIG_SPL_FS_LOAD_ARGS_NAME="iot-gate-imx8plus-falcon.dtb"`, and
`CONFIG_SYS_SPL_ARGS_ADDR=0x43000000`. This indicates that successful
Falcon-mode SPL is intended to boot Linux instead of entering the normal
full-U-Boot command flow. The Falcon configuration compiled, but that does
**not** prove the complete runtime chain: the media/partition containing those
files, ATF/BL31 handoff details, actual device-tree fixups, kernel arguments,
and fallback behavior remain unverified. Do not infer that ATF can be removed.

## DDR finding: do not run Falcon on the 4-GiB victim yet

In the pinned source, `board/compulab/plat/imx8mp/ddr/ddr.h` selects a
different `lpddr4_array` under `CONFIG_SPL_OS_BOOT`. That Falcon array has
only three 2-GiB entries and is not conditioned on `CONFIG_DRAM_D4`. The normal
non-Falcon array, when built with `CONFIG_DRAM_D4`, includes:

```text
id=0xff070010, subind=0x4, size=4096
timing=&ucm_dram_timing_01061010_4G
```

The Falcon SPL discovers the DRAM identity and matches both ID and subindex.
If the 4-GiB module reports the above Kingston identity, it cannot match the
Falcon array. The source then tries the available configurations in sequence;
when they are exhausted, it reports an unsupported configuration and does
not continue booting. This is a source-level blocker for testing Falcon on the
4-GiB Kingston device as-is. It is not a build result and has not been tested
on hardware.

The historical Kingston patch adds this identity to the Falcon array and uses
`ucm_dram_timing_01061010_4G`. That is the **same timing object** selected by
the normal `CONFIG_DRAM_D4` Kingston row, not a different table relative to
normal boot. A separately named
`ucm_dram_timing_ff070010_kingston_4G` object is declared and defined in the
source, but no source reference to it was found. Consequently, the historical
patch does not use that Kingston-named object; its choice matches the existing
normal-path mapping. The available evidence does not include target logs
proving which EEPROM ID/subindex the qualified appliance reported.

The F2 local source edit below adds the missing row using the normal-path
timing object, under `CONFIG_DRAM_D4`. The actual Yocto build compiled the
change, but the Falcon SPL link overflowed; there is still no bootable F2
artifact. Do not enable the historical patch as a substitute. No Device 2
Falcon boot should be attempted before the size issue is resolved and a
reversible recovery path is ready.

## F1 configuration and artifact measurements

Both generated configurations set `CONFIG_SPL_MAX_SIZE=0x26000` (155,648
bytes). The normal and Falcon generated `.config` values and U-Boot outputs
were inspected from the current `2023.04` work directory:

| Measurement | Normal `sd` | `falcon` |
|---|---:|---:|
| Generated config | `iot-gate-imx8plus_defconfig/.config` | `iot-gate-imx8plus_falcon_defconfig/.config` |
| `CONFIG_SPL_OS_BOOT` | unset | `y` |
| `CONFIG_SPL_MAX_SIZE` | `0x26000` | `0x26000` |
| SPL no-DTB image (`spl/u-boot-spl-nodtb.bin`) | 151,672 bytes | 155,232 bytes |
| Headroom under `0x26000` | 3,976 bytes | **416 bytes** |
| SPL ELF (`spl/u-boot-spl`) | 2,197,312 bytes | 2,518,936 bytes |
| SPL linker map | 488,460 bytes | 517,852 bytes |
| U-Boot ELF (`u-boot.elf`) | 1,334,328 bytes | 1,280,672 bytes |
| U-Boot map (`u-boot.map`) | 1,522,125 bytes | 1,469,074 bytes |
| U-Boot binary (`u-boot.bin`) | 1,268,216 bytes | 1,214,560 bytes |
| SPL with appended DTB (`spl/u-boot-spl.bin`) | 160,036 bytes | 164,109 bytes |

The limit comparison uses `u-boot-spl-nodtb.bin`: the linker layout gives the
SPL image a `0x26000` SRAM region, while `u-boot-spl.bin` is formed by
concatenating the DTB after the no-DTB image. Do not treat the larger
concatenated file as an overflow.

The historical `CONFIG_SPL_MAX_SIZE` increase from `0x26000` to `0x2C000` is
**not justified by the current F1 output**: the Falcon no-DTB image fits, with
416 bytes to spare. The margin is narrow and must be remeasured after adding
the missing 4-GiB Kingston configuration; the current Falcon result does not
include that entry or its 4-GiB timing data.

The Falcon linker map and ELF are available. Its linked data includes the
2-GiB timing object selected by all three current Falcon table rows. The
linker map shows approximately 102,068 bytes of `.text`, 20,540 bytes of
`.rodata`, and 26,092 bytes of `.data`; the active 2-GiB timing data is within
`.data`. No overflow exists in this build, so an overflow-contributor
investigation is not applicable yet.

BitBake deployed these artifacts:

| Variant | Deployed U-Boot binary | Deployed SPL image |
|---|---|---|
| Normal | `u-boot-sd-2023.04-r0.bin` (1,268,216 bytes) | `u-boot-spl.bin-sd-2023.04-r0` (160,036 bytes) |
| Falcon | `u-boot-falcon-2023.04-r0.bin` (1,214,560 bytes) | `u-boot-spl.bin-falcon-2023.04-r0` (164,109 bytes) |

These are BitBake deploy-directory outputs only; no artifact was installed to
a device. The default unqualified `u-boot.bin` and `u-boot-spl.bin` symlinks
currently point to the Falcon variants because it is the last selected
configuration. Use the explicitly suffixed names when comparing variants.

## CompuLab PR #15: architectural reference

CompuLab PR #15, [“imx8m-mini:spl:ddr: Move back to internal DDR timing
storage”](https://github.com/compulab-yokneam/u-boot-compulab/pull/15), is
available locally as branch `pr-15` (head `a15ea454c51eee096523d1b0f4eaec961d695d41`)
and remains an unmerged PR. Its stated design stores timing profiles as
compact changes against a shared base rather than carrying full repeated
blocks or loading full blocks from MMC; the PR description claims about 90%
storage reduction. It is an i.MX8M Mini implementation, not evidence that the
same approach works on i.MX8MP. Nothing from the PR was applied or cherry-picked.

The F1 map shows that this Falcon configuration links only the 2-GiB timing
object, referenced by all three current Falcon table rows, so PR #15's
compaction has not been measured as a saving for this current image. However,
the 416-byte margin is small, and a correct 4-GiB table entry may pull in
additional timing data. A narrowly scoped i.MX8MP feasibility experiment is
worthwhile if that addition exceeds the current limit: use the same normal
profile semantics, reconstruct the timing tables in scratch RAM, and compare
the resulting no-DTB SPL and map. Treat the PR's 90% figure as Mini-specific
until measured on i.MX8MP.

## F2: add the Falcon 4-GiB Kingston row

The local U-Boot fork is on `imx8mp-falcon-4g` at the pinned F1 baseline
`facddf8faa629f07cffb2dae8b92d6f6a96e9583`. The only U-Boot source edit is in
`board/compulab/plat/imx8mp/ddr/ddr.h`: inside the existing
`CONFIG_SPL_OS_BOOT` table, a `CONFIG_DRAM_D4`-guarded Kingston row was added
with `id=0xff070010`, `subind=0x4`, `size=4096`, and
`timing=&ucm_dram_timing_01061010_4G` under `CONFIG_SPL_BUILD`. This matches
the normal D4 Kingston row's validated timing object. No other U-Boot code,
recipe patch, configuration selection, or source pin was changed.

The exact source change is:

```diff
 #ifdef CONFIG_SPL_OS_BOOT
 static const struct lpddr4_desc lpddr4_array[] = {
     ...
     { .name = "Samsung", .id = 0x01061010, .subind = 0x2, .size = 2048, .count = 1,
 #ifdef CONFIG_SPL_BUILD
         .timing = &ucm_dram_timing_01061010_2G
 #endif
     },
+#ifdef CONFIG_DRAM_D4
+    { .name = "Kingston", .id = 0xff070010, .subind = 0x4, .size = 4096, .count = 1,
+#ifdef CONFIG_SPL_BUILD
+        .timing = &ucm_dram_timing_01061010_4G
+#endif
+    },
+#endif /* CONFIG_DRAM_D4 */
 };
 #endif /* CONFIG_SPL_OS_BOOT */
 ```

### F2 build and result

The earlier attempt in this execution environment could not enter the actual
Yocto container: it encountered a stale `meta-qt5` layer path, missing host
tools, and an inaccessible Docker socket. That environment-specific failure
is superseded by the successful build in Casey's Yocto environment. The
reported invocation was `bitbake u-boot-compulab`; it used
`UBOOT_CONFIG="sd falcon"` and the same fork source pin as F1. The invocation
was a 100% sstate hit, so BitBake success alone would not prove source
compilation. The supplied Falcon link/map evidence does: it contains the new
Kingston reference and makes `lpddr4_timing_01061010_4G.o` live. I did not
independently inspect those external logs or artifacts in this pass.

The Falcon link fails with `.sram` overflow of **13,952 bytes** against
`CONFIG_SPL_MAX_SIZE=0x26000` (155,648 bytes). This implies an approximately
169,600-byte no-DTB SPL footprint, a 14,368-byte increase over F1's 155,232
bytes. Since the link failed, 169,600 bytes is an implied footprint, not the
size of a successfully linked F2 no-DTB binary.

The supplied F2 map shows the 4-GiB timing object and these newly live symbols:
`ucm_dram_timing_01061010_4G`, `ddr_dram_fsp_msg`, `ddr_phy_pie`,
`ddr_fsp0_2d_cfg`, `ddr_fsp2_cfg`, `ddr_fsp1_cfg`, `ddr_fsp0_cfg`,
`ddr_ddrphy_trained_csr`, `ddr_ddrphy_cfg`, and `ddr_ddrc_cfg`. The F2 source
was therefore incorporated; the artifact is not a successful link or
deployable image. The earlier local invocation's sstate, stamps, and output
files were F1 and are not F2 evidence.

The F1 size decision has now changed: the F2 configuration exceeds
`0x26000`, but the historical `0x2C000` change is not enabled in this
investigation. F3 below measures whether a compact timing representation
could recover enough SRAM with margin before recommending that path.

## F3: i.MX8MP timing-compaction feasibility

### Measured source data

I parsed the original 2-GiB and 4-GiB timing initializer arrays in
`lpddr4_timing_01061010_2G.c` and
`lpddr4_timing_01061010_4G.c`. Each `dram_cfg_param` entry is two 32-bit
values (8 bytes). Ordered comparisons and a PR #15-style positional diff
produced the following results:

| Table | 2G bytes | 4G bytes | Common ordered entries | 4G patch records / bytes |
|---|---:|---:|---:|---:|
| `ddr_ddrc_cfg` | 872 | 880 | 86 | 24 / 288 |
| `ddr_ddrphy_cfg` | 1,640 | 1,640 | 201 | 4 / 48 |
| `ddr_ddrphy_trained_csr` | 5,752 | 5,752 | 719 (byte-identical) | 0 / 0 |
| `ddr_fsp0_cfg` | 280 | 280 | 28 | 7 / 84 |
| `ddr_fsp1_cfg` | 288 | 288 | 34 | 2 / 24 |
| `ddr_fsp2_cfg` | 288 | 288 | 34 | 2 / 24 |
| `ddr_fsp0_2d_cfg` | 296 | 288 | 29 | 8 / 96 |
| `ddr_phy_pie` | 4,648 | 4,648 | 576 | 5 / 60 |
| **Total** | **14,064** | **14,064** | **1,707 / 13,656 bytes** | **52 / 624** |

The 102 unmatched ordered entries across the two profiles occupy 816 bytes
of raw table data. The FSP message and `dram_timing_info` metadata add 120
bytes per profile (64 and 56 bytes respectively), excluding alignment and
linker-specific overhead. FSP rate metadata differs: the 2G profile uses
3000 MT/s for FSP0 and the 4G profile uses 3200 MT/s; the other rates match.

### Reconstruction check and size estimate

The analysis generated positional SET/INSERT/DELETE patch lists from the
source arrays and applied them in both directions. Reconstructed arrays were
serialized as ordered little-endian 32-bit register/value pairs and compared
byte-for-byte with the original initializers. **All eight arrays for both
profiles matched.** This validates the data transformation only; no C runtime
implementation was generated, compiled, or linked.

With the existing 2G arrays as the base, the 4G payload needs 52 patch records
at 12 bytes each: 624 bytes instead of 14,064 bytes of raw 4G arrays. The
best-case storage-only reduction is therefore **13,440 bytes**, leaving the
implied F2 image at least **512 bytes over** the existing 155,648-byte limit,
before patch metadata, runtime code, alignment, or scratch storage. The
two-profile raw arrays total 28,128 bytes; base-plus-patch table data would be
14,688 bytes, including the shared 5,752-byte trained-CSR array.

This is an optimistic storage estimate, not predicted SPL output. PR #15's
safe runtime reconstruction keeps output arrays in scratch memory while the
base remains available. For these i.MX8MP tables, the reconstructed 4G
scratch arrays require up to 8,320 bytes (plus a 64-byte FSP-message array).
That SRAM alone reduces the maximum net saving to about 5,056 bytes, leaving
about 8,896 bytes of the measured overflow; patch-application code, descriptor
metadata, and alignment make the result worse. The comparison therefore shows
that PR #15-style compaction cannot recover the measured 13,952-byte overflow
with comfortable headroom and is not a sufficient standalone alternative to
the `0x2C000` path.

### F3 decision and next experiment

**Measured facts:** F1 fits with 416 bytes spare; F2 links the 4G timing
object and overflows by 13,952 bytes; the exact source-derived 4G patch payload
is 624 bytes; byte-for-byte reconstruction passes.

**Conclusion:** Do not pursue PR #15-style compaction as the fix for this
overflow. It is an architectural reference, not a drop-in i.MX8MP change, and
its best-case data saving is insufficient even before safe reconstruction
overhead.

**Recommended next experiment:** in a separate reviewable change, test the
historical `CONFIG_SPL_MAX_SIZE=0x2C000` limit increase with the same F2 source
and build only U-Boot. Do not enable ATF Falcon or imx-mkimage Falcon
integration in that experiment; first inspect the resulting no-DTB SPL,
linker map, and ELF and confirm the exact margin. This F3 pass changed no
U-Boot source or configuration and ran no build or device operation.

## F1 work directories

The inspected configuration build directories are under:

```text
build-imx8mp/tmp/work/iot_gate_imx8plus-poky-linux/u-boot-compulab/2023.04/build/
```

The Falcon `.config` confirms SPL OS/FIT, MMC/FAT, memory, address, and image
name settings. Its SPL ELF/map and no-DTB image are present and measured above.
Do not flash any resulting artifact during F1 or F2.

## Commit boundary and next steps

The F1 source change is limited to the U-Boot append's source pin and
configuration selection; both variants compiled successfully. F2 adds one
local U-Boot table row only. The disabled historical ATF, imx-mkimage,
Kingston recipe patch, fast-eMMC, and SPL-size artifacts remain disabled and
are not part of F1/F2.

## F2 source change (reference)

The F2 source correction is a single `CONFIG_SPL_OS_BOOT` Falcon-table entry,
guarded by `CONFIG_DRAM_D4`, for:

```text
name=Kingston
id=0xff070010
subind=0x4
size=4096
timing=&ucm_dram_timing_01061010_4G
```

This exactly matches the normal `CONFIG_DRAM_D4` Kingston row's ID, subindex,
size, and timing object. Keep it as a source-only U-Boot change; do not alter
normal boot, EEPROM discovery, or the memory training data. The existing
historical Kingston patch describes this same row, but it remains disabled and
must not be enabled as a recipe patch.

The table row above is the F2 source edit. F3's source-data analysis found
that compact timing storage alone cannot resolve the overflow. The next
authorized investigation is the isolated `0x2C000` U-Boot-only size-limit
experiment described above; do not enable historical ATF or imx-mkimage
Falcon integration, and do not perform runtime/device validation.
