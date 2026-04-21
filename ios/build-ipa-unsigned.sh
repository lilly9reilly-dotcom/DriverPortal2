#!/bin/sh
# Build an unsigned IPA locally on macOS without an Apple Developer account.
# The resulting IPA can be re-signed later (e.g. via Sideloadly, AltStore, or
# `codesign` with your own provisioning profile) to install on a real iPhone.
set -eu

PROJECT_NAME="DriverPortalIOS"
SCHEME="DriverPortalIOS"
CONFIGURATION="Release"
ARCHIVE_PATH="build/${PROJECT_NAME}.xcarchive"
IPA_DIR="build/ipa"

echo "Generating Xcode project..."
xcodegen generate

echo "Cleaning previous build output..."
rm -rf build

echo "Archiving app (unsigned)..."
xcodebuild \
  -project "${PROJECT_NAME}.xcodeproj" \
  -scheme "${SCHEME}" \
  -configuration "${CONFIGURATION}" \
  -destination generic/platform=iOS \
  -archivePath "${ARCHIVE_PATH}" \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO \
  archive

APP_PATH="${ARCHIVE_PATH}/Products/Applications/${PROJECT_NAME}.app"
if [ ! -d "${APP_PATH}" ]; then
  echo "App bundle not found at ${APP_PATH}" >&2
  exit 1
fi

echo "Packaging unsigned IPA..."
mkdir -p "${IPA_DIR}/Payload"
cp -R "${APP_PATH}" "${IPA_DIR}/Payload/"
( cd "${IPA_DIR}" && zip -qr "${PROJECT_NAME}-unsigned.ipa" Payload && rm -rf Payload )

echo "Unsigned IPA created at ${IPA_DIR}/${PROJECT_NAME}-unsigned.ipa"
