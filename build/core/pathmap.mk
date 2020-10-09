pathmap_PROJ := \
    qcom-audio:hardware/qcom-caf/msm8998/audio \
    qcom-media:hardware/qcom-caf/msm8998/media \
    qcom-wlan:hardware/qcom-caf/wlan \
    qcom-display:hardware/qcom-caf/msm8998/display \
    qcom-bt-vendor:hardware/qcom-caf/bt \
    qcom-data-ipa-cfg-mgr:vendor/qcom/opensource/data-ipa-cfg-mgr \
    qcom-dataservices:vendor/qcom/opensource/dataservices \
    qcom-thermal-hardware/qcom-caf/thermal \
    qcom-vr:hardware/qcom-caf/vr

#
# Returns the path to the requested module's include directory,
# relative to the root of the source tree.  Does not handle external
# modules.
#
# $(1): a list of modules (or other named entities) to find the includes for
#
define include-path-for
$(foreach n,$(1),$(patsubst $(n):%,%,$(filter $(n):%,$(pathmap_INCL))))
endef

# Enter project path into pathmap
#
# $(1): name
# $(2): path
#
define project-set-path
$(eval pathmap_PROJ += $(1):$(2))
endef

# Returns the path to the requested module's include directory,
# relative to the root of the source tree.
#
# $(1): a list of modules (or other named entities) to find the projects for
define project-path-for
$(foreach n,$(1),$(patsubst $(n):%,%,$(filter $(n):%,$(pathmap_PROJ))))
endef

include vendor/rs/build/core/vendor/qcom_boards.mk
include vendor/rs/build/core/utils.mk
