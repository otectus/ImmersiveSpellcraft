package com.otectus.immersivespellcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * A small vector batch for the panel: thick lines, arcs, rings and rectangles as colored triangles. Culling is off for
 * the batch, since the winding of a line or arc segment depends on its direction and GUI culling would drop half of them.
 */
public final class Draw {
    private final BufferBuilder buffer;
    private final Matrix4f matrix;

    private Draw(GuiGraphics graphics) {
        graphics.flush();
        matrix = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
    }

    public static Draw begin(GuiGraphics graphics) {
        return new Draw(graphics);
    }

    public void end() {
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void v(float x, float y, int c) {
        buffer.vertex(matrix, x, y, 0).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF).endVertex();
    }

    public Draw tri(float x0, float y0, float x1, float y1, float x2, float y2, int c) {
        v(x0, y0, c); v(x1, y1, c); v(x2, y2, c);
        return this;
    }

    public Draw quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int c) {
        tri(x0, y0, x1, y1, x2, y2, c);
        return tri(x0, y0, x2, y2, x3, y3, c);
    }

    public Draw rect(float x0, float y0, float x1, float y1, int c) {
        return quad(x0, y0, x0, y1, x1, y1, x1, y0, c);
    }

    public Draw frame(float x0, float y0, float x1, float y1, float w, int c) {
        rect(x0, y0, x1, y0 + w, c);
        rect(x0, y1 - w, x1, y1, c);
        rect(x0, y0 + w, x0 + w, y1 - w, c);
        return rect(x1 - w, y0 + w, x1, y1 - w, c);
    }

    public Draw line(float x0, float y0, float x1, float y1, float width, int c) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return this;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        return quad(x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, c);
    }

    /** Ring segment between radii {@code r0} and {@code r1} from angle {@code a0} to {@code a1} (radians, 0 = right). */
    public Draw arc(float cx, float cy, float r0, float r1, double a0, double a1, int c) {
        int n = Math.max(4, (int) Math.ceil(Math.abs(a1 - a0) / (Math.PI / 36)));
        for (int i = 0; i < n; i++) {
            double t0 = a0 + (a1 - a0) * i / n, t1 = a0 + (a1 - a0) * (i + 1) / n;
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            quad(cx + c0 * r0, cy + s0 * r0, cx + c0 * r1, cy + s0 * r1, cx + c1 * r1, cy + s1 * r1, cx + c1 * r0, cy + s1 * r0, c);
        }
        return this;
    }

    public Draw disc(float cx, float cy, float r, int c) {
        int n = Math.max(10, (int) (r * 2.5f));
        for (int i = 0; i < n; i++) {
            double t0 = 2 * Math.PI * i / n, t1 = 2 * Math.PI * (i + 1) / n;
            tri(cx, cy, cx + (float) Math.cos(t0) * r, cy + (float) Math.sin(t0) * r, cx + (float) Math.cos(t1) * r, cy + (float) Math.sin(t1) * r, c);
        }
        return this;
    }

    public Draw ring(float cx, float cy, float r, float width, int c) {
        return arc(cx, cy, r - width / 2, r + width / 2, 0, 2 * Math.PI, c);
    }
}
