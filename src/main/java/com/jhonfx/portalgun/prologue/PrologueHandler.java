package com.jhonfx.portalgun.prologue;

import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import java.util.UUID;

public final class PrologueHandler {
    private static final String ROOT="PortalGunPrologue";
    private static final DustParticleOptions PURPLE=new DustParticleOptions(new Vector3f(0.82f,0.04f,1f),1.35f);

    @SubscribeEvent public void onTame(AnimalTameEvent event) {
        if(!(event.getAnimal() instanceof Wolf wolf)||!(event.getTamer() instanceof ServerPlayer player)) return;
        boolean hasBlue=player.getInventory().items.stream().anyMatch(stack -> stack.is(ModItems.BLUE_PROTOTYPE_PORTAL_GUN.get()))
                || player.getOffhandItem().is(ModItems.BLUE_PROTOTYPE_PORTAL_GUN.get());
        if(!hasBlue){
            player.displayClientMessage(Component.literal("§7Você sente que este vínculo ainda precisa de alguma tecnologia de portal azul..."),false);
            return;
        }
        CompoundTag tag=data(player); if(tag.getInt("Stage")>0) return;
        tag.putInt("Stage",1); tag.putLong("Start",player.level().getGameTime()); rememberDog(tag,wolf); save(player,tag);
        spawnProjection(player,tag);
        player.displayClientMessage(Component.translatable("message.portalgun.prologue_started",wolf.getDisplayName()),false);
        player.displayClientMessage(Component.literal("§aRick Prime detectou o protótipo azul. §fFale com a projeção dele para descobrir o que ele quer."),false);
    }

    @SubscribeEvent public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("portalgun")
                .then(Commands.literal("prologue")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> startNow(context.getSource().getPlayerOrException()))
                        .then(Commands.literal("start")
                                .executes(context -> startNow(context.getSource().getPlayerOrException())))));
    }

    private static int startNow(ServerPlayer player) {
        Wolf dog=player.serverLevel().getEntitiesOfClass(Wolf.class,new AABB(player.blockPosition()).inflate(32),
                wolf -> wolf.isTame()&&player.getUUID().equals(wolf.getOwnerUUID())).stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if(dog==null) {
            player.displayClientMessage(Component.translatable("message.portalgun.prologue_no_dog"),false);
            return 0;
        }
        player.serverLevel().getEntitiesOfClass(CitadelNpcEntity.class,new AABB(player.blockPosition()).inflate(48),
                npc -> npc.getVariant()==CitadelNpcEntity.Variant.PRIME).forEach(Entity::discard);
        CompoundTag tag=new CompoundTag(); tag.putInt("Stage",1); tag.putLong("Start",player.level().getGameTime());
        rememberDog(tag,dog); save(player,tag); spawnProjection(player,tag);
        player.displayClientMessage(Component.translatable("message.portalgun.prologue_forced",dog.getDisplayName()),false);
        return 1;
    }

    @SubscribeEvent public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide||!(event.player instanceof ServerPlayer player)) return;
        CompoundTag tag=data(player); int stage=tag.getInt("Stage"); long now=player.level().getGameTime();
        if(tag.getBoolean("ProjectionPending"))tickProjectionArrival(player,tag,now);
        if(stage==6||stage==7){
            Entity dog=findDog(player,tag);if(dog!=null){rememberDog(tag,dog);save(player,tag);}
            tickRefusal(player,tag,stage,now);return;
        }
        if(event.player.tickCount%20!=0)return;
        if(stage>0&&stage<5){Entity dog=findDog(player,tag);if(dog!=null){rememberDog(tag,dog);save(player,tag);}}
        if(stage==4&&has(player,ModItems.PROTOTYPE_PORTAL_GUN.get())){
            tag.putInt("Stage",5);save(player,tag);
            player.displayClientMessage(Component.translatable("message.portalgun.prologue_green_complete"),false);
            return;
        }
        if(stage==1&&now-tag.getLong("Start")>=24000&&!tag.getBoolean("Projection")&&!tag.getBoolean("ProjectionPending")) spawnProjection(player,tag);
        if(stage==2&&now-tag.getLong("Next")>=1200&&!tag.getBoolean("Projection")&&!tag.getBoolean("ProjectionPending")) spawnProjection(player,tag);
    }

    /** Accepting Prime's deal is completed by actually building the green prototype. */
    @SubscribeEvent public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !event.getCrafting().is(ModItems.PROTOTYPE_PORTAL_GUN.get())) return;
        CompoundTag tag=data(player);
        if(tag.getInt("Stage")!=4)return;
        tag.putInt("Stage",5);
        save(player,tag);
        player.displayClientMessage(Component.translatable("message.portalgun.prologue_green_complete"),false);
    }

    public static void interactPrime(CitadelNpcEntity prime,Player raw) {
        if(!(raw instanceof ServerPlayer player)) return; CompoundTag tag=data(player); int stage=tag.getInt("Stage");
        if(stage==1) {
            give(player,new ItemStack(ModItems.IMPOSSIBLE_COMPONENT.get())); tag.putInt("Stage",2); tag.putLong("Next",player.level().getGameTime());
            player.displayClientMessage(Component.translatable("message.portalgun.prime_first"),false);
        } else if(stage==2) {
            give(player,new ItemStack(ModItems.PRIME_COMMUNICATOR.get())); tag.putInt("Stage",3);
            player.displayClientMessage(Component.translatable("message.portalgun.prime_offer"),false);
        } else return;
        tag.putBoolean("Projection",false); save(player,tag); prime.discard();
    }

    public static void answer(Player raw,boolean refuse) {
        if(!(raw instanceof ServerPlayer player)) return; CompoundTag tag=data(player); if(tag.getInt("Stage")!=3) return;
        if(refuse) {
            Entity dog=findDog(player,tag); if(dog==null){player.displayClientMessage(Component.translatable("message.portalgun.prologue_dog_missing"),false);return;}
            tag.putInt("Stage",6);tag.putLong("RefuseAt",player.level().getGameTime());
            player.displayClientMessage(Component.translatable("message.portalgun.prime_argument_1"),false);
        } else { tag.putInt("Stage",4); player.displayClientMessage(Component.translatable("message.portalgun.prime_accepted"),false); }
        consume(player,ModItems.PRIME_COMMUNICATOR.get()); save(player,tag);
    }

    private static void tickRefusal(ServerPlayer player,CompoundTag tag,int stage,long now){
        Entity dog=findDog(player,tag); if(dog==null){tag.putInt("Stage",5);save(player,tag);return;}
        if(stage==6){
            long elapsed=now-tag.getLong("RefuseAt"); portalAbove(player,tag,dog,elapsed);
            if(elapsed==20)player.displayClientMessage(Component.translatable("message.portalgun.prime_argument_2"),false);
            if(elapsed==40)player.displayClientMessage(Component.translatable("message.portalgun.prime_argument_3"),false);
            if(elapsed>=60){
                ItemEntity bomb=new ItemEntity(player.serverLevel(),dog.getX(),dog.getY()+3.25,dog.getZ(),new ItemStack(ModItems.PRIME_BOMB.get()));
                bomb.setPickUpDelay(200);bomb.setDeltaMovement(0,-0.08,0);player.serverLevel().addFreshEntity(bomb);
                tag.putUUID("Bomb",bomb.getUUID());tag.putInt("Stage",7);tag.putLong("BombAt",now);save(player,tag);
                player.serverLevel().playSound(null,dog.blockPosition(),SoundEvents.DISPENSER_LAUNCH,SoundSource.HOSTILE,1.6f,.55f);
            }
        }else{
            Entity bomb=tag.hasUUID("Bomb")?player.serverLevel().getEntity(tag.getUUID("Bomb")):null;
            long elapsed=now-tag.getLong("BombAt");
            if(bomb!=null){player.serverLevel().sendParticles(PURPLE,bomb.getX(),bomb.getY()+.25,bomb.getZ(),5,.25,.25,.25,.025);if(elapsed%10==0)player.serverLevel().playSound(null,bomb.blockPosition(),SoundEvents.NOTE_BLOCK_HAT.value(),SoundSource.HOSTILE,1f,.5f+elapsed/45f);}
            if(elapsed>=40){
                if(tag.hasUUID("DogPortal")) com.jhonfx.portalgun.entity.CinematicPortal.close(player.serverLevel(),tag.getUUID("DogPortal"));
                tag.remove("DogPortal");
                explodeBomb(player.serverLevel(),dog,bomb);tag.putInt("Stage",5);save(player,tag);player.displayClientMessage(Component.translatable("message.portalgun.prime_refused"),false);
            }
        }
    }

    private static void portalAbove(ServerPlayer player,CompoundTag tag,Entity dog,long tick){
        ServerLevel level=player.serverLevel();
        com.jhonfx.portalgun.entity.PortalEntity portal=tag.hasUUID("DogPortal")
                && level.getEntity(tag.getUUID("DogPortal")) instanceof com.jhonfx.portalgun.entity.PortalEntity found ? found : null;
        if(portal==null){
            UUID id=com.jhonfx.portalgun.entity.CinematicPortal.spawn(level,dog.position().add(0,3.25,0),
                    net.minecraft.core.Direction.DOWN,com.jhonfx.portalgun.entity.PortalColor.GREEN,1.05f);
            if(id!=null){tag.putUUID("DogPortal",id);save(player,tag);portal=(com.jhonfx.portalgun.entity.PortalEntity)level.getEntity(id);}
        }
        // The real portal follows the dog until Prime releases the bomb.
        if(portal!=null)portal.moveTo(dog.getX(),dog.getY()+3.25,dog.getZ(),portal.getYRot(),portal.getXRot());
        if(tick%20==0)level.playSound(null,dog.blockPosition(),SoundEvents.PORTAL_AMBIENT,SoundSource.HOSTILE,1.2f,.8f);
    }

    private static void explodeBomb(ServerLevel level,Entity dog,Entity bomb){
        double x=dog.getX(),y=dog.getY()+.6,z=dog.getZ();
        for(int ring=1;ring<=5;ring++)for(int i=0;i<48;i++){double a=i*Math.PI*2/48d;double r=ring*.75;level.sendParticles(i%5==0?net.minecraft.core.particles.ParticleTypes.FLASH:PURPLE,x+Math.cos(a)*r,y+(ring-2)*.18,z+Math.sin(a)*r,1,.03,.05,.03,.08);}
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,x,y,z,120,2,1.5,2,.12);
        level.playSound(null,dog.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundSource.HOSTILE,2.4f,.45f);
        level.explode(null,x,y,z,2.2f,net.minecraft.world.level.Level.ExplosionInteraction.NONE);dog.kill();if(bomb!=null)bomb.discard();
    }

    private static void spawnProjection(ServerPlayer player,CompoundTag tag) {
        if(tag.getBoolean("Projection")||tag.getBoolean("ProjectionPending"))return;
        double a=player.getRandom().nextDouble()*Math.PI*2;
        double px=player.getX()+Math.cos(a)*5.8,pz=player.getZ()+Math.sin(a)*5.8,py=player.getY()+1.05;
        net.minecraft.core.Direction facing=net.minecraft.core.Direction.getNearest(
                (float)(player.getX()-px),0,(float)(player.getZ()-pz));
        UUID portal=com.jhonfx.portalgun.entity.CinematicPortal.spawn(player.serverLevel(),new net.minecraft.world.phys.Vec3(px,py,pz),
                facing,com.jhonfx.portalgun.entity.PortalColor.GREEN,.92f);
        if(portal==null)return;
        tag.putUUID("ProjectionPortal",portal);tag.putBoolean("ProjectionPending",true);
        tag.putLong("ProjectionPortalAt",player.level().getGameTime());
        tag.putDouble("ProjectionX",px+facing.getStepX()*.9);tag.putDouble("ProjectionY",player.getY());tag.putDouble("ProjectionZ",pz+facing.getStepZ()*.9);
        tag.putFloat("ProjectionYaw",facing.toYRot());save(player,tag);
        player.serverLevel().playSound(null,new net.minecraft.core.BlockPos((int)Math.floor(px),(int)Math.floor(py),(int)Math.floor(pz)),
                com.jhonfx.portalgun.init.ModSounds.PORTAL_SPAWN.get(),SoundSource.PLAYERS,1.35f,.92f);
    }
    private static void tickProjectionArrival(ServerPlayer player,CompoundTag tag,long now){
        long elapsed=now-tag.getLong("ProjectionPortalAt");
        if(elapsed>=20&&!tag.getBoolean("Projection")){
            CitadelNpcEntity npc=ModEntityTypes.CITADEL_CITIZEN.get().create(player.serverLevel());
            if(npc!=null){npc.moveTo(tag.getDouble("ProjectionX"),tag.getDouble("ProjectionY"),tag.getDouble("ProjectionZ"),tag.getFloat("ProjectionYaw"),0);npc.setVariant(CitadelNpcEntity.Variant.PRIME);npc.setPersistenceRequired();player.serverLevel().addFreshEntity(npc);tag.putBoolean("Projection",true);save(player,tag);}
        }
        if(elapsed>=55){if(tag.hasUUID("ProjectionPortal"))com.jhonfx.portalgun.entity.CinematicPortal.close(player.serverLevel(),tag.getUUID("ProjectionPortal"));tag.remove("ProjectionPortal");tag.remove("ProjectionPending");save(player,tag);}
    }
    private static Entity findDog(ServerPlayer player,CompoundTag tag) {
        if(!tag.hasUUID("Dog")) return null; UUID id=tag.getUUID("Dog");
        ServerLevel level=player.serverLevel();
        if(tag.contains("DogDimension")){
            net.minecraft.resources.ResourceLocation dimension=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("DogDimension"));
            if(dimension!=null){
                ServerLevel saved=player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,dimension));
                if(saved!=null)level=saved;
            }
        }
        if(tag.contains("DogPos"))level.getChunkAt(net.minecraft.core.BlockPos.of(tag.getLong("DogPos")));
        return level.getEntity(id);
    }
    private static void rememberDog(CompoundTag tag,Entity dog){
        tag.putUUID("Dog",dog.getUUID());
        tag.putString("DogDimension",dog.level().dimension().location().toString());
        tag.putLong("DogPos",dog.blockPosition().asLong());
    }
    private static CompoundTag data(Player p){ return p.getPersistentData().getCompound(ROOT); }
    private static void save(Player p,CompoundTag tag){ p.getPersistentData().put(ROOT,tag); }
    private static void give(Player p,ItemStack s){ if(!p.getInventory().add(s))p.drop(s,false); }
    private static void consume(Player p,Item item){ if(p.getAbilities().instabuild)return; for(ItemStack s:p.getInventory().items)if(s.is(item)){s.shrink(1);return;} }
    private static boolean has(Player p,Item item){for(ItemStack s:p.getInventory().items)if(s.is(item))return true;return p.getOffhandItem().is(item);}
}
