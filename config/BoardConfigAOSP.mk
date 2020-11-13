include vendor/rs/config/BoardConfigKernel.mk
include vendor/rs/config/BoardConfigSoong.mk
include vendor/rs/config/BoardConfigQcom.mk
include vendor/rs/build/core/pathmap.mk

TARGET_SYSTEM_PROP += \
    vendor/rs/config/system.prop

# Tri-state-key layout
PRODUCT_COPY_FILES += \
    vendor/rs/config/keylayout/tri-state-key.kl:$(TARGET_COPY_OUT_VENDOR)/usr/keylayout/tri-state-key.kl
