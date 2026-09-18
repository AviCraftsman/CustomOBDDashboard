# Setup & Development Guide

## Prerequisites

* Android Studio (latest stable) or a JDK 17+/Gradle-compatible command-line setup.
* Android SDK Platform 36 (`compileSdk` / `targetSdk`) and Android SDK Build-Tools.
* An Android Automotive OS emulator/device, or an Android phone/tablet paired with an
  ELM327-compatible Bluetooth OBD-II adapter, for full end-to-end testing.
* An OBD-II Bluetooth adapter whose paired device name contains `OBD`, `V-LINK`, `ELM327`, or `Vgate`
  (see `ObdManager.connect()`), if testing against real vehicle hardware.

## Getting the code building

1. Clone the repository.
2. Open the project root in Android Studio, or use the Gradle Wrapper from the command line:

   ```bash
   ./gradlew assembleDebug
   ```

3. The wrapper will download the pinned Gradle distribution (see
   `gradle/wrapper/gradle-wrapper.properties`) and resolve dependencies declared in
   `gradle/libs.versions.toml`.

## Running the app

The project produces a single `:app` module targeting Android Automotive OS, with a phone-side
companion `MainActivity`:

* **Phone/tablet**: Install and launch normally; `MainActivity` requests the Bluetooth/location
  permissions required to talk to the OBD-II adapter and shows a simple waiting-for-connection screen.
* **Car head unit (Android Automotive)**: The app registers as a navigation-category `CarAppService`
  (`IbizaCarService`) in `AndroidManifest.xml`. Deploy to an Android Automotive emulator/device and
  launch it from the car launcher; `IbizaSession`/`MainScreen` will take over the car display surface.

If no OBD-II adapter is connected, `MainScreen` automatically falls back to a **simulation mode** that
generates synthetic telemetry, so the dashboard can be exercised without real vehicle hardware.

## Project configuration

* `applicationId`: `com.javirmmn.customobddashboard`
* `namespace`: `com.example.ibizacustommap`
* `minSdk`: 26, `targetSdk`/`compileSdk`: 36
* Build system: Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)
* UI: Jetpack Compose (phone) + `androidx.car.app` templates/`Canvas` drawing (car surface)

## Tests

* Unit tests: `app/src/test/java/com/example/ibizacustommap/` (run with `./gradlew test`)
* Instrumented tests: `app/src/androidTest/java/com/example/ibizacustommap/` (run with
  `./gradlew connectedAndroidTest` against a connected device/emulator)

## Related documents

* [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md) — high-level architecture and data flow.
* [`docs/FILE_STRUCTURE.md`](./FILE_STRUCTURE.md) — full annotated file tree.
