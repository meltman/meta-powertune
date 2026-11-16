SUMMARY = "PowerTune appliance startup and runtime policy"
DESCRIPTION = "Appliance-specific sysctl, NetworkManager, and first-boot policies"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
COMPATIBLE_MACHINE = "iot-gate-imx8plus"

RDEPENDS:${PN} = "systemd networkmanager-daemon"

SRC_URI = " \
    file://90-powertune-appliance.conf \
    file://powertune-appliance.conf \
    file://20-powertune-can-unmanaged.conf \
    file://10-powertune-first-boot.conf \
    file://00-powertune.preset \
"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${sysconfdir}/sysctl.d
    install -m 0644 ${WORKDIR}/90-powertune-appliance.conf \
        ${D}${sysconfdir}/sysctl.d/90-powertune-appliance.conf

    install -d ${D}${sysconfdir}/tmpfiles.d
    install -m 0644 ${WORKDIR}/powertune-appliance.conf \
        ${D}${sysconfdir}/tmpfiles.d/powertune-appliance.conf

    install -d ${D}${sysconfdir}/NetworkManager/conf.d
    install -m 0644 ${WORKDIR}/20-powertune-can-unmanaged.conf \
        ${D}${sysconfdir}/NetworkManager/conf.d/20-powertune-can-unmanaged.conf

    install -d ${D}${sysconfdir}/systemd/system/cl-growfs-rootfs.service.d
    install -m 0644 ${WORKDIR}/10-powertune-first-boot.conf \
        ${D}${sysconfdir}/systemd/system/cl-growfs-rootfs.service.d/10-powertune-first-boot.conf

    install -d ${D}${sysconfdir}/systemd/system-preset
    install -m 0644 ${WORKDIR}/00-powertune.preset \
        ${D}${sysconfdir}/systemd/system-preset/00-powertune.preset
}

FILES:${PN} += " \
    ${sysconfdir}/sysctl.d/90-powertune-appliance.conf \
    ${sysconfdir}/tmpfiles.d/powertune-appliance.conf \
    ${sysconfdir}/NetworkManager/conf.d/20-powertune-can-unmanaged.conf \
    ${sysconfdir}/systemd/system/cl-growfs-rootfs.service.d/10-powertune-first-boot.conf \
    ${sysconfdir}/systemd/system-preset/00-powertune.preset \
"
