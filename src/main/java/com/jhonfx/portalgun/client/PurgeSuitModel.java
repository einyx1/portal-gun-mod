package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.item.PurgeSuitItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class PurgeSuitModel extends GeoModel<PurgeSuitItem> {
    private static ResourceLocation id(String path){return new ResourceLocation(PortalGunMod.MOD_ID,path);}
    @Override public ResourceLocation getModelResource(PurgeSuitItem item){return id("geo/purge_suit.geo.json");}
    @Override public ResourceLocation getTextureResource(PurgeSuitItem item){return id("textures/models/armor/purge_suit.png");}
    @Override public ResourceLocation getAnimationResource(PurgeSuitItem item){return id("animations/purge_suit.animation.json");}
}
