package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.client.ClientScreens;
import com.jhonfx.portalgun.client.MeeseeksBoxRenderer;
import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class MeeseeksBoxItem extends Item implements GeoItem {
    public static final int MAX_CHARGES=8;
    private static final RawAnimation ACTIVATE=RawAnimation.begin().thenPlay("animation.meeseeks_box.activate");
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public MeeseeksBoxItem(Properties properties){super(properties);SingletonGeoAnimatable.registerSyncedAnimatable(this);}
    public static int charges(ItemStack stack){var tag=stack.getOrCreateTag();if(!tag.contains("Charges"))tag.putInt("Charges",MAX_CHARGES);return tag.getInt("Charges");}
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);charges(stack);
        if(player.isShiftKeyDown()){
            InteractionHand other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            ItemStack fuel=player.getItemInHand(other);
            if(fuel.is(ModItems.BLUE_PORTAL_FLUID.get())){
                if(!level.isClientSide){stack.getOrCreateTag().putInt("Charges",MAX_CHARGES);if(!player.getAbilities().instabuild)fuel.shrink(1);player.displayClientMessage(Component.literal("Caixa recarregada: 8 cargas."),true);}
                return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
            }
        }
        if(level.isClientSide)ClientScreens.openMeeseeksMenu(hand);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag){tooltip.add(Component.literal("CARGAS: "+charges(stack)+"/"+MAX_CHARGES));tooltip.add(Component.literal("Escolha uma ordem ao ativar"));}
    @Override public boolean isBarVisible(ItemStack stack){return charges(stack)<MAX_CHARGES;}
    @Override public int getBarWidth(ItemStack stack){return Math.round(13f*charges(stack)/MAX_CHARGES);}
    @Override public int getBarColor(ItemStack stack){return 0x27e7f2;}
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){private MeeseeksBoxRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new MeeseeksBoxRenderer();return renderer;}});}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>(this,"box",0,s->PlayState.STOP).triggerableAnim("activate",ACTIVATE));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
