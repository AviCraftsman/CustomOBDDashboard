# CustomOBDDashboard

A custom Android Automotive OS (AAOS) dashboard app (internal module name `IbizaCustomMap`) that connects
to a Bluetooth ELM327/OBD-II adapter and renders live engine telemetry (RPM, speed, gear, oil/intake
temperature, boost pressure, throttle, AFR, engine load, G-force, MAF) as a custom GT3-style digital
dashboard on the car's head unit display. A companion phone `Activity` handles the required Bluetooth and
location runtime permissions.

If no OBD-II adapter is available, the app automatically falls back to a built-in simulation mode that
drives the same dashboard with synthetic telemetry.

## Documentation

Full project documentation lives in [`docs/`](./docs/README.md):

* [Architecture](./docs/ARCHITECTURE.md) — components, threading model, and data flow.
* [Setup](./docs/SETUP.md) — prerequisites, build, and run instructions.
* [File Structure](./docs/FILE_STRUCTURE.md) — the entire repository file tree with descriptions.

## Quick start

```bash
./gradlew assembleDebug
```

See [`docs/SETUP.md`](./docs/SETUP.md) for full details, including running on an Android Automotive
emulator/device.

## Project layout at a glance

* `app/` — the Android Automotive application module (Kotlin, Jetpack Compose, `androidx.car.app`).
* `gradle/` — Gradle Wrapper and version catalog.
* `docs/` — project documentation (this index and linked pages).
