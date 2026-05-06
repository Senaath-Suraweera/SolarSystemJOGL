package com.physics;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Font;
import java.awt.Color;

/**
 * Renders the solar system with accurate orbital mechanics.
 *
 * Displays a HUD showing the current simulation date and time controls.
 * The light source sits at the origin (inside the Sun).
 */
public class SphereRenderer implements GLEventListener {

    private final GLU glu = new GLU();
    private final Camera camera;
    private final SimClock clock;
    private SolarSystem solarSystem;
    private TextRenderer textRenderer;
    private int viewportWidth, viewportHeight;

    public SphereRenderer(Camera camera, SimClock clock) {
        this.camera = camera;
        this.clock  = clock;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0.01f, 0.01f, 0.03f, 1f);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        // Light at origin (Sun)
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        float[] lightAmb  = { 0.1f, 0.1f, 0.1f, 1f };
        float[] lightDiff = { 1.0f, 0.95f, 0.85f, 1f };
        float[] lightSpec = { 1f, 1f, 1f, 1f };

        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT,  lightAmb, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE,  lightDiff, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpec, 0);

        gl.glLightf(GL2.GL_LIGHT0, GL2.GL_CONSTANT_ATTENUATION,  1.0f);
        gl.glLightf(GL2.GL_LIGHT0, GL2.GL_LINEAR_ATTENUATION,    0.003f);
        gl.glLightf(GL2.GL_LIGHT0, GL2.GL_QUADRATIC_ATTENUATION, 0.0005f);

        // Build solar system and upload all geometry to the GPU
        solarSystem = new SolarSystem(clock);
        solarSystem.init(gl);

        // HUD text
        textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 14));
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        // Camera
        camera.applyMovementAndLookAt(gl);

        // Light at Sun
        float[] lightPos = { 0f, 0f, 0f, 1f };
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);

        // Update & draw
        solarSystem.update();
        solarSystem.draw(gl);

        // HUD overlay
        drawHUD(gl);
    }

    /**
     * Draw a 2D text overlay with date, speed, and controls.
     */
    private void drawHUD(GL2 gl) {
        textRenderer.beginRendering(viewportWidth, viewportHeight);

        // Date & speed (top-left)
        textRenderer.setColor(Color.WHITE);
        String dateStr = "Date: " + clock.getDateString();
        textRenderer.draw(dateStr, 10, viewportHeight - 22);

        String speedStr;
        double mult = clock.getSpeedMultiplier();
        if (mult >= 86400) {
            speedStr = String.format("Speed: %.0f days/sec", mult / 86400.0);
        } else if (mult >= 3600) {
            speedStr = String.format("Speed: %.0f hrs/sec", mult / 3600.0);
        } else {
            speedStr = String.format("Speed: %.0fx", mult);
        }
        if (clock.isPaused()) speedStr += "  [PAUSED]";
        textRenderer.draw(speedStr, 10, viewportHeight - 42);

        // Controls (bottom-left)
        textRenderer.setColor(new Color(0.6f, 0.6f, 0.7f));
        textRenderer.draw("WASD: move  |  Mouse: look  |  Scroll: zoom", 10, 38);
        textRenderer.draw("+/-: speed  |  Space: pause  |  R: reset date  |  Esc: quit", 10, 18);

        textRenderer.endRendering();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height == 0) height = 1;

        viewportWidth  = width;
        viewportHeight = height;

        float aspect = (float) width / height;
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        // Far plane at 500 to see Neptune orbit (30 AU × 8 scale = 240 units)
        glu.gluPerspective(45.0, aspect, 0.1, 500.0);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        if (solarSystem != null) {
            solarSystem.dispose(gl);
        }
        if (textRenderer != null) {
            textRenderer.dispose();
        }
    }
}