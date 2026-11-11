FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRCBRANCH = "imx8mp-falcon-4g"
SRCREV = "912387948f8d751f40a8b2444ddd9a1c4fe616b4"
export SOURCE_DATE_EPOCH = "1794412504"
SRC_URI:remove = "git://github.com/compulab-yokneam/u-boot-compulab;protocol=https;branch=${SRCBRANCH}"
SRC_URI:prepend = "git://github.com/meltman/u-boot-compulab.git;protocol=https;branch=imx8mp-falcon-4g "

SRC_URI += "file://fw_env.config"

UBOOT_CONFIG:iot-gate-imx8plus = "sd falcon"
UBOOT_CONFIG[falcon] = "iot-gate-imx8plus_falcon_defconfig,sdcard"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
}
