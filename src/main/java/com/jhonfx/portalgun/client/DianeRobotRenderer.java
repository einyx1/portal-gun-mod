package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.DianeRobotEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DianeRobotRenderer extends GeoEntityRenderer<DianeRobotEntity> {
    public DianeRobotRenderer(EntityRendererProvider.Context context) {
        super(context, new DianeRobotModel());
        shadowRadius = 1.6f;
    }
}
