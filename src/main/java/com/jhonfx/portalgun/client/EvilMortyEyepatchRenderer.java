package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.item.EvilMortyEyepatchItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class EvilMortyEyepatchRenderer extends GeoArmorRenderer<EvilMortyEyepatchItem> {
    public EvilMortyEyepatchRenderer() { super(new EvilMortyEyepatchModel()); }
}
