SUMMARY = "PowerTune i.MX8MP Falcon Linux FIT"
LICENSE = "CLOSED"
COMPATIBLE_MACHINE = "^iot-gate-imx8plus$"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = " \
    file://powertune-falcon.its \
    file://verify-falcon-fit.py \
"
S = "${WORKDIR}"
B = "${WORKDIR}/build"

inherit deploy

DEPENDS = "u-boot-mkimage-native dtc-native python3-native"
do_compile[depends] += " \
    virtual/kernel:do_deploy \
    imx-atf:do_deploy \
    optee-os:do_deploy \
    u-boot-compulab:do_deploy \
"

do_configure[noexec] = "1"
do_install[noexec] = "1"

do_compile() {
    (cd "${DEPLOY_DIR_IMAGE}" && \
        sha256sum -c iot-gate-imx8plus-falcon-control.sha256) || \
        bbfatal "Falcon U-Boot control FDT unavailable or inconsistent; enable the falcon UBOOT_CONFIG"
    install -d "${B}"
    install -m 0644 "${WORKDIR}/powertune-falcon.its" "${B}/"
    install -m 0644 "${DEPLOY_DIR_IMAGE}/Image" "${B}/Image"
    install -m 0644 "${DEPLOY_DIR_IMAGE}/bl31-imx8mp-falcon.bin-optee" "${B}/bl31.bin"
    install -m 0644 "${DEPLOY_DIR_IMAGE}/tee.bin" "${B}/tee.bin"
    install -m 0644 "${DEPLOY_DIR_IMAGE}/iot-gate-imx8plus-falcon-control.dtb" "${B}/control.dtb"

    cd "${B}"
    mkimage -f powertune-falcon.its -E -B 0x1000 -p 0x10000 Image.itb
    python3 "${WORKDIR}/verify-falcon-fit.py" Image.itb
}

do_deploy() {
    install -Dm 0644 "${B}/Image.itb" "${DEPLOYDIR}/Image.itb"
}
addtask deploy after do_compile
