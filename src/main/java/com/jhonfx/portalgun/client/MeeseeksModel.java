package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.MeeseeksEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MeeseeksModel extends GeoModel<MeeseeksEntity>{
    @Override public ResourceLocation getModelResource(MeeseeksEntity e){return id("geo/meeseeks.geo.json");}
    @Override public ResourceLocation getTextureResource(MeeseeksEntity e){return id("textures/entity/meeseeks.png");}
    @Override public ResourceLocation getAnimationResource(MeeseeksEntity e){return id("animations/meeseeks.animation.json");}
    private ResourceLocation id(String p){return new ResourceLocation(PortalGunMod.MOD_ID,p);}
}
