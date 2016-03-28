# Bluetooth LE Analyzer

[![Build Status](https://travis-ci.org/akinaru/bluetooth-le-analyzer.svg?branch=master)](https://travis-ci.org/akinaru/bluetooth-le-analyzer)
[![License](http://badge.kloud51.com/pypi/l/html2text.svg)](LICENSE.md)


[![Download Bluetooth LE Analyzer from Google Play](http://www.android.com/images/brand/android_app_on_play_large.png)](https://play.google.com/store/apps/details?id=com.github.akinaru.rfdroid)
[![Download latest debug from drone.io](https://raw.githubusercontent.com/kageiit/images-host/master/badges/drone-io-badge.png)](https://drone.io/github.com/akinaru/bluetooth-le-analyzer/files/bleanalyzer/app/build/outputs/apk/app-debug.apk)


Android app showing Bluetooth advertising packets and measuring reception rate using one RFduino module

![screenshot](img/screen.gif)

## Build

### Get code source

```
git clone git@github.com:akinaru/bluetooth-le-analyzer.git
cd bluetooth-le-analyzer
git submodule update --init --recursive
```

### Build Android App

```
./gradlew clean build
```

### Build & Upload RFduino software

```
make
```

refer to https://github.com/akinaru/rfduino-makefile for troubleshoot

## External projects

* BLE AD frame parser : https://github.com/TakahikoKawasaki/nv-bluetooth

* DiscreteSeekbar : https://github.com/AnderWeb/discreteSeekBar

* MPAndroidChart : https://github.com/PhilJay/MPAndroidChart

* Makefile for rfduino : https://github.com/akinaru/rfduino-makefile

* RFduino : https://github.com/RFduino/RFduino

## Useful links

Thanks to Tolson for his Lazarus Library

* http://forum.rfduino.com/index.php?topic=801.0

## License

```
Copyright (C) 2016  Bertrand Martel

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

Foobar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
```