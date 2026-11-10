FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:iot-gate-imx8plus = " file://0001-imx8mp-add-isolated-falcon-linux-handoff.patch"

do_compile:append:iot-gate-imx8plus() {
    if ${BUILD_OPTEE}; then
        if [ "${ATF_PLATFORM}" != "imx8mp" ]; then
            bbfatal "Falcon BL31 requires ATF_PLATFORM=imx8mp"
        fi
        oe_runmake clean BUILD_BASE=build-optee-falcon
        oe_runmake BUILD_BASE=build-optee-falcon SPD=opteed FALCON_LINUX=1 bl31
    else
        bbnote "Skipping Falcon BL31: OP-TEE is not enabled for this machine"
    fi
}

do_deploy:append:iot-gate-imx8plus() {
    if ${BUILD_OPTEE}; then
        install -m 0644 ${S}/build-optee-falcon/${ATF_PLATFORM}/${OUTPUT_FOLDER}/bl31.bin \
            ${DEPLOYDIR}/bl31-${ATF_PLATFORM}-falcon.bin-optee
    fi
}
