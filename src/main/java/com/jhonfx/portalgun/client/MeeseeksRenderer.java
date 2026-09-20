package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.MeeseeksEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class MeeseeksRenderer extends GeoEntityRenderer<MeeseeksEntity>{
    public MeeseeksRenderer(EntityRendererProvider.Context context){super(context,new MeeseeksModel());shadowRadius=.42f;}
}
