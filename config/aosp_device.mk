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

PRODUCT_ENFORCE_RRO_TARGETS := framework-res

PRODUCT_VENDOR_KERNEL_HEADERS := device/oneplus/cheeseburger/kernel-headers

PRODUCT_SOONG_NAMESPACES += \
    vendor/qcom/opensource/data-ipa-cfg-mgr \
    vendor/qcom/opensource/dataservices

# Camera API1 ZSL
PRODUCT_PROPERTY_OVERRIDES += \
    camera.disable_zsl_mode=1
