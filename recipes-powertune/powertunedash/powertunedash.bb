SUMMARY = "PowerTune Dashboard"
DESCRIPTION = "PowerTune automotive dashboard application"
LICENSE = "CLOSED"

inherit qmake5 useradd systemd

USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "--system --user-group --home-dir /var/lib/powertune --no-create-home --shell /bin/bash --groups dialout,video gauges"

DEPENDS = " \
    qtbase \
    qtdeclarative \
    qtserialport \
    qtserialbus \
    qtcharts \
    qtlocation \
    qtsensors \
    qtmultimedia \
"

SRC_URI = "git:///work/PowerTuneDash;protocol=file;branch=imx8-qt6"
SRC_URI += " \
    file://powertunedash.service \
    file://eglfs.json \
    file://powertunedash.logrotate \
"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/PowertuneQMLGui ${D}${bindir}/powertunedash

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/powertunedash.service \
        ${D}${systemd_system_unitdir}/powertunedash.service

    install -d ${D}${sysconfdir}/powertune
    install -m 0644 ${WORKDIR}/eglfs.json \
        ${D}${sysconfdir}/powertune/eglfs.json

    install -d ${D}${sysconfdir}/logrotate.d
    install -m 0644 ${WORKDIR}/powertunedash.logrotate \
        ${D}${sysconfdir}/logrotate.d/powertunedash
}

FILES:${PN} += " \
    ${bindir}/powertunedash \
    ${systemd_system_unitdir}/powertunedash.service \
    ${sysconfdir}/powertune/eglfs.json \
    ${sysconfdir}/logrotate.d/powertunedash \
"

SYSTEMD_SERVICE:${PN} = "powertunedash.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
