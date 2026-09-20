package com.jhonfx.portalgun.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

/** Multi-weapon Purge armor; active abilities are handled centrally by CombatTechHandler. */
public final class PurgeSuitItem extends ArmorItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.purge_suit.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PurgeSuitItem(ArmorMaterial material,Type type,Properties properties){
        super(material,type,properties);
        GeoItem.registerSyncedAnimatable(this);
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){
        private com.jhonfx.portalgun.client.PurgeSuitRenderer renderer;
        @Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack,
                EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){
            if(renderer==null)renderer=new com.jhonfx.portalgun.client.PurgeSuitRenderer();
            renderer.prepForRender(entity,stack,slot,original);return renderer;
        }});}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<>(this,"suit",2,state->state.setAndContinue(IDLE))
                .triggerableAnim("missile",RawAnimation.begin().thenPlay("animation.purge_suit.missile"))
                .triggerableAnim("flamethrower",RawAnimation.begin().thenPlay("animation.purge_suit.flamethrower"))
                .triggerableAnim("shock",RawAnimation.begin().thenPlay("animation.purge_suit.shock"))
                .triggerableAnim("blade",RawAnimation.begin().thenPlay("animation.purge_suit.blade"))
                .triggerableAnim("saw",RawAnimation.begin().thenPlay("animation.purge_suit.saw")));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
