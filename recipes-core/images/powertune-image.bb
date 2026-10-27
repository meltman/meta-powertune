require recipes-fsl/images/imx-image-core.bb

SUMMARY = "PowerTune image for CompuLab IOT-GATE-iMX8Plus"

IMAGE_BASENAME = "powertune-image"

IMAGE_FEATURES:remove = "ptest-pkgs"

EXTRA_IMAGE_FEATURES:remove = "ptest-pkgs"

IMAGE_INSTALL:append = " \
    bash \
    bash-completion \
    vim-tiny \
    nano \
    less \
    tmux \
    screen \
    htop \
    tree \
    file \
    procps \
    rsync \
    git \
    sudo \
    openssh \
    openssh-sftp-server \
    gdb \
    gdbserver \
    strace \
    lsof \
    tcpdump \
    ethtool \
    iproute2 \
    net-tools \
    curl \
    wget \
    jq \
    can-utils \
    gpsd \
    gpsd-machine-conf \
    modemmanager \
    minicom \
    picocom \
    i2c-tools \
    usbutils \
    pciutils \
    unzip \
    zip \
    sysstat \
    packagegroup-powertune-qt5-runtime \
"
