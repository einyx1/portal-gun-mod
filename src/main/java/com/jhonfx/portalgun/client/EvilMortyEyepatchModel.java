package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.item.EvilMortyEyepatchItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class EvilMortyEyepatchModel extends GeoModel<EvilMortyEyepatchItem> {
    private static ResourceLocation id(String path) { return new ResourceLocation(PortalGunMod.MOD_ID, path); }
    @Override public ResourceLocation getModelResource(EvilMortyEyepatchItem item) { return id("geo/evil_morty_eyepatch.geo.json"); }
    @Override public ResourceLocation getTextureResource(EvilMortyEyepatchItem item) { return id("textures/models/armor/evil_morty_eyepatch_worn.png"); }
    @Override public ResourceLocation getAnimationResource(EvilMortyEyepatchItem item) { return id("animations/evil_morty_eyepatch.animation.json"); }
}
