# Migration: New Google Sheet + New Account

Date: 2026-07-19
Target spreadsheet:
https://docs.google.com/spreadsheets/d/1a7r3rXY7dPyUjKCdvNopK2Y9ufKYhda0o6DCYBukv2o/edit

## Applied in active code

1) Apps Script backend
- File: apps-script/Main.gs
- Change: SPREADSHEET_ID -> 1a7r3rXY7dPyUjKCdvNopK2Y9ufKYhda0o6DCYBukv2o

2) Android Gas module
- File: app/src/gas/java/com/driver/portal/GasSheetConfig.kt
- Change: GAS_SHEET_ID -> 1a7r3rXY7dPyUjKCdvNopK2Y9ufKYhda0o6DCYBukv2o

3) GPS Apps Script module
- File: gps-apps-script/Main.gs
- Change: GPS_SPREADSHEET_ID configured to new sheet ID

## Important note before production rollout

Apps that call Apps Script via URL still depend on deployed web app URLs.
These URLs include deployment IDs like:
https://script.google.com/macros/s/AKfy.../exec

After deploying from the new account, replace script URL constants in:
- app/src/main/java/com/driver/portal/network/GoogleSheetConfig.kt
- app/src/main/assets/map.html
- app/src/owner/java/com/driver/portal/OwnerMainActivity.kt
- ios/DriverPortalIOS/Services/GoogleSheetConfig.swift

## Safe rollout order

1) Deploy Apps Script from new account (new /exec URL).
2) Replace URL constants in Android/iOS files above.
3) Smoke test actions: login, trip, factory, history, wallet, gps.
4) Build new app binaries with version bump.
5) Keep old backups unchanged (already preserved in backups/ and temp-*).
