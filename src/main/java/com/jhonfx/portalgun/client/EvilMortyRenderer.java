package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.entity.EvilMortyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class EvilMortyRenderer extends GeoEntityRenderer<EvilMortyEntity> {
    public EvilMortyRenderer(EntityRendererProvider.Context c){super(c,new EvilMortyModel());shadowRadius=.42f;}
    @Override public void preRender(PoseStack pose,EvilMortyEntity e,BakedGeoModel model,MultiBufferSource source,
        com.mojang.blaze3d.vertex.VertexConsumer buffer,boolean re,float partial,int light,int overlay,float red,float green,float blue,float alpha){
        pose.scale(.88f,.88f,.88f);super.preRender(pose,e,model,source,buffer,re,partial,light,overlay,red,green,blue,alpha);
    }
}
