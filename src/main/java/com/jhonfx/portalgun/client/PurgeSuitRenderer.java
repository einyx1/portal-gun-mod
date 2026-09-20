package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.item.PurgeSuitItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class PurgeSuitRenderer extends GeoArmorRenderer<PurgeSuitItem> {
    public PurgeSuitRenderer(){super(new PurgeSuitModel());withScale(1.04f,1.04f);}
    @Override public void preRender(PoseStack pose,PurgeSuitItem suit,BakedGeoModel model,MultiBufferSource buffers,
                                    VertexConsumer vertices,boolean reRender,float partial,int light,int overlay,
                                    float red,float green,float blue,float alpha){
        super.preRender(pose,suit,model,buffers,vertices,reRender,partial,light,overlay,red,green,blue,alpha);
        int mode=0;if(currentEntity instanceof LivingEntity living){ItemStack chest=living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);if(!chest.isEmpty())mode=Math.floorMod(chest.getOrCreateTag().getInt("PortalGunPurgeMode"),5);}
        show(model,"right_launcher",mode==0);show(model,"left_launcher",mode==0);
        show(model,"flamethrower",mode==1);show(model,"shock_coil",mode==2);
        show(model,"arm_blade",mode==3);show(model,"arm_saw",mode==4);
    }
    private static void show(BakedGeoModel model,String name,boolean visible){model.getBone(name).ifPresent(b->{b.setHidden(!visible);b.setChildrenHidden(!visible);});}
}
