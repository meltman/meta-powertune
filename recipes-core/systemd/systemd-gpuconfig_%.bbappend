do_install:append:iot-gate-imx8plus() {
    rm -f ${D}${sysconfdir}/systemd/system/multi-user.target.wants/gpuconfig.service
}
