package com.jhonfx.portalgun.network;

import com.jhonfx.portalgun.ModConfig;
import com.jhonfx.portalgun.entity.MeeseeksEntity;
import com.jhonfx.portalgun.entity.MeeseeksOrder;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.item.MeeseeksBoxItem;
import software.bernie.geckolib.animatable.GeoItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.function.Supplier;

public record MeeseeksCommandPacket(InteractionHand hand, MeeseeksOrder order) {
    public static void encode(MeeseeksCommandPacket p, FriendlyByteBuf b) { b.writeEnum(p.hand); b.writeEnum(p.order); }
    public static MeeseeksCommandPacket decode(FriendlyByteBuf b) {
        return new MeeseeksCommandPacket(b.readEnum(InteractionHand.class), b.readEnum(MeeseeksOrder.class));
    }

    public static void handle(MeeseeksCommandPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack box = player.getItemInHand(p.hand);
            if (!(box.getItem() instanceof MeeseeksBoxItem)) return;

            // ── Config: enforce max active Meeseeks per player ────────────────
            int max = ModConfig.MEESEEKS_MAX.get();
            long active=0;
            for(ServerLevel level:player.server.getAllLevels())for(net.minecraft.world.entity.Entity entity:level.getAllEntities())
                if(entity instanceof MeeseeksEntity e&&e.isAlive()&&e.getPersistentData().hasUUID("BoxOwner")
                        &&player.getUUID().equals(e.getPersistentData().getUUID("BoxOwner")))active++;
            if (active >= max) {
                player.displayClientMessage(Component.literal(
                        "§cLimite de Meeseeks atingido (" + max + "). Dispense um antes de invocar outro."), true);
                return;
            }

            // Griefing orders check config
            if (p.order.requiresGriefing() && !ModConfig.MEESEEKS_GRIEFING.get()) {
                player.displayClientMessage(Component.literal(
                        "§cOrdens de construção/mineração estão desativadas na configuração do servidor."), true);
                return;
            }

            LivingEntity target = p.order == MeeseeksOrder.KILL ? lookTarget(player) : null;
            if(p.order==MeeseeksOrder.KILL&&target==null){
                player.displayClientMessage(Component.translatable("message.portalgun.meeseeks_no_target"),true);
                return;
            }
            int charges = MeeseeksBoxItem.charges(box);
            if (charges <= 0) {
                player.displayClientMessage(Component.literal("A Caixa de Meeseeks está sem energia."), true);
                return;
            }

            MeeseeksEntity meeseeks = ModEntityTypes.MEESEEKS.get().create(player.serverLevel());
            if (meeseeks == null) return;

            Vec3 spawn = player.position().add(player.getLookAngle().multiply(2, 0, 2));
            meeseeks.moveTo(spawn.x, player.getY(), spawn.z, player.getYRot(), 0);
            if(!player.serverLevel().noCollision(meeseeks,meeseeks.getBoundingBox())){
                player.displayClientMessage(Component.translatable("message.portalgun.meeseeks_no_space"),true);
                return;
            }
            meeseeks.getPersistentData().putUUID("BoxOwner", player.getUUID());
            meeseeks.configure(player, p.order, target);
            player.serverLevel().addFreshEntity(meeseeks);

            ((MeeseeksBoxItem) box.getItem()).triggerAnim(player,
                    GeoItem.getOrAssignId(box, player.serverLevel()), "box", "activate");
            player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    meeseeks.getX(), meeseeks.getY() + 1, meeseeks.getZ(), 80, .5, 1, .5, .04);
            player.serverLevel().sendParticles(ParticleTypes.CLOUD,
                    meeseeks.getX(), meeseeks.getY() + 1, meeseeks.getZ(), 24, .25, .5, .25, .03);

            if (!player.getAbilities().instabuild) box.getOrCreateTag().putInt("Charges", charges - 1);
            player.displayClientMessage(Component.literal("MR. MEESEEKS: I'M MR. MEESEEKS! LOOK AT ME!"), false);
        });
        ctx.setPacketHandled(true);
    }

    private static LivingEntity lookTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition(), end = start.add(player.getLookAngle().scale(32));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(32)).inflate(2);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive()).stream()
                .filter(e -> distanceToLine(e.getBoundingBox().getCenter(), start, end) < 2.2)
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static double distanceToLine(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / ab.lengthSqr()));
        return p.distanceTo(a.add(ab.scale(t)));
    }
}
