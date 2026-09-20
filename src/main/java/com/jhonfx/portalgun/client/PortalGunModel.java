package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.item.PortalGunItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PortalGunModel extends GeoModel<PortalGunItem> {
    @Override
    public ResourceLocation getModelResource(PortalGunItem gun) {
        return new ResourceLocation(PortalGunMod.MOD_ID, "geo/" + gun.variant().model() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PortalGunItem gun) {
        return new ResourceLocation(PortalGunMod.MOD_ID, "textures/item/" + gun.variant().texture() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(PortalGunItem gun) {
        return new ResourceLocation(PortalGunMod.MOD_ID, "animations/portal_gun.animation.json");
    }
}
