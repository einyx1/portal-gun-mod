package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.CurveNodeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

public final class CurveNodeRenderer extends EntityRenderer<CurveNodeEntity> {
    public CurveNodeRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0f; // floating sphere, no shadow needed
    }
    @Override public ResourceLocation getTextureLocation(CurveNodeEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
    @Override public void render(CurveNodeEntity entity, float yaw, float partial,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, Math.sin((entity.tickCount + partial) * .025 + entity.getId()) * .12, 0);
        var vertices = buffers.getBuffer(RenderType.lightning());
        EnergyMesh.sphere(pose, vertices, 1.12f, 1f, .78f, .035f, .85f);
        EnergyMesh.sphere(pose, vertices, 1.23f, 1f, .86f, .1f, .16f);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
}
