#!/bin/bash

# 重命名 release apk 文件

VERSION_NAME="$(grep 'versionName' app/build.gradle.kts | awk -F '"' '{print $2}' | sed 's/-/\./g')"
ORIGINAL_APK="app/release/app-release.apk"
NEW_APK="app/release/ink.chyk.neuqrcode.${VERSION_NAME}.apk"

if [ -f $ORIGINAL_APK ]; then
    mv $ORIGINAL_APK $NEW_APK
    echo "Rename $ORIGINAL_APK to $NEW_APK"
else
    echo "File $ORIGINAL_APK not found"
fi