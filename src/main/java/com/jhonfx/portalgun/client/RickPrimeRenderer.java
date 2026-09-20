package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.RickPrimeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RickPrimeRenderer extends GeoEntityRenderer<RickPrimeEntity> {
    public RickPrimeRenderer(EntityRendererProvider.Context context) {
        super(context, new RickPrimeModel());
        shadowRadius = 0.55f;
    }
}
