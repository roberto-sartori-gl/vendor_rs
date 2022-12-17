install-recovery_path := VENDOR/bin/install-recovery.sh

intermediates := $(call intermediates-dir-for,PACKAGING,target_files)
name := $(TARGET_PRODUCT)
ifeq ($(TARGET_BUILD_TYPE),debug)
	  name := $(name)_debug
endif
target_files_name := $(name)-target_files-$(FILE_NAME_TAG)
ota_name := $(name)-ota-$(FILE_NAME_TAG)
target_files_zip_root := $(intermediates)/$(target_files_name)

INTERNAL_OTA_PACKAGE_TARGET_CUSTOM := $(PRODUCT_OUT)/$(name)-ota.zip
INTERNAL_OTA_METADATA_CUSTOM := $(PRODUCT_OUT)/ota_metadata_custom

$(INTERNAL_OTA_PACKAGE_TARGET_CUSTOM): KEY_CERT_PAIR := $(DEFAULT_KEY_CERT_PAIR)
$(INTERNAL_OTA_PACKAGE_TARGET_CUSTOM): .KATI_IMPLICIT_OUTPUTS := $(INTERNAL_OTA_METADATA_CUSTOM)
$(INTERNAL_OTA_PACKAGE_TARGET_CUSTOM): $(BUILT_TARGET_FILES_PACKAGE) $(OTA_FROM_TARGET_FILES) $(INTERNAL_OTATOOLS_FILES)
	@echo "Package OTA (custom): $@"
	$(hide) $(HOST_OUT_EXECUTABLES)/sign_target_files_apks -o --default_key_mappings vendor/rs/config/security/ $(BUILT_TARGET_FILES_PACKAGE) $(BUILT_TARGET_FILES_PACKAGE).signed
	$(hide) mv $(BUILT_TARGET_FILES_PACKAGE).signed $(BUILT_TARGET_FILES_PACKAGE)
	$(hide) rm -rf $(target_files_zip_root)
	$(hide) mkdir -p $(target_files_zip_root)
	$(hide) unzip -o $(BUILT_TARGET_FILES_PACKAGE) -d $(target_files_zip_root)/
	$(hide) echo "#!/vendor/bin/sh" > $(target_files_zip_root)/$(install-recovery_path)
	$(hide) echo "log -t recovery 'Recovery update is disabled!'" >> $(target_files_zip_root)/$(install-recovery_path)
	$(hide) cd $(target_files_zip_root) && zip -q -r ../$(target_files_name).zip $(install-recovery_path)
	$(hide) zip -q -d $(BUILT_TARGET_FILES_PACKAGE) 'IMAGES/*'
	$(hide) $(ADD_IMG_TO_TARGET_FILES) -a $(BUILT_TARGET_FILES_PACKAGE)
	$(hide) unzip -o -j $(BUILT_TARGET_FILES_PACKAGE) -d $(target_files_zip_root)/IMAGES 'IMAGES/*'
	$(call build-ota-package-target,$@,-k $(KEY_CERT_PAIR) --output_metadata_path $(INTERNAL_OTA_METADATA_CUSTOM))
	$(hide) mkdir -p $(target_files_zip_root)/OTA_TMP
	$(hide) unzip -o $@ -d $(target_files_zip_root)/OTA_TMP 'META-INF/com/google/android/updater-script'
	$(hide) sed -i 1d $(target_files_zip_root)/OTA_TMP/META-INF/com/google/android/updater-script
	$(hide) mv $@ $(target_files_zip_root)/OTA_TMP/tmp.zip
	$(hide) cd $(target_files_zip_root)/OTA_TMP && zip -q -u -r tmp.zip 'META-INF/com/google/android/updater-script'
	$(hide) java -jar -Djava.library.path="out/host/linux-x86/lib64" $(HOST_OUT_EXECUTABLES)/../framework/signapk.jar -w vendor/rs/config/security/releasekey.x509.pem vendor/rs/config/security/releasekey.pk8 $(target_files_zip_root)/OTA_TMP/tmp.zip $@
	$(hide) rm -rf $(target_files_zip_root)/OTA_TMP

.PHONY: otapackage_custom
otapackage_custom: $(INTERNAL_OTA_PACKAGE_TARGET_CUSTOM)
	@echo "Package Complete: $(INTERNAL_OTA_PACKAGE_TARGET_CUSTOM)" >&2
