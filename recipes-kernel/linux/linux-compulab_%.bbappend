FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
    file://powertune-usb-can.cfg \
"

SRC_URI:append:iot-gate-imx8plus = " \
    file://0001-arm64-dts-compulab-add-powertune-d4-falcon.patch \
"

do_compile:append:iot-gate-imx8plus() {
    oe_runmake compulab/iot-gate-imx8plus-powertune-d4-falcon.dtb \
        CC="${KERNEL_CC} $cc_extra " LD="${KERNEL_LD}" \
        OBJCOPY="${KERNEL_OBJCOPY}" STRIP="${KERNEL_STRIP}" ${KERNEL_EXTRA_ARGS}

    falcon_dtb="${B}/arch/${ARCH}/boot/dts/compulab/iot-gate-imx8plus-powertune-d4-falcon.dtb"
    falcon_size=$(stat -c %s "$falcon_dtb")
    if [ "$falcon_size" -gt 2097152 ]; then
        bbfatal "Falcon DTB exceeds its 2 MiB FDT envelope: $falcon_size bytes"
    fi
}

do_deploy:append:iot-gate-imx8plus() {
    install -d "${DEPLOYDIR}/${KERNEL_DEPLOYSUBDIR}"
    install -m 0644 \
        "${B}/arch/${ARCH}/boot/dts/compulab/iot-gate-imx8plus-powertune-d4-falcon.dtb" \
        "${DEPLOYDIR}/${KERNEL_DEPLOYSUBDIR}/iot-gate-imx8plus-falcon.dtb"
}
