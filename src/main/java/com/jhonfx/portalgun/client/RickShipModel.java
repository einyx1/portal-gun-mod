package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.RickShipEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RickShipModel extends GeoModel<RickShipEntity> {
    @Override public ResourceLocation getModelResource(RickShipEntity entity) { return id("geo/rick_ship.geo.json"); }
    @Override public ResourceLocation getTextureResource(RickShipEntity entity) { return id("textures/entity/rick_ship.png"); }
    @Override public ResourceLocation getAnimationResource(RickShipEntity entity) { return id("animations/rick_ship.animation.json"); }
    private static ResourceLocation id(String path) { return new ResourceLocation(PortalGunMod.MOD_ID, path); }
}
