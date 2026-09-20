package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.EvilMortyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class EvilMortyModel extends GeoModel<EvilMortyEntity> {
    @Override public ResourceLocation getModelResource(EvilMortyEntity e){return id("geo/evil_morty.geo.json");}
    @Override public ResourceLocation getTextureResource(EvilMortyEntity e){return id("textures/entity/evil_morty.png");}
    @Override public ResourceLocation getAnimationResource(EvilMortyEntity e){return id("animations/evil_morty.animation.json");}
    private static ResourceLocation id(String path){return new ResourceLocation(PortalGunMod.MOD_ID,path);}
}
