#!/bin/sh
set -eu

PROJECT_NAME="DriverPortalIOS"
SCHEME="DriverPortalIOS"
CONFIGURATION="Release"
ARCHIVE_PATH="build/${PROJECT_NAME}.xcarchive"
EXPORT_PATH="build/ipa"
EXPORT_OPTIONS_PLIST="${1:-ExportOptions-AdHoc.plist}"

echo "Generating Xcode project..."
xcodegen generate

echo "Cleaning previous build output..."
rm -rf build

echo "Archiving app..."
xcodebuild \
  -project "${PROJECT_NAME}.xcodeproj" \
  -scheme "${SCHEME}" \
  -configuration "${CONFIGURATION}" \
  -destination generic/platform=iOS \
  -archivePath "${ARCHIVE_PATH}" \
  archive

echo "Exporting IPA using ${EXPORT_OPTIONS_PLIST}..."
xcodebuild \
  -exportArchive \
  -archivePath "${ARCHIVE_PATH}" \
  -exportPath "${EXPORT_PATH}" \
  -exportOptionsPlist "${EXPORT_OPTIONS_PLIST}"

echo "IPA exported to ${EXPORT_PATH}"
