# Driver Portal Admin Desktop

This folder contains an isolated Windows desktop app for the accounting manager.
It provides password-protected access and multiple system tabs in one place.
It does not modify Android, iOS, or Apps Script code.

## Safety model

- Runs as a separate app in `desktop-admin` only.
- Loads all configured system URLs from a local config file.
- Keeps `nodeIntegration` disabled and `contextIsolation` enabled.
- No backend contract changes.

## Login and tabs configuration

The app reads settings from:

`desktop-admin/manager-config.json`

You can control:

- App title
- Login password hash
- Password hint
- Tabs list and URLs (Admin dashboard, demand templates, Google Sheet, tracking dashboard, etc.)

### Default password

Default password is:

`123456`

Change it before production use.

### Change password

Set `security.passwordHash` in `manager-config.json` to SHA-256 hash of your new password.

PowerShell helper:

```powershell
$text = "YourStrongPassword"
$bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
$sha = [System.Security.Cryptography.SHA256]::Create()
($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString("x2") }) -join ""
```

Copy the output hash into `passwordHash`.

## Requirements

- Node.js 20 LTS or newer

## Run locally

```powershell
cd desktop-admin
npm install
npm start
```

## Build Windows installer

```powershell
cd desktop-admin
npm run dist
```

Installer output will be under `desktop-admin/dist`.

## Optional custom URL

You can point to a different deployment URL:

```powershell
$env:DRIVER_PORTAL_ADMIN_URL = "https://script.google.com/macros/s/<DEPLOYMENT_ID>/exec?page=admin"
npm start
```
