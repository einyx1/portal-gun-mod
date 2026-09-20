package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.DianeRobotEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DianeRobotModel extends GeoModel<DianeRobotEntity> {
    @Override public ResourceLocation getModelResource(DianeRobotEntity entity) { return id("geo/diane_robot.geo.json"); }
    @Override public ResourceLocation getTextureResource(DianeRobotEntity entity) { return id("textures/entity/diane_robot.png"); }
    @Override public ResourceLocation getAnimationResource(DianeRobotEntity entity) { return id("animations/diane_robot.animation.json"); }
    private static ResourceLocation id(String path) { return new ResourceLocation(PortalGunMod.MOD_ID, path); }
}
