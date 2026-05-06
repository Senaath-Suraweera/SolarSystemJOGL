package com.physics;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Sphere geometry stored in GPU-side VBOs.
 * Vertex layout (interleaved, 6 floats per vertex): x y z nx ny nz
 */
public class Sphere {

    private final float[] vertices;
    private final int[] indices;

    private int vboVertex = 0;
    private int vboIndex  = 0;
    private int indexCount;

    public Sphere(float radius, int stacks, int sectors) {
        vertices   = buildVertices(radius, stacks, sectors);
        indices    = buildIndices(stacks, sectors);
        indexCount = indices.length;
    }

    private static float[] buildVertices(float radius, int stacks, int sectors) {
        float[] verts = new float[(stacks + 1) * (sectors + 1) * 6];
        int idx = 0;

        float stackStep  = (float)(Math.PI / stacks);
        float sectorStep = (float)(2.0 * Math.PI / sectors);

        for (int i = 0; i <= stacks; i++) {
            float phi = (float)(Math.PI / 2) - i * stackStep;
            float y   = radius * (float) Math.sin(phi);
            float xzR = radius * (float) Math.cos(phi);

            for (int j = 0; j <= sectors; j++) {
                float theta = j * sectorStep;
                float x = xzR * (float) Math.cos(theta);
                float z = xzR * (float) Math.sin(theta);

                verts[idx++] = x;
                verts[idx++] = y;
                verts[idx++] = z;
                verts[idx++] = x / radius;
                verts[idx++] = y / radius;
                verts[idx++] = z / radius;
            }
        }
        return verts;
    }

    private static int[] buildIndices(int stacks, int sectors) {
        int[] idx = new int[stacks * sectors * 6];
        int ptr = 0;

        for (int i = 0; i < stacks; i++) {
            int k1 = i * (sectors + 1);
            int k2 = k1 + (sectors + 1);

            for (int j = 0; j < sectors; j++, k1++, k2++) {
                if (i != 0) {
                    idx[ptr++] = k1;
                    idx[ptr++] = k2;
                    idx[ptr++] = k1 + 1;
                }
                if (i != stacks - 1) {
                    idx[ptr++] = k1 + 1;
                    idx[ptr++] = k2;
                    idx[ptr++] = k2 + 1;
                }
            }
        }

        int[] trimmed = new int[ptr];
        System.arraycopy(idx, 0, trimmed, 0, ptr);
        return trimmed;
    }

    public void init(GL2 gl) {
        int[] ids = new int[2];
        gl.glGenBuffers(2, ids, 0);
        vboVertex = ids[0];
        vboIndex  = ids[1];

        FloatBuffer vb = Buffers.newDirectFloatBuffer(vertices);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboVertex);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, vb, GL2.GL_STATIC_DRAW);

        IntBuffer ib = Buffers.newDirectIntBuffer(indices);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, vboIndex);
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, (long) indices.length * Integer.BYTES, ib, GL2.GL_STATIC_DRAW);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void draw(GL2 gl) {
        final int stride = 6 * Float.BYTES;

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboVertex);
        gl.glVertexPointer(3, GL2.GL_FLOAT, stride, 0L);
        gl.glNormalPointer(GL2.GL_FLOAT, stride, 3L * Float.BYTES);

        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, vboIndex);
        gl.glDrawElements(GL2.GL_TRIANGLES, indexCount, GL2.GL_UNSIGNED_INT, 0L);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    }

    public void dispose(GL2 gl) {
        if (vboVertex != 0) {
            gl.glDeleteBuffers(2, new int[]{ vboVertex, vboIndex }, 0);
            vboVertex = vboIndex = 0;
        }
    }
}
