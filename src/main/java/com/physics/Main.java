package com.physics;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;

/**
 * Entry point: solar system simulation with real orbital mechanics.
 *
 * Controls:
 *   Mouse drag    → look around
 *   Scroll        → zoom in/out
 *   W/A/S/D       → move forward / left / backward / right
 *   Q/E           → move down / up
 *   Shift         → sprint (3× speed)
 *   +/=           → speed up time (2×)
 *   -             → slow down time (½)
 *   Space         → pause/resume
 *   R             → reset to today's date
 *   Escape        → exit
 */
public class Main {

    private static final int WIDTH  = 1024;
    private static final int HEIGHT = 768;
    private static final int FPS    = 60;

    public static void main(String[] args) {
        // -- GL setup --
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(true);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);

        // -- Camera: start above and behind, looking down at the system --
        Camera camera = new Camera(0f, 20f, 45f, -90f, -25f);

        // -- Simulation clock: default 1 day/sec so you can see orbits --
        SimClock clock = new SimClock(86400.0 * 3);  // 3 days per real second

        // -- Window --
        GLWindow window = GLWindow.create(caps);
        window.setTitle("Solar System Simulation");
        window.setSize(WIDTH, HEIGHT);
        window.addGLEventListener(new SphereRenderer(camera, clock));

        // -- Mouse listener --
        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                camera.startDrag(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                camera.stopDrag();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                camera.drag(e.getX(), e.getY());
            }

            @Override
            public void mouseWheelMoved(MouseEvent e) {
                camera.scroll(e.getRotation()[1]);
            }
        });

        // -- Keyboard listener --
        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                short code = e.getKeyCode();
                char  ch   = e.getKeyChar();

                // Exit
                if (code == KeyEvent.VK_ESCAPE) {
                    new Thread(() -> { window.destroy(); System.exit(0); }).start();
                    return;
                }

                // Time controls
                if (code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_ADD
                        || ch == '+' || ch == '=') {
                    clock.faster();
                    return;
                }
                if (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT
                        || ch == '-') {
                    clock.slower();
                    return;
                }
                if (code == KeyEvent.VK_SPACE) {
                    clock.togglePause();
                    return;
                }
                if (ch == 'r' || ch == 'R') {
                    clock.resetToNow();
                    return;
                }

                // Movement
                camera.keyPressed(code, ch);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                camera.keyReleased(e.getKeyCode(), e.getKeyChar());
            }
        });

        // -- Animator --
        FPSAnimator animator = new FPSAnimator(window, FPS, true);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                new Thread(() -> { animator.stop(); System.exit(0); }).start();
            }
        });

        window.setVisible(true);
        animator.start();
    }
}