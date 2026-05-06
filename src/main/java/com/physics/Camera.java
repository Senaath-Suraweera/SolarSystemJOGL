package com.physics;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;

/**
 * First-person camera controlled by mouse (look) and keyboard (move).
 *
 * Orientation is stored as yaw (horizontal rotation) and pitch (vertical tilt).
 * From these we derive a forward, right, and up vector each frame to position
 * the camera via gluLookAt.
 *
 * Controls:
 *   Mouse drag  → look around (yaw & pitch)
 *   W / S       → move forward / backward
 *   A / D       → strafe left / right
 *   Q / E       → move down / up
 *   Shift       → move faster (sprint)
 */
public class Camera {

    // Position in world space
    private float posX, posY, posZ;

    // Euler angles (degrees)
    private float yaw;    // left/right rotation  (around Y axis)
    private float pitch;  // up/down tilt          (around X axis, clamped ±89°)

    // Movement speeds
    private float moveSpeed   = 0.05f;
    private float sprintMult  = 3.0f;
    private float sensitivity = 0.3f;   // mouse look sensitivity

    // Derived direction vectors (recalculated each frame)
    private float forwardX, forwardY, forwardZ;
    private float rightX,   rightY,   rightZ;
    private float upX,      upY,      upZ;

    // Input state — set by the input handler, consumed each frame
    private boolean moveForward, moveBackward;
    private boolean strafeLeft,  strafeRight;
    private boolean moveUp,      moveDown;
    private boolean sprinting;

    // Mouse dragging state
    private boolean dragging;
    private int lastMouseX, lastMouseY;

    private final GLU glu = new GLU();

    public Camera(float x, float y, float z, float yaw, float pitch) {
        this.posX  = x;
        this.posY  = y;
        this.posZ  = z;
        this.yaw   = yaw;
        this.pitch = pitch;
        updateVectors();
    }

    /* ------------------------------------------------------------------ */
    /*  Direction vectors from yaw/pitch                                   */
    /* ------------------------------------------------------------------ */

    private void updateVectors() {
        float yawRad   = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Forward vector (where the camera is looking)
        forwardX = (float) (Math.cos(pitchRad) * Math.sin(yawRad));
        forwardY = (float) (Math.sin(pitchRad));
        forwardZ = (float) (Math.cos(pitchRad) * -Math.cos(yawRad));

        // Right vector (perpendicular to forward on the XZ plane)
        rightX = (float) Math.cos(yawRad);
        rightY = 0;
        rightZ = (float) Math.sin(yawRad);

        // Up vector (cross product of right × forward)
        upX = rightY * forwardZ - rightZ * forwardY;
        upY = rightZ * forwardX - rightX * forwardZ;
        upZ = rightX * forwardY - rightY * forwardX;
    }

    /* ------------------------------------------------------------------ */
    /*  Called once per frame: apply movement then set the GL view matrix  */
    /* ------------------------------------------------------------------ */

    public void applyMovementAndLookAt(GL2 gl) {
        // -- Process movement --
        float speed = moveSpeed * (sprinting ? sprintMult : 1f);

        if (moveForward)  { posX += forwardX * speed; posY += forwardY * speed; posZ += forwardZ * speed; }
        if (moveBackward) { posX -= forwardX * speed; posY -= forwardY * speed; posZ -= forwardZ * speed; }
        if (strafeRight)  { posX += rightX * speed;   posY += rightY * speed;   posZ += rightZ * speed; }
        if (strafeLeft)   { posX -= rightX * speed;   posY -= rightY * speed;   posZ -= rightZ * speed; }
        if (moveUp)       { posY += speed; }
        if (moveDown)     { posY -= speed; }

        updateVectors();

        // -- Set the modelview matrix --
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(
                posX,              posY,              posZ,               // eye
                posX + forwardX,   posY + forwardY,   posZ + forwardZ,   // center (look-at)
                upX,               upY,               upZ                // up
        );
    }

    /* ------------------------------------------------------------------ */
    /*  Mouse look                                                         */
    /* ------------------------------------------------------------------ */

    public void startDrag(int x, int y) {
        dragging   = true;
        lastMouseX = x;
        lastMouseY = y;
    }

    public void stopDrag() {
        dragging = false;
    }

    public void drag(int x, int y) {
        if (!dragging) return;

        int dx = x - lastMouseX;
        int dy = y - lastMouseY;
        lastMouseX = x;
        lastMouseY = y;

        yaw   += dx * sensitivity;
        pitch -= dy * sensitivity;   // invert Y so dragging up looks up

        // Clamp pitch to prevent flipping
        if (pitch >  89f) pitch =  89f;
        if (pitch < -89f) pitch = -89f;

        updateVectors();
    }

    /* ------------------------------------------------------------------ */
    /*  Scroll zoom (move along the forward axis)                          */
    /* ------------------------------------------------------------------ */

    private float zoomSpeed = 0.5f;

    public void scroll(float rotation) {
        // Scroll up (negative rotation) → zoom in (move forward)
        // Scroll down (positive rotation) → zoom out (move backward)
        posX -= forwardX * rotation * zoomSpeed;
        posY -= forwardY * rotation * zoomSpeed;
        posZ -= forwardZ * rotation * zoomSpeed;
    }

    /* ------------------------------------------------------------------ */
    /*  Keyboard input flags                                               */
    /* ------------------------------------------------------------------ */

    public void keyPressed(short keyCode, char keyChar) {
        switch (Character.toLowerCase(keyChar)) {
            case 'w' -> moveForward  = true;
            case 's' -> moveBackward = true;
            case 'a' -> strafeLeft   = true;
            case 'd' -> strafeRight  = true;
            case 'q' -> moveDown     = true;
            case 'e' -> moveUp       = true;
        }
        // NEWT key codes: Shift
        if (keyCode == com.jogamp.newt.event.KeyEvent.VK_SHIFT) {
            sprinting = true;
        }
    }

    public void keyReleased(short keyCode, char keyChar) {
        switch (Character.toLowerCase(keyChar)) {
            case 'w' -> moveForward  = false;
            case 's' -> moveBackward = false;
            case 'a' -> strafeLeft   = false;
            case 'd' -> strafeRight  = false;
            case 'q' -> moveDown     = false;
            case 'e' -> moveUp       = false;
        }
        if (keyCode == com.jogamp.newt.event.KeyEvent.VK_SHIFT) {
            sprinting = false;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Getters                                                            */
    /* ------------------------------------------------------------------ */

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }
}