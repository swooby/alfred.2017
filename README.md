[![license](https://img.shields.io/github/license/mashape/apistatus.svg?maxAge=2592000)](https://raw.githubusercontent.com/swooby/alfred/m1/LICENSE)
[![Stories in Ready](https://badge.waffle.io/swooby/alfred.svg?label=ready&title=Ready)](http://waffle.io/swooby/alfred)
[![Build Status](https://travis-ci.org/swooby/alfred.svg?branch=master)](https://travis-ci.org/swooby/alfred)
[![codecov](https://codecov.io/gh/swooby/alfred/branch/m1/graph/badge.svg)](https://codecov.io/gh/swooby/alfred)

# Alfred: Personal Assistant [for Android]

<!--
[![license](https://img.shields.io/github/license/mashape/apistatus.svg?maxAge=2592000)](https://raw.githubusercontent.com/SmartFoo/smartfoo/master/LICENSE)
[![Stories in Ready](https://badge.waffle.io/SmartFoo/smartfoo.svg?label=ready&title=Ready)](http://waffle.io/SmartFoo/smartfoo)
[![Build Status](https://travis-ci.org/SmartFoo/smartfoo.svg?branch=master)](https://travis-ci.org/SmartFoo/smartfoo)
[![codecov](https://codecov.io/gh/SmartFoo/smartfoo/branch/master/graph/badge.svg)](https://codecov.io/gh/SmartFoo/smartfoo)
[![App](https://api.bintray.com/packages/smartfoo/maven/smartfoo-android-lib-core/images/download.svg)](https://bintray.com/smartfoo/maven/smartfoo-android-lib-core/_latestVersion)
-->

[Journal](JOURNAL.md)

Alfred is an Open Source personal project of mine to get my Android to
 eventually do all of the following [and more]:

1. Text-To-Speech all current/incoming notifications.
1. BT/BLE Scanner that detects when a button is pressed on certain BT/BLE devices.
1. Speech-To-Text various commands.
1. Do all of this in an always helpful non-annoying way.

The target audience is me: I ride my motorcycle a lot and do not have
 any desire to look at or manipulate the screen. I would like to know
 when my wife texts me and what she said, or when I get an important
 reminder/email/call. It is also nice to know any other useful
 information such as the current time, weather, traffic conditions, or
 current song that is playing; automatically attenuating the volume of
 commercials is another bonus.

Anyone that regularly finds themself using a hands-and-screen-free
environment might find this app useful.

## Milestones
\* = Not Yet Implemented

* M4: Speech-To-Text (aka: Voice Recognition & Commands)
* M3: BLE Scanner that detects button presses
* M2: More useful app state and UI
  This is where the app starts to crystallize in to something useful.
  * *Bluetooth Headset Listener and Profile Handling
  * *Offline Storage (Firebase?)
  * *Location Listener
  * *Analytics
      * *Volume up/down; time in song
      * *...
* M1: Generally reliable speaking of the notifications that I encounter on a daily basis.
  * Release to store
  * Sign TravisCI build
  * Upload TravisCI build to Store
  * Force disabled while Phone offhook
  * Notification Parsers:
    * Pandora
    * Spotify
  * Speak Volume, Cellular, WiFi, Screen, Charging, Phone states
  * Reliable startup and detection of profiles and notification access

### Issues:
1. Phone numbers may be spoken as "six billion one hundred ninety seven million nine hundred sixty six thousand two hundred ninety nine".
1. Notifications may repeat a lot (ex: MyGlass Connected, Chrome Media Playing, Google Play Updates, etc)
I have a temporary mitigation for this, but will hopefully eliminate this with a future refactor of NotificationParserManager.
1. 20170501: Not disabling when Profile == "Any" and turning off my Bluetooth Interphone F5MC
1. 20170501: DebugActivity Share doesn't sent file (99% certain this is just a permissions issue)

### Unscheduled Ideas (Unsorted; typically most recent at top):
* Better handling of startup on some problematic devices:
  * http://stackoverflow.com/a/41627296/252308
* Interop w/ http://x.ai?
* Support "Direct Boot"
  * https://developer.android.com/training/articles/direct-boot.html
  * https://android-developers.googleblog.com/2016/04/developing-for-direct-boot.html
* Option to not speak volume if screen unlocked (or some other way to defeat speech)
* Options to not speak some other things if screen unlocked
* Better Facebook messenger parsing
* How to detect phone boot and launch app without requiring screen unlock?
* Screen unlock/lock listener
* Button to test adding two notifications and reading them serially but cancelable
* Button to test repetitive notifications
* "Share to Alfred" could read whatever is being shared
* When you plug in, Alfred says "Good Morning|Afternoon|Evening Sir|Mame" and then runs down general status
* Speak "Shutting down" (and/or detect "Restarting") when power off/reboot detected
* Detect volume up/down and play bonk sound if at max/min
* https://developers.google.com/android/reference/com/google/android/gms/location/ActivityRecognitionApi
* https://docs.api.ai/
  * https://github.com/api-ai/apiai-android-client
* https://developers.google.com/actions/
  * https://github.com/actions-on-google/actions-on-google-nodejs
  * https://github.com/frogermcs/Google-Actions-Java-SDK
* http://stackoverflow.com/questions/41410211/how-to-open-maps-and-calls-using-action-recognize-speech
* Add ability to go in to Debug Mode
* Have Android debugging/charging parser add/remove announce charger stat (and remove ChargePortListener?)
* Media Button Controller or Notification Action to repeat last speech or specifically Song info.
  Maybe to also just enable/disable Profile?
* Bluetooth controls to like/add song or start new station from song
* Notification bottom text should indicate Notification Listener status
* Better queueing of Notification processing to only process/speak the next one after the current one finishes speaking
* DEBUG mode to better catch broken parsers
  * Notify parsers that don't have app installed
  * Shortcut/Action to install app
  * Listener for when app is updated
  * Extract resource from app and mock a notification to see if it still works.
* Log notification content to location
* Widget that allows shortcut of another name
* FooTTSBuilder equals and cache to not say if recently spoken
* Notification controls to disable/enable/snooze/etc
* Ongoing Notification w/ Action to snooze or disable (until morning?)
* Snooze for X minutes/hours
* Always snooze at this time
* Always snooze at this location
* Option to remind me every X minutes/hours that Alfred is disabled
* Option to announce time [quarter, bottom, or top of hour]
* Per-Parser User-Option to not announce app name?
* Per-Parser User-Option to turn off parser

### R&D:
* Google Now output Intents
  https://developer.android.com/guide/components/intents-common.html#Now
* Launch Google Now
  http://stackoverflow.com/questions/22585891/launch-google-now-or-phone-default-voice-search
  http://stackoverflow.com/questions/18049157/how-to-programmatically-initiate-a-google-now-voice-search/18052047#18052047
  http://stackoverflow.com/questions/22230937/how-can-i-launch-googlenow-by-an-intent-with-adding-a-search-query
  http://stackoverflow.com/questions/18049157/how-to-programmatically-initiate-a-google-now-voice-search
  https://www.reddit.com/r/tasker/comments/3eyezg/launch_google_now_via_intent/
* Voice Recognition

### Related Projects:
* https://github.com/futhrevo/SpeechTrials
* https://github.com/Kaljurand/speechutils
  * https://github.com/Kaljurand/K6nele
  * https://github.com/Kaljurand/Arvutaja
  * https://github.com/willblaschko/AlexaAndroid
* https://github.com/mayurkharche/VoiceAssistant
  Shows how to use ai.api.android.AIService
* https://github.com/AnnaForAndroid/ANNA
* https://github.com/abhi007tyagi/JARVIS

