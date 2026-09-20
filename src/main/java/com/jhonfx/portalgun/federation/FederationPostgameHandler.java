package com.jhonfx.portalgun.federation;

import com.jhonfx.portalgun.entity.FederationAlienEntity;
import com.jhonfx.portalgun.init.ModBlocks;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Persistent post-Prime sabotage campaign inside the Galactic Federation. */
public final class FederationPostgameHandler {
    private static final ResourceLocation FEDERATION = new ResourceLocation("portalgun", "federation");
    public static final BlockPos COMMS = FederationBuilder.CENTER.offset(0, 1, 52);
    public static final BlockPos INHIBITOR = FederationBuilder.PRISON_POS.offset(0, 1, 0);
    public static final BlockPos COMMAND = FederationBuilder.CENTER.offset(0, 1, -75);
    public static final BlockPos POWER = FederationBuilder.CENTER.offset(-55, 1, 0);
    public static final BlockPos DEFENSE = FederationBuilder.CUSTOMS_POS.offset(0, 1, 0);
    public static final BlockPos ADMIN = FederationBuilder.CENTER.offset(0, 9, -75);
    private static final BlockPos RESEARCH = FederationBuilder.CENTER.offset(52, 1, -61);
    private static final BlockPos REACTOR = FederationBuilder.CENTER.offset(-52, 1, -61);
    private static final BlockPos ARMORY = FederationBuilder.CENTER.offset(-55, 1, -10);
    private static final BlockPos[][] OBJECTIVES = {
            {COMMS.offset(-4, 0, 0), COMMS, COMMS.offset(4, 0, 0)},
            {INHIBITOR},
            {COMMAND.offset(-5, 0, 4), COMMAND.offset(0, 0, 4), COMMAND.offset(5, 0, 4)},
            {REACTOR.offset(-4, 1, 0), REACTOR.offset(4, 1, 0), REACTOR.offset(0, 1, -4), REACTOR.offset(0, 1, 4)},
            {DEFENSE.offset(-8, 0, 0), DEFENSE, DEFENSE.offset(8, 0, 0)},
            {ADMIN}
    };
    private static final String[] NAMES = {
            "Matriz de Comunicações", "Inibidor de Portais", "Console de Comando",
            "Núcleo de Energia", "Rede de Defesa", "Terminal Administrativo"
    };

    @SubscribeEvent
    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 40 != 0 || !player.level().dimension().location().equals(FEDERATION)) return;
        FederationSavedData state = FederationSavedData.get(player.serverLevel());
        if(FederationBuilder.isBuilding(player.serverLevel()))return;
        long evacuateAt=player.getPersistentData().getLong("PortalGunFederationEvacuateAt");
        if(evacuateAt>0&&player.level().getGameTime()>=evacuateAt){
            player.getPersistentData().remove("PortalGunFederationEvacuateAt");
            ServerLevel overworld=player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if(overworld!=null){BlockPos spawn=player.getRespawnPosition();if(spawn==null||player.getRespawnDimension()!=net.minecraft.world.level.Level.OVERWORLD)spawn=overworld.getSharedSpawnPos();player.teleportTo(overworld,spawn.getX()+.5,spawn.getY()+1,spawn.getZ()+.5,player.getYRot(),player.getXRot());player.displayClientMessage(Component.literal("§aEVACUAÇÃO CONCLUÍDA — §fa estação da Federação colapsou atrás de você."),false);}
            return;
        }
        if (state.isDestroyed()) return;
        if (state.invasionStage() == 0 && player.getPersistentData().getBoolean("RickPrimeDefeated")) {
            state.beginInvasion();
            player.displayClientMessage(Component.literal("§c[FEDERAÇÃO] §fOperação de invasão iniciada. Sabote os seis sistemas para derrubar o regime."), false);
            announce(player, state.invasionStage());
        }
        ensureCurrentObjective(player.serverLevel(), state);
        if(player.tickCount%200==0&&state.invasionStage()>=1&&state.invasionStage()<=6){int stage=state.invasionStage();BlockPos target=currentObjective(state);long defenders=countDefenders(player.serverLevel(),stage);player.displayClientMessage(Component.literal("§c[INVASÃO "+stage+"/6] §f"+action(stage)+" "+NAMES[stage-1]+" §7["+(state.objectiveProgress()+1)+"/"+OBJECTIVES[stage-1].length+"] — "+(int)Math.sqrt(player.blockPosition().distSqr(target))+"m — defensores: "+defenders),true);}
    }

    @SubscribeEvent
    public void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !player.level().dimension().location().equals(FEDERATION)) return;
        FederationSavedData state = FederationSavedData.get(player.serverLevel());
        int stage = state.invasionStage();
        if (stage < 1 || stage > 6 || (stage != 2 && stage != 5)
                || !event.getPos().equals(currentObjective(state))) return;

        ServerLevel level = player.serverLevel();
        if (hasDefenders(level, stage)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(stage==6
                    ? "§cTERMINAL SELADO — derrote o Alto Comandante Zarn e sua guarda."
                    : "§cSISTEMA PROTEGIDO — elimine a equipe de defesa primeiro."), true);
            return;
        }
        completeNode(level, player, state);
    }

    @SubscribeEvent
    public void interactTerminal(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !player.level().dimension().location().equals(FEDERATION)) return;
        ServerLevel level = player.serverLevel();
        FederationSavedData state = FederationSavedData.get(level);
        int stage = state.invasionStage();

        if (stage >= 1 && stage <= 6 && stage != 2 && stage != 5
                && event.getPos().equals(currentObjective(state))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!level.getBlockState(event.getPos()).is(ModBlocks.FEDERATION_TERMINAL.get())) return;
            if (hasDefenders(level, stage)) {
                player.displayClientMessage(Component.literal(stage == 6
                        ? "§cACESSO NEGADO — derrote o Alto Comandante Zarn e sua guarda."
                        : "§cCONSOLE BLOQUEADO — elimine a equipe de defesa primeiro."), true);
                return;
            }
            long now=level.getGameTime(),last=player.getPersistentData().getLong("PortalGunFederationTerminalUse");
            if(now-last<12)return;
            player.getPersistentData().putLong("PortalGunFederationTerminalUse",now);
            completeNode(level, player, state);
            return;
        }

        // Functional interiors remain available before and after the sabotage campaign.
        if (event.getPos().equals(RESEARCH) || event.getPos().equals(REACTOR) || event.getPos().equals(ARMORY)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getPos().equals(RESEARCH)) {
                player.displayClientMessage(Component.literal("§b[LABORATÓRIO XENOBIOLÓGICO] §fAnálise: assinatura P-0 ausente; 847 espécies catalogadas."), false);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 600, 0, false, false));
            } else if (event.getPos().equals(REACTOR)) {
                int stability = state.isDestroyed() ? 0 : Math.max(8, 100 - state.invasionStage() * 11);
                player.displayClientMessage(Component.literal("§d[REATOR DIMENSIONAL] §fEstabilidade: " + stability + "% — " + (state.isDestroyed()?"NÚCLEO COLAPSADO":"fluxo sob contenção")), false);
            } else {
                player.displayClientMessage(Component.literal("§c[ARSENAL FEDERAL] §fAutorização militar necessária. Estoque rastreado pela rede central."), false);
            }
        }
    }

    private static void ensureCurrentObjective(ServerLevel level, FederationSavedData state) {
        int stage=state.invasionStage();
        if (stage < 1 || stage > 6) return;
        BlockPos pos = currentObjective(state);
        if (!level.getBlockState(pos).is(ModBlocks.FEDERATION_TERMINAL.get()))
            level.setBlock(pos, ModBlocks.FEDERATION_TERMINAL.get().defaultBlockState(), 3);
        if(state.defenseStageSpawned()<stage){spawnDefenseWave(level,pos,stage);state.markDefenseSpawned(stage);}
    }

    private static BlockPos currentObjective(FederationSavedData state){
        int stage=Math.max(1,Math.min(6,state.invasionStage()));
        BlockPos[] nodes=OBJECTIVES[stage-1];
        return nodes[Math.min(state.objectiveProgress(),nodes.length-1)];
    }

    private static String action(int stage){return switch(stage){case 1->"Hackeie";case 2->"Destrua";case 3->"Falsifique a autorização de";case 4->"Sobrecarregue";case 5->"Desarme";case 6->"Execute o protocolo final em";default->"Sabote";};}

    private static void completeNode(ServerLevel level,ServerPlayer player,FederationSavedData state){
        int stage=state.invasionStage();
        BlockPos pos=currentObjective(state);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,70,1,1,1,.18);
        level.playSound(null,pos,SoundEvents.GENERIC_EXPLODE,SoundSource.BLOCKS,1.4f,.65f);
        state.advanceObjective();
        int done=state.objectiveProgress(),required=OBJECTIVES[stage-1].length;
        player.displayClientMessage(Component.literal("§a"+NAMES[stage-1]+": §fsubsistema "+done+"/"+required+" neutralizado."),false);
        if(done<required){ensureCurrentObjective(level,state);announceAll(level,stage);return;}
        give(player,new ItemStack(ModItems.BLEMFLARCKS.get(),stage==6?32:6+stage*2));
        if(stage==6)beginFinale(level,player,state);else{state.advanceInvasion();announceAll(level,state.invasionStage());}
    }

    private static void spawnDefenseWave(ServerLevel level,BlockPos center,int stage){
        int soldiers=Math.min(5,1+stage/2);
        for(int i=0;i<soldiers;i++){
            FederationAlienEntity alien=ModEntityTypes.FEDERATION_ALIEN.get().create(level);if(alien==null)continue;
            double a=i*Math.PI*2/Math.max(1,soldiers);alien.moveTo(center.getX()+.5+Math.cos(a)*5,center.getY()+1,center.getZ()+.5+Math.sin(a)*5,0,0);
            alien.setRank(stage>=6&&i==0?FederationAlienEntity.Rank.COMMANDER:stage>=4?FederationAlienEntity.Rank.ELITE:FederationAlienEntity.Rank.SOLDIER);
            if(stage==6&&i==0){alien.setCustomName(Component.literal("Alto Comandante Zarn"));alien.setCustomNameVisible(true);}
            alien.getPersistentData().putInt("FederationSabotageStage",stage);alien.setPersistenceRequired();level.addFreshEntity(alien);
        }
        if(stage>=4){int heavyCount=stage==6?2:1;for(int i=0;i<heavyCount;i++){
            var heavy=ModEntityTypes.FEDERATION_HEAVY.get().create(level);if(heavy==null)continue;
            heavy.moveTo(center.getX()+3-i*6,center.getY()+1,center.getZ()+3,0,0);
            heavy.getPersistentData().putInt("FederationSabotageStage",stage);heavy.setPersistenceRequired();level.addFreshEntity(heavy);
        }}
        level.playSound(null,center,SoundEvents.BEACON_ACTIVATE,SoundSource.HOSTILE,2f,.6f);
    }

    private static boolean hasDefenders(ServerLevel level,int stage){
        return countDefenders(level,stage)>0;
    }
    private static long countDefenders(ServerLevel level,int stage){
        return level.getEntities((net.minecraft.world.entity.Entity)null,
                new net.minecraft.world.phys.AABB(FederationBuilder.CENTER).inflate(150),
                e->e.isAlive()&&e.getPersistentData().getInt("FederationSabotageStage")==stage).size();
    }

    private static void give(ServerPlayer player,ItemStack stack){if(!player.getInventory().add(stack))player.drop(stack,false);}

    private static void announce(ServerPlayer player, int stage) {
        if (stage < 1 || stage > 6) return;
        FederationSavedData state=FederationSavedData.get(player.serverLevel());
        BlockPos p = currentObjective(state);
        player.displayClientMessage(Component.literal("§eOBJETIVO " + stage + "/6: §f"+action(stage)+" " + NAMES[stage - 1]
                + " §7["+(state.objectiveProgress()+1)+"/"+OBJECTIVES[stage-1].length+"] (" + p.toShortString() + ")"), false);
    }

    private static void announceAll(ServerLevel level, int stage) {
        for (ServerPlayer player : level.players()) announce(player, stage);
    }

    private static void beginFinale(ServerLevel level,ServerPlayer source,FederationSavedData state){
        state.beginCollapse();source.getPersistentData().putBoolean("PortalGunFederationSaboteur",true);
        give(source,new ItemStack(ModItems.WEAPON_ENERGY_CELL.get(),4));give(source,new ItemStack(ModItems.QUANTUM_ISOTOPE.get(),2));
        ResourceLocation scene=new ResourceLocation("portalgun","federation_collapse");
        for(ServerPlayer player:level.players()){player.getPersistentData().remove(FederationThreatHandler.KEY_INHIBIT);player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,280,4,false,false));player.displayClientMessage(Component.literal("§4PROTOCOLO DE COLAPSO: §f10 segundos. Transmissão de evacuação iniciada."),false);com.jhonfx.portalgun.event.CinematicSessionGuard.start(scene,net.minecraft.world.phys.Vec3.atCenterOf(FederationBuilder.CENTER),player,player);}
        level.playSound(null,FederationBuilder.CENTER,SoundEvents.BEACON_DEACTIVATE,SoundSource.BLOCKS,4f,.45f);
    }

    @SubscribeEvent public void levelTick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)||!level.dimension().location().equals(FEDERATION))return;
        FederationSavedData state=FederationSavedData.get(level);
        if(state.isDestroyed()){tickRuins(level,state);return;}
        if(state.collapseTicks()<=0)return;
        int left=state.tickCollapse();
        if(left%40==0&&left>0){int seconds=left/20;for(ServerPlayer player:level.players())player.displayClientMessage(Component.literal("§cCOLAPSO EM "+seconds+"s"),true);BlockPos[] row=OBJECTIVES[Math.floorMod(left/40,OBJECTIVES.length)];BlockPos pulse=row[Math.floorMod(left/20,row.length)];level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,pulse.getX()+.5,pulse.getY()+1,pulse.getZ()+.5,2,2,1,2,.05);level.playSound(null,pulse,SoundEvents.GENERIC_EXPLODE,SoundSource.BLOCKS,2.2f,.7f);}
        if(left==0)completeDestruction(level,state);
    }

    private static void completeDestruction(ServerLevel level,FederationSavedData state) {
        state.finishInvasion();
        for (FederationAlienEntity alien : level.getEntitiesOfClass(FederationAlienEntity.class,
                new net.minecraft.world.phys.AABB(FederationBuilder.CENTER).inflate(130))) alien.discard();
        level.getEntitiesOfClass(com.jhonfx.portalgun.entity.FederationDroneEntity.class,new net.minecraft.world.phys.AABB(FederationBuilder.CENTER).inflate(150)).forEach(net.minecraft.world.entity.Entity::discard);
        level.getEntitiesOfClass(com.jhonfx.portalgun.entity.FederationHeavyTrooperEntity.class,new net.minecraft.world.phys.AABB(FederationBuilder.CENTER).inflate(150)).forEach(net.minecraft.world.entity.Entity::discard);
        // The physical collapse resumes from SavedData in small batches below.
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 0.5, 103, -40.5, 18, 38, 12, 38, .08);
        level.playSound(null, FederationBuilder.CENTER, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, .55f);
        for (ServerPlayer player : level.players()) {
            player.getPersistentData().putInt(FederationThreatHandler.KEY_WANTED, 0);
            player.getPersistentData().remove(FederationThreatHandler.KEY_INHIBIT);
            player.getPersistentData().putBoolean("PortalGunFederationLiberated", true);
            if(!player.getPersistentData().getBoolean("PortalGunFederationLiberationReward")){
                player.getPersistentData().putBoolean("PortalGunFederationLiberationReward",true);
                give(player,new ItemStack(ModItems.FEDERATION_THREAT_SCANNER.get()));
                give(player,new ItemStack(ModItems.WEAPON_ENERGY_CELL.get(),3));
            }
            player.getPersistentData().putLong("PortalGunFederationEvacuateAt",level.getGameTime()+60);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,100,4,false,false));
            player.displayClientMessage(Component.literal("§bA FEDERAÇÃO CAIU. §fEvacuação automática em 3 segundos; as ruínas ficarão salvas permanentemente."), false);
        }
    }

    private static void tickRuins(ServerLevel level,FederationSavedData state) {
        BlockPos[] centers={COMMAND,POWER,DEFENSE,COMMS,INHIBITOR,RESEARCH,REACTOR,ADMIN};
        int start=state.ruinCursor(),total=centers.length*3375;
        if(start>=total)return;
        int cursor=start,end=Math.min(total,start+600);
        long deadline=System.nanoTime()+4_000_000L;
        do {
            int local=cursor%3375,x=local%15-7,y=(local/15)%15-7,z=local/225-7;
            BlockPos pos=centers[cursor/3375].offset(x,y,z);
            if(x*x+y*y+z*z<=49&&Math.floorMod(pos.asLong()*31L,100)<62
                    &&!level.getBlockState(pos).is(Blocks.BEDROCK)&&!level.getBlockState(pos).isAir())
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            cursor++;
        }while(cursor<end&&System.nanoTime()<deadline);
        state.advanceRuins(cursor-start);
    }
}
