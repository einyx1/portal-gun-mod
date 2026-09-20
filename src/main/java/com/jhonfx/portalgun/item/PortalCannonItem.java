package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.entity.PortalColor;
import com.jhonfx.portalgun.entity.PortalEntity;
import com.jhonfx.portalgun.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** Late-game heavy device: two shots create a vehicle-sized portal pair. */
public final class PortalCannonItem extends Item {
    private static final String FIRST_ID="PortalCannonFirst",FIRST_DIM="PortalCannonDimension",FIRST_POS="PortalCannonPosition";
    public PortalCannonItem(Properties properties){super(properties);}

    @Override public InteractionResultHolder<ItemStack> use(Level raw, Player user, InteractionHand hand){
        ItemStack stack=user.getItemInHand(hand);
        if(raw.isClientSide)return InteractionResultHolder.success(stack);
        ServerPlayer player=(ServerPlayer)user;ServerLevel level=player.serverLevel();
        if(player.isShiftKeyDown()){clearAnchor(player,stack);return InteractionResultHolder.consume(stack);}
        HitResult ray=player.pick(96,0,false);
        if(!(ray instanceof BlockHitResult hit)||ray.getType()!=HitResult.Type.BLOCK){player.displayClientMessage(Component.translatable("message.portalgun.cannon_need_surface"),true);return InteractionResultHolder.fail(stack);}
        PortalEntity fresh=spawn(level,hit,stack.hasTag()&&stack.getTag().hasUUID(FIRST_ID)?PortalColor.YELLOW:PortalColor.BLUE,player);
        if(fresh==null)return InteractionResultHolder.fail(stack);
        var tag=stack.getOrCreateTag();
        if(!tag.hasUUID(FIRST_ID)){
            tag.putUUID(FIRST_ID,fresh.getUUID());tag.putString(FIRST_DIM,level.dimension().location().toString());tag.putLong(FIRST_POS,fresh.blockPosition().asLong());
            player.displayClientMessage(Component.translatable("message.portalgun.cannon_anchor_set"),true);
        }else{
            ResourceLocation dimension=ResourceLocation.tryParse(tag.getString(FIRST_DIM));
            ServerLevel firstLevel=dimension==null?null:player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,dimension));PortalEntity first=null;
            if(firstLevel!=null){firstLevel.getChunkAt(BlockPos.of(tag.getLong(FIRST_POS)));Entity found=firstLevel.getEntity(tag.getUUID(FIRST_ID));if(found instanceof PortalEntity portal)first=portal;}
            if(first==null){fresh.discard();clearTag(stack);player.displayClientMessage(Component.translatable("message.portalgun.cannon_anchor_lost"),true);return InteractionResultHolder.fail(stack);}
            first.setLinkedPortal(fresh);fresh.setLinkedPortal(first);clearTag(stack);
            stack.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));
            player.getCooldowns().addCooldown(this,30);
            player.displayClientMessage(Component.translatable("message.portalgun.cannon_linked"),true);
        }
        level.playSound(null,player.blockPosition(),com.jhonfx.portalgun.init.ModSounds.PORTAL_SPAWN.get(),SoundSource.PLAYERS,1.8f,.65f);
        return InteractionResultHolder.consume(stack);
    }

    private static PortalEntity spawn(ServerLevel level,BlockHitResult hit,PortalColor color,ServerPlayer owner){
        PortalEntity portal=ModEntityTypes.PORTAL.get().create(level);if(portal==null)return null;
        Direction facing=hit.getDirection();Vec3 normal=Vec3.atLowerCornerOf(facing.getNormal()).scale(.08);
        portal.setPos(hit.getLocation().add(normal));portal.setPortalFacing(facing);portal.setColor(color);portal.setPortalScale(1.65f);
        portal.setLifetimeTicks(20*60*10);portal.setSafeMode(false);portal.setOwnerPlayerId(owner.getUUID());level.addFreshEntity(portal);return portal;
    }

    private static void clearAnchor(ServerPlayer player,ItemStack stack){
        if(!stack.hasTag()||!stack.getTag().hasUUID(FIRST_ID)){player.displayClientMessage(Component.translatable("message.portalgun.cannon_no_anchor"),true);return;}
        var tag=stack.getTag();ResourceLocation dimension=ResourceLocation.tryParse(tag.getString(FIRST_DIM));
        ServerLevel anchor=dimension==null?null:player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
        if(anchor!=null){anchor.getChunkAt(BlockPos.of(tag.getLong(FIRST_POS)));Entity e=anchor.getEntity(tag.getUUID(FIRST_ID));if(e instanceof PortalEntity portal)portal.discardWithViews();}
        clearTag(stack);player.displayClientMessage(Component.translatable("message.portalgun.cannon_cleared"),true);
    }
    private static void clearTag(ItemStack stack){var tag=stack.getOrCreateTag();tag.remove(FIRST_ID);tag.remove(FIRST_DIM);tag.remove(FIRST_POS);}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,net.minecraft.world.item.TooltipFlag flag){lines.add(Component.translatable("tooltip.portalgun.portal_cannon"));lines.add(Component.translatable(stack.hasTag()&&stack.getTag().hasUUID(FIRST_ID)?"tooltip.portalgun.portal_cannon_armed":"tooltip.portalgun.portal_cannon_ready"));}
}
