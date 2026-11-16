SYSTEMD_SERVICE:${PN} = "iotg-imx8plus-ie-config.service iotg-imx8plus-gw-config.service iotg-imx8plus-stack-config.service"
SYSTEMD_AUTO_ENABLE:${PN}:iot-gate-imx8plus = "disable"

pkg_postinst:${PN}:iot-gate-imx8plus() {
    :
}
