package com.physics;

/**
 * Stores the six classical Keplerian orbital elements for a body and
 * computes its 3D heliocentric position at any given time.
 *
 * Orbital elements (J2000 epoch):
 *   a  = semi-major axis (AU)
 *   e  = eccentricity
 *   i  = inclination (degrees)
 *   Ω  = longitude of ascending node (degrees)
 *   ω  = argument of perihelion (degrees)
 *   M0 = mean anomaly at epoch (degrees)
 *   T  = orbital period (days)
 *
 * Position computation:
 *   1. Advance mean anomaly:  M = M0 + (360 / T) * Δt
 *   2. Solve Kepler's equation:  M = E − e·sin(E)  for E  (Newton-Raphson)
 *   3. True anomaly:  tan(ν/2) = √((1+e)/(1−e)) · tan(E/2)
 *   4. Radius:  r = a · (1 − e·cos(E))
 *   5. Rotate from orbital plane to 3D using i, Ω, ω
 */
public class OrbitalElements {

    // Orbital elements
    public final double a;     // semi-major axis (AU)
    public final double e;     // eccentricity
    public final double i;     // inclination (radians)
    public final double omega; // longitude of ascending node Ω (radians)
    public final double w;     // argument of perihelion ω (radians)
    public final double M0;    // mean anomaly at J2000 epoch (radians)
    public final double T;     // orbital period (days)

    // Physical
    public final double radiusKm;     // equatorial radius in km
    public final double axialTilt;    // obliquity (degrees)
    public final double rotationPeriodDays; // sidereal rotation period (days, negative = retrograde)

    public OrbitalElements(double a, double e, double iDeg, double omegaDeg,
                           double wDeg, double M0Deg, double T,
                           double radiusKm, double axialTilt, double rotationPeriodDays) {
        this.a     = a;
        this.e     = e;
        this.i     = Math.toRadians(iDeg);
        this.omega = Math.toRadians(omegaDeg);
        this.w     = Math.toRadians(wDeg);
        this.M0    = Math.toRadians(M0Deg);
        this.T     = T;
        this.radiusKm    = radiusKm;
        this.axialTilt   = axialTilt;
        this.rotationPeriodDays = rotationPeriodDays;
    }

    /**
     * Compute the mean anomaly at a given time offset from J2000.
     * @param daysSinceJ2000 elapsed days since J2000.0 epoch
     * @return mean anomaly in radians
     */
    public double meanAnomaly(double daysSinceJ2000) {
        double n = 2.0 * Math.PI / T;   // mean motion (radians/day)
        double M = M0 + n * daysSinceJ2000;
        // Normalize to [0, 2π)
        M = M % (2.0 * Math.PI);
        if (M < 0) M += 2.0 * Math.PI;
        return M;
    }

    /**
     * Solve Kepler's equation  M = E - e·sin(E)  using Newton-Raphson.
     * @param M mean anomaly (radians)
     * @return eccentric anomaly E (radians)
     */
    public double eccentricAnomaly(double M) {
        double E = M;  // initial guess
        for (int iter = 0; iter < 30; iter++) {
            double dE = (E - e * Math.sin(E) - M) / (1.0 - e * Math.cos(E));
            E -= dE;
            if (Math.abs(dE) < 1e-12) break;
        }
        return E;
    }

    /**
     * Compute the true anomaly from the eccentric anomaly.
     * @param E eccentric anomaly (radians)
     * @return true anomaly ν (radians)
     */
    public double trueAnomaly(double E) {
        double sinNu = Math.sqrt(1 - e * e) * Math.sin(E) / (1 - e * Math.cos(E));
        double cosNu = (Math.cos(E) - e) / (1 - e * Math.cos(E));
        return Math.atan2(sinNu, cosNu);
    }

    /**
     * Compute the heliocentric distance (AU).
     * @param E eccentric anomaly (radians)
     * @return distance from the Sun in AU
     */
    public double heliocentricDistance(double E) {
        return a * (1.0 - e * Math.cos(E));
    }

    /**
     * Compute the full 3D heliocentric position at a given time.
     *
     * @param daysSinceJ2000 elapsed days since J2000.0 epoch
     * @return double[3] = {x, y, z} in AU, ecliptic coordinates
     */
    public double[] position(double daysSinceJ2000) {
        double M  = meanAnomaly(daysSinceJ2000);
        double E  = eccentricAnomaly(M);
        double nu = trueAnomaly(E);
        double r  = heliocentricDistance(E);

        // Position in the orbital plane
        double xOrbital = r * Math.cos(nu);
        double yOrbital = r * Math.sin(nu);

        // Rotate by argument of perihelion (ω)
        double cosW = Math.cos(w), sinW = Math.sin(w);
        double x1 = cosW * xOrbital - sinW * yOrbital;
        double y1 = sinW * xOrbital + cosW * yOrbital;

        // Rotate by inclination (i)
        double cosI = Math.cos(i), sinI = Math.sin(i);
        double x2 = x1;
        double y2 = y1 * cosI;
        double z2 = y1 * sinI;

        // Rotate by longitude of ascending node (Ω)
        double cosO = Math.cos(omega), sinO = Math.sin(omega);
        double x3 = cosO * x2 - sinO * y2;
        double y3 = sinO * x2 + cosO * y2;
        double z3 = z2;

        return new double[]{ x3, z3, y3 };   // swap Y/Z so Y is up in OpenGL
    }

    /**
     * Self-rotation angle in degrees at a given time.
     */
    public double rotationAngle(double daysSinceJ2000) {
        if (rotationPeriodDays == 0) return 0;
        double rotationsCompleted = daysSinceJ2000 / rotationPeriodDays;
        return (rotationsCompleted * 360.0) % 360.0;
    }
}