# Use the ALSA-backed generic QAudioOutput implementation for target
# QSoundEffect warning audio; the product image does not run a Pulse server.
PACKAGECONFIG:remove = "pulseaudio"
PACKAGECONFIG:append = " alsa"
