# RIL Wrapper
PRODUCT_PACKAGES += \
    libril-wrapper

# Extra Apps
PRODUCT_PACKAGES += \
    SettingsExtra \
    LedManagerExtra

# Theme picker
PRODUCT_PACKAGES += \
    ThemePicker \
    ThemeStub

PRODUCT_PACKAGES += \
    messaging

# Tri-state-key
PRODUCT_PACKAGES += \
    tri-state-key_daemon

# Custom light HAL
PRODUCT_PACKAGES += \
    light_daemon

# Quick Access Wallet
PRODUCT_PACKAGES += \
    QuickAccessWallet

# Radio config, needed by RIL
PRODUCT_PACKAGES += \
    android.hardware.radio.config@1.0.vendor

# Netd
PRODUCT_PACKAGES += \
    android.system.net.netd@1.1.vendor

# Bluetooth
PRODUCT_PACKAGES += \
    android.hardware.bluetooth@1.0.vendor \
    android.hardware.bluetooth@1.1.vendor

PRODUCT_ENFORCE_RRO_TARGETS := framework-res

PRODUCT_VENDOR_KERNEL_HEADERS := device/oneplus/cheeseburger/kernel-headers

PRODUCT_SOONG_NAMESPACES += \
    vendor/qcom/opensource/data-ipa-cfg-mgr \
    vendor/qcom/opensource/dataservices

# Camera API1 ZSL
PRODUCT_PROPERTY_OVERRIDES += \
    camera.disable_zsl_mode=1

PRODUCT_BROKEN_VERIFY_USES_LIBRARIES := true

# Installs gsi keys into ramdisk, to boot a GSI with verified boot.
$(call inherit-product, $(SRC_TARGET_DIR)/product/gsi_keys.mk)
