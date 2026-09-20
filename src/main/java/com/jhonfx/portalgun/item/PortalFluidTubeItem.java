package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.entity.PortalColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A removable, reusable 100% portal-fluid cartridge. */
public class PortalFluidTubeItem extends Item {
    public static final int MAX_CHARGE = 1000;
    public static final String TAG_CHARGE = "TubeCharge";
    private final PortalColor color;

    public PortalFluidTubeItem(Properties properties, PortalColor color) {
        super(properties);
        this.color = color;
    }

    public PortalColor color() {
        return color;
    }

    public static int getCharge(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_CHARGE)
                ? Math.max(0, Math.min(MAX_CHARGE, stack.getTag().getInt(TAG_CHARGE)))
                : MAX_CHARGE;
    }

    public static void setCharge(ItemStack stack, int charge) {
        stack.getOrCreateTag().putInt(TAG_CHARGE, Math.max(0, Math.min(MAX_CHARGE, charge)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < MAX_CHARGE;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getCharge(stack) / MAX_CHARGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return color.rgb();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int percent = Math.round(100.0f * getCharge(stack) / MAX_CHARGE);
        tooltip.add(Component.translatable("tooltip.portalgun.tube_charge", percent).withStyle(ChatFormatting.GRAY));
    }
}
