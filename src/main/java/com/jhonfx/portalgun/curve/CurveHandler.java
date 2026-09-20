package com.jhonfx.portalgun.curve;

import com.jhonfx.portalgun.citadel.CitadelBuilder;
import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.entity.RickPrimeEntity;
import com.jhonfx.portalgun.entity.EvilMortyEntity;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.init.ModSounds;
import com.jhonfx.portalgun.item.RickBrainItem;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

public final class CurveHandler {
    private static final net.minecraft.core.BlockPos ARENA=CitadelBuilder.CENTER.offset(0,2,-76);
    private static final DustParticleOptions GREEN=new DustParticleOptions(new Vector3f(0.15f,1f,0.34f),1.5f);
    private static final DustParticleOptions CYAN=new DustParticleOptions(new Vector3f(0.15f,0.85f,1f),1.3f);
    @SubscribeEvent public void onDrops(LivingDropsEvent event){
        if(!(event.getSource().getEntity() instanceof ServerPlayer player))return;
        boolean prime=event.getEntity() instanceof RickPrimeEntity;
        boolean rick=event.getEntity() instanceof CitadelNpcEntity npc&&npc.getVariant()==CitadelNpcEntity.Variant.RICK;
        if(!prime&&!rick||(!prime&&player.getRandom().nextFloat()>0.45f))return;
        int amount=prime?60:28+player.getRandom().nextInt(25);
        ItemStack brain=RickBrainItem.create(ModItems.RICK_BRAIN.get(),amount);
        event.getDrops().add(new ItemEntity(event.getEntity().level(),event.getEntity().getX(),event.getEntity().getY(),event.getEntity().getZ(),brain));
    }
    public static void begin(ServerPlayer player){
        CurveSavedData d=CurveSavedData.get(player.serverLevel());
        if(d.broken()){player.displayClientMessage(Component.translatable("message.portalgun.curve_already_broken"),true);return;}
        if(d.breaking()||d.battleStarted())return;
        if(d.neuralData()<100||d.fluid()<100){player.displayClientMessage(Component.translatable("message.portalgun.curve_requirements",d.neuralData(),d.fluid()),false);return;}
        d.begin(player.serverLevel().getGameTime()+60);
        miniExplosion(player.serverLevel());
        player.serverLevel().playSound(null, ARENA, ModSounds.EVIL_MORTY_ESCAPE.get(), SoundSource.MUSIC, 4.0f, 1.0f);
        player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.portalgun.curve_sequence"),false);
    }
    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return; MinecraftServer server=event.getServer();
        ServerLevel citadel=server.getLevel(com.jhonfx.portalgun.item.CitadelRemoteItem.CITADEL); if(citadel==null)return;
        CurveSavedData d=CurveSavedData.get(citadel); long tick=citadel.getGameTime();
        tickCitadelCollapse(citadel,d);
        if(d.breaking()){
            long left=d.finishTime()-tick; effects(citadel,left);
            if(left<=0)startBattle(citadel,d);
        } else if(d.battleStarted()) {
            boolean alive=!citadel.getEntitiesOfClass(EvilMortyEntity.class,new net.minecraft.world.phys.AABB(ARENA).inflate(48),e->e.isAlive()&&!e.isStoryAlly()).isEmpty();
            if(!alive)spawnEvilMorty(citadel,d);
            if(tick%5==0)rift(citadel,60,true);
        } else if(d.broken()&&tick%5==0)rift(citadel,18,false);
    }
    private static void effects(ServerLevel level,long left){
        if(level.getGameTime()%4==0)rift(level,(int)Math.max(1,240-left),true);
        if(left%40==0)for(ServerPlayer p:level.players())p.displayClientMessage(Component.translatable("message.portalgun.curve_countdown",Math.max(0,left/20)),true);
        if(left%60==0)level.playSound(null,CitadelBuilder.CENTER,SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,2.5f,0.6f+(240-left)/300f);
    }
    private static void rift(ServerLevel level,int phase,boolean violent){
        double cx=ARENA.getX()+.5,cy=ARENA.getY()+4,cz=ARENA.getZ()+.5;
        int count=violent?80:12; double radius=violent?Math.min(9,2+phase/30d):7;
        for(int i=0;i<count;i++){double a=i*Math.PI*2/count+level.getGameTime()*0.07;double y=Math.sin(a*3)*radius*.45;
            level.sendParticles(i%4==0?net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK:(i%2==0?GREEN:CYAN),cx+Math.cos(a)*radius,cy+y,cz,1,.05,.05,.05,.01);}
    }
    private static void startBattle(ServerLevel level,CurveSavedData d){d.startBattle();spawnEvilMorty(level,d);}
    private static void spawnEvilMorty(ServerLevel level,CurveSavedData d){
        EvilMortyEntity boss=ModEntityTypes.EVIL_MORTY.get().create(level);if(boss==null)return;
        boss.moveTo(ARENA.getX()+.5,ARENA.getY()+1,ARENA.getZ()-6.5,180,0);boss.configureArena(ARENA);boss.setPersistenceRequired();level.addFreshEntity(boss);
        level.playSound(null,ARENA,SoundEvents.END_PORTAL_SPAWN,SoundSource.HOSTILE,3f,1.55f);
    }
    private static void miniExplosion(ServerLevel level){
        level.explode(null,ARENA.getX()+.5,ARENA.getY()+1,ARENA.getZ()+.5,1.7f,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,ARENA.getX()+.5,ARENA.getY()+1,ARENA.getZ()+.5,3,.5,.5,.5,0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,ARENA.getX()+.5,ARENA.getY()+1,ARENA.getZ()+.5,100,2,1,2,.12);
    }
    public static void onEvilMortyDefeated(ServerLevel level,net.minecraft.core.BlockPos pos){
        CurveSavedData d=CurveSavedData.get(level);if(!d.battleStarted()||d.broken())return;complete(level,d);
    }
    private static void complete(ServerLevel level,CurveSavedData d){
        d.complete();rift(level,240,true);level.playSound(null,CitadelBuilder.CENTER,SoundEvents.END_PORTAL_SPAWN,SoundSource.BLOCKS,4f,.55f);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.portalgun.curve_broken"),false);
        for(ServerPlayer p:level.players()){ItemStack key=new ItemStack(ModItems.CURVE_COORDINATES.get());if(!p.getInventory().add(key))p.drop(key,false);}
    }

    private static void tickCitadelCollapse(ServerLevel level, CurveSavedData data) {
        int remaining = data.collapseTicks();
        if (remaining <= 0) return;
        int next = data.advanceCollapse();

        // ── Alarm pulses every 20 ticks during full collapse ──────────────────
        if (next > 45 && next % 20 == 0) {
            level.playSound(null, CitadelBuilder.CENTER,
                    SoundEvents.NOTE_BLOCK_BASS.value(),
                    SoundSource.BLOCKS, 3.5f, 0.45f);
        }

        // ── Warning to players still in Citadel ───────────────────────────────
        String warnMsg = switch (next) {
            case 220 -> "§c[ALERTA DE ESTRUTURA]§r A Curva Finita Central está colapsando!";
            case 160 -> "§c[ALERTA CRÍTICO]§r Falha estrutural detectada. Evacue imediatamente!";
            case 100 -> "§4[EMERGÊNCIA]§r Integridade da Cidadela: §c12%§r. Detonação iminente.";
            case  60 -> "§4[SISTEMA DE EMERGÊNCIA]§r Evacuação obrigatória em §c3 segundos!";
            default  -> null;
        };
        if (warnMsg != null) {
            for (ServerPlayer p : level.players())
                p.displayClientMessage(Component.literal(warnMsg), false);
        }

        // ── Cascading explosions + smoke ──────────────────────────────────────
        if (next % 10 == 0 && next > 45) {
            double a = next * 0.73;
            double r = 18 + (240 - next) * 0.27;
            double x = CitadelBuilder.CENTER.getX() + Math.cos(a) * r;
            double z = CitadelBuilder.CENTER.getZ() + Math.sin(a) * r;
            double y = 99 + level.random.nextInt(22);
            level.explode(null, x, y, z, 2.8f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, x, y + 4, z, 1, 0, 0, 0, 0);
            // Smoke pillars rising from rubble
            for (int i = 0; i < 8; i++) {
                double sx = x + level.random.nextGaussian() * 3;
                double sz = z + level.random.nextGaussian() * 3;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, sx, y, sz, 1, 0.3, 1.2, 0.3, 0.04);
            }
            // Electrical sparks for "circuit failure" VFX
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, y + 2, z, 30, 1.5, 0.5, 1.5, 0.15);
            level.playSound(null, net.minecraft.core.BlockPos.containing(x, y, z),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.5f, 0.75f + level.random.nextFloat() * 0.5f);
        }

        // ── Evacuate before the physical deletion sweep ──────────────────────
        if (next == 205) {
            ServerLevel overworld = level.getServer().overworld();
            net.minecraft.core.BlockPos spawn = overworld.getSharedSpawnPos();
            for (ServerPlayer p : java.util.List.copyOf(level.players())) {
                p.displayClientMessage(Component.literal(
                        "§4[TELETRANSPORTE DE EMERGÊNCIA]§r Sobrevivente evacuado para o Overworld."), false);
                p.teleportTo(overworld, spawn.getX() + .5, spawn.getY() + 1, spawn.getZ() + .5,
                        p.getYRot(), p.getXRot());
            }
            level.playSound(null, CitadelBuilder.CENTER,
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 4.0f, 0.4f);
        }

        // ── Block destruction sweep ───────────────────────────────────────────
        if (next <= 200 && next >= 0) {
            int slice = 200 - next;
            int minX  = -100 + slice;
            for (int bx = minX; bx <= minX; bx++) {
                for (int bz = -100; bz <= 100; bz++) {
                    for (int by = 90; by <= 132; by++) {
                        net.minecraft.core.BlockPos pos = CitadelBuilder.CENTER.offset(bx, by - CitadelBuilder.CENTER.getY(), bz);
                        if (!level.getBlockState(pos).isAir())
                            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            // Debris particles for sweep
            if (slice % 6 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        CitadelBuilder.CENTER.getX() + minX, 115, CitadelBuilder.CENTER.getZ(),
                        20, 10, 4, 10, 0.08);
            }
        }

        // ── Final cleanup ─────────────────────────────────────────────────────
        if (next <= 0) {
            level.getEntitiesOfClass(com.jhonfx.portalgun.entity.CitadelNpcEntity.class,
                    new net.minecraft.world.phys.AABB(CitadelBuilder.CENTER).inflate(108), e -> true)
                    .forEach(net.minecraft.world.entity.Entity::discard);
            CitadelBuilder.ensureRefuge(level);
            level.getEntitiesOfClass(com.jhonfx.portalgun.entity.CurveNodeEntity.class,
                    new net.minecraft.world.phys.AABB(CitadelBuilder.CENTER).inflate(120), e -> true)
                    .forEach(net.minecraft.world.entity.Entity::discard);
            level.getAllEntities().forEach(e -> {
                if (e.getPersistentData().getBoolean("PortalGunCurveGuide")) e.discard();
            });
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§4[ A CIDADELA DOS RICKS FOI DESTRUÍDA. A CURVA FINITA CENTRAL ESTÁ QUEBRADA. ]"), false);
        }
    }
}
