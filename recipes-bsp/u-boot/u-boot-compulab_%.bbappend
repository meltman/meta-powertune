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

do_compile:remove() {
    export SOURCE_DATE_EPOCH=$(date +%s)
}

do_deploy:append() {
    falcon_build="${B}/iot-gate-imx8plus_falcon_defconfig"
    falcon_ddr="${falcon_build}/u-boot-spl-ddr.bin"
    deploy_name="u-boot-spl-ddr.bin-${MACHINE}-falcon"

    [ -f "${falcon_ddr}" ] || \
        bbfatal "Missing Falcon Binman SPL+DDR payload: ${falcon_ddr}"

    # Require Binman to have patched the image-position/size symbol block.
    grep -aq 'BSYM' "${falcon_ddr}" || \
        bbfatal "Falcon SPL+DDR payload lacks patched Binman symbols"

    install -m 0644 \
        "${falcon_ddr}" \
        "${DEPLOYDIR}/${deploy_name}"

    sha256sum \
        "${DEPLOYDIR}/${deploy_name}" \
        > "${DEPLOYDIR}/${deploy_name}.sha256"
}
