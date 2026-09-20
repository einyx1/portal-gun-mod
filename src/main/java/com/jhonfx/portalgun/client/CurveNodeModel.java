package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.CurveNodeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CurveNodeModel extends GeoModel<CurveNodeEntity> {
    @Override public ResourceLocation getModelResource(CurveNodeEntity e) { return id("geo/curve_node_sphere.geo.json"); }
    @Override public ResourceLocation getTextureResource(CurveNodeEntity e) { return id("textures/entity/curve_node_sphere.png"); }
    @Override public ResourceLocation getAnimationResource(CurveNodeEntity e) { return id("animations/curve_node.animation.json"); }
    private ResourceLocation id(String p) { return new ResourceLocation(PortalGunMod.MOD_ID, p); }
}
