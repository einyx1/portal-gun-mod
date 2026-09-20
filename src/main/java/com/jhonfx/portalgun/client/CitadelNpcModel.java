package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CitadelNpcModel extends GeoModel<CitadelNpcEntity> {
    @Override public ResourceLocation getModelResource(CitadelNpcEntity entity) {
        return id(switch (entity.getVariant()) {
            case MORTY, MORTY_NERVOUS, MORTY_REBEL, MORTY_WORKER -> "geo/citadel_morty.geo.json";
            case GUARD -> "geo/citadel_guard.geo.json";
            case COUNCIL -> "geo/citadel_council.geo.json";
            case RICK_COMMANDO, RICK_CYBORG, RICK_ASSASSIN, RICK_SOLDIER,
                    RICK_AUGMENTED, RICK_SCARRED, RICK_ELDER -> "geo/captive_rick.geo.json";
            default -> "geo/citadel_rick.geo.json";
        });
    }
    @Override public ResourceLocation getTextureResource(CitadelNpcEntity entity) {
        return id(switch(entity.getVariant()) {
            case MORTY, MORTY_NERVOUS, MORTY_WORKER -> "textures/entity/citadel_morty.png";
            case MORTY_REBEL -> "textures/entity/evil_morty.png";
            case C524 -> "textures/entity/citadel_c524.png";
            case PRIME -> "textures/entity/rick_prime.png";
            case COUNCIL -> "textures/entity/citadel_council.png";
            case GUARD -> "textures/entity/citadel_guard.png";
            default -> "textures/entity/citadel_rick.png";
        });
    }
    @Override public ResourceLocation getAnimationResource(CitadelNpcEntity entity) {
        return id(CitadelNpcEntity.isMortyVariant(entity.getVariant())
                ? "animations/evil_morty.animation.json" : "animations/rick_prime.animation.json");
    }
    private ResourceLocation id(String path) { return new ResourceLocation(PortalGunMod.MOD_ID,path); }
}
