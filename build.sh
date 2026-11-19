#!/bin/bash
# Builds AutoSkip.apk using only a JDK and the Android SDK's own tools
# (no Gradle, no Android Studio).
#
# Needs: a JDK, plus an Android SDK with "build-tools" and a "platforms"
# package installed. The SDK is looked up in $ANDROID_HOME, then in
# ~/Library/Android/sdk.
set -euo pipefail
cd "$(dirname "$0")"

MIN_SDK=26
TARGET_SDK=35
VERSION_CODE=2
VERSION_NAME=1.1

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BUILD_TOOLS="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1 || true)"
PLATFORM="$(ls -d "$SDK"/platforms/android-* 2>/dev/null | sort -V | tail -1 || true)"
if [ -z "$BUILD_TOOLS" ] || [ -z "$PLATFORM" ]; then
  echo "Android SDK build-tools/platforms not found under: $SDK" >&2
  echo "Set ANDROID_HOME to your SDK folder." >&2
  exit 1
fi
ANDROID_JAR="$PLATFORM/android.jar"
echo "SDK tools: $BUILD_TOOLS"
echo "Platform:  $PLATFORM"

OUT=build
rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== Resources"
"$BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o "$OUT/res.zip"
"$BUILD_TOOLS/aapt2" link -o "$OUT/unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest app/src/main/AndroidManifest.xml \
  --java "$OUT/gen" \
  --min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  "$OUT/res.zip"

echo "== Compile"
find app/src/main/java "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac --release 8 -Xlint:-options -classpath "$ANDROID_JAR" -d "$OUT/classes" @"$OUT/sources.txt"

echo "== Dex"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BUILD_TOOLS/d8" --release --lib "$ANDROID_JAR" --min-api "$MIN_SDK" \
  --output "$OUT/dex" @"$OUT/classes.txt"

echo "== Package and sign"
(cd "$OUT/dex" && zip -q -j ../unsigned.apk classes.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

# One signing key, kept next to the project, so later builds install as updates.
KEYSTORE=debug.keystore
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android \
    -alias autoskip -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=AutoSkip Debug" >/dev/null 2>&1
fi
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
  --out AutoSkip.apk "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" verify AutoSkip.apk

echo "Built: $(pwd)/AutoSkip.apk"
