include vendor/rs/config/BoardConfigKernel.mk
include vendor/rs/config/BoardConfigSoong.mk
include vendor/rs/config/BoardConfigQcom.mk
include vendor/rs/build/core/pathmap.mk

TARGET_SYSTEM_PROP += \
    vendor/rs/config/system.prop

DEVICE_PACKAGE_OVERLAYS += vendor/rs/overlay

# Tri-state-key layout
PRODUCT_COPY_FILES += \
    vendor/rs/config/keylayout/tri-state-key.kl:$(TARGET_COPY_OUT_VENDOR)/usr/keylayout/tri-state-key.kl

# Styles & wallpapers
PRODUCT_COPY_FILES += \
    vendor/rs/config/permissions/privapp_whitelist_com.android.wallpaper.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/privapp_whitelist_com.android.wallpaper.xml \
    vendor/rs/config/permissions/default_com.android.wallpaper.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/default-permissions/default_com.android.wallpaper.xml

include device/lineage/sepolicy/common/sepolicy.mk
BOARD_SEPOLICY_DIRS += vendor/rs/sepolicy

SKIP_BOOT_JARS_CHECK := true
