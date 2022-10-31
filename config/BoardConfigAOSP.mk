include vendor/rs/config/BoardConfigSoong.mk
include vendor/rs/config/BoardConfigQcom.mk
include vendor/rs/build/core/pathmap.mk

TARGET_SYSTEM_PROP += \
    vendor/rs/config/system.prop

DEVICE_PACKAGE_OVERLAYS += vendor/rs/overlay

# Styles & wallpapers
PRODUCT_COPY_FILES += \
    vendor/rs/config/permissions/privapp_whitelist_com.android.wallpaper.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/privapp_whitelist_com.android.wallpaper.xml \
    vendor/rs/config/permissions/default_com.android.wallpaper.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/default-permissions/default_com.android.wallpaper.xml

# Custom init.rc with all the instructions that needs to be run from the system partition
PRODUCT_COPY_FILES += \
    vendor/rs/prebuilt/init.aosp.rc:$(TARGET_COPY_OUT_SYSTEM)/etc/init/init.aosp.rc

# Custom init.rc with instructions that needs to be run from the vendor partition
PRODUCT_COPY_FILES += \
    vendor/rs/prebuilt/init.vendor.aosp.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.vendor.aosp.rc

# Prebuilt kernel
PRODUCT_COPY_FILES += \
    kernel/oneplus/prebuilt/Image.gz-dtb:kernel

# Init rc files for recovery
PRODUCT_COPY_FILES += \
    vendor/rs/config/recovery/root/init.recovery.qcom.rc:root/init.recovery.qcom.rc \
    vendor/rs/config/recovery/root/init.recovery.qcom.usb.rc:root/init.recovery.qcom.usb.rc

include device/lineage/sepolicy/common/sepolicy.mk
BOARD_SEPOLICY_DIRS += vendor/rs/sepolicy

# Explicitly disable AVB
BOARD_AVB_ENABLE := false

# Recovery
TARGET_RECOVERY_UI_LIB := librecovery_ui_custom

# Power
TARGET_TAP_TO_WAKE_NODE := "/proc/touchpanel/double_tap_enable"
TARGET_USES_INTERACTION_BOOST := true

# Disable extended compress format
AUDIO_FEATURE_ENABLED_EXTENDED_COMPRESS_FORMAT := false

# VINTF
DEVICE_MANIFEST_FILE += vendor/rs/config/vintf/android.hardware.vibrator_v1.0.xml
