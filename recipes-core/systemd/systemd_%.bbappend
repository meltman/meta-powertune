do_install:append:iot-gate-imx8plus() {
    sed -i '/^enable systemd-network-generator.service$/d' \
        ${D}${systemd_unitdir}/system-preset/90-systemd.preset
    sed -i '/^Also=systemd-network-generator.service$/d' \
        ${D}${systemd_system_unitdir}/systemd-networkd.service
}
