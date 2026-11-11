SUMMARY = "Isolated i.MX8MP Falcon SPL boot container"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"
COMPATIBLE_MACHINE = "^iot-gate-imx8plus$"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = " \
    git://github.com/nxp-imx/imx-mkimage.git;protocol=https;branch=lf-6.6.52_2.2.0 \
    file://verify-falcon-boot.py \
"
SRCREV = "71b8c18af93a5eb972d80fbec290006066cff24f"
S = "${WORKDIR}/git"
B = "${S}"

inherit deploy

DEPENDS = "binutils-native zlib-native python3-native"
do_compile[depends] += " \
    u-boot-compulab:do_deploy \
    ${@' '.join('%s:do_deploy' % r for r in d.getVar('IMX_EXTRA_FIRMWARE').split())} \
"
do_configure[noexec] = "1"
do_install[noexec] = "1"

FALCON_SPL_SHA256 = "e2487946f1ba8e4469a60f1551e013330c1b8b0694590ebe9157ec8bfb59255a"
FALCON_BOOT_NAME = "flash.bin_iot-gate-imx8plus_falcon_d4"

do_compile() {
    [ "${DRAM_CONF}" = "d4" ] || bbfatal "Falcon container requires DRAM_CONF=d4"
    spl="${DEPLOY_DIR_IMAGE}/u-boot-spl.bin-${MACHINE}-falcon"
    [ -f "$spl" ] || bbfatal "Missing Falcon SPL: $spl"
    printf '%s  %s\n' "${FALCON_SPL_SHA256}" "$spl" | sha256sum -c - || \
        bbfatal "Falcon SPL does not match the reviewed Candidate A"

    install -m 0644 "$spl" "${S}/iMX8M/u-boot-spl.bin"
    for firmware in ${DDR_FIRMWARE_NAME}; do
        [ -f "${DEPLOY_DIR_IMAGE}/$firmware" ] || bbfatal "Missing DDR firmware: $firmware"
        install -m 0644 "${DEPLOY_DIR_IMAGE}/$firmware" "${S}/iMX8M/$firmware"
    done
    oe_runmake -C "${S}/iMX8M" -f soc.mak SOC=iMX8MP SOC_DIR=iMX8M \
        CC="${BUILD_CC}" \
        CFLAGS="${BUILD_CFLAGS} -std=c99 -I${STAGING_INCDIR_NATIVE} -L${STAGING_LIBDIR_NATIVE}" \
        BUILD_LDFLAGS="${BUILD_LDFLAGS}" u-boot-spl-ddr.bin mkimage_imx8

    cd "${S}/iMX8M"
    ./mkimage_imx8 -version v2 -fit -loader u-boot-spl-ddr.bin 0x920000 \
        -out "${FALCON_BOOT_NAME}" > falcon-container-map.txt 2>&1 || \
        bbfatal "Falcon mkimage failed; inspect ${S}/iMX8M/falcon-container-map.txt"
    # mkimage advertises an extra 0x60 bytes beyond its zero-filled CSF.
    dd if=/dev/zero bs=96 count=1 >> "${FALCON_BOOT_NAME}" || \
        bbfatal "Failed to pad Falcon ROM load extent"
    python3 "${WORKDIR}/verify-falcon-boot.py" "${FALCON_BOOT_NAME}" \
        u-boot-spl.bin "${DEPLOY_DIR_IMAGE}/u-boot-spl.bin-${MACHINE}-sd" . \
        > falcon-container-verification.txt || \
        bbfatal "Falcon container verification failed"
    sha256sum "${FALCON_BOOT_NAME}" u-boot-spl.bin u-boot-spl-ddr.bin \
        ${DDR_FIRMWARE_NAME} > falcon-container-inputs.sha256
}

do_deploy() {
    install -m 0644 "${S}/iMX8M/${FALCON_BOOT_NAME}" "${DEPLOYDIR}/${FALCON_BOOT_NAME}"
    install -d "${DEPLOYDIR}/falcon-boot-evidence"
    for file in u-boot-spl.bin u-boot-spl-ddr.bin falcon-container-map.txt \
        falcon-container-verification.txt falcon-container-inputs.sha256; do
        install -m 0644 "${S}/iMX8M/$file" "${DEPLOYDIR}/falcon-boot-evidence/$file"
    done
}
addtask deploy after do_compile
