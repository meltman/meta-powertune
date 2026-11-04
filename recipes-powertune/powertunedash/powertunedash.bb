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

SRC_URI = "git:///work/PowerTuneDash;protocol=file;branch=imx8-appliance"
SRC_URI += " \
    file://powertunedash.service \
    file://eglfs.json \
    file://powertunedash.logrotate \
    file://test-assets/black-cat.png \
    file://test-assets/cars.gif \
    file://test-assets/state-off.gif \
    file://test-assets/state-off.png \
    file://test-assets/state-on.gif \
    file://test-assets/state-on.png \
"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/PowertuneQMLGui ${D}${bindir}/powertunedash

    # Development/bench utility for exercising PowerTune's daemon UDP interface.
    install -m 0755 ${S}/tools/powertune-udp-sim.py \
        ${D}${bindir}/pt-udp-sim

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/powertunedash.service \
        ${D}${systemd_system_unitdir}/powertunedash.service

    install -d ${D}${sysconfdir}/powertune
    install -m 0644 ${WORKDIR}/eglfs.json \
        ${D}${sysconfdir}/powertune/eglfs.json

    install -d ${D}${sysconfdir}/logrotate.d
    install -m 0644 ${WORKDIR}/powertunedash.logrotate \
        ${D}${sysconfdir}/logrotate.d/powertunedash

    install -d ${D}${datadir}/powertune/test-assets
    install -m 0644 ${WORKDIR}/test-assets/black-cat.png \
        ${D}${datadir}/powertune/test-assets/black-cat.png
    install -m 0644 ${WORKDIR}/test-assets/cars.gif \
        ${D}${datadir}/powertune/test-assets/cars.gif
    install -m 0644 ${WORKDIR}/test-assets/state-off.gif \
        ${D}${datadir}/powertune/test-assets/state-off.gif
    install -m 0644 ${WORKDIR}/test-assets/state-off.png \
        ${D}${datadir}/powertune/test-assets/state-off.png
    install -m 0644 ${WORKDIR}/test-assets/state-on.gif \
        ${D}${datadir}/powertune/test-assets/state-on.gif
    install -m 0644 ${WORKDIR}/test-assets/state-on.png \
        ${D}${datadir}/powertune/test-assets/state-on.png
}

pkg_postinst_ontarget:${PN}() {
    seed_dir="${datadir}/powertune/test-assets"
    data_dir="${localstatedir}/lib/powertune"
    logo_dir="${localstatedir}/lib/powertune/Logo"
    seed_marker="$data_dir/.regression-assets-seeded-v1"

    if [ ! -e "$seed_marker" ] && [ ! -L "$seed_marker" ]; then
        if [ ! -d "$data_dir" ]; then
            install -d -o root -g gauges -m 2775 "$data_dir"
        fi
        if [ ! -d "$logo_dir" ]; then
            install -d -o root -g gauges -m 2775 "$logo_dir"
        fi

        for asset in black-cat.png cars.gif state-off.gif state-off.png state-on.gif state-on.png; do
            if [ ! -e "$logo_dir/$asset" ] && [ ! -L "$logo_dir/$asset" ]; then
                install -o root -g gauges -m 0664 "$seed_dir/$asset" "$logo_dir/$asset"
            fi
        done

        : > "$seed_marker"
        chown root:gauges "$seed_marker"
        chmod 0644 "$seed_marker"
    fi
}

FILES:${PN} += " \
    ${bindir}/powertunedash \
    ${bindir}/pt-udp-sim \
    ${systemd_system_unitdir}/powertunedash.service \
    ${sysconfdir}/powertune/eglfs.json \
    ${sysconfdir}/logrotate.d/powertunedash \
    ${datadir}/powertune/test-assets \
"

SYSTEMD_SERVICE:${PN} = "powertunedash.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
