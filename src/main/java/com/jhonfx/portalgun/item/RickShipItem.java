package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.entity.RickShipEntity;
import com.jhonfx.portalgun.init.ModEntityTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RickShipItem extends Item {
    public RickShipItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        RickShipEntity ship = ModEntityTypes.RICK_SHIP.get().create(level);
        if (ship == null) return InteractionResult.FAIL;
        Vec3 at = context.getClickLocation().add(Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(0.8));
        ship.moveTo(at.x, at.y, at.z, context.getPlayer() == null ? 0 : context.getPlayer().getYRot(), 0);
        Player owner = context.getPlayer();
        if(!level.noCollision(ship,ship.getBoundingBox())){
            if(owner!=null)owner.displayClientMessage(Component.translatable("message.portalgun.ship_no_space"),true);
            ship.discard();
            return InteractionResult.FAIL;
        }
        if (owner != null) ship.setOwner(owner.getUUID());
        ItemStack stack = context.getItemInHand(); ship.loadFromItem(stack);
        if (!stack.hasTag() || !stack.getTag().contains("ShipData")) ship.setHome(context.getClickedPos().above());
        level.addFreshEntity(ship);
        if (owner == null || !owner.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level, List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        int energy = stack.hasTag() && stack.getTag().contains("ShipData") ? stack.getTag().getCompound("ShipData").getInt("Energy") : RickShipEntity.MAX_ENERGY;
        lines.add(Component.translatable("tooltip.portalgun.rick_ship"));
        lines.add(Component.translatable("tooltip.portalgun.rick_ship_cargo"));
        lines.add(Component.translatable("message.portalgun.rick_ship_energy", energy, RickShipEntity.MAX_ENERGY));
    }
}
