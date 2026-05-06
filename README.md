# SolarSystem

A real-time 3D solar system simulation written in Java with [JOGL](https://jogamp.org/jogl/www/) (OpenGL bindings). Planets orbit the Sun using genuine Keplerian orbital mechanics derived from NASA JPL element tables, starting at the current real-world date and ticking forward at a user-controlled rate.

![n/a](docs/screenshot.png)

## What it does

- Renders the Sun and all eight planets (plus Saturn's rings) as lit, textured-shaded spheres in OpenGL.
- Each planet's heliocentric position at any moment is computed from its **six classical orbital elements** at the J2000 epoch — not faked circular orbits.
- The simulation clock starts at the current system date and advances continuously. You can pause, reverse, speed up, and reset it with keyboard shortcuts.
- A camera lets you fly around the system (WASD + mouse-look + scroll-zoom).
- A HUD overlay shows the current simulated date, time speed, and controls.

## Controls

| Input | Action |
|---|---|
| Mouse drag | Look around |
| Scroll | Zoom in/out |
| W / A / S / D | Move forward / left / back / right |
| Q / E | Move down / up |
| Shift | Sprint (3× movement speed) |
| `+` / `=` | Speed up time (2×) |
| `-` | Slow down time (½×) |
| Space | Pause / resume |
| R | Reset date to now |
| Esc | Exit |

Default starting speed: **3 days per real second**.

## Running it

You need **JDK 21** installed. From a release build:

```
java -jar SolarSystem.jar
```

To build from source:

```
mvn package
java -jar target/SolarSystem.jar
```

For installer packages (no JDK required by the user) see [`BUILD.md`](BUILD.md).

## How it's implemented

The codebase lives in `src/main/java/com/physics/`. There are nine classes; here's what each one is responsible for and how they fit together.

### Orbital mechanics — `OrbitalElements.java`

Each planet is described by the **six classical Keplerian orbital elements** at the J2000.0 epoch (2000-01-01T12:00:00 TT):

| Symbol | Meaning |
|---|---|
| `a` | Semi-major axis (AU) |
| `e` | Eccentricity |
| `i` | Inclination relative to the ecliptic |
| `Ω` | Longitude of ascending node |
| `ω` | Argument of perihelion |
| `M₀` | Mean anomaly at epoch |
| `T` | Orbital period (days) |

To compute a planet's 3D heliocentric position at simulation time `Δt` (days since J2000):

1. **Advance mean anomaly:** `M = M₀ + (2π / T)·Δt`
2. **Solve Kepler's equation `M = E − e·sin(E)`** for the eccentric anomaly `E` using **Newton-Raphson iteration** (converges in ~5 steps to 1e-12 tolerance).
3. **True anomaly:** `tan(ν/2) = √((1+e)/(1−e)) · tan(E/2)`
4. **Heliocentric distance:** `r = a·(1 − e·cos E)`
5. **Position in orbital plane:** `(r·cos ν, r·sin ν, 0)`
6. **Rotate** by `ω` (argument of perihelion), then `i` (inclination), then `Ω` (longitude of ascending node) to map into the ecliptic frame.
7. Swap Y/Z so OpenGL's Y-up convention works.

The same class also stores each body's **physical** parameters: equatorial radius, axial tilt, and sidereal rotation period (negative for retrograde rotators like Venus and Uranus).

### Body catalog — `SolarSystem.java`

Hardcodes the orbital + physical elements for the Sun and eight planets, sourced from NASA JPL planetary fact sheets and Standish (1992) approximate Keplerian elements. Each row in the source file is annotated with its actual values. `update()` advances the clock and recomputes positions; `draw()` renders orbit paths first, then the bodies on top.

### Per-planet rendering — `Planet.java`

Wraps an `OrbitalElements` plus material colors. On `update(t)` it re-evaluates the heliocentric position; on `draw(gl)` it pushes a transform stack: translate to position → tilt by obliquity → rotate by current rotation angle → render the sphere mesh. Saturn additionally draws a ring system. A precomputed orbit polyline (sampled at 256 points along the path) is uploaded to a VBO once at init time so the orbit trails are essentially free to draw every frame.

### Geometry — `Sphere.java`

Builds a UV-sphere mesh with configurable stack/sector resolution. Vertices are interleaved as `(x, y, z, nx, ny, nz)` and uploaded to a **GPU-side VBO** once at init. Drawing is a single indexed `glDrawElements` call per planet, with normals shading the body via OpenGL's fixed-function lighting model.

### Lighting

A single `GL_LIGHT0` is positioned at the origin (inside the Sun) every frame, with quadratic attenuation tuned so the inner planets are bright and Neptune is dim — physically inaccurate (real solar irradiance falls off far more sharply across this range) but visually informative.

### Time — `SimClock.java`

Maps wall-clock time to simulation time via a configurable speed multiplier (default 3 days/sec). The clock seeds itself with the current real date converted to "days since J2000" so the starting positions match where the planets actually are right now. Supports pause, faster (2×), slower (½×), and reset-to-now.

### Camera — `Camera.java`

A simple FPS-style camera with yaw/pitch from mouse drag, WASD translation in the camera's local frame, scroll-wheel zoom, and shift-to-sprint. `applyMovementAndLookAt()` runs once per frame and emits the appropriate `gluLookAt` call.

### Window + render loop — `Main.java` and `SphereRenderer.java`

`Main.java` wires together a NEWT `GLWindow`, an `FPSAnimator` (60 Hz), the camera, the clock, and the keyboard/mouse listeners.

`SphereRenderer` is the JOGL `GLEventListener`. Per frame:

1. Clear color + depth buffers.
2. Apply camera transforms.
3. Reposition the Sun's light source.
4. `solarSystem.update()` — advance clock, recompute planet positions.
5. `solarSystem.draw(gl)` — orbit paths, then bodies.
6. `drawHUD(gl)` — 2D text overlay using JOGL's `TextRenderer`.

#### macOS deadlock note

JOGL's `TextRenderer` uses Java2D under the hood, which on macOS lazily initializes the AWT graphics environment (Metal pipeline). If that init runs from the NEWT display thread, it deadlocks against AppKit. `Main.java` works around this by pre-initializing `GraphicsEnvironment` on the AWT EDT before any NEWT code runs:

```java
javax.swing.SwingUtilities.invokeAndWait(
    java.awt.GraphicsEnvironment::getLocalGraphicsEnvironment);
```

### Profiling — `DebugTimer.java`

A tiny per-frame profiler. Each major stage (camera, update, draw, HUD, total frame) calls `DebugTimer.log("label", startNanos)`. Output goes to stdout — useful when you change something and want to see if it ticks at 60 FPS.

## Project layout

```
src/main/java/com/physics/
  Main.java               — entry point, window + input setup
  SphereRenderer.java     — JOGL render loop, HUD
  SolarSystem.java        — body catalog with real orbital elements
  Planet.java             — per-body update/draw, orbit trails
  OrbitalElements.java    — Keplerian → 3D position math
  Sphere.java             — UV sphere mesh + VBOs
  SimClock.java           — wall-clock → sim-time mapping
  Camera.java             — FPS camera
  DebugTimer.java         — per-frame timing logger
```

## Tech stack

- **Java 21**
- **JOGL 2.6.0** — OpenGL bindings, NEWT windowing, `TextRenderer`, `FPSAnimator`
- **Maven** with **maven-shade-plugin** to produce a single fat-JAR containing all native libraries for macOS (Intel + ARM), Linux (x86_64 / aarch64 / arm), and Windows.
- **GitHub Actions** matrix to produce per-OS native installers via `jpackage` (see [`BUILD.md`](BUILD.md)).

## Data sources

- NASA JPL planetary fact sheets — <https://nssdc.gsfc.nasa.gov/planetary/factsheet/>
- Standish, E. M. (1992). *Keplerian elements for approximate positions of the major planets.*

