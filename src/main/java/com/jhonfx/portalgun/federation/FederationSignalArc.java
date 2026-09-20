package com.jhonfx.portalgun.federation;

import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.entity.EvilMortyEntity;
import com.jhonfx.portalgun.entity.DianeRobotEntity;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.item.NeuralScannerItem;
import com.jhonfx.portalgun.item.PortalGunItem;
import com.jhonfx.portalgun.omega.OmegaStructureBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

/**
 * ARCO "ASSINATURA P-0" — a Nova Federação Galáctica descobre pistas
 * antigas de Rick Prime e tenta usar o jogador para chegar até ele.
 *
 * Estágios (persistidos em player.getPersistentData() sob "PortalGunP0Arc"):
 *   0  não iniciado
 *   1  interceptado — recebeu coordenada do Complexo Administrativo
 *   2  conheceu o oficial Gromflomite
 *   3  completou "Arquivos da Guerra Dimensional" — ganhou Fragmento P-0
 *   4  Scanner Neural upgradado para Multiversal
 *   5  caçando ecos/decoys de P-0 (localizar P-0)
 *   6  Rick C-137 apareceu
 *   7  Federação trai o jogador (Ordem Federal 71 — wanted forçado a 5)
 *   8  Evil Morty se junta
 *   9  sinal 100% — localização do "Rick Prime" (armadilha) revelada
 *  10  dentro da instalação da armadilha
 *  11  arena dos Ricks cativos (battle royale)
 *  12  arena concluída — Resíduo de Teleporte Prime coletado
 *  13  cálculo final feito no Dispositivo Ômega — UNMORTRICKEN liberado
 */
public final class FederationSignalArc {
    private static final String ROOT = "PortalGunP0Arc";
    private static final ResourceLocation FEDERATION_ID = new ResourceLocation("portalgun", "federation");
    private static final int DECOYS_TARGET = 5; // representa a busca pelos "63 alvos" narrativamente
    private static final Random RANDOM = new Random();

    // ── NBT helpers ──────────────────────────────────────────────────────────
    private static CompoundTag data(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }
    private static void save(ServerPlayer player, CompoundTag tag) { player.getPersistentData().put(ROOT, tag); }
    private static int stage(ServerPlayer player) { return data(player).getInt("Stage"); }
    private static void setStage(ServerPlayer player, int stage) {
        CompoundTag tag = data(player); tag.putInt("Stage", stage); tag.putLong("StageTime", player.level().getGameTime());
        save(player, tag);
    }

    /** Stable admin/test API. Keeping this here avoids commands duplicating story NBT rules. */
    public static int getStage(ServerPlayer player) { return stage(player); }

    public static void setStageForTesting(ServerPlayer player, int value) {
        setStage(player, Math.max(0, Math.min(14, value)));
    }

    public static void resetForTesting(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        root.remove(ROOT);
        for (String key : new String[]{
                "PortalGunUnmortrickenUnlocked", "RickPrimeDefeated", "PortalGunDianeSpawned",
                "PortalGunArenaStart", "PortalGunC524Fragment", "PortalGunOutsideCurveAccess"}) {
            root.remove(key);
        }
    }
    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
    private static void msg(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), false);
    }

    // ── Comando manual de debug/avanço ──────────────────────────────────────
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("portalgun")
                .then(Commands.literal("p0")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> { forceIntercept(ctx.getSource().getPlayerOrException()); return 1; })));
    }

    private static void forceIntercept(ServerPlayer player) {
        if (stage(player) > 0) { msg(player, "§7[P-0] Você já está nesse arco."); return; }
        triggerIntercept(player);
    }

    // ── ESTÁGIO 1: Interceptação ao entrar na Federação ─────────────────────
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player) || player.level().isClientSide()) return;
        tickStoryPortals(player);
        if (player.tickCount % 40 != 0) return;

        int st = stage(player);
        boolean inFederation = player.level().dimension().location().equals(FEDERATION_ID);
        boolean hasPortalGun = player.getInventory().items.stream().anyMatch(s -> s.getItem() instanceof PortalGunItem);
        if(player.tickCount%200==0)showObjective(player,st,inFederation);

        // Layout v3 moved the orbital dock. Rewrite old saved destinations
        // instead of leaving returning players above the former corridor.
        if(st>=13)ensureUnmortrickenDestination(player);

        // One-time migration for players who lost the residue through the old
        // void-facing arena exit. New completions are tracked at grant time.
        if(st==12&&!player.getPersistentData().getBoolean("PortalGunResidueGrantTracked")){
            boolean hasResidue=player.getInventory().items.stream().anyMatch(s->s.is(ModItems.PRIME_TELEPORT_RESIDUE.get()));
            if(!hasResidue){give(player,new ItemStack(ModItems.PRIME_TELEPORT_RESIDUE.get()));
                msg(player,"§aResíduo de Teleporte Prime recuperado após a falha da saída da arena.");}
            player.getPersistentData().putBoolean("PortalGunResidueGrantTracked",true);
        }

        // Stage-12 migration and permanent survival route. New completions are
        // sent straight here; old preview saves receive a visible portal in the
        // fake lab plus a named destination in every Portal Gun they carry.
        if(st==12&&inFederation){
            ensureOmegaDestination(player);
            if(player.blockPosition().distSqr(PrimeArenaBuilder.TRAP_ARRIVAL)<80*80
                    && PrimeArenaBuilder.ensureLegacyOmegaPortal(player.serverLevel(),player)){
                msg(player,"§aROTA RESTAURADA: §fum portal verde para o Dispositivo Ômega abriu no laboratório.");
            }
        }

        // Estágio 0 → 1: primeira vez na Federação carregando Portal Gun com pouco wanted
        if (st == 0 && inFederation && hasPortalGun) {
            int wanted = player.getPersistentData().getInt(FederationThreatHandler.KEY_WANTED);
            if (wanted <= 2) triggerIntercept(player);
        }

        // Estágio 1 → 2: chegou perto do centro administrativo
        if (st == 1 && inFederation && player.blockPosition().distSqr(FederationBuilder.CENTER) < 20 * 20) {
            setStage(player, 2);
            msg(player, "§b[OFICIAL GROMFLOMITE]§r \"Portador registrado. A Federação deseja negociar. Vá até o Arquivo Central.\"");
        }

        // Migration/repair for saves that already reached the trap reveal while
        // older builds only wrote an invisible active destination.
        if (st == 10 || st == 11) ensureTrapDestination(player, false);

        // Estágio 5: ticka a busca por decoys automaticamente perto do jogador (ativado via scanner)
        // (a ação real de scan é feita via NeuralScannerItem chamando triggerDecoyScan)

        // Estágio 9 → 10: após tempo de "sinal 100%", ativa a armadilha automaticamente ao usar Portal Gun de novo
    }

    private static void showObjective(ServerPlayer player,int stage,boolean inFederation){
        String objective=switch(stage){
            case 1->"Vá ao Complexo Administrativo da Federação";
            case 2->"Acesse o Arquivo Central em 0, 97, -72";
            case 3,4->"Instale o upgrade no Scanner Neural";
            case 5->"Use o Scanner Neural: ecos P-0 "+data(player).getInt("Decoys")+"/"+DECOYS_TARGET;
            case 6->"Encontre Rick C-137 e escute o plano";
            case 7->"Escape da emboscada da Federação";
            case 8->"Ajude Evil Morty a filtrar as assinaturas";
            case 9->"Abra o destino Armadilha P-0 salvo na Portal Gun";
            case 10->"Explore a instalação da armadilha";
            case 11->"Sobreviva ao battle royale dos Ricks";
            case 12->"Retorne ao Dispositivo Ômega e conclua o cálculo";
            case 13->"Destino UNMORTRICKEN liberado";
            default->null;
        };
        if(objective==null)return;
        if((stage==1||stage==2)&&inFederation){BlockPos target=stage==1?FederationBuilder.CENTER:FederationBuilder.CENTER.offset(0,1,-72);objective+=" — "+(int)Math.sqrt(player.blockPosition().distSqr(target))+"m";}
        player.displayClientMessage(Component.literal("§6[P-0] §f"+objective),true);
    }

    private static void triggerIntercept(ServerPlayer player) {
        setStage(player, 1);
        msg(player, "§e[TRANSMISSÃO DA FEDERAÇÃO]§r \"Portador de tecnologia interdimensional não registrada. Desligue seu sistema de portal.\"");
        msg(player, "§e[TRANSMISSÃO]§r \"Você não está sendo preso. A Federação deseja negociar.\"");
        msg(player, "§7Coordenada recebida: §fGromflom Prime — Complexo Administrativo (" +
                FederationBuilder.CENTER.getX() + ", " + FederationBuilder.CENTER.getY() + ", " + FederationBuilder.CENTER.getZ() + ")");
    }

    public static void interactCommander(ServerPlayer player) {
        int current = stage(player);
        if (current == 0) triggerIntercept(player);
        if (stage(player) == 1) {
            setStage(player, 2);
            msg(player, "§b[COMANDANTE VRAX]§r \"Se quer respostas sobre P-0, acesse o Arquivo Central atrás de mim. Não tente roubar tecnologia federal.\"");
            msg(player, "§eOBJETIVO: §fUse o terminal do Arquivo Central em 0, 97, -72.");
        } else if (stage(player) == 2) {
            msg(player, "§b[COMANDANTE VRAX]§r \"O Arquivo Central está em 0, 97, -72. Faça a leitura e volte vivo.\"");
        } else {
            msg(player, "§b[COMANDANTE VRAX]§r \"A operação P-0 já está em andamento. Consulte seu scanner.\"");
        }
    }

    // ── ESTÁGIO 3: Arquivo da Guerra Dimensional (bloco ARCHIVE na Federação) ─
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState state = player.level().getBlockState(event.getPos());

        // Interagir com o arquivo (reaproveita CitadelServiceBlock.Service.ARCHIVE já usado na Cidadela)
        if (state.getBlock() instanceof com.jhonfx.portalgun.block.CitadelServiceBlock service
                && player.level().dimension().location().equals(FEDERATION_ID)) {
            if (stage(player) == 2) {
                completeArchiveMission(player);
                event.setCanceled(true);
            }
        }

        // Combinação final no Dispositivo Ômega (estágio 12 → 13)
        if (state.is(com.jhonfx.portalgun.init.ModBlocks.OMEGA_DEVICE.get()) && stage(player) == 12) {
            tryFinalCalculation(player);
        }
    }

    private static void completeArchiveMission(ServerPlayer player) {
        setStage(player, 3);
        give(player, new ItemStack(ModItems.P0_SIGNATURE_FRAGMENT.get()));
        give(player, new ItemStack(ModItems.MULTIVERSAL_SCANNER_UPGRADE_CHIP.get()));
        msg(player, "§b[ARQUIVOS DA GUERRA DIMENSIONAL]§r Banco de dados acessado.");
        msg(player, "§7PORTAL SIGNATURE DATABASE\n" +
                "§7C-137 .............. REGISTERED\n" +
                "§7J-19ζ7 ............. REGISTERED\n" +
                "§7C-500A ............. REGISTERED\n" +
                "§cUNKNOWN-001 ......... NO MATCH\n" +
                "§cDESIGNATION: P-0  (PRIMEIRO REGISTRO: 47 ANOS ATRÁS)");
        msg(player, "§aVocê recebeu: §fFragmento de Assinatura P-0");
        // Upgrade automático do scanner (aplica flag em qualquer Scanner Neural já no inventário)
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof NeuralScannerItem) {
                s.getOrCreateTag().putBoolean("MultiversalUpgrade", true);
            }
        }
        setStage(player, 5);
        msg(player, "§b[SCANNER MULTIVERSAL]§r Upgrade instalado. Use o Scanner Neural para \"Localizar P-0\".");
    }

    // ── ESTÁGIO 5: Busca por ecos de P-0 (chamado pelo NeuralScannerItem) ────
    public static void triggerDecoyScan(ServerPlayer player) {
        if (stage(player) != 5) return;
        CompoundTag tag = data(player);
        int found = tag.getInt("Decoys");

        String[][] flavors = {
            {"§7Laboratório abandonado.", "§7Restos de equipamento e um diário corrompido."},
            {"§7Um Rick morto.", "§7Não é ele. A assinatura é falsa — uma isca."},
            {"§cUm robô com a forma de Rick Prime.", "§cEle ataca ao ser detectado!"},
            {"§7Estação destruída.", "§7Nada além de escombros e radiação residual."},
            {"§eUm clone instável.", "§eEle se desfaz em poucos segundos."},
            {"§4Uma bomba camuflada de artefato.", "§4Ative distância — ela detona sozinha."},
            {"§7Um falso esconderijo.", "§7Perfeitamente vazio. Armadilha psicológica."}
        };
        String[] pick = flavors[RANDOM.nextInt(flavors.length)];
        msg(player, "§b[SCANNER MULTIVERSAL]§r " + pick[0]);
        msg(player, pick[1]);

        // Chance de spawnar um mob hostil relacionado ao "eco" encontrado (perigo crescente)
        if (player.level() instanceof ServerLevel level && RANDOM.nextFloat() < 0.6f) {
            Vec3 spot = player.position().add(RANDOM.nextGaussian() * 6, 0, RANDOM.nextGaussian() * 6);
            var alien = ModEntityTypes.FEDERATION_ALIEN.get().create(level);
            if (alien != null) {
                alien.moveTo(spot.x, player.getY(), spot.z, 0, 0);
                alien.setTarget(player);
                level.addFreshEntity(alien);
            }
            level.sendParticles(ParticleTypes.PORTAL, spot.x, player.getY() + 1, spot.z, 30, 0.5, 1, 0.5, 0.1);
        }

        found++;
        tag.putInt("Decoys", found);
        save(player, tag);

        if (found >= DECOYS_TARGET) {
            setStage(player, 6);
            scheduleRickC137(player);
        } else {
            msg(player, "§7Ecos investigados: " + found + "/" + DECOYS_TARGET);
        }
    }

    // ── ESTÁGIO 6: Rick C-137 aparece ────────────────────────────────────────
    private static void scheduleRickC137(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 behind = player.position().subtract(player.getLookAngle().scale(3));
        CompoundTag tag=data(player);
        java.util.UUID portal=com.jhonfx.portalgun.entity.CinematicPortal.spawn(level,behind.add(0,1,0),
                Direction.SOUTH,com.jhonfx.portalgun.entity.PortalColor.GREEN,.82f);
        tag.putBoolean("C137ArrivalScheduled",true);tag.putLong("C137ArrivalAt",level.getGameTime()+30);
        tag.putDouble("C137X",behind.x);tag.putDouble("C137Y",behind.y);tag.putDouble("C137Z",behind.z);
        if(portal!=null)tag.putUUID("C137Portal",portal);save(player,tag);
        level.playSound(null,BlockPos.containing(behind),com.jhonfx.portalgun.init.ModSounds.PORTAL_SPAWN.get(),SoundSource.PLAYERS,1.4f,.92f);
        msg(player,"§aUm portal verde se abre atrás de você...");
    }

    private static void spawnRickC137(ServerPlayer player,CompoundTag tag) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 behind=new Vec3(tag.getDouble("C137X"),tag.getDouble("C137Y"),tag.getDouble("C137Z"));
        CitadelNpcEntity rick = ModEntityTypes.CITADEL_CITIZEN.get().create(level);
        if (rick == null) return;
        rick.setVariant(CitadelNpcEntity.Variant.RICK);
        rick.setCustomName(Component.literal("Rick C-137"));
        rick.setCustomNameVisible(true);
        rick.moveTo(behind.x, behind.y, behind.z, player.getYRot() + 180, 0);
        rick.setPersistenceRequired();
        rick.getPersistentData().putBoolean("PortalGunC137Cameo", true);
        level.addFreshEntity(rick);
        tag.putBoolean("C137Spawned",true);save(player,tag);

        msg(player, "§aRick: §f\"Okay. Ou você é absurdamente sortudo ou acabou de passar duas semanas seguindo o cara que eu procuro há décadas.\"");
        msg(player, "§7(Interaja com ele para continuar)");
    }

    /** Chamado pelo CitadelNpcEntity ao interagir com o Rick C-137 marcado (cameo). */
    public static boolean interactC137Cameo(CitadelNpcEntity rick, ServerPlayer player) {
        if (!rick.getPersistentData().getBoolean("PortalGunC137Cameo")) return false;
        int st = stage(player);
        if (st != 6) return true; // já passou dessa parte, ignora

        msg(player, "§aRick: §f\"Onde conseguiu isso?\"");
        msg(player, "§7Você: §fFederação Galáctica.");
        msg(player, "§aRick: §f\"Claro. Os insetos guardaram meus rastros por cinquenta anos mas não conseguem guardar uma economia por cinco minutos.\"");
        msg(player, "§aRick: §f*analisa o scanner* ...\"Ah, merda.\"");
        msg(player, "§7CROSS-CONTACT DETECTED: §cC-137");

        // Rick "rouba" o scanner por um instante — narrativo apenas, não remove item de verdade
        rick.discard();
        federationBetrayal(player);
        return true;
    }

    // ── ESTÁGIO 7: A Federação trai o jogador (Ordem Federal 71) ─────────────
    private static void federationBetrayal(ServerPlayer player) {
        setStage(player, 7);
        player.getPersistentData().putInt(FederationThreatHandler.KEY_WANTED, 5);
        msg(player, "§4[ORDEM FEDERAL 71]§r Capturar o portador. Confiscar tecnologia P-0.");
        msg(player, "§cVocê precisa fugir da Federação!");
        setStage(player, 8);
        // Evil Morty se junta pouco depois de escapar
    }

    // ── ESTÁGIO 8: Evil Morty aparece ────────────────────────────────────────
    @SubscribeEvent
    public void onKillCheck(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        if (stage(player) != 8) return;

        boolean stillInFederation = player.level().dimension().location().equals(FEDERATION_ID);
        int wanted = player.getPersistentData().getInt(FederationThreatHandler.KEY_WANTED);
        if (stillInFederation || wanted >= 4) return; // ainda fugindo

        CompoundTag tag=data(player);if(tag.getBoolean("EvilAllyArrivalScheduled"))return;
        // Escapou — primeiro o portal amarelo real abre; o aliado atravessa depois.
        if (player.level() instanceof ServerLevel level) {
            Vec3 spot=player.position().add(player.getLookAngle().scale(4));
            java.util.UUID portal=com.jhonfx.portalgun.entity.CinematicPortal.spawn(level,spot.add(0,1,0),
                    Direction.SOUTH,com.jhonfx.portalgun.entity.PortalColor.YELLOW,.78f);
            tag.putBoolean("EvilAllyArrivalScheduled",true);tag.putLong("EvilAllyArrivalAt",level.getGameTime()+30);
            tag.putDouble("EvilAllyX",spot.x);tag.putDouble("EvilAllyY",spot.y);tag.putDouble("EvilAllyZ",spot.z);
            if(portal!=null)tag.putUUID("EvilAllyPortal",portal);save(player,tag);
            level.playSound(null,BlockPos.containing(spot),com.jhonfx.portalgun.init.ModSounds.PORTAL_SPAWN.get(),SoundSource.PLAYERS,1.5f,1.16f);
            msg(player,"§6Um portal amarelo se abre à sua frente...");
        }
    }

    private static void tickStoryPortals(ServerPlayer player){
        CompoundTag tag=data(player);long now=player.level().getGameTime();ServerLevel level=player.serverLevel();
        if(stage(player)==6&&!tag.getBoolean("C137ArrivalScheduled")&&!tag.getBoolean("C137Spawned"))scheduleRickC137(player);
        tag=data(player);
        if(tag.getBoolean("C137ArrivalScheduled")&&!tag.getBoolean("C137Spawned")&&now>=tag.getLong("C137ArrivalAt"))spawnRickC137(player,tag);
        if(tag.getBoolean("C137ArrivalScheduled")&&!tag.getBoolean("C137PortalClosed")&&now>=tag.getLong("C137ArrivalAt")+50){
            if(tag.hasUUID("C137Portal"))com.jhonfx.portalgun.entity.CinematicPortal.close(level,tag.getUUID("C137Portal"));tag.putBoolean("C137PortalClosed",true);save(player,tag);}
        tag=data(player);
        if(tag.getBoolean("EvilAllyArrivalScheduled")&&!tag.getBoolean("EvilAllySpawned")&&now>=tag.getLong("EvilAllyArrivalAt")){
            var morty=ModEntityTypes.EVIL_MORTY_ALLY.get().create(level);
            if(morty!=null){morty.setStoryAlly(true);morty.moveTo(tag.getDouble("EvilAllyX"),tag.getDouble("EvilAllyY"),tag.getDouble("EvilAllyZ"),180,0);
                morty.setPersistenceRequired();morty.getPersistentData().putBoolean("PortalGunP0Cameo",true);level.addFreshEntity(morty);}
            tag.putBoolean("EvilAllySpawned",true);setStage(player,9);tag=data(player);tag.putLong("SignalTime",now);save(player,tag);
            msg(player,"§6Evil Morty: §f\"Se existe uma arma capaz de apagar uma pessoa do infinito, eu prefiro saber onde ela está.\"");}
        tag=data(player);
        if(tag.getBoolean("EvilAllyArrivalScheduled")&&!tag.getBoolean("EvilAllyPortalClosed")&&now>=tag.getLong("EvilAllyArrivalAt")+50){
            if(tag.hasUUID("EvilAllyPortal"))com.jhonfx.portalgun.entity.CinematicPortal.close(level,tag.getUUID("EvilAllyPortal"));tag.putBoolean("EvilAllyPortalClosed",true);save(player,tag);}
    }

    // ── ESTÁGIO 9 → 10: sinal 100% após um tempo, revela local da armadilha ──
    @SubscribeEvent
    public void onSignalTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (stage(player) != 9) return;
        if (player.tickCount % 100 != 0) return;

        long elapsed = player.level().getGameTime() - data(player).getLong("SignalTime");
        if (elapsed < 600) return; // ~30s de suspense
        setStage(player, 10);
        msg(player, "§b[SCANNER MULTIVERSAL]§r P-0 SIGNATURE — MATCH: §a100.000%");
        msg(player, "§aRick: §f\"É ele.\"");
        msg(player, "§7Um novo destino foi salvo na sua Portal Gun.");

        ensureTrapDestination(player,true);
        ServerLevel federation=player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,FEDERATION_ID));
        if(federation!=null)PrimeArenaBuilder.ensureBuilt(federation);
    }

    // ── ESTÁGIO 10 → 11: entrar na instalação da armadilha ───────────────────
    @SubscribeEvent
    public void onArenaTrigger(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (stage(player) != 10) return;
        if (!player.level().dimension().location().equals(FEDERATION_ID)) return;
        if (player.blockPosition().distSqr(PrimeArenaBuilder.TRAP_ENTRANCE) > 6 * 6) return;

        springTrap(player);
    }

    private static void springTrap(ServerPlayer player) {
        setStage(player, 11);
        msg(player, "§7Você encontra uma instalação pequena. Sem exército. Sem armadura absurda. Sem nada.");
        msg(player, "§7\"Foi fácil demais\", você pensa.");
        // O "falso Prime" se dissolve em gosma preta
        msg(player, "§4A criatura se dissolve em uma gosma preta...");
        msg(player, "§c[SCANNER] P-0 SIGNATURE ERROR — PORTAL HIJACK DETECTED");
        msg(player, "§4TELEPORTE FORÇADO.");
        msg(player, "§4[RICK PRIME]§r \"Quatro salas. Dois Ricks em cada. O último vivo ganha o cachorro de volta. Palavra de Rick.\"");
        msg(player, "§7A promessa soa ensaiada demais para ser verdadeira.");

        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos arena = PrimeArenaBuilder.ensureBuilt(level);
        player.teleportTo(level, arena.getX() + .5, arena.getY() + 1, arena.getZ() + .5, 0, 0);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));

        PrimeArenaBuilder.spawnCaptiveRicks(level, arena);
        player.getPersistentData().putLong("PortalGunArenaStart", level.getGameTime());
    }

    // ── ESTÁGIO 11 → 12: última luta acaba com Diane-Bot ─────────────────────
    @SubscribeEvent
    public void onArenaTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (stage(player) != 11) return;
        if (player.tickCount % 40 != 0) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        BlockPos arena = PrimeArenaBuilder.currentArenaCenter(level);
        if (arena == null) return;

        List<CitadelNpcEntity> ricksLeft = level.getEntitiesOfClass(CitadelNpcEntity.class,
                new AABB(arena).inflate(PrimeArenaBuilder.RADIUS),
                e -> CitadelNpcEntity.isCaptiveRick(e.getVariant()) && e.isAlive());

        for(int quadrant=0;quadrant<4;quadrant++){
            final int q=quadrant;
            List<CitadelNpcEntity> local=ricksLeft.stream().filter(r->r.getPersistentData().getBoolean("PortalGunArenaRick")
                    &&r.getPersistentData().getInt("PortalGunArenaQuadrant")==q).toList();
            if(local.size()<=1&&PrimeArenaBuilder.openWinnerGate(level,arena,q)){
                if(local.size()==1)local.get(0).getPersistentData().putBoolean("PortalGunArenaFinalRound",true);
                msg(player,local.size()==1?"§eCOMPARTIMENTO "+(q+1)+" ABERTO§r — um vencedor avançou para a rodada final."
                        :"§eCOMPARTIMENTO "+(q+1)+" ABERTO§r — nenhum Rick sobreviveu.");
            }
        }

        if (ricksLeft.size() <= 1 && !player.getPersistentData().getBoolean("PortalGunDianeSpawned")) {
            // Último Rick sobrevivente pensa que venceu — Diane-Bot aparece e o mata
            player.getPersistentData().putBoolean("PortalGunDianeSpawned", true);
            msg(player, "§7O último Rick olha para a tela. Prime aparece: §f\"Parabéns.\"");
            msg(player, "§7Uma porta abre. Ele acredita que Diane está do outro lado.");
            var diane = ModEntityTypes.DIANE_ROBOT.get().create(level);
            if (diane != null) {
                net.minecraft.world.entity.LivingEntity victim=ricksLeft.isEmpty()?player:ricksLeft.get(0);
                diane.moveTo(victim.getX() + 1, victim.getY(), victim.getZ(), 0, 0);
                diane.setTarget(victim);
                level.addFreshEntity(diane);
            }
        } else if (ricksLeft.isEmpty() && player.getPersistentData().getBoolean("PortalGunDianeSpawned")
                && level.getEntitiesOfClass(DianeRobotEntity.class,
                    new AABB(arena).inflate(PrimeArenaBuilder.RADIUS), DianeRobotEntity::isAlive).isEmpty()) {
            arenaComplete(player, level, arena);
        }
    }

    private static void arenaComplete(ServerPlayer player, ServerLevel level, BlockPos arena) {
        setStage(player, 12);
        give(player, new ItemStack(ModItems.PRIME_TELEPORT_RESIDUE.get()));
        player.getPersistentData().putBoolean("PortalGunResidueGrantTracked",true);
        msg(player, "§aVocê coleta a gosma que trouxe vocês até aqui.");
        msg(player, "§7NOVO ITEM: §fResíduo de Teleporte Prime");
        msg(player, "§7\"Não é apenas uma assinatura. Existe um destino codificado nela.\"");
        // Libera o bloqueio dimensional da sala
        PrimeArenaBuilder.releaseJam(level, arena, player);
        level.sendParticles(ParticleTypes.END_ROD, arena.getX(), arena.getY() + 2, arena.getZ(), 60, 3, 1, 3, 0.1);
        ensureOmegaDestination(player);
        msg(player, "§7Rick Prime mentiu sobre o cachorro. O portal verde de evacuação agora leva diretamente ao Dispositivo Ômega.");
    }

    // ── ESTÁGIO 13: cálculo final no Dispositivo Ômega ───────────────────────
    private static void tryFinalCalculation(ServerPlayer player) {
        boolean hasFragment = player.getInventory().items.stream().anyMatch(s -> s.is(ModItems.P0_SIGNATURE_FRAGMENT.get()));
        boolean hasResidue   = player.getInventory().items.stream().anyMatch(s -> s.is(ModItems.PRIME_TELEPORT_RESIDUE.get()));
        boolean hasScanner   = player.getInventory().items.stream().anyMatch(s ->
                s.getItem() instanceof NeuralScannerItem && s.getOrCreateTag().getBoolean("MultiversalUpgrade"));

        if (!hasFragment || !hasResidue || !hasScanner) {
            msg(player, "§cItens insuficientes para o cálculo. É necessário: Fragmento P-0, Resíduo de Teleporte Prime, e Scanner Multiversal.");
            return;
        }

        msg(player, "§7A tela começa a calcular...");
        msg(player, "§7DECOYS REMOVED: 63 → 14 → 6 → 3 → 1");
        msg(player, "§aORIGIN NODE FOUND — RICK PRIME — LOCALIZAÇÃO CONFIRMADA");
        setStage(player, 13);

        // Consome os itens de lore (mantém o scanner)
        consumeOne(player, ModItems.P0_SIGNATURE_FRAGMENT.get());
        consumeOne(player, ModItems.PRIME_TELEPORT_RESIDUE.get());

        // The confirmed signature unlocks and saves the real Prime dimension.
        player.getPersistentData().putBoolean("PortalGunUnmortrickenUnlocked", true);
        for (int slot=0;slot<player.getInventory().getContainerSize();slot++) {
            ItemStack gun=player.getInventory().getItem(slot);
            if (!(gun.getItem() instanceof PortalGunItem item) || item.variant().isPrototype()) continue;
            saveStoryDestination(gun,"UNMORTRICKEN",UnmortrickenFacility.ARRIVAL,UnmortrickenFacility.ID.toString());
        }
        ServerLevel destinationLevel = player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, UnmortrickenFacility.ID));
        if (destinationLevel != null) UnmortrickenFacility.ensureBuilt(destinationLevel);
        msg(player, "§6[MISSÃO PRINCIPAL] UNMORTRICKEN§r — Encontre Rick Prime.");
        msg(player, "§7Destino salvo nas armas compatíveis: portalgun:unmortricken.");
    }

    private static void ensureTrapDestination(ServerPlayer player,boolean announce){
        ServerLevel federation=player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,FEDERATION_ID));
        if(federation!=null){federation.getChunkAt(PrimeArenaBuilder.TRAP_ENTRANCE);PrimeArenaBuilder.ensureBuilt(federation);}
        int saved=0;BlockPos trap=PrimeArenaBuilder.TRAP_ARRIVAL;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            ItemStack gun=player.getInventory().getItem(slot);if(!(gun.getItem() instanceof PortalGunItem))continue;
            if(!hasSavedLocation(gun,"Armadilha P-0",trap,"portalgun:federation")){saveStoryDestination(gun,"Armadilha P-0",trap,"portalgun:federation");saved++;}
        }
        if(saved>0&&(announce||!data(player).getBoolean("TrapDestinationRepaired"))){msg(player,"§aDestino salvo: §fArmadilha P-0 §7(Federação)");CompoundTag tag=data(player);tag.putBoolean("TrapDestinationRepaired",true);save(player,tag);}
    }

    private static void ensureOmegaDestination(ServerPlayer player){
        int saved=0;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            ItemStack gun=player.getInventory().getItem(slot);
            if(!(gun.getItem() instanceof PortalGunItem))continue;
            if(!hasSavedLocation(gun,"Dispositivo Ômega",PrimeArenaBuilder.OMEGA_RETURN_ARRIVAL,"portalgun:federation")){
                saveStoryDestination(gun,"Dispositivo Ômega",PrimeArenaBuilder.OMEGA_RETURN_ARRIVAL,"portalgun:federation");saved++;
            }
        }
        if(saved>0)msg(player,"§aDestino salvo: §fDispositivo Ômega §7(portalgun:federation)");
    }

    private static void ensureUnmortrickenDestination(ServerPlayer player){
        if(!player.getPersistentData().getBoolean("PortalGunUnmortrickenUnlocked"))return;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            ItemStack gun=player.getInventory().getItem(slot);
            if(!(gun.getItem() instanceof PortalGunItem item)||item.variant().isPrototype())continue;
            if(!hasSavedLocation(gun,"UNMORTRICKEN",UnmortrickenFacility.ARRIVAL,UnmortrickenFacility.ID.toString()))
                saveStoryDestination(gun,"UNMORTRICKEN",UnmortrickenFacility.ARRIVAL,UnmortrickenFacility.ID.toString());
        }
    }

    /** Admin recovery entry point: builds in the correct dimension and repairs every owned gun. */
    public static int renderTrapFor(ServerPlayer player){
        ServerLevel federation=player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,FEDERATION_ID));
        if(federation==null){msg(player,"§cA dimensão portalgun:federation não está carregada ou registrada.");return 0;}
        federation.getChunkAt(PrimeArenaBuilder.TRAP_ENTRANCE);PrimeArenaBuilder.ensureBuilt(federation);
        BlockPos p=PrimeArenaBuilder.TRAP_ARRIVAL;
        boolean safe=federation.getBlockState(p).isAir()&&federation.getBlockState(p.above()).isAir()
                &&federation.getBlockState(p.below()).isFaceSturdy(federation,p.below(),Direction.UP);
        ensureTrapDestination(player,true);
        msg(player,safe?"§aArmadilha P-0 renderizada e validada em §f300, 98, 300 §7na Federação."
                :"§cA armadilha foi construída, mas o teste do ponto de chegada falhou.");
        return safe?1:0;
    }

    /** Admin-safe recovery for a missing calculation hall or destination. */
    public static int renderOmegaFor(ServerPlayer player){
        ServerLevel federation=player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,FEDERATION_ID));
        if(federation==null){msg(player,"§cA dimensão portalgun:federation não está carregada ou registrada.");return 0;}
        PrimeArenaBuilder.ensureOmegaReturn(federation);
        ensureOmegaDestination(player);
        BlockPos console=PrimeArenaBuilder.OMEGA_RETURN_CENTER.relative(Direction.NORTH,OmegaStructureBuilder.CONSOLE_DISTANCE);
        boolean built=federation.getBlockState(console).is(com.jhonfx.portalgun.init.ModBlocks.OMEGA_DEVICE.get());
        msg(player,built?"§aDispositivo Ômega renderizado em §f-300, 98, 300 §7na Federação."
                :"§cA construção do Dispositivo Ômega não foi validada.");
        return built?1:0;
    }

    private static boolean hasSavedLocation(ItemStack gun,String name,BlockPos pos,String dimension){PortalGunItem.initialize(gun);ListTag list=gun.getOrCreateTag().getList(PortalGunItem.TAG_SAVED_LOCATIONS,Tag.TAG_COMPOUND);
        for(Tag raw:list){CompoundTag entry=(CompoundTag)raw;if(entry.getString("Name").equalsIgnoreCase(name))return entry.getString("Dimension").equals(dimension)
                &&Math.abs(entry.getDouble("X")-(pos.getX()+.5))<.1&&Math.abs(entry.getDouble("Y")-pos.getY())<.1&&Math.abs(entry.getDouble("Z")-(pos.getZ()+.5))<.1;}return false;}
    private static void saveStoryDestination(ItemStack gun,String name,BlockPos pos,String dimension){
        PortalGunItem.initialize(gun);CompoundTag dest=new CompoundTag();dest.putString("Name",name);dest.putDouble("X",pos.getX()+.5);dest.putDouble("Y",pos.getY());dest.putDouble("Z",pos.getZ()+.5);dest.putString("Dimension",dimension);
        CompoundTag root=gun.getOrCreateTag();root.put(PortalGunItem.TAG_DESTINATION,dest.copy());root.putString(PortalGunItem.TAG_MODE,com.jhonfx.portalgun.item.PortalMode.CUSTOM.name());
        ListTag list=root.getList(PortalGunItem.TAG_SAVED_LOCATIONS,Tag.TAG_COMPOUND);for(int i=list.size()-1;i>=0;i--)if(list.getCompound(i).getString("Name").equalsIgnoreCase(name))list.remove(i);list.add(0,dest.copy());while(list.size()>24)list.remove(list.size()-1);root.put(PortalGunItem.TAG_SAVED_LOCATIONS,list);
    }

    private static void consumeOne(ServerPlayer player, net.minecraft.world.item.Item item) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(item)) { s.shrink(1); return; }
        }
    }
}
