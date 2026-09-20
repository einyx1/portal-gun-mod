package com.jhonfx.portalgun.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;

public final class RickBrainItem extends Item {
    public static final String TAG_DATA="CurveData";
    public RickBrainItem(Properties properties) { super(properties); }
    public static ItemStack create(Item item,int data) { ItemStack stack=new ItemStack(item); stack.getOrCreateTag().putInt(TAG_DATA,Math.max(1,Math.min(65,data))); return stack; }
    public static int data(ItemStack stack) { return stack.hasTag()?Math.max(1,stack.getTag().getInt(TAG_DATA)):25; }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.portalgun.rick_brain_data",data(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.portalgun.rick_brain_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
