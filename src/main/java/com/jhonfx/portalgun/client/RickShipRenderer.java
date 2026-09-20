package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.RickShipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class RickShipRenderer extends GeoEntityRenderer<RickShipEntity> {
    public RickShipRenderer(EntityRendererProvider.Context context) { super(context, new RickShipModel()); shadowRadius = 1.8f; }
    @Override public void render(RickShipEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose(); pose.scale(1.05f, 1.05f, 1.05f); super.render(entity, yaw, partialTick, pose, buffers, light); pose.popPose();
    }
}
