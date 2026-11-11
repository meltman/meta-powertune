#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-2.0-only
"""Verify the specific unsigned i.MX8MP loader-only container we produce."""

import hashlib
import pathlib
import struct
import sys

TRAINING = (
    ("lpddr4_pmu_train_1d_imem_202006.bin", 0x8000),
    ("lpddr4_pmu_train_1d_dmem_202006.bin", 0x4000),
    ("lpddr4_pmu_train_2d_imem_202006.bin", 0x8000),
    ("lpddr4_pmu_train_2d_dmem_202006.bin", 0x4000),
)


def verify(image, falcon, normal, firmware_dir):
    if not falcon or not normal or falcon == normal:
        raise ValueError("SPL input is empty or Falcon is identical to normal")
    if len(image) < 64 or image[:4] != b"\xd1\x00\x20\x41":
        raise ValueError("invalid ROM IVT header")
    entry, reserved1, dcd, boot_ptr, self_addr, csf, reserved2 = struct.unpack_from(
        "<7I", image, 4
    )
    start, size, plugin, padding = struct.unpack_from("<4I", image, 32)
    if (entry != 0x920000 or self_addr != entry - 64 or
            boot_ptr != self_addr + 32 or start != self_addr or
            reserved1 or reserved2 or dcd or plugin or padding or
            image[48:64] != bytes(16)):
        raise ValueError("unexpected ROM loader header")
    loader = falcon + bytes((-len(falcon)) % 4)
    for name, padded_size in TRAINING:
        data = (firmware_dir / name).read_bytes()
        if not data or len(data) > padded_size:
            raise ValueError(f"invalid firmware size: {name}")
        loader += data + bytes(padded_size - len(data))
    payload_end = 64 + len(loader)
    csf_offset = (payload_end + 511) & ~511
    if (image[64:payload_end] != loader or
            image[payload_end:csf_offset] != bytes(csf_offset - payload_end) or
            csf != start + csf_offset or size != csf_offset + 0x2060 or
            len(image) != csf_offset + 0x2060 or
            image[csf_offset:] != bytes(0x2060)):
        raise ValueError("loader, DDR firmware, padding, or CSF extent mismatch")
    if len(image) > 0x3f0000:
        raise ValueError("container overlaps the recorded boot0 environment")
    extracted = image[64:64 + len(falcon)]
    print("IVT offset=0x0 entry=0x920000 loader_offset=0x40")
    print(f"SPL size={len(extracted)} sha256={hashlib.sha256(extracted).hexdigest()}")
    print(f"normal SPL sha256={hashlib.sha256(normal).hexdigest()}")
    print(f"DDR firmware offset=0x{64 + ((len(falcon) + 3) & ~3):x}")
    print(f"CSF offset=0x{csf_offset:x} container_size={len(image)}")
    print(f"container sha256={hashlib.sha256(image).hexdigest()}")


def main():
    if len(sys.argv) != 5:
        raise SystemExit("usage: verify-falcon-boot.py IMAGE FALCON_SPL SD_SPL FIRMWARE_DIR")
    image, falcon, normal, firmware_dir = map(pathlib.Path, sys.argv[1:])
    try:
        verify(image.read_bytes(), falcon.read_bytes(), normal.read_bytes(), firmware_dir)
    except (OSError, ValueError, struct.error) as error:
        raise SystemExit(f"Falcon boot verification failed: {error}") from error


if __name__ == "__main__":
    main()
