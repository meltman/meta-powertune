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
    file://dashboards/performance-reference-v1.txt \
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

# Build provenance from the exact fetched source revision. Git is used only at
# build time here; the application never runs Git or needs .git. The version is
# the commit date (reproducible for a given revision); the revision is the
# short commit SHA. No branch is embedded: a fetched checkout cannot establish
# it robustly.
do_configure:prepend() {
    rev="$(git -C ${S} rev-parse --short=7 HEAD 2>/dev/null || true)"
    ver="$(git -C ${S} log -1 --format=%cd --date=format:%Y.%m.%d HEAD 2>/dev/null || true)"
    export PT_BUILD_REVISION="${rev:-unknown}"
    export PT_BUILD_VERSION="${ver:-unknown}"
    bbnote "PowerTune build provenance: version=$PT_BUILD_VERSION revision=$PT_BUILD_REVISION"
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/PowertuneQMLGui ${D}${bindir}/powertunedash

    # Development/bench utility for exercising PowerTune's daemon UDP interface.
    install -m 0755 ${S}/tools/powertune-udp-sim.py \
        ${D}${bindir}/pt-udp-sim
    install -m 0755 ${S}/tools/pt-usb-can.py \
        ${D}${bindir}/pt-usb-can

    # Open Hondata K-Pro CAN -> PowerTune UDP integration bridge.
    install -m 0755 ${S}/tools/kpro-powertune-bridge.py \
        ${D}${bindir}/kpro-powertune-bridge
    install -m 0644 ${S}/tools/kpro_can.py \
        ${D}${bindir}/kpro_can.py
    install -m 0644 ${S}/tools/powertune_udp.py \
        ${D}${bindir}/powertune_udp.py

    # Deterministic K-Pro workload generator and profiling harness.
    install -m 0755 ${S}/tools/kpro-workload.py \
        ${D}${bindir}/kpro-workload
    install -m 0644 ${S}/tools/kpro_workload.py \
        ${D}${bindir}/kpro_workload.py
    install -m 0755 ${S}/tools/pt-profile-workload.sh \
        ${D}${bindir}/pt-profile-workload

    install -d ${D}${datadir}/powertune/kpro-workloads
    install -m 0644 ${S}/tests/fixtures/hondata/*.json \
        ${D}${datadir}/powertune/kpro-workloads/

    install -d ${D}${datadir}/powertune/udp-workloads
    install -m 0644 ${S}/tests/fixtures/udp/*.json \
        ${D}${datadir}/powertune/udp-workloads/

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/powertunedash.service \
        ${D}${systemd_system_unitdir}/powertunedash.service

    install -d ${D}${sysconfdir}/powertune
    install -m 0644 ${WORKDIR}/eglfs.json \
        ${D}${sysconfdir}/powertune/eglfs.json

    install -d ${D}${sysconfdir}/logrotate.d
    install -m 0644 ${WORKDIR}/powertunedash.logrotate \
        ${D}${sysconfdir}/logrotate.d/powertunedash

    # Canonical performance-profiling dashboard.
    install -d ${D}${datadir}/powertune/dashboards
    install -m 0644 ${WORKDIR}/dashboards/performance-reference-v1.txt \
        ${D}${datadir}/powertune/dashboards/performance-reference-v1.txt

    # Seed the mutable PowerTune export location on a fresh image.
    install -d ${D}${localstatedir}/lib/powertune/UserDashboards
    install -m 0664 ${WORKDIR}/dashboards/performance-reference-v1.txt \
        ${D}${localstatedir}/lib/powertune/UserDashboards/Dash1Export.txt

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
    ${bindir}/pt-usb-can \
    ${bindir}/kpro-powertune-bridge \
    ${bindir}/kpro_can.py \
    ${bindir}/powertune_udp.py \
    ${bindir}/kpro-workload \
    ${bindir}/kpro_workload.py \
    ${bindir}/pt-profile-workload \
    ${datadir}/powertune/kpro-workloads \
    ${datadir}/powertune/udp-workloads \
    ${systemd_system_unitdir}/powertunedash.service \
    ${sysconfdir}/powertune/eglfs.json \
    ${sysconfdir}/logrotate.d/powertunedash \
    ${datadir}/powertune/test-assets \
    ${datadir}/powertune/dashboards/performance-reference-v1.txt \
    ${localstatedir}/lib/powertune/UserDashboards/Dash1Export.txt \
"

SYSTEMD_SERVICE:${PN} = "powertunedash.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
