package com.physics;

import com.jogamp.opengl.GL2;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds all solar system bodies using real orbital elements (J2000 epoch).
 *
 * Orbital data sources:
 *   - NASA JPL planetary fact sheets
 *   - Standish (1992) approximate Keplerian elements
 *
 * Element columns:
 *   a (AU), e, i (°), Ω (°), ω (°), M0 (°), T (days),
 *   radius (km), axial tilt (°), rotation period (days)
 */
public class SolarSystem {

    private final List<Planet> planets = new ArrayList<>();
    private final SimClock clock;

    public SolarSystem(SimClock clock) {
        this.clock = clock;
        buildPlanets();
    }

    private void buildPlanets() {
        // ---- Sun (no orbit) ----
        planets.add(new Planet("Sun", null,
                new float[]{1.0f, 0.9f, 0.3f, 1f},
                new float[]{0.8f, 0.65f, 0.15f, 1f},
                false));

        // ---- Mercury ----
        //  a=0.387 AU, e=0.2056, i=7.00°, Ω=48.33°, ω=29.12°, M0=174.79°, T=87.969 days
        //  radius=2439.7 km, tilt=0.034°, rotation=58.646 days
        planets.add(new Planet("Mercury",
                new OrbitalElements(0.387098, 0.205630, 7.005, 48.331, 29.124, 174.796, 87.9691,
                        2439.7, 0.034, 58.646),
                new float[]{0.7f, 0.65f, 0.6f, 1f},
                new float[]{0.25f, 0.23f, 0.2f, 1f},
                false));

        // ---- Venus ----
        //  a=0.723 AU, e=0.0068, i=3.39°, Ω=76.68°, ω=54.88°, M0=50.42°, T=224.701 days
        //  radius=6051.8 km, tilt=177.36° (retrograde), rotation=-243.025 days
        planets.add(new Planet("Venus",
                new OrbitalElements(0.723332, 0.006772, 3.3946, 76.680, 54.884, 50.416, 224.701,
                        6051.8, 177.36, -243.025),
                new float[]{0.9f, 0.75f, 0.4f, 1f},
                new float[]{0.35f, 0.28f, 0.15f, 1f},
                false));

        // ---- Earth ----
        //  a=1.000 AU, e=0.0167, i=0.00°, Ω=-11.26°, ω=114.21°, M0=358.62°, T=365.256 days
        //  radius=6371.0 km, tilt=23.44°, rotation=0.9973 days
        planets.add(new Planet("Earth",
                new OrbitalElements(1.000001, 0.016709, 0.0, -11.260, 114.208, 358.617, 365.256,
                        6371.0, 23.44, 0.99727),
                new float[]{0.2f, 0.5f, 0.85f, 1f},
                new float[]{0.08f, 0.15f, 0.3f, 1f},
                false));

        // ---- Mars ----
        //  a=1.524 AU, e=0.0934, i=1.85°, Ω=49.56°, ω=286.50°, M0=19.37°, T=686.980 days
        //  radius=3389.5 km, tilt=25.19°, rotation=1.026 days
        planets.add(new Planet("Mars",
                new OrbitalElements(1.523679, 0.093401, 1.8497, 49.558, 286.502, 19.373, 686.980,
                        3389.5, 25.19, 1.02596),
                new float[]{0.8f, 0.35f, 0.15f, 1f},
                new float[]{0.3f, 0.12f, 0.05f, 1f},
                false));

        // ---- Jupiter ----
        //  a=5.203 AU, e=0.0489, i=1.30°, Ω=100.46°, ω=273.87°, M0=20.02°, T=4332.59 days
        //  radius=69911 km, tilt=3.13°, rotation=0.4135 days
        planets.add(new Planet("Jupiter",
                new OrbitalElements(5.202887, 0.048498, 1.3030, 100.464, 273.867, 20.020, 4332.59,
                        69911.0, 3.13, 0.41354),
                new float[]{0.8f, 0.65f, 0.45f, 1f},
                new float[]{0.3f, 0.22f, 0.15f, 1f},
                false));

        // ---- Saturn ----
        //  a=9.537 AU, e=0.0557, i=2.49°, Ω=113.64°, ω=339.39°, M0=317.02°, T=10759.22 days
        //  radius=58232 km, tilt=26.73°, rotation=0.4440 days
        planets.add(new Planet("Saturn",
                new OrbitalElements(9.536676, 0.054151, 2.4845, 113.642, 339.392, 317.020, 10759.22,
                        58232.0, 26.73, 0.4401),
                new float[]{0.85f, 0.75f, 0.5f, 1f},
                new float[]{0.32f, 0.28f, 0.18f, 1f},
                true));

        // ---- Uranus ----
        //  a=19.19 AU, e=0.0472, i=0.77°, Ω=74.01°, ω=96.99°, M0=142.24°, T=30688.5 days
        //  radius=25362 km, tilt=97.77°, rotation=-0.7183 days (retrograde)
        planets.add(new Planet("Uranus",
                new OrbitalElements(19.18916, 0.047168, 0.7734, 74.006, 96.999, 142.239, 30688.5,
                        25362.0, 97.77, -0.71833),
                new float[]{0.6f, 0.8f, 0.85f, 1f},
                new float[]{0.2f, 0.3f, 0.32f, 1f},
                false));

        // ---- Neptune ----
        //  a=30.07 AU, e=0.0086, i=1.77°, Ω=131.78°, ω=276.34°, M0=256.23°, T=60182.0 days
        //  radius=24622 km, tilt=28.32°, rotation=0.6713 days
        planets.add(new Planet("Neptune",
                new OrbitalElements(30.06992, 0.008586, 1.7700, 131.784, 276.340, 256.228, 60182.0,
                        24622.0, 28.32, 0.67125),
                new float[]{0.25f, 0.4f, 0.9f, 1f},
                new float[]{0.08f, 0.12f, 0.35f, 1f},
                false));
    }

    public void init(GL2 gl) {
        for (Planet p : planets) {
            p.init(gl);
        }
    }

    public void dispose(GL2 gl) {
        for (Planet p : planets) {
            p.dispose(gl);
        }
    }

    /**
     * Advance simulation and update all planet positions.
     */
    public void update() {
        long start = System.nanoTime();

        clock.tick();
        double t = clock.getDaysSinceJ2000();

        for (Planet p : planets) {
            long pStart = System.nanoTime();
            p.update(t);
            DebugTimer.log("Planet update: " + p.getName(), pStart);
        }

        DebugTimer.log("SolarSystem.update()", start);
    }

    /**
     * Draw orbit paths then all bodies.
     */
    public void draw(GL2 gl) {
        long start = System.nanoTime();

        for (Planet p : planets) {
            p.drawOrbitPath(gl);
        }

        for (Planet p : planets) {
            long pStart = System.nanoTime();
            p.draw(gl);
            DebugTimer.log("Planet draw: " + p.getName(), pStart);
        }

        DebugTimer.log("SolarSystem.draw()", start);
    }

    public List<Planet> getPlanets() { return planets; }
    public SimClock getClock()       { return clock; }
}