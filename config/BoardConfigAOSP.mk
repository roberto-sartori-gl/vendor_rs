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

include device/lineage/sepolicy/common/sepolicy.mk
BOARD_SEPOLICY_DIRS += vendor/rs/sepolicy

SKIP_BOOT_JARS_CHECK := true

# Explicitly disable AVB
BOARD_AVB_ENABLE := false

# Used to avoid getting errors for stuff we are not actually building
# Remove when and if possible
ALLOW_MISSING_DEPENDENCIES := true

# Allow include prebuilt binaries as on A11
BUILD_BROKEN_ELF_PREBUILT_PRODUCT_COPY_FILES := true

# Allow building with broken sysprop SELinux policies
BUILD_BROKEN_ENFORCE_SYSPROP_OWNER := true

# Recovery
TARGET_RECOVERY_UI_LIB := librecovery_ui_custom

# Power
TARGET_TAP_TO_WAKE_NODE := "/proc/touchpanel/double_tap_enable"
TARGET_USES_INTERACTION_BOOST := true
