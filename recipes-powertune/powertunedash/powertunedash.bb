SUMMARY = "PowerTune Dashboard"
DESCRIPTION = "PowerTune automotive dashboard application"
LICENSE = "CLOSED"

inherit qmake5

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

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/PowertuneQMLGui ${D}${bindir}/powertunedash
}

FILES:${PN} += "${bindir}/powertunedash"
