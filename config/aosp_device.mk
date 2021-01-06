# RIL Wrapper
PRODUCT_PACKAGES += \
    libril-wrapper

# SettingsExtra
PRODUCT_PACKAGES += \
    SettingsExtra

# Theme picker
PRODUCT_PACKAGES += \
    ThemePicker \
    ThemeStub

PRODUCT_PACKAGES += \
    messaging

# Stub package to be use to remove other packages
PRODUCT_PACKAGES += \
    PackagesRemover

PRODUCT_ENFORCE_RRO_TARGETS := framework-res

PRODUCT_VENDOR_KERNEL_HEADERS := device/oneplus/cheeseburger/kernel-headers

PRODUCT_SOONG_NAMESPACES += \
    vendor/qcom/opensource/data-ipa-cfg-mgr

# Camera API1 ZSL
PRODUCT_PROPERTY_OVERRIDES += \
    camera.disable_zsl_mode=1

# Custom .rc file
PRODUCT_COPY_FILES += \
    vendor/rs/config/aosp.rc:system/etc/init/aosp.rc

# Slider key events monitor service
PRODUCT_COPY_FILES += \
    vendor/rs/config/tri-state/tri-state-key_event_up:system/bin/tri-state-key_event_up \
    vendor/rs/config/tri-state/tri-state-key_event_middle:system/bin/tri-state-key_event_middle \
    vendor/rs/config/tri-state/tri-state-key_event_down:system/bin/tri-state-key_event_down
