<div align="center">
  <img src="assets/logo.png" alt="InstaEclipse" width="120" />
  <h1>InstaEclipse Web Manager</h1>
  <p>A modern React companion manager and preference controller for InstaEclipse.</p>

  <p>
    <a href="https://t.me/InstaEclipsechat"><img alt="Telegram Chat" src="https://img.shields.io/badge/Telegram-Chat%20@InstaEclipsechat-26A5E4?style=for-the-badge&logo=telegram"/></a>
    <a href="https://t.me/instaeclipse_channel"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-Channel-26A5E4?style=for-the-badge&logo=telegram"/></a>
    <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge"/></a>
  </p>
</div>

---

## Overview

**InstaEclipse Web Manager v2.0** is a full-featured companion interface and in-app APK installer for InstaEclipse. All feature branches and patches have been consolidated into the unified v2 release. It provides in-app direct APK downloads with SHA-256 verification, real-time preference management, configuration import/export, interactive OpenStreetMap GPS spoofing, diagnostic log inspection, and an advanced Theme Customizer with live Instagram mockup preview.

---

## 📦 Releases & Downloads

- **Latest APK**: `InstaEclipse_v2.0.0_release.apk` (Build 20)
- **Target Compatibility**: `com.instagram.android` 443.0.0.48.82+ (ARM64-v8a / ARMv7a)
- **Official Telegram Chat**: [@InstaEclipsechat](https://t.me/InstaEclipsechat) for direct downloads and community support.
- **In-App Installation**: Tap "Download & Install APK" inside the app for direct download and package installer verification.

---

## ✨ Features

### 👻 Ghost Mode
- **Hide DM Seen**: Read messages without dispatching read receipts.
- **Hide Typing Indicator**: Type in direct messages with complete privacy.
- **Hide Story Views**: Watch stories without appearing in viewer rosters.
- **Hide Live Presence**: Join live video streams anonymously.
- **Bypass Screenshot Detection**: Capture chats without triggering notification alerts.
- **Unlimited View-Once Media**: Replay ephemeral photos and videos indefinitely.

### 🚫 Ad & Analytics Blocking
- Block sponsored posts, feed commercials, and stories promotions.
- Strip tracking parameters from shared URLs (`?igsh=...`, `utm_*`).
- Silence telemetry logging endpoints to optimize network usage.

### 🧹 Clean Feed & Distraction-Free
- Hide Suggested Posts, Suggested Users, and "Catch Up" carousels.
- Hide Reels feed, Explore search feed, and shopping navigation.
- Hide likes count, follower counts, and comment threads.

### 🎨 Theme Customizer
- **30 Curated Presets**: Midnight Eclipse, Pure OLED, Arctic Frost, Rose Gold, Cyber Neon, and 25 more.
- **15 Granular Color Slots**: Customize background, surface, accent, buttons, icons, glyphs, status bar, and links.
- **Live Instagram UI Mockup**: Real-time interactive feed and direct message rendering reflecting your custom palette.

### 📍 OpenStreetMap GPS Spoofing
- Interactive coordinate picker with real-time center pin locator.
- Search engine integration via OpenStreetMap Nominatim.
- Instant presets for major global cities (Tokyo, New York, Paris, London, Sydney, Dubai, Cairo, Rio de Janeiro).

### ⚙️ Developer Options & MetaConfig Overrides
- Enable Meta internal developer settings and feature experiment gates.
- Built-in community presets: Base Overrides, Media & Stories, Quality & Compression, Navigation & UI.
- JSON MetaConfig override editor and instant backup/restore.

### 📋 Diagnostic Logs & State Sync
- Filter diagnostic module logs by tags: `HOOK`, `DEXKIT`, `SYNC`, `SETTINGS`, `INFO`, `ERROR`.
- Export logs as `.log` files or copy directly to clipboard.
- Backup configuration as JSON or restore previous profiles with one tap.

---

## 🛠 Tech Stack

- **Framework**: React 18 with TypeScript
- **Bundler**: Vite
- **Styling**: Tailwind CSS
- **Icons**: Lucide Icons
- **State Management**: Persistent local storage cache with real-time broadcast channel

---

## 🚀 Running Locally

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build production bundle
npm run build
```
