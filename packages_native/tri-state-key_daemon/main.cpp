/*
 * Copyright (C) 2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#define LOG_TAG "tri-state-key_daemon"
#include <android-base/logging.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <unistd.h>
#include "uevent_listener.h"

using android::Uevent;
using android::UeventListener;
int main() {
    UeventListener uevent_listener;
    LOG(DEBUG) << "Started";
    uevent_listener.Poll([](const Uevent& uevent) {

        if (uevent.action == "change" && uevent.path == "/devices/virtual/switch/tri-state-key") {
            LOG(DEBUG) << "tri-state-key event detected, launching broadcast...";
            system("am broadcast -a com.oneplus.TRI_STATE_EVENT");
        }
        return;
    });
}

