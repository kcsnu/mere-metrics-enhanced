# Mere Metrics: Weight Tracker (Enhanced)

This repository contains the enhanced version of Mere Metrics: Weight Tracker, a Java Android app for tracking daily weight entries against a goal. Compared with the original version, it refactors the app structure, adds analytics and insights, and replaces the helper-based database layer with Room. The original version is available at [mere-metrics-original](https://github.com/kcsnu/mere-metrics-original), and the full portfolio is at [kcsnu.github.io](https://kcsnu.github.io/).

## Enhancements in this version

Three categories of improvement over the original:

1. **Software design and engineering** — refactored from a fragment-centered design into layered packages (`ui`, `service`, `data`, `model`, `util`). The history view is now rendered with RecyclerView rather than a manually generated table.
2. **Algorithms and data structures** — added an analytics engine with ISO date normalization, range filtering, rolling averages, weekly and monthly change calculations, percent progress toward the goal, and a projected goal-reach date.
3. **Databases** — migrated persistence from `SQLiteOpenHelper` to Room, with Room entities, DAOs, versioned schema migrations, foreign keys, unique constraints, and indexed queries. Existing user data is preserved across upgrades rather than being dropped and recreated.

## Requirements

- A recent version of Android Studio that supports Android Gradle Plugin 9.0 and compileSdk 36
- Android SDK 36 installed through the Android Studio SDK Manager
- Java 11-compatible JDK (bundled with Android Studio)
- An emulator running Android 14 (API 34) or higher, or a physical device on Android 14 or higher

## How to load and run

1. Clone the repository:

   ```bash
   git clone https://github.com/kcsnu/mere-metrics-enhanced.git
   ```

2. Open the project folder in Android Studio.
3. Let Gradle sync finish. The project uses Gradle 9.1.0, which is downloaded automatically via the wrapper.
4. Select an emulator or connected device from the device picker.
5. Press Run.

## Features

- Account creation and login with PBKDF2-hashed passwords
- Goal weight setting
- Daily weight entry tracking with create, read, update, and delete
- Weight history rendered via RecyclerView with per-entry edit and delete actions
- Insights screen with date-range filtering, rolling averages, trend calculations, goal progress, and a projected goal-reach date
- Optional SMS notification when the goal is reached (requires SMS runtime permission)
- Room-based persistence with explicit migrations that preserve existing data

## Running the tests

The project includes both unit tests and instrumentation tests.

- **Unit tests** (no device required): `./gradlew test`
- **Instrumentation tests** (emulator or device required): `./gradlew connectedAndroidTest`

Unit tests include analytics logic, goal-reach detection, date handling, and service-layer behavior. Instrumentation tests include Room database behavior and schema migration checks.

## Project structure

```text
app/src/main/java/com/example/cs360_charlton_molloy_keir/
├── data/
│   ├── AuthRepository.java
│   ├── UserPreferencesRepository.java
│   ├── WeightRepository.java
│   └── room/           Room database, entities, and DAOs
├── model/              Plain data models
├── service/            Business logic and application services
├── ui/                 Activities, fragments, adapters
└── util/               Shared utilities
```

## Notes

- SMS permission is requested at runtime. Granting it enables the notification feature. Denying it leaves the rest of the app fully functional.
- Room schemas are exported to `app/schemas/` during build for version control and migration verification.
- Schema migrations from earlier database versions are handled explicitly, so existing user data is preserved on upgrade rather than dropped and recreated.

## Related

- Original version: [mere-metrics-original](https://github.com/kcsnu/mere-metrics-original)
- Portfolio site: [kcsnu.github.io](https://kcsnu.github.io/)
