package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.block.OmegaComponentBlock;
import com.jhonfx.portalgun.block.entity.OmegaMachineBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OmegaMachineModel extends GeoModel<OmegaMachineBlockEntity> {
    private String component(OmegaMachineBlockEntity entity) {
        return entity.getBlockState().getBlock() instanceof OmegaComponentBlock block
                ? block.component().id() : "console";
    }

    @Override public ResourceLocation getModelResource(OmegaMachineBlockEntity entity) {
        return id("geo/omega_" + component(entity) + ".geo.json");
    }

    @Override public ResourceLocation getTextureResource(OmegaMachineBlockEntity entity) {
        return id("textures/block/omega_machine.png");
    }

    @Override public ResourceLocation getAnimationResource(OmegaMachineBlockEntity entity) {
        return id("animations/omega_" + component(entity) + ".animation.json");
    }

    private ResourceLocation id(String path) { return new ResourceLocation(PortalGunMod.MOD_ID, path); }
}
