# AquaCoach — Smart Hydration AI

AquaCoach is a native Android hydration tracker built with Jetpack Compose. Log water intake in a tap, watch progress fill an animated ring, and let a smart goal calculator and scheduled reminders keep you on track throughout the day.

## Features

- **Quick Add logging** — one-tap buttons for common amounts (150/250/350/500 ml) plus a custom-amount dialog.
- **Animated progress ring** — today's intake vs. daily goal, with a haptic tap when the goal is reached.
- **Today's log** — a running list of the day's entries with swipe-free delete.
- **History & analytics** *(Premium)* — 7-day bar chart, daily breakdown, current streak, and 7-day average.
- **Smart Goal Calculator** *(Premium)* — recommends a daily goal from body weight, activity level, and hot/humid climate.
- **Hydration reminders** — local notifications via WorkManager; free tier gets a fixed interval, Premium unlocks a custom interval and active-hours window.
- **Unit preference** — switch between milliliters and fluid ounces.
- **Premium via Google Play Billing** — yearly subscription, one-time lifetime purchase, and an optional one-time "support pack" tip; includes restore purchases.
- **Material 3 dynamic theming** — follows system light/dark mode and Android 12+ dynamic (wallpaper-based) color, with a curated fallback palette on older devices.
- **Accessibility-minded UI** — content descriptions on interactive controls and charts, merged semantics for compound widgets, and haptic feedback on key interactions.

## Requirements

- **Android Studio** Koala (2024.1) or newer
- **JDK 17**
- **Android SDK**: compileSdk 34, targetSdk 34
- **Minimum OS**: Android 7.0 (API 24) — `java.time` APIs are desugared for API 24–25
- **Kotlin** with Jetpack Compose (Compose BOM 2024.10.00)
- A device or emulator running **Android 12+ (API 31+)** to see dynamic Material You theming; earlier versions fall back to the built-in Aqua palette

## Build Instructions

1. Clone the repository and open it in Android Studio, or build from the command line.
2. Ensure `local.properties` points at a valid Android SDK (`sdk.dir=...`); Android Studio generates this automatically on first sync.
3. Build a debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install on a connected device/emulator:
   ```bash
   ./gradlew installDebug
   ```
5. Run unit tests:
   ```bash
   ./gradlew test
   ```
6. Run instrumented tests (requires a connected device/emulator):
   ```bash
   ./gradlew connectedAndroidTest
   ```

Google Play Billing product IDs are defined in `PremiumProducts` (`data/billing/BillingModels.kt`); to test purchases end-to-end you'll need a Play Console app listing with matching in-app products/subscriptions and a signed build installed via internal testing.

## Project Structure

```
app/src/main/java/com/factory/aquacoachsmarthydrationai/
├── AquaCoachApplication.kt      # App-level DI: builds repository, billing & premium managers
├── MainActivity.kt              # Single-activity host; installs splash screen, edge-to-edge, theme
├── data/
│   ├── billing/                 # BillingManager (Play Billing), PremiumManager, product/state models
│   ├── local/                   # Room database, WaterEntry entity, DAO
│   ├── preferences/              # DataStore-backed user preferences (goal, units, reminders, etc.)
│   └── repository/              # HydrationRepository — single source of truth for the UI layer
├── navigation/                  # Screen routes and the bottom-nav NavHost
├── notification/                # Reminder scheduling (WorkManager), notification channel, boot receiver
├── ui/
│   ├── components/               # Reusable composables: progress ring, quick-add button, pro badge, lock prompt
│   ├── home/                     # Today screen — logging, progress, today's entries
│   ├── history/                  # History screen — streak, average, weekly chart, daily breakdown
│   ├── settings/                 # Settings screen — goal, smart calculator, units, reminders
│   ├── paywall/                  # Premium paywall — tiers, purchase flow, restore, legal links
│   └── theme/                    # Material 3 color schemes, dynamic color, typography
└── util/                        # DateUtils, SmartGoalCalculator
```

Each screen follows a `Screen` + `ViewModel` (+ `ViewModelFactory`) pattern: the `ViewModel` exposes a single `StateFlow<UiState>` combined from `HydrationRepository`/`PremiumManager` flows, and the composable renders that state and forwards user actions back as function calls.
