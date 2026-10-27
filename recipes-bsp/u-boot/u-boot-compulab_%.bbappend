# Build both the known-good normal U-Boot and the CompuLab-provided
# Falcon-mode configuration for the IOT-GATE-IMX8PLUS.
UBOOT_CONFIG:iot-gate-imx8plus = "sd falcon"

UBOOT_CONFIG[falcon] = "iot-gate-imx8plus_falcon_defconfig,sdcard"

