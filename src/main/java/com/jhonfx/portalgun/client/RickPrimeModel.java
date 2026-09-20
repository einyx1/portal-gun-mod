package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.RickPrimeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RickPrimeModel extends GeoModel<RickPrimeEntity> {
    @Override public ResourceLocation getModelResource(RickPrimeEntity entity) {
        return id("geo/rick_prime.geo.json");
    }
    @Override public ResourceLocation getTextureResource(RickPrimeEntity entity) {
        return id("textures/entity/rick_prime.png");
    }
    @Override public ResourceLocation getAnimationResource(RickPrimeEntity entity) {
        return id("animations/rick_prime.animation.json");
    }
    private ResourceLocation id(String path) { return new ResourceLocation(PortalGunMod.MOD_ID, path); }
}
