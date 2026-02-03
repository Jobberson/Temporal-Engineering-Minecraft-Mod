package com.snog.temporalengineering.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snog.temporalengineering.common.blockentity.TemporalFieldGeneratorBlockEntity;
import com.snog.temporalengineering.common.config.TemporalConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class TemporalFieldGeneratorRenderer implements BlockEntityRenderer<TemporalFieldGeneratorBlockEntity>
{
    public TemporalFieldGeneratorRenderer(BlockEntityRendererProvider.Context ctx)
    {
    }

    @Override
    public void render(TemporalFieldGeneratorBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (be == null || be.getLevel() == null)
        {
            return;
        }

        if (!be.getShowArea())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }

        // Distance LOD
        Vec3 cam = mc.player.position();
        double dx = (be.getBlockPos().getX() + 0.5) - cam.x;
        double dy = (be.getBlockPos().getY() + 0.5) - cam.y;
        double dz = (be.getBlockPos().getZ() + 0.5) - cam.z;
        double distSqr = dx * dx + dy * dy + dz * dz;

        int segments = 12;
        if (distSqr < 12.0 * 12.0)
        {
            segments = 32;
        }
        else if (distSqr < 24.0 * 24.0)
        {
            segments = 24;
        }
        else if (distSqr < 48.0 * 48.0)
        {
            segments = 16;
        }

        float radius = TemporalConfig.FIELD_RADIUS.get();
        if (radius <= 0.0f)
        {
            return;
        }

        // Color: subtle blue, faint alpha
        float r = 0.25f;
        float g = 0.65f;
        float b = 1.00f;
        float a = 0.18f;

        var consumer = buffer.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Three great circles: XZ, XY, YZ
        drawCircleXZ(poseStack, consumer, radius, segments, r, g, b, a);
        drawCircleXY(poseStack, consumer, radius, segments, r, g, b, a);
        drawCircleYZ(poseStack, consumer, radius, segments, r, g, b, a);

        poseStack.popPose();
    }

    private void drawCircleXZ(PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer consumer, float radius, int segments, float r, float g, float b, float a)
    {
        float step = (float) (Math.PI * 2.0 / segments);

        for (int i = 0; i < segments; i++)
        {
            float a0 = i * step;
            float a1 = (i + 1) * step;

            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;

            line(poseStack, consumer, x0, 0.0f, z0, x1, 0.0f, z1, r, g, b, a);
        }
    }

    private void drawCircleXY(PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer consumer, float radius, int segments, float r, float g, float b, float a)
    {
        float step = (float) (Math.PI * 2.0 / segments);

        for (int i = 0; i < segments; i++)
        {
            float a0 = i * step;
            float a1 = (i + 1) * step;

            float x0 = (float) Math.cos(a0) * radius;
            float y0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;

            line(poseStack, consumer, x0, y0, 0.0f, x1, y1, 0.0f, r, g, b, a);
        }
    }

    private void drawCircleYZ(PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer consumer, float radius, int segments, float r, float g, float b, float a)
    {
        float step = (float) (Math.PI * 2.0 / segments);

        for (int i = 0; i < segments; i++)
        {
            float a0 = i * step;
            float a1 = (i + 1) * step;

            float y0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float y1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;

            line(poseStack, consumer, 0.0f, y0, z0, 0.0f, y1, z1, r, g, b, a);
        }
    }

    private void line(PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                      float x0, float y0, float z0, float x1, float y1, float z1,
                      float r, float g, float b, float a)
    {
        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();

        consumer.vertex(pose, x0, y0, z0).color(r, g, b, a).normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
        consumer.vertex(pose, x1, y1, z1).color(r, g, b, a).normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
    }
}