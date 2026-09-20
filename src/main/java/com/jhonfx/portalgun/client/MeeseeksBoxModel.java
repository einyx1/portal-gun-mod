package com.jhonfx.portalgun.client;
import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.item.MeeseeksBoxItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public final class MeeseeksBoxModel extends GeoModel<MeeseeksBoxItem>{
 @Override public ResourceLocation getModelResource(MeeseeksBoxItem i){return id("geo/meeseeks_box.geo.json");}
 @Override public ResourceLocation getTextureResource(MeeseeksBoxItem i){return id("textures/item/meeseeks_box.png");}
 @Override public ResourceLocation getAnimationResource(MeeseeksBoxItem i){return id("animations/meeseeks_box.animation.json");}
 private ResourceLocation id(String p){return new ResourceLocation(PortalGunMod.MOD_ID,p);}
}
