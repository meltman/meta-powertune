SUMMARY = "PowerTune Qt5 runtime dependency bundle"
DESCRIPTION = "Qt5 runtime, QML modules, and PowerTuneDash required by PowerTune"
LICENSE = "MIT"

# Deliberately do not inherit packagegroup.
# packagegroup inherits allarch, which is incompatible with Debian's
# dynamically renamed architecture-specific Qt5 packages.
ALLOW_EMPTY:${PN} = "1"
PACKAGE_ARCH = "${MACHINE_ARCH}"

RDEPENDS:${PN} = " \
    powertunedash \
    qtbase \
    qtbase-plugins \
    qtdeclarative \
    qtdeclarative-qmlplugins \
    qtquickcontrols-qmlplugins \
    qtquickcontrols2-qmlplugins \
    qtgraphicaleffects-qmlplugins \
    qtcharts-qmlplugins \
    qtlocation-qmlplugins \
    qtmultimedia-qmlplugins \
    qtsensors-qmlplugins \
    qtxmlpatterns-qmlplugins \
    qtvirtualkeyboard-qmlplugins \
"

