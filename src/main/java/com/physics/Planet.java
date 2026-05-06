package com.physics;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import java.nio.FloatBuffer;

/**
 * A planet (or the Sun) rendered with accurate orbital mechanics.
 *
 * Position is computed from Keplerian orbital elements at the current
 * simulation time.  Rendering uses a display scale factor to convert
 * AU to OpenGL units so everything is visible.
 *
 * All static geometry (sphere, orbit path, ring) is uploaded to GPU VBOs
 * once in init() and drawn with a single glDrawElements/glDrawArrays call.
 */
public class Planet {

    private final String name;
    private final OrbitalElements orbit;   // null for the Sun
    private final Sphere mesh;
    private final boolean isSun;
    private final boolean hasRing;

    // Material colors — stored as fields to avoid per-frame allocation
    private final float[] diffuseColor;
    private final float[] ambientColor;

    private static final float[] SUN_EMISSION    = { 1.0f, 0.85f, 0.3f,  1f };
    private static final float[] SUN_SPECULAR    = { 0f,   0f,    0f,    1f };
    private static final float[] ZERO_EMISSION   = { 0f,   0f,    0f,    1f };
    private static final float[] PLANET_SPECULAR = { 0.3f, 0.3f,  0.3f,  1f };

    // Display scaling
    private static final float DISTANCE_SCALE    = 8.0f;
    private static final float SUN_DISPLAY_RADIUS = 0.8f;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final float  PLANET_BASE_SIZE = 0.15f;

    // Computed each frame
    private float displayX, displayY, displayZ;
    private final float displayRadius;
    private double selfRotationAngle;

    // Orbit path VBO
    private static final int ORBIT_SEGMENTS = 256;
    private int vboOrbitPath = 0;

    // Ring VBO (Saturn only)
    private static final int RING_SEGMENTS = 80;
    private int vboRing = 0;
    private int ringVertexCount = 0;

    public Planet(String name, OrbitalElements orbit,
                  float[] diffuseColor, float[] ambientColor, boolean hasRing) {
        this.name         = name;
        this.orbit        = orbit;
        this.isSun        = (orbit == null);
        this.hasRing      = hasRing;
        this.diffuseColor = diffuseColor;
        this.ambientColor = ambientColor;

        if (isSun) {
            this.displayRadius = SUN_DISPLAY_RADIUS;
        } else {
            float relativeSize = (float)(orbit.radiusKm / EARTH_RADIUS_KM);
            float r = PLANET_BASE_SIZE * relativeSize;
            this.displayRadius = Math.min(0.45f, Math.max(0.04f, r));
        }

        int detail = displayRadius > 0.25f ? 48 : 32;
        this.mesh = new Sphere(displayRadius, detail, detail);
    }

    public void init(GL2 gl) {
        mesh.init(gl);

        if (!isSun && orbit != null) {
            vboOrbitPath = uploadOrbitPath(gl);
        }

        if (hasRing) {
            vboRing = uploadRing(gl);
        }
    }

    private int uploadOrbitPath(GL2 gl) {
        float[] verts = buildOrbitPathVertices();

        int[] ids = new int[1];
        gl.glGenBuffers(1, ids, 0);
        FloatBuffer fb = Buffers.newDirectFloatBuffer(verts);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, ids[0]);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, (long) verts.length * Float.BYTES, fb, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        return ids[0];
    }

    private float[] buildOrbitPathVertices() {
        float[] verts = new float[ORBIT_SEGMENTS * 3];
        int ptr = 0;

        double cosW = Math.cos(orbit.w),     sinW = Math.sin(orbit.w);
        double cosI = Math.cos(orbit.i),     sinI = Math.sin(orbit.i);
        double cosO = Math.cos(orbit.omega), sinO = Math.sin(orbit.omega);

        for (int seg = 0; seg < ORBIT_SEGMENTS; seg++) {
            double M  = 2.0 * Math.PI * seg / ORBIT_SEGMENTS;
            double E  = orbit.eccentricAnomaly(M);
            double nu = orbit.trueAnomaly(E);
            double r  = orbit.heliocentricDistance(E);

            double xOrb = r * Math.cos(nu);
            double yOrb = r * Math.sin(nu);

            double x1 = cosW * xOrb - sinW * yOrb;
            double y1 = sinW * xOrb + cosW * yOrb;

            double x2 = x1;
            double y2 = y1 * cosI;
            double z2 = y1 * sinI;

            double x3 = cosO * x2 - sinO * y2;
            double y3 = sinO * x2 + cosO * y2;
            double z3 = z2;

            verts[ptr++] = (float)(x3 * DISTANCE_SCALE);
            verts[ptr++] = (float)(z3 * DISTANCE_SCALE);
            verts[ptr++] = (float)(y3 * DISTANCE_SCALE);
        }
        return verts;
    }

    private int uploadRing(GL2 gl) {
        float inner = displayRadius * 1.3f;
        float outer = displayRadius * 2.3f;
        ringVertexCount = (RING_SEGMENTS + 1) * 2;
        float[] verts = new float[ringVertexCount * 3];
        int ptr = 0;

        for (int seg = 0; seg <= RING_SEGMENTS; seg++) {
            float angle = (float)(2.0 * Math.PI * seg / RING_SEGMENTS);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            verts[ptr++] = cos * inner; verts[ptr++] = 0f; verts[ptr++] = sin * inner;
            verts[ptr++] = cos * outer; verts[ptr++] = 0f; verts[ptr++] = sin * outer;
        }

        int[] ids = new int[1];
        gl.glGenBuffers(1, ids, 0);
        FloatBuffer fb = Buffers.newDirectFloatBuffer(verts);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, ids[0]);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, (long) verts.length * Float.BYTES, fb, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        return ids[0];
    }

    public void update(double daysSinceJ2000) {
        if (isSun) {
            displayX = displayY = displayZ = 0f;
            selfRotationAngle = 0;
            return;
        }

        double[] pos = orbit.position(daysSinceJ2000);
        displayX = (float)(pos[0] * DISTANCE_SCALE);
        displayY = (float)(pos[1] * DISTANCE_SCALE);
        displayZ = (float)(pos[2] * DISTANCE_SCALE);

        selfRotationAngle = orbit.rotationAngle(daysSinceJ2000);
    }

    public void draw(GL2 gl) {
        gl.glPushMatrix();

        gl.glTranslatef(displayX, displayY, displayZ);

        if (!isSun && orbit != null) {
            gl.glRotatef((float) orbit.axialTilt, 0f, 0f, 1f);
            gl.glRotatef((float) selfRotationAngle, 0f, 1f, 0f);
        }

        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE,  diffuseColor, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT,  ambientColor, 0);

        if (isSun) {
            gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION,  SUN_EMISSION, 0);
            gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR,  SUN_SPECULAR, 0);
        } else {
            gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION,  ZERO_EMISSION,   0);
            gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR,  PLANET_SPECULAR, 0);
            gl.glMaterialf(GL2.GL_FRONT,  GL2.GL_SHININESS, 32f);
        }

        mesh.draw(gl);

        if (hasRing) {
            drawRing(gl);
        }

        gl.glPopMatrix();
    }

    private void drawRing(GL2 gl) {
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_EMISSION,
                new float[]{ 0f, 0f, 0f, 0.85f }, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE,
                new float[]{ 0.76f, 0.70f, 0.50f, 0.85f }, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT,
                new float[]{ 0.3f, 0.28f, 0.2f, 0.85f }, 0);

        gl.glNormal3f(0f, 1f, 0f);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboRing);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0L);
        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, ringVertexCount);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }

    public void drawOrbitPath(GL2 gl) {
        if (isSun || orbit == null || vboOrbitPath == 0) return;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(0.2f, 0.2f, 0.3f);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboOrbitPath);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0L);
        gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, ORBIT_SEGMENTS);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);

        gl.glEnable(GL2.GL_LIGHTING);
    }

    public void dispose(GL2 gl) {
        mesh.dispose(gl);
        if (vboOrbitPath != 0) {
            gl.glDeleteBuffers(1, new int[]{ vboOrbitPath }, 0);
            vboOrbitPath = 0;
        }
        if (vboRing != 0) {
            gl.glDeleteBuffers(1, new int[]{ vboRing }, 0);
            vboRing = 0;
        }
    }

    public String getName()    { return name; }
    public float getDisplayX() { return displayX; }
    public float getDisplayY() { return displayY; }
    public float getDisplayZ() { return displayZ; }
}
