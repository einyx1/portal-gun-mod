package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.block.entity.PortalSolutionBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PortalSolutionRenderer extends GeoBlockRenderer<PortalSolutionBlockEntity> {
    public PortalSolutionRenderer() { super(new PortalSolutionModel()); }
}
