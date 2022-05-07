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

$(INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY): $(BUILT_TARGET_FILES_PACKAGE) $(OTA_FROM_TARGET_FILES) $(INTERNAL_OTATOOLS_FILES)
	@echo "Package OTA without recovery: $@"
	$(hide) $(HOST_OUT_EXECUTABLES)/sign_target_files_apks -o --default_key_mappings vendor/rs/config/security/ $(BUILT_TARGET_FILES_PACKAGE) $(BUILT_TARGET_FILES_PACKAGE).signed
	$(hide) mv $(BUILT_TARGET_FILES_PACKAGE).signed $(BUILT_TARGET_FILES_PACKAGE)
	$(hide) rm -rf $(target_files_zip_root)
	$(hide) mkdir -p $(target_files_zip_root)
	$(hide) unzip -o $(BUILT_TARGET_FILES_PACKAGE) -d $(target_files_zip_root)/
	$(hide) echo "#!/vendor/bin/sh" > $(target_files_zip_root)/$(install-recovery_path)
	$(hide) echo "log -t recovery 'Recovery update is disabled!'" >> $(target_files_zip_root)/$(install-recovery_path)
	$(hide) cd $(target_files_zip_root) && zip -q -r ../$(target_files_name).zip $(install-recovery_path)
	$(hide) zip -q -d $(BUILT_TARGET_FILES_PACKAGE) 'IMAGES/vendor*'
	$(hide) $(ADD_IMG_TO_TARGET_FILES) -a $(BUILT_TARGET_FILES_PACKAGE)
	$(hide) $(HOST_OUT_EXECUTABLES)/ota_from_target_files $(BUILT_TARGET_FILES_PACKAGE) $@

.PHONY: otapackage_norecovery
otapackage_norecovery: $(INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY)
	@echo "Package Complete: $(INTERNAL_OTA_PACKAGE_TARGET_NO_RECOVERY)" >&2
