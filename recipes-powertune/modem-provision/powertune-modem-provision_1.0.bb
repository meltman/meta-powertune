SUMMARY = "Declarative modem hardware provisioning for PowerTune platforms"
DESCRIPTION = "Applies and verifies persistent modem settings when explicitly run"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=c517258fbb807ea861b6f07fb902d44e"

SRC_URI = " \
    file://LICENSE \
    file://modem-provision \
    file://powertune-modem-provision.service \
    file://profiles/quectel-ec25.json \
"

S = "${WORKDIR}"

inherit systemd

RDEPENDS:${PN} = "python3-core systemd"

do_install() {
    install -d ${D}${datadir}/licenses/${PN}
    install -m 0644 ${WORKDIR}/LICENSE ${D}${datadir}/licenses/${PN}/LICENSE

    install -d ${D}${libexecdir}/powertune
    install -m 0755 ${WORKDIR}/modem-provision ${D}${libexecdir}/powertune/modem-provision

    install -d ${D}${datadir}/powertune/modem-profiles
    install -m 0644 ${WORKDIR}/profiles/quectel-ec25.json \
        ${D}${datadir}/powertune/modem-profiles/quectel-ec25.json

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/powertune-modem-provision.service \
        ${D}${systemd_system_unitdir}/powertune-modem-provision.service
}

FILES:${PN} += " \
    ${libexecdir}/powertune/modem-provision \
    ${datadir}/licenses/${PN}/LICENSE \
    ${datadir}/powertune/modem-profiles/quectel-ec25.json \
    ${systemd_system_unitdir}/powertune-modem-provision.service \
"

SYSTEMD_SERVICE:${PN} = "powertune-modem-provision.service"
SYSTEMD_AUTO_ENABLE:${PN}:iot-gate-imx8plus = "disable"
