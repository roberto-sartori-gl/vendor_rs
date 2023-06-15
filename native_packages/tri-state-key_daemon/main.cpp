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
#include <iostream>
#include <fstream>
#include <string>
#include "uevent_listener.h"

using android::Uevent;
using android::UeventListener;
int main()
{
    UeventListener uevent_listener;
    LOG(DEBUG) << "Started";
    uevent_listener.Poll([](const Uevent &uevent) {
        if (uevent.action == "change" && uevent.path == "/devices/virtual/switch/tri-state-key")
        {
            LOG(DEBUG) << "tri-state-key event detected";
            std::string line;
            int state = 0;
            std::ifstream keystate_node("/sys/devices/virtual/switch/tri-state-key/state");
            if (keystate_node.is_open())
            {
                while (std::getline(keystate_node, line))
                {
                    state = std::stoi(line);
                }
                keystate_node.close();
            }

            if (state == 1)
            {
                // Silent mode - slider up
                system("setprop sys.slider_up.vibrate 1");
                system("service call audio 9 i32 3 i32 -100 i32 0");
                system("service call audio 40 i32 0 s16 \"shell\"");
            }
            else if (state == 2)
            {
                // Vibration mode - slider middle
                system("setprop sys.slider_middle.vibrate 1");
                system("service call audio 9 i32 3 i32 100 i32 0");
                system("service call audio 40 i32 1 s16 \"shell\"");
            }
            else if (state == 3)
            {
                // Normal mode - slider down
                system("service call audio 9 i32 3 i32 100 i32 0");
                system("service call audio 40 i32 2 s16 \"shell\"");
            }
        }
        return;
    });
}
