package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.PortalColor;
import com.jhonfx.portalgun.entity.PortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Renderiza o sprite animado original como um portal emissivo na face atingida. */
public class PortalEntityRenderer extends EntityRenderer<PortalEntity> {

    private static final ResourceLocation BLUE_TEXTURE =
            new ResourceLocation("portalgun", "textures/entity/portal_blue.png");
    private static final ResourceLocation ORANGE_TEXTURE =
            new ResourceLocation("portalgun", "textures/entity/portal_orange.png");
    private static final ResourceLocation GREEN_TEXTURE =
            new ResourceLocation("portalgun", "textures/entity/portal_green.png");
    private static final int FRAME_COUNT = 40;

    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PortalEntity entity) {
        return switch (entity.getColor()) {
            case BLUE -> BLUE_TEXTURE;
            case GREEN -> GREEN_TEXTURE;
            case YELLOW -> ORANGE_TEXTURE;
        };
    }

    @Override
    public void render(PortalEntity entity, float entityYaw, float partialTicks,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float scale = entity.getPortalScale();
        Direction facing = entity.getPortalFacing();

        // Orienta o quad conforme a direção da parede/chão/teto.
        switch (facing) {
            case UP, DOWN -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
            default -> {
            }
        }

        float w = 5.0f * scale;
        float h = 4.875f * scale;

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity)));
        var matrix = poseStack.last().pose();
        int frame = Math.floorMod((int) (entity.tickCount + partialTicks) / 2, FRAME_COUNT);
        float v0 = frame / (float) FRAME_COUNT;
        float v1 = (frame + 1) / (float) FRAME_COUNT;

        consumer.vertex(matrix, -w / 2, -h / 2, 0).color(r, g, b, 1.0f).uv(0, v1).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, w / 2, -h / 2, 0).color(r, g, b, 1.0f).uv(1, v1).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, w / 2, h / 2, 0).color(r, g, b, 1.0f).uv(1, v0).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        consumer.vertex(matrix, -w / 2, h / 2, 0).color(r, g, b, 1.0f).uv(0, v0).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();

        // Reverse winding makes the portal visible from both sides.
        consumer.vertex(matrix, -w / 2, h / 2, -0.006f).color(r, g, b, 1.0f).uv(0, v0).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, w / 2, h / 2, -0.006f).color(r, g, b, 1.0f).uv(1, v0).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, w / 2, -h / 2, -0.006f).color(r, g, b, 1.0f).uv(1, v1).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, -w / 2, -h / 2, -0.006f).color(r, g, b, 1.0f).uv(0, v1).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, -1).endVertex();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
