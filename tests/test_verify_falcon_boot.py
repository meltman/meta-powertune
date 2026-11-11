#!/usr/bin/env python3
"""Host-only tests of the Falcon container verifier, not of ROM or SPL."""

import contextlib
import importlib.util
import io
import pathlib
import struct
import tempfile
import unittest

SCRIPT = (pathlib.Path(__file__).resolve().parents[1] /
          "recipes-bsp/falcon-boot/files/verify-falcon-boot.py")
spec = importlib.util.spec_from_file_location("verify_falcon_boot", SCRIPT)
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class VerifyFalconBootTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = pathlib.Path(self.temp.name)
        self.falcon = b"falcon-test-spl"
        self.normal = b"normal-test-spl"
        loader = self.falcon + bytes((-len(self.falcon)) % 4)
        for index, (name, size) in enumerate(verifier.TRAINING):
            firmware = bytes([index + 1]) * 31
            (self.path / name).write_bytes(firmware)
            loader += firmware + bytes(size - len(firmware))
        end = 64 + len(loader)
        csf_offset = (end + 511) & ~511
        header = bytearray(64)
        header[:4] = b"\xd1\x00\x20\x41"
        base = 0x920000 - 64
        struct.pack_into("<7I", header, 4, 0x920000, 0, 0,
                         base + 32, base, base + csf_offset, 0)
        struct.pack_into("<4I", header, 32, base, csf_offset + 0x2060, 0, 0)
        self.image = bytes(header) + loader + bytes(csf_offset - end + 0x2060)

    def check(self, image=None, falcon=None, normal=None):
        with contextlib.redirect_stdout(io.StringIO()):
            verifier.verify(self.image if image is None else image,
                            self.falcon if falcon is None else falcon,
                            self.normal if normal is None else normal, self.path)

    def test_valid(self):
        self.check()

    def test_primary_spl_and_header_rejected(self):
        for offset in (0, 4, 64):
            with self.subTest(offset=offset):
                image = bytearray(self.image)
                image[offset] ^= 1
                with self.assertRaises(ValueError):
                    self.check(bytes(image))
        with self.assertRaises(ValueError):
            self.check(normal=self.falcon)

    def test_corrupt_firmware_or_extra_payload_rejected(self):
        image = bytearray(self.image)
        image[64 + ((len(self.falcon) + 3) & ~3)] ^= 1
        with self.assertRaises(ValueError):
            self.check(bytes(image))
        with self.assertRaises(ValueError):
            self.check(self.image + b"second-loader")
        with self.assertRaises(ValueError):
            self.check(self.image[:-1])


if __name__ == "__main__":
    unittest.main()
