# android-mrz-reader [![Build Status](https://travis-ci.com/mercuriete/android-mrz-reader.svg?branch=master)](https://travis-ci.com/mercuriete/android-mrz-reader)
MRZ camera reader

Only recognize letters "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<" appearing in Machine Readable Zone of Passports and other IDs.

Runs the Tesseract OCR engine using [tess-two](https://github.com/rmtheis/tess-two), a fork of Tesseract Tools for Android, with or without Cube.

https://github.com/rmtheis has adapted most of the code making up the core structure of this project from the ZXing Barcode Scanner. Along with Tesseract-OCR and Tesseract Tools for Android (tesseract-android-tools), several open source projects have been used in this project, including leptonica, google-api-translate-java, microsoft-translator-java-api, and jtar.
 
## Installation

To build and run the app, I clone this project once for all, then on vim I execute the noremap on next line, then in android-studio, I use File/Close project, "terminate process", reopen this thumb project, use "run"

The noremap is: noremap à :w<bar>!rsync -avz /data/data/com.termux.nix/files/home/p/r/K/INFORMATIQUE/android/android-ocr-live-scanner* mol7_adb: ; cvs commit -m sauver<c-m> 

## Running

To switch on or off the Torch, tap anywhere far from the resizable OCR'ed area of the live image.

## License

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

    /*
     * Copyright 2011 Robert Theis
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

One of the jar files in the android/libs directory (google-api-translate-java-0.98-mod2.jar) is licensed under the [GNU Lesser GPL](http://www.gnu.org/licenses/lgpl.html).
# tested with:
Android Studio Giraffe | 2022.3.1 Patch 2
Build #AI-223.8836.35.2231.10811636, built on September 14, 2023


Runtime version: 17.0.6+0-17.0.6b829.9-10027231 amd64
VM: OpenJDK 64-Bit Server VM by JetBrains s.r.o.

