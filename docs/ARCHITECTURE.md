# Architecture Overview

CustomOBDDashboard (internal module name `IbizaCustomMap`) is an Android Automotive OS (AAOS) app that
reads live engine telemetry from a Bluetooth ELM327/OBD-II adapter and renders a custom, GT3-style
digital dashboard directly onto the car's head unit display.

## High-level components

```
┌───────────────────┐      onCreateSession()      ┌────────────────┐
│  IbizaCarService   │ ───────────────────────────▶│  IbizaSession   │
│ (CarAppService)    │                              │  (Session)      │
└───────────────────┘                              └───────┬────────┘
                                                             │ onCreateScreen()
                                                             ▼
                                                     ┌────────────────┐
                                                     │  MainScreen     │
                                                     │ (Screen +       │
                                                     │  SurfaceCallback)│
                                                     └───────┬────────┘
                        owns / drives                        │
        ┌──────────────────────────────────────────────────┬─┴─────────────────────────┐
        ▼                                                    ▼                            ▼
┌───────────────┐                                   ┌────────────────┐         ┌──────────────────┐
│  ObdManager    │  raw hex strings                  │  ObdDecoder     │         │  Render thread    │
│ (Bluetooth I/O)│ ─────────────────────────────────▶│ (pure parsing)  │──values▶│ (Canvas drawing)  │
└───────────────┘                                   └────────────────┘         └──────────────────┘
```

* **`IbizaCarService`** — The Android Automotive/Android Auto entry point (`CarAppService`). It validates
  the connecting host and creates an `IbizaSession` when the car head unit connects.
* **`IbizaSession`** — The `Session` implementation; its only job is to instantiate `MainScreen` as the
  app's initial `Screen`.
* **`MainScreen`** — The core of the app. It:
  * Implements `SurfaceCallback` to receive the raw drawing `Surface` exposed by the car host.
  * Spawns a dedicated background thread (`obdThread`) that connects to the OBD-II adapter via
    `ObdManager`, polls PIDs (RPM, speed, boost, throttle, oil/intake temp, AFR, engine load, MAF, etc.),
    and decodes responses using `ObdDecoder`.
  * Falls back to a built-in **simulation mode** (`modoSimulacion`) that generates smooth synthetic wave
    data when no adapter is found, so the UI can still be exercised/demoed without hardware.
  * Maintains a `HandlerThread` dedicated to rendering (`renderThread`/`renderHandler`) and redraws the
    dashboard on a fixed tick (`REFRESH_TICK_MS`) plus whenever the surface/visible area changes.
  * Uses a `telemetryLock` to safely hand off the latest telemetry values (mutated by the OBD thread) to
    an immutable `TelemetrySnapshot` consumed by the render thread, avoiding torn reads.
  * Draws the entire dashboard manually with `android.graphics.Canvas`/`Paint` (LED-style RPM bar, speed,
    gear, and left/right telemetry columns with color-coded warning thresholds).
* **`ObdManager`** — Wraps classic Bluetooth (RFCOMM/SPP) discovery, connection, command sending, and
  response reading against a paired ELM327-compatible adapter (matched by device name containing
  "OBD", "V-LINK", "ELM327", or "Vgate").
* **`ObdDecoder`** — Stateless parsing utilities that convert raw ELM327 hex responses (e.g. `410C1AF8`)
  into typed engineering values (RPM, °C, %, AFR ratio, kPa, etc.), following standard OBD-II Mode 01 PID
  formulas.
* **`MainActivity`** — The phone/tablet-side launcher `Activity`. It is Jetpack Compose–based and is only
  responsible for requesting the Bluetooth/location runtime permissions required before the car-side
  service can operate, showing a minimal status screen while waiting for an OBD2 connection.
* **`ui/theme`** — Standard Jetpack Compose Material3 theme scaffolding (colors, typography, theme
  composable) used by `MainActivity`.

## Threading model

| Thread            | Responsibility                                                                 |
|-------------------|---------------------------------------------------------------------------------|
| Main/UI thread    | Android lifecycle callbacks, Compose UI (phone activity), car host callbacks    |
| `obdThread`       | Bluetooth I/O with the OBD-II adapter, PID polling loop, telemetry mutation      |
| `renderThread`    | Periodic + event-driven redraws of the dashboard `Canvas` onto the car `Surface` |

Telemetry values are only ever mutated inside `synchronized(telemetryLock) { ... }` blocks and are copied
into an immutable `TelemetrySnapshot` before being read by the render thread, to avoid data races.

## Data flow summary

1. `MainScreen` starts `obdThread`, which uses `ObdManager` to connect to the adapter over Bluetooth SPP.
2. The thread sends AT initialization commands (`ATZ`, `ATE0`, `ATH0`, `ATS0`, `ATSP0`, `ATDPN`) then loops
   sending Mode 01 PID requests (e.g. `01 0C` for RPM, `01 0D` for speed).
3. Raw responses are cleaned up (removing `SEARCHING...`, prompt characters, whitespace) and decoded by
   `ObdDecoder` into numeric values.
4. Decoded values are written to shared telemetry fields under `telemetryLock`.
5. The render thread reads a `TelemetrySnapshot` and repaints the dashboard onto the car's `Surface` using
   `Canvas`/`Paint`.
6. If the adapter cannot be found/connected after a few retries, `modoSimulacion` (simulation mode) is
   enabled and synthetic sine/cosine-wave telemetry drives the same rendering path.

## Related documents

* [`docs/SETUP.md`](./SETUP.md) — building and running the app.
* [`docs/FILE_STRUCTURE.md`](./FILE_STRUCTURE.md) — full annotated file tree.
