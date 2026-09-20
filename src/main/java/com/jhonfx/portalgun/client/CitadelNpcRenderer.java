package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CitadelNpcRenderer extends GeoEntityRenderer<CitadelNpcEntity> {
    public CitadelNpcRenderer(EntityRendererProvider.Context context) { super(context,new CitadelNpcModel()); shadowRadius=0.45f; }
    @Override public void preRender(PoseStack poseStack, CitadelNpcEntity entity, BakedGeoModel model,
                                    MultiBufferSource source, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                    boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha) {
        if(CitadelNpcEntity.isMortyVariant(entity.getVariant())) poseStack.scale(0.86f,0.86f,0.86f);
        if(entity.getVariant()==CitadelNpcEntity.Variant.PRIME) poseStack.scale(1.04f,1.04f,1.04f);
        if(entity.getVariant()==CitadelNpcEntity.Variant.GUARD) poseStack.scale(1.03f,1.03f,1.03f);
        if(CitadelNpcEntity.isCaptiveRick(entity.getVariant())) {
            float scale=switch(entity.getVariant()) {
                case RICK_CYBORG -> 1.10f;
                case RICK_ASSASSIN -> .96f;
                case RICK_ELDER -> .93f;
                default -> 1.02f;
            };
            poseStack.scale(scale,scale,scale);
        }
        super.preRender(poseStack,entity,model,source,buffer,isReRender,partialTick,packedLight,packedOverlay,red,green,blue,alpha);
    }
}
