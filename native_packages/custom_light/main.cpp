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
#define LOG_TAG "light_daemon"
#include <android-base/logging.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <unistd.h>
#include <iostream>
#include <fstream>
#include <string>
#include <unistd.h>

#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/_system_properties.h>

const static std::string kRedLedPath = "/sys/class/leds/red/brightness";
const static std::string kGreenLedPath = "/sys/class/leds/green/brightness";
const static std::string kBlueLedPath = "/sys/class/leds/blue/brightness";
const static std::string kRedDutyPctsPath = "/sys/class/leds/red/duty_pcts";
const static std::string kGreenDutyPctsPath = "/sys/class/leds/green/duty_pcts";
const static std::string kBlueDutyPctsPath = "/sys/class/leds/blue/duty_pcts";
const static std::string kRedStartIdxPath = "/sys/class/leds/red/start_idx";
const static std::string kGreenStartIdxPath = "/sys/class/leds/green/start_idx";
const static std::string kBlueStartIdxPath = "/sys/class/leds/blue/start_idx";
const static std::string kRedPauseLoPath = "/sys/class/leds/red/pause_lo";
const static std::string kGreenPauseLoPath = "/sys/class/leds/green/pause_lo";
const static std::string kBluePauseLoPath = "/sys/class/leds/blue/pause_lo";
const static std::string kRedPauseHiPath = "/sys/class/leds/red/pause_hi";
const static std::string kGreenPauseHiPath = "/sys/class/leds/green/pause_hi";
const static std::string kBluePauseHiPath = "/sys/class/leds/blue/pause_hi";
const static std::string kRedRampStepMsPath = "/sys/class/leds/red/ramp_step_ms";
const static std::string kGreenRampStepMsPath = "/sys/class/leds/green/ramp_step_ms";
const static std::string kBlueRampStepMsPath = "/sys/class/leds/blue/ramp_step_ms";
const static std::string kRedBlinkPath = "/sys/class/leds/red/blink";
const static std::string kGreenBlinkPath = "/sys/class/leds/green/blink";
const static std::string kBlueBlinkPath = "/sys/class/leds/blue/blink";
const static std::string kRgbBlinkPath = "/sys/class/leds/rgb/rgb_blink";

int main()
{
    char red_light[128];
    __system_property_get("sys.red_light", red_light);
    char blue_light[128];
    __system_property_get("sys.blue_light", blue_light);
    char green_light[128];
    __system_property_get("sys.green_light", green_light);
    char blink[1];
    __system_property_get("sys.blink_light", blink);

    const char del[2] = "-";

    char *red_config[20];
    int iCurName = 0;

    char *token_red;

    /* get the first token */
    token_red = strtok(red_light, del);

    /* walk through other tokens */
    while (token_red != NULL)
    {
        red_config[iCurName] = (char *)malloc(strlen(token_red) + 1);
        strcpy(red_config[iCurName], token_red);
        iCurName++;

        token_red = strtok(NULL, del);
    }

    char *green_config[20];
    iCurName = 0;

    char *token_green;

    /* get the first token */
    token_green = strtok(green_light, del);

    /* walk through other tokens */
    while (token_green != NULL)
    {
        green_config[iCurName] = (char *)malloc(strlen(token_green) + 1);
        strcpy(green_config[iCurName], token_green);
        iCurName++;

        token_green = strtok(NULL, del);
    }

    char *blue_config[20];
    iCurName = 0;

    char *token_blue;

    /* get the first token */
    token_blue = strtok(blue_light, del);

    /* walk through other tokens */
    while (token_blue != NULL)
    {
        blue_config[iCurName] = (char *)malloc(strlen(token_blue) + 1);
        strcpy(blue_config[iCurName], token_blue);
        iCurName++;

        token_blue = strtok(NULL, del);
    }
    // get all the values we need
    int blink_status = atoi(blink);

    std::ofstream blink_rgb_stream;
    blink_rgb_stream.open(kRgbBlinkPath);

    blink_rgb_stream << "0" << std::endl;
    blink_rgb_stream.close();

    if (blink_status == 0)
    {

        std::ofstream blink_red_stream;
        blink_red_stream.open(kRedBlinkPath);
        std::ofstream blink_blue_stream;
        blink_blue_stream.open(kBlueBlinkPath);
        std::ofstream blink_green_stream;
        blink_green_stream.open(kGreenBlinkPath);
        std::ofstream brightness_red_stream;
        brightness_red_stream.open(kRedLedPath);
        std::ofstream brightness_blue_stream;
        brightness_blue_stream.open(kBlueLedPath);
        std::ofstream brightness_green_stream;
        brightness_green_stream.open(kGreenLedPath);
        blink_red_stream << blink_status;
        blink_blue_stream << blink_status;
        blink_green_stream << blink_status;
        brightness_red_stream << red_config[0];
        brightness_blue_stream << blue_config[0];
        brightness_green_stream << green_config[0];
        blink_red_stream.close();
        blink_blue_stream.close();
        blink_green_stream.close();
        brightness_red_stream.close();
        brightness_blue_stream.close();
        brightness_green_stream.close();
    }
    else if (blink_status == 1)
    {
        std::ofstream mStartIdx_red_status;
        mStartIdx_red_status.open(kRedStartIdxPath);
        std::ofstream mStartIdx_blue_status;
        mStartIdx_blue_status.open(kBlueStartIdxPath);
        std::ofstream mStartIdx_green_status;
        mStartIdx_green_status.open(kGreenStartIdxPath);

        std::ofstream mDutyPcts_red_status;
        mDutyPcts_red_status.open(kRedDutyPctsPath);
        std::ofstream mDutyPcts_blue_status;
        mDutyPcts_blue_status.open(kBlueDutyPctsPath);
        std::ofstream mDutyPcts_green_status;
        mDutyPcts_green_status.open(kGreenDutyPctsPath);

        std::ofstream mPauseLo_red_status;
        mPauseLo_red_status.open(kRedPauseLoPath);
        std::ofstream mPauseLo_blue_status;
        mPauseLo_blue_status.open(kBluePauseLoPath);
        std::ofstream mPauseLo_green_status;
        mPauseLo_green_status.open(kGreenPauseLoPath);

        std::ofstream mPauseHi_red_status;
        mPauseHi_red_status.open(kRedPauseHiPath);
        std::ofstream mPauseHi_blue_status;
        mPauseHi_blue_status.open(kBluePauseHiPath);
        std::ofstream mPauseHi_green_status;
        mPauseHi_green_status.open(kGreenPauseHiPath);

        std::ofstream mRampStepMs_red_status;
        mRampStepMs_red_status.open(kRedRampStepMsPath);
        std::ofstream mRampStepMs_blue_status;
        mRampStepMs_blue_status.open(kBlueRampStepMsPath);
        std::ofstream mRampStepMs_green_status;
        mRampStepMs_green_status.open(kGreenRampStepMsPath);

        mStartIdx_red_status << red_config[1];
        mStartIdx_blue_status << blue_config[1];
        mStartIdx_green_status << green_config[1];

        mDutyPcts_red_status << red_config[2];
        mDutyPcts_blue_status << blue_config[2];
        mDutyPcts_green_status << green_config[2];

        mPauseLo_red_status << red_config[3];
        mPauseLo_blue_status << blue_config[3];
        mPauseLo_green_status << green_config[3];

        mPauseHi_red_status << red_config[4];
        mPauseHi_blue_status << blue_config[4];
        mPauseHi_green_status << green_config[4];

        mRampStepMs_red_status << red_config[5];
        mRampStepMs_blue_status << blue_config[5];
        mRampStepMs_green_status << green_config[5];

        mStartIdx_red_status.close();
        mStartIdx_blue_status.close();
        mStartIdx_green_status.close();
        mDutyPcts_blue_status.close();
        mDutyPcts_red_status.close();
        mDutyPcts_green_status.close();
        mPauseLo_blue_status.close();
        mPauseLo_red_status.close();
        mPauseLo_green_status.close();
        mPauseHi_blue_status.close();
        mPauseHi_red_status.close();
        mPauseHi_green_status.close();
        mRampStepMs_red_status.close();
        mRampStepMs_blue_status.close();
        mRampStepMs_green_status.close();

        std::ofstream blink_rgb_stream_f;
        blink_rgb_stream_f.open(kRgbBlinkPath);
        blink_rgb_stream_f << "1" << std::endl;
        blink_rgb_stream_f.close();
    }
    return 0;
}
