package com.jhonfx.portalgun.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Wearable combat-analysis implant recovered from Evil Morty's technology. */
public final class EvilMortyEyepatchItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public EvilMortyEyepatchItem(ArmorMaterial material, Properties properties) {
        super(material, Type.HELMET, properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private com.jhonfx.portalgun.client.EvilMortyEyepatchRenderer renderer;
            @Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity entity, ItemStack stack,
                    net.minecraft.world.entity.EquipmentSlot slot,
                    net.minecraft.client.model.HumanoidModel<?> original) {
                if (renderer == null) renderer = new com.jhonfx.portalgun.client.EvilMortyEyepatchRenderer();
                renderer.prepForRender(entity, stack, slot, original);
                return renderer;
            }
        });
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.portalgun.evil_morty_eyepatch"));
        tooltip.add(Component.translatable("tooltip.portalgun.evil_morty_eyepatch_dodge"));
    }
}
