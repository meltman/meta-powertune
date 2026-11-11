FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRCBRANCH = "imx8mp-falcon-4g"
SRCREV = "7efc6dbe4b85eafdf99e818ef0cad53a9b1bec2d"
SRC_URI:remove = "git://github.com/compulab-yokneam/u-boot-compulab;protocol=https;branch=${SRCBRANCH}"
SRC_URI:prepend = "git://github.com/meltman/u-boot-compulab.git;protocol=https;branch=imx8mp-falcon-4g "

SRC_URI += "file://fw_env.config"

UBOOT_CONFIG:iot-gate-imx8plus = "sd falcon"
UBOOT_CONFIG[falcon] = "iot-gate-imx8plus_falcon_defconfig,sdcard"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
}
