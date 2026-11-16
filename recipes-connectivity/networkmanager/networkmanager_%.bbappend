do_install:append:iot-gate-imx8plus() {
    sed -i '/^Also=NetworkManager-wait-online.service$/d' \
        ${D}${systemd_system_unitdir}/NetworkManager.service
}
