package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.FederationDroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** Armored surveillance pod with four physical stabilizers and an energy lens. */
public final class FederationDroneRenderer extends EntityRenderer<FederationDroneEntity> {
    public FederationDroneRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .4f; }
    @Override public ResourceLocation getTextureLocation(FederationDroneEntity entity) {
        return new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    }
    @Override public void render(FederationDroneEntity entity, float yaw, float partial,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, .4, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        var blocks = Minecraft.getInstance().getBlockRenderer();
        for (int part = 0; part < 5; part++) {
            pose.pushPose();
            if (part > 0) {
                pose.mulPose(Axis.YP.rotationDegrees(part * 90));
                pose.translate(.32, -.08, 0);
                pose.scale(.45f, .12f, .16f);
            } else pose.scale(.52f, .32f, .52f);
            pose.translate(-.5, -.5, -.5);
            blocks.renderSingleBlock(Blocks.NETHERITE_BLOCK.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.translate(0, 0, -.28);
        EnergyMesh.sphere(pose, buffers.getBuffer(RenderType.lightning()), .13f, .15f, 1f, .36f, .8f);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
}
