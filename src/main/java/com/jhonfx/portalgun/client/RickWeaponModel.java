package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.item.RickWeaponItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RickWeaponModel extends GeoModel<RickWeaponItem>{
    @Override public ResourceLocation getModelResource(RickWeaponItem item){return new ResourceLocation(PortalGunMod.MOD_ID,"geo/weapon/"+item.assetId()+".geo.json");}
    @Override public ResourceLocation getTextureResource(RickWeaponItem item){return new ResourceLocation(PortalGunMod.MOD_ID,"textures/item/"+item.assetId()+".png");}
    @Override public ResourceLocation getAnimationResource(RickWeaponItem item){return new ResourceLocation(PortalGunMod.MOD_ID,"animations/weapon/"+item.assetId()+".animation.json");}
}
