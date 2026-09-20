package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.block.PortalSolutionBlock;
import com.jhonfx.portalgun.block.entity.PortalSolutionBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PortalSolutionModel extends GeoModel<PortalSolutionBlockEntity> {
    @Override
    public ResourceLocation getModelResource(PortalSolutionBlockEntity entity) {
        int level = entity.getBlockState().getValue(PortalSolutionBlock.LEVEL);
        String amount = level == 3 ? "full" : level == 2 ? "mid" : "low";
        return id("geo/travel_solution_" + amount + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PortalSolutionBlockEntity entity) {
        PortalSolutionBlock block = (PortalSolutionBlock) entity.getBlockState().getBlock();
        String name = switch (block.color()) {
            case BLUE -> "interspatial_solution";
            case GREEN -> "interdimensional_solution";
            case YELLOW -> "extradimensional_solution";
        };
        return id("textures/block/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(PortalSolutionBlockEntity entity) {
        return id("animations/solution.animation.json");
    }

    private ResourceLocation id(String path) { return new ResourceLocation(PortalGunMod.MOD_ID, path); }
}
