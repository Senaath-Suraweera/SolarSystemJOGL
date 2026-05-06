package com.physics;

/**
 * Simulation clock that maps wall-clock time to simulation time.
 *
 * The simulation starts at the current real date/time (computed as days
 * since J2000.0 = 2000-01-01T12:00:00 TT).  A speed multiplier lets
 * the user fast-forward or slow down time.
 *
 * Speed multiplier examples:
 *    1        → real time (1 second = 1 second)
 *    86400    → 1 second of wall time = 1 day of sim time
 *    2592000  → 1 second = 30 days (watch orbits in real time)
 */
public class SimClock {

    /** J2000.0 epoch in milliseconds since Unix epoch */
    private static final long J2000_MILLIS = 946728000000L; // 2000-01-01T12:00:00 UTC

    private double speedMultiplier;   // sim seconds per real second
    private double simDaysSinceJ2000; // current simulation time
    private long   lastWallTimeNanos;
    private boolean paused = false;

    public SimClock(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;

        // Start simulation at the current real date
        long nowMillis = System.currentTimeMillis();
        this.simDaysSinceJ2000 = (nowMillis - J2000_MILLIS) / (1000.0 * 86400.0);
        this.lastWallTimeNanos = System.nanoTime();
    }

    /**
     * Advance the simulation clock. Call once per frame.
     */
    public void tick() {
        long now = System.nanoTime();
        if (!paused) {
            double realElapsedSeconds = (now - lastWallTimeNanos) / 1_000_000_000.0;
            double simElapsedSeconds  = realElapsedSeconds * speedMultiplier;
            double simElapsedDays     = simElapsedSeconds / 86400.0;
            simDaysSinceJ2000 += simElapsedDays;
        }
        lastWallTimeNanos = now;
    }

    /** Current simulation time as days since J2000.0 */
    public double getDaysSinceJ2000() {
        return simDaysSinceJ2000;
    }

    /** Speed up */
    public void faster() {
        speedMultiplier *= 2.0;
        if (speedMultiplier > 1e9) speedMultiplier = 1e9;
    }

    /** Slow down */
    public void slower() {
        speedMultiplier /= 2.0;
        if (speedMultiplier < 1.0) speedMultiplier = 1.0;
    }

    /** Toggle pause */
    public void togglePause() {
        paused = !paused;
    }

    /** Reset to current real date */
    public void resetToNow() {
        long nowMillis = System.currentTimeMillis();
        simDaysSinceJ2000 = (nowMillis - J2000_MILLIS) / (1000.0 * 86400.0);
    }

    public double getSpeedMultiplier() { return speedMultiplier; }
    public boolean isPaused()          { return paused; }

    /**
     * Convert current sim time to a rough calendar date string.
     */
    public String getDateString() {
        // J2000.0 = 2000-01-01T12:00
        long simMillis = J2000_MILLIS + (long)(simDaysSinceJ2000 * 86400.0 * 1000.0);
        java.util.Date date = new java.util.Date(simMillis);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}