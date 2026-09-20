package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.FederationAlienEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

public final class FederationAlienRenderer extends HumanoidMobRenderer<FederationAlienEntity, HumanoidModel<FederationAlienEntity>> {
    private static final ResourceLocation TEXTURE=new ResourceLocation(PortalGunMod.MOD_ID,"textures/entity/federation_alien.png");
    private static final ResourceLocation COMMANDER=new ResourceLocation(PortalGunMod.MOD_ID,"textures/entity/federation_commander.png");
    public FederationAlienRenderer(EntityRendererProvider.Context context){super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),.48f);}
    @Override public ResourceLocation getTextureLocation(FederationAlienEntity entity){return entity.getRank()==FederationAlienEntity.Rank.COMMANDER?COMMANDER:TEXTURE;}
    @Override protected void scale(FederationAlienEntity entity,PoseStack pose,float partial){float scale=switch(entity.getRank()){case COMMANDER->1.22f;case ELITE,PRISON_GUARD->1.10f;case SCIENTIST->.92f;default->1f;};pose.scale(scale,scale,scale);}
}
