# PowerTune Falcon Mode support for i.MX8M Plus.
# Pass the Falcon-prepared FDT address to Linux through BL31.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:mx8mp-generic-bsp = " \
    file://0001-imx8mp-add-falcon-mode-support.patch \
"
