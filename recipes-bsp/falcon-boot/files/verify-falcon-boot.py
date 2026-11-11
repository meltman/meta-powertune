#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-2.0-only
"""Verify the unsigned i.MX8MP Falcon loader-only container."""

import hashlib
import pathlib
import struct
import sys


LOAD_ADDR = 0x920000

TRAINING = (
    ("2D DMEM", "lpddr4_pmu_train_2d_dmem_202006.bin"),
    ("2D IMEM", "lpddr4_pmu_train_2d_imem_202006.bin"),
    ("1D DMEM", "lpddr4_pmu_train_1d_dmem_202006.bin"),
    ("1D IMEM", "lpddr4_pmu_train_1d_imem_202006.bin"),
)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def parse_bsym(payload):
    magic = payload.find(b"BSYM")
    if magic < 0:
        raise ValueError("Binman BSYM block not found")

    values_start = magic + 8
    needed = values_start + len(TRAINING) * 16
    if needed > len(payload):
        raise ValueError("truncated Binman BSYM block")

    records = []
    for index, (label, filename) in enumerate(TRAINING):
        offset = values_start + index * 16
        size, runtime = struct.unpack_from("<QQ", payload, offset)
        records.append((label, filename, size, runtime))

    return magic, records


def verify_bsym_payload(payload, firmware_dir):
    magic, records = parse_bsym(payload)

    ranges = []

    for label, filename, size, runtime in records:
        firmware = (firmware_dir / filename).read_bytes()

        if not firmware:
            raise ValueError(f"empty firmware image: {filename}")

        if size != len(firmware):
            raise ValueError(
                f"{label} BSYM size mismatch: "
                f"recorded={size} actual={len(firmware)}"
            )

        if runtime < LOAD_ADDR:
            raise ValueError(f"{label} runtime address below loader base")

        payload_offset = runtime - LOAD_ADDR
        payload_end = payload_offset + size

        if payload_end > len(payload):
            raise ValueError(f"{label} extends beyond Binman payload")

        if payload[payload_offset:payload_end] != firmware:
            raise ValueError(
                f"{label} bytes do not match {filename} at "
                f"payload offset 0x{payload_offset:x}"
            )

        ranges.append((label, payload_offset, size, runtime))

    return magic, ranges


def verify(image, falcon, normal, packed, firmware_dir):
    if not falcon or not normal or falcon == normal:
        raise ValueError("SPL input is empty or Falcon is identical to normal")

    if not packed:
        raise ValueError("Falcon Binman SPL+DDR payload is empty")

    if len(image) < 64 or image[:4] != b"\xd1\x00\x20\x41":
        raise ValueError("invalid ROM IVT header")

    entry, reserved1, dcd, boot_ptr, self_addr, csf, reserved2 = (
        struct.unpack_from("<7I", image, 4)
    )
    start, size, plugin, padding = struct.unpack_from("<4I", image, 32)

    if (
        entry != LOAD_ADDR
        or self_addr != entry - 64
        or boot_ptr != self_addr + 32
        or start != self_addr
        or reserved1
        or reserved2
        or dcd
        or plugin
        or padding
        or image[48:64] != bytes(16)
    ):
        raise ValueError("unexpected ROM loader header")

    bsym_offset, firmware_ranges = verify_bsym_payload(
        packed, firmware_dir
    )

    payload_end = 64 + len(packed)
    csf_offset = (payload_end + 511) & ~511

    if image[64:payload_end] != packed:
        raise ValueError("container loader differs from deployed Binman payload")

    if image[payload_end:csf_offset] != bytes(csf_offset - payload_end):
        raise ValueError("non-zero alignment padding before CSF")

    if csf != start + csf_offset:
        raise ValueError("CSF pointer does not match CSF file offset")

    if size != csf_offset + 0x2060:
        raise ValueError("ROM boot-data size does not match CSF extent")

    if len(image) != csf_offset + 0x2060:
        raise ValueError("container length does not match ROM load extent")

    if image[csf_offset:] != bytes(0x2060):
        raise ValueError("CSF/tail extent is not zero-filled")

    if len(image) > 0x3f0000:
        raise ValueError("container overlaps the recorded boot0 environment")

    # The raw SPL is independently hash-locked by the recipe. Binman patches
    # its symbol block, so it is intentionally not byte-identical to the
    # unpatched raw SPL.
    version_marker = b"U-Boot SPL "
    version_start = falcon.find(version_marker)
    if version_start < 0:
        raise ValueError("raw Falcon SPL identity not found")

    version_end = falcon.find(b"\0", version_start)
    if version_end < 0:
        raise ValueError("raw Falcon SPL identity is not terminated")

    raw_version = falcon[version_start:version_end]
    if raw_version not in packed:
        raise ValueError(
            "packed payload does not contain raw Falcon SPL identity"
        )

    print("IVT offset=0x0 entry=0x920000 loader_offset=0x40")
    print(f"raw SPL size={len(falcon)} sha256={sha256(falcon)}")
    print(f"normal SPL sha256={sha256(normal)}")
    print(
        f"Binman SPL+DDR size={len(packed)} "
        f"sha256={sha256(packed)} BSYM=0x{bsym_offset:x}"
    )

    for label, offset, fw_size, runtime in sorted(
        firmware_ranges, key=lambda item: item[1]
    ):
        print(
            f"{label} offset=0x{offset:x} runtime=0x{runtime:x} "
            f"size={fw_size}"
        )

    print(f"CSF offset=0x{csf_offset:x} container_size={len(image)}")
    print(f"container sha256={sha256(image)}")


def main():
    if len(sys.argv) != 6:
        raise SystemExit(
            "usage: verify-falcon-boot.py "
            "IMAGE FALCON_SPL SD_SPL FALCON_SPL_DDR FIRMWARE_DIR"
        )

    paths = list(map(pathlib.Path, sys.argv[1:]))

    try:
        verify(
            paths[0].read_bytes(),
            paths[1].read_bytes(),
            paths[2].read_bytes(),
            paths[3].read_bytes(),
            paths[4],
        )
    except (OSError, ValueError, struct.error) as error:
        raise SystemExit(
            f"Falcon boot verification failed: {error}"
        ) from error


if __name__ == "__main__":
    main()
