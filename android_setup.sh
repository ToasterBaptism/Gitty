#!/usr/bin/env bash
set -euo pipefail

# Idempotent Android dev environment setup for Linux
# - JDK 17
# - Android SDK, cmdline-tools, platforms (33,34), build-tools, platform-tools
# - Emulator + system image + AVD
# - NDK + CMake
# - Gradle (global) and Android Studio
# - Env vars, licenses, basic validations

# Determine sudo availability
if command -v sudo >/dev/null 2>&1; then
  SUDO="sudo"
else
  SUDO=""
fi

log() {
  echo "[SETUP] $*"
}

export DEBIAN_FRONTEND=noninteractive

log "Updating apt and installing base dependencies"
$SUDO apt-get update -y
$SUDO apt-get install -y --no-install-recommends \
  ca-certificates curl wget unzip zip tar xz-utils git \
  lib32stdc++6 lib32z1 libc6-i386 \
  libgl1-mesa-glx libglu1-mesa mesa-utils \
  qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils \
  python3 \
  build-essential ninja-build rsync \
  software-properties-common

log "Installing JDK 17"
if ! java -version 2>&1 | grep -q "17"; then
  $SUDO apt-get install -y openjdk-17-jdk
fi
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))

log "Preparing Android SDK directories"
ANDROID_SDK_ROOT=/opt/android-sdk
ANDROID_HOME=$ANDROID_SDK_ROOT
CMDLINE_DIR="$ANDROID_SDK_ROOT/cmdline-tools/latest"
if [ ! -d "$ANDROID_SDK_ROOT" ]; then
  $SUDO mkdir -p "$ANDROID_SDK_ROOT"
  $SUDO chown -R $(id -u):$(id -g) "$ANDROID_SDK_ROOT"
fi

log "Installing Android cmdline-tools"
if [ ! -d "$CMDLINE_DIR" ]; then
  TMPDIR=$(mktemp -d)
  pushd "$TMPDIR" >/dev/null
  URL=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  wget -q "$URL" -O cmdline.zip
  unzip -q cmdline.zip -d cmdline
  mkdir -p "$CMDLINE_DIR"
  mv cmdline/cmdline-tools/* "$CMDLINE_DIR"/
  popd >/dev/null
  rm -rf "$TMPDIR"
fi

export ANDROID_SDK_ROOT ANDROID_HOME PATH
export PATH="$CMDLINE_DIR/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"

log "Installing SDK components via sdkmanager"
which sdkmanager >/dev/null 2>&1 || { echo "sdkmanager not found in PATH"; exit 1; }

yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null || true
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" \
  "emulator" \
  "platforms;android-34" \
  "platforms;android-33" \
  "build-tools;34.0.0" \
  "build-tools;33.0.2" \
  "system-images;android-34;google_apis_playstore;x86_64" \
  "sources;android-34" \
  "sources;android-33" \
  "extras;google;google_play_services" \
  "ndk;26.3.11579264" \
  "cmake;3.22.1"

log "Accepting all SDK licenses"
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null || true

log "Creating AVD if missing"
AVD_NAME=Pixel_7_API_34
AVD_DIR="$HOME/.android/avd/${AVD_NAME}.avd"
mkdir -p "$HOME/.android"
if ! grep -q "^$AVD_NAME$" <(avdmanager list avd | sed -n 's/^\s\+Name: \(.*\)$/\1/p'); then
  echo no | avdmanager create avd -n "$AVD_NAME" -k "system-images;android-34;google_apis_playstore;x86_64" --device "pixel_7" --abi x86_64 --force
fi

log "Installing Gradle 8.7 globally"
GRADLE_VER=8.7
GRADLE_DIR=/opt/gradle/gradle-$GRADLE_VER
if [ ! -d "$GRADLE_DIR" ]; then
  TMPDIR=$(mktemp -d)
  pushd "$TMPDIR" >/dev/null
  wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VER}-bin.zip
  $SUDO mkdir -p /opt/gradle
  $SUDO unzip -q gradle-${GRADLE_VER}-bin.zip -d /opt/gradle
  popd >/dev/null
  rm -rf "$TMPDIR"
fi
export PATH="/opt/gradle/gradle-${GRADLE_VER}/bin:$PATH"

log "Installing Android Studio"
STUDIO_DIR=/opt/android-studio
if [ ! -d "$STUDIO_DIR" ]; then
  TMPDIR=$(mktemp -d)
  pushd "$TMPDIR" >/dev/null
  STUDIO_URL=https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2024.2.1.12/android-studio-2024.2.1.12-linux.tar.gz
  wget -q "$STUDIO_URL" -O studio.tgz || wget -q https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2024.1.1.12/android-studio-2024.1.1.12-linux.tar.gz -O studio.tgz
  $SUDO mkdir -p /opt
  $SUDO tar -xzf studio.tgz -C /opt
  $SUDO mv /opt/android-studio* "$STUDIO_DIR" || true
  popd >/dev/null
  rm -rf "$TMPDIR"
fi
if [ ! -e /usr/local/bin/studio ]; then
  echo "#!/usr/bin/env bash" | $SUDO tee /usr/local/bin/studio >/dev/null
  echo "export ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT" | $SUDO tee -a /usr/local/bin/studio >/dev/null
  echo "export JAVA_HOME=$JAVA_HOME" | $SUDO tee -a /usr/local/bin/studio >/dev/null
  echo 'exec /opt/android-studio/bin/studio "$@"' | $SUDO tee -a /usr/local/bin/studio >/dev/null
  $SUDO chmod +x /usr/local/bin/studio
fi

log "Writing environment script to /etc/profile.d/android_env.sh"
ENV_SCRIPT=/etc/profile.d/android_env.sh
$SUDO bash -lc "cat > $ENV_SCRIPT <<ENVEOF
export JAVA_HOME=$JAVA_HOME
export ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT
export ANDROID_HOME=$ANDROID_SDK_ROOT
export PATH=$CMDLINE_DIR/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:/opt/gradle/gradle-$GRADLE_VER/bin:\$PATH
ENVEOF"

log "Validating tool versions"
java -version
javac -version
gradle -v | head -n 5
sdkmanager --version || true

log "Booting emulator headlessly for smoke test (60s budget)"
"$ANDROID_SDK_ROOT/emulator/emulator" -avd "$AVD_NAME" -no-window -gpu swiftshader_indirect -no-snapshot -no-boot-anim -accel off -camera-back none -camera-front none -netdelay none -netspeed full -memory 2048 -noaudio -no-accel 1>/tmp/emulator.log 2>&1 &
EMUPID=$!
SECONDS=0
BOOTED=0
while [ $SECONDS -lt 60 ]; do
  if adb wait-for-device 1>/dev/null 2>&1; then
    BOOTPROP=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || true
    if [ "$BOOTPROP" = "1" ]; then
      BOOTED=1
      break
    fi
  fi
  sleep 3
  SECONDS=$((SECONDS+3))

done
if [ $BOOTED -eq 1 ]; then
  log "Emulator booted"
else
  log "Emulator did not fully boot within timeout; continuing"
fi
kill $EMUPID 2>/dev/null || true
sleep 2

log "Setup complete"