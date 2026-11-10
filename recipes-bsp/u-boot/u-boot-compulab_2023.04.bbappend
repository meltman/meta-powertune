do_deploy:append:iot-gate-imx8plus() {
    case " ${UBOOT_CONFIG} " in
        *" falcon "*) ;;
        *)
            printf '%s\n' disabled > "${DEPLOYDIR}/iot-gate-imx8plus-falcon-control.sha256"
            return
            ;;
    esac

    falcon_build="${B}/iot-gate-imx8plus_falcon_defconfig"
    for setting in \
        'CONFIG_IMX8MP=y' \
        'CONFIG_SPL_OS_BOOT=y' \
        'CONFIG_SPL_LOAD_FIT=y' \
        'CONFIG_SPL_FIT_IMAGE_TINY=y' \
        'CONFIG_SPL_OF_LIBFDT=y' \
        'CONFIG_SYS_CACHELINE_SIZE=64' \
        'CONFIG_SYS_SPL_ARGS_ADDR=0x43000000' \
        'CONFIG_CUSTOM_SYS_SPL_MALLOC_ADDR=0x42200000' \
        'CONFIG_SYS_SPL_MALLOC_SIZE=0x80000' \
        '# CONFIG_IMX_TRUSTY_OS is not set' \
        '# CONFIG_IMX_HAB is not set' \
        '# CONFIG_SPL_FIT_SIGNATURE is not set'
    do
        grep -Fxq "$setting" "$falcon_build/.config" || \
            bbfatal "Unsupported Falcon SPL configuration: $setting"
    done
    if grep -Eq '^CONFIG_(SPL_ATF|SPL_OPTEE_IMAGE|SPL_FIT_IMAGE_POST_PROCESS)=y$' \
        "$falcon_build/.config"; then
        bbfatal "Unsupported Falcon SPL FIT dispatch configuration"
    fi
    install -m 0644 "$falcon_build/u-boot.dtb" \
        "${DEPLOYDIR}/iot-gate-imx8plus-falcon-control.dtb"
    (cd "${DEPLOYDIR}" && sha256sum iot-gate-imx8plus-falcon-control.dtb \
        > iot-gate-imx8plus-falcon-control.sha256)
}
