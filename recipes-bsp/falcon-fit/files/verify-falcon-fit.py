#!/usr/bin/env python3
import os
import struct
import subprocess
import sys


def fail(message):
    raise SystemExit("Falcon FIT: " + message)


def prop(blob, node, name, kind="s"):
    result = subprocess.run(
        ["fdtget", "-t", kind, blob, node, name],
        check=True, text=True, stdout=subprocess.PIPE,
    )
    return result.stdout.strip()


def check(condition, message):
    if not condition:
        fail(message)


def integer(blob, node, name):
    return int(prop(blob, node, name, "x"), 16)


def main():
    if len(sys.argv) != 2:
        fail("usage: verify-falcon-fit.py Image.itb")
    fit = sys.argv[1]
    with open(fit, "rb") as stream:
        header = stream.read(40)
        check(len(header) == 40, "truncated FIT header")
        magic, metadata_size = struct.unpack_from(">II", header)
        check(magic == 0xD00DFEED, "invalid FIT magic")
        check(0 < metadata_size <= 0x8000, "FIT metadata exceeds 32 KiB")
        file_size = os.fstat(stream.fileno()).st_size
        check(prop(fit, "/configurations", "default") == "conf-falcon",
              "unexpected default configuration")
        conf = "/configurations/conf-falcon"
        for key, value in (("firmware", "bl31"), ("loadables", "kernel tee"),
                           ("fdt", "control-fdt")):
            check(prop(fit, conf, key) == value, "unexpected " + key)

        payloads = (
            ("bl31", "bl31.bin", 0x00970000, 0x20000, "firmware",
             "arm-trusted-firmware"),
            ("kernel", "Image", 0x48000000, 0x4000000, "kernel", "linux"),
            ("tee", "tee.bin", 0x56000000, 0x1e00000, "firmware", "tee"),
            ("control-fdt", "control.dtb", 0x43300000, 0x100000, "flat_dt",
             None),
        )
        ranges = []
        for name, source, address, limit, image_type, os_name in payloads:
            node = "/images/" + name
            check(prop(fit, node, "type") == image_type, name + " type")
            check(prop(fit, node, "arch") == "arm64", name + " arch")
            check(prop(fit, node, "compression") == "none", name + " compression")
            if os_name:
                check(prop(fit, node, "os") == os_name, name + " OS")
                check(integer(fit, node, "entry") == address, name + " entry")
            check(integer(fit, node, "load") == address, name + " load")
            props = subprocess.run(
                ["fdtget", "-p", fit, node], check=True, text=True,
                stdout=subprocess.PIPE,
            ).stdout.splitlines()
            check("data" not in props and "data-offset" not in props and
                  "data-position" in props, name + " must use absolute external data")
            position = integer(fit, node, "data-position")
            size = integer(fit, node, "data-size")
            check(position >= 0x10000 and position % 0x1000 == 0,
                  name + " external alignment")
            check(0 < size <= limit and position + size <= file_size,
                  name + " size or FIT bounds")
            ranges.append((position, position + size, name))
            with open(source, "rb") as original:
                expected = original.read()
            stream.seek(position)
            check(len(expected) == size and stream.read(size) == expected,
                  name + " payload does not match " + source)
            print(f"{name}: file 0x{position:x}+0x{size:x}, "
                  f"load 0x{address:x}..0x{address + size:x}")

        for left, right in zip(sorted(ranges), sorted(ranges)[1:]):
            check(left[1] <= right[0], left[2] + " overlaps " + right[2])

    with open("Image", "rb") as image:
        header = image.read(64)
    check(len(header) == 64 and header[56:60] == b"ARM\x64",
          "invalid arm64 Image header")
    text_offset, image_size, flags = struct.unpack_from("<QQQ", header, 8)
    check(text_offset == 0 and not (flags & 1) and flags & 8,
          "Image requires a different offset, endianness or placement")
    check(os.path.getsize("Image") <= image_size <= 0x4000000,
          "Image runtime size exceeds the Linux envelope")

    check(prop("control.dtb", "/mcu_rdc", "compatible") == "imx8m,mcu_rdc",
          "control FDT lacks the i.MX8MP RDC node")
    for key in ("start-config", "stop-config"):
        cells = prop("control.dtb", "/mcu_rdc", key, "x").split()
        check(cells and len(cells) % 5 == 0, "invalid RDC " + key)
    print(f"FIT metadata: {metadata_size} bytes; "
          f"kernel runtime: 0x48000000..0x{0x48000000 + image_size:x}")


if __name__ == "__main__":
    main()
