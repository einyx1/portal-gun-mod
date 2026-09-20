package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.block.entity.OmegaMachineBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class OmegaMachineRenderer extends GeoBlockRenderer<OmegaMachineBlockEntity> {
    public OmegaMachineRenderer() {
        super(new OmegaMachineModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
