# Kharcha — Personal Finance Tracker for Nepal

A local-first Android finance tracker with ML Kit OCR receipt scanning, designed for Nepal (Rs./NPR).

## Features

- Log income & expenses with categories
- OCR receipt scanning (fully offline, bundled ML Kit)
- Monthly budget tracking with warnings
- Savings goal progress on Dashboard
- CSV & JSON export
- Daily reminder notifications

## Build & Run

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (Hedgehog or later)

1. Open Android Studio → **Open** → select this project directory
2. Allow Android Studio to sync Gradle
3. Run on an emulator or physical device (minSdk 24)

> No API keys or `.env` files are required. The app runs fully offline.

## Architecture

- **Room** — local database (transactions, budgets, savings goals, settings)
- **ViewModel + Repository** — MVVM with coroutines/Flow
- **Jetpack Compose** — declarative UI
- **ML Kit text-recognition** — bundled offline OCR model
- **FileProvider** — secure camera/sharing via `com.example.fileprovider`
