install-recovery_path := VENDOR/bin/install-recovery.sh

intermediates := $(call intermediates-dir-for,PACKAGING,target_files)
name := $(TARGET_PRODUCT)
ifeq ($(TARGET_BUILD_TYPE),debug)
	  name := $(name)_debug
endif
target_files_name := $(name)-target_files-$(FILE_NAME_TAG)
ota_name := $(name)-ota-$(FILE_NAME_TAG)
target_files_zip_root := $(intermediates)/$(target_files_name)

INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY := $(PRODUCT_OUT)/$(ota_name)-no_recovery.zip
INTERNAL_OTA_METADATA_NO_RECOVERY := $(PRODUCT_OUT)/ota_metadata_no_recovery

$(INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY): KEY_CERT_PAIR := $(DEFAULT_KEY_CERT_PAIR)
$(INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY): .KATI_IMPLICIT_OUTPUTS := $(INTERNAL_OTA_METADATA_NO_RECOVERY)
$(INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY): $(BUILT_TARGET_FILES_PACKAGE) $(OTA_FROM_TARGET_FILES) $(INTERNAL_OTATOOLS_FILES)
	@echo "Package OTA without recovery: $@"
	$(hide) echo "#!/vendor/bin/sh" > $(target_files_zip_root)/$(install-recovery_path)
	$(hide) echo "log -t recovery 'Recovery update is disabled!'" >> $(target_files_zip_root)/$(install-recovery_path)
	$(hide) cd $(target_files_zip_root) && zip -q -r ../$(target_files_name).zip $(install-recovery_path)
	$(hide) zip -q -d $(BUILT_TARGET_FILES_PACKAGE) 'IMAGES/*'
	$(hide) $(ADD_IMG_TO_TARGET_FILES) -a $(BUILT_TARGET_FILES_PACKAGE)
	$(hide) unzip -o -j $(BUILT_TARGET_FILES_PACKAGE) -d $(target_files_zip_root)/IMAGES 'IMAGES/*'
	$(call build-ota-package-target,$@,-k $(KEY_CERT_PAIR) --output_metadata_path $(INTERNAL_OTA_METADATA_NO_RECOVERY))

.PHONY: otapackage_norecovery
otapackage_norecovery: $(INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY)
	@echo "Package Complete: $(INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY)" >&2
