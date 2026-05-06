package com.physics;

public class DebugTimer {

    private static long lastFrameTime = System.nanoTime();

    public static void log(String label, long startTime) {
        long end = System.nanoTime();
        double ms = (end - startTime) / 1_000_000.0;
        System.out.printf("[DEBUG] %-30s : %.3f ms%n", label, ms);
    }

    public static void logFrame() {
        long now = System.nanoTime();
        double fps = 1_000_000_000.0 / (now - lastFrameTime);
        lastFrameTime = now;

        System.out.printf("[FPS] %.2f%n", fps);
    }
}