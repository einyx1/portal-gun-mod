package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.FederationHeavyTrooperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Uses the Federation skin supplied by the player, fitted to the heavy's hitbox. */
public final class FederationHeavyRenderer extends HumanoidMobRenderer<FederationHeavyTrooperEntity, HumanoidModel<FederationHeavyTrooperEntity>> {
    public FederationHeavyRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), .65f);
    }
    @Override protected void scale(FederationHeavyTrooperEntity entity, PoseStack pose, float partial) {
        pose.scale(1.55f, 1.35f, 1.55f);
    }
    @Override public ResourceLocation getTextureLocation(FederationHeavyTrooperEntity entity) {
        return new ResourceLocation("portalgun", "textures/entity/federation_alien.png");
    }
}
