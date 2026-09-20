package com.jhonfx.portalgun.init;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.entity.PortalEntity;
import com.jhonfx.portalgun.entity.RickPrimeEntity;
import com.jhonfx.portalgun.entity.DianeRobotEntity;
import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.entity.CurveNodeEntity;
import com.jhonfx.portalgun.entity.EvilMortyEntity;
import com.jhonfx.portalgun.entity.FederationAlienEntity;
import com.jhonfx.portalgun.entity.FederationDroneEntity;
import com.jhonfx.portalgun.entity.FederationHeavyTrooperEntity;
import com.jhonfx.portalgun.entity.MeeseeksEntity;
import com.jhonfx.portalgun.entity.RickShipEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PortalGunMod.MOD_ID);

    public static final RegistryObject<EntityType<PortalEntity>> PORTAL = ENTITY_TYPES.register(
            "portal",
            () -> EntityType.Builder.<PortalEntity>of(PortalEntity::new, MobCategory.MISC)
                    .sized(4.8f, 4.8f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("portal")
    );

    public static final RegistryObject<EntityType<RickPrimeEntity>> RICK_PRIME = ENTITY_TYPES.register(
            "rick_prime", () -> EntityType.Builder.of(RickPrimeEntity::new, MobCategory.MONSTER)
                    .sized(0.65f, 1.95f).clientTrackingRange(12).updateInterval(1).fireImmune()
                    .build("rick_prime"));

    public static final RegistryObject<EntityType<DianeRobotEntity>> DIANE_ROBOT = ENTITY_TYPES.register(
            "diane_robot", () -> EntityType.Builder.of(DianeRobotEntity::new, MobCategory.MONSTER)
                    .sized(3.1f, 4.1f).clientTrackingRange(14).updateInterval(1).fireImmune()
                    .build("diane_robot"));

    public static final RegistryObject<EntityType<CitadelNpcEntity>> CITADEL_CITIZEN = ENTITY_TYPES.register(
            "citadel_citizen", () -> EntityType.Builder.of(CitadelNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.62f, 1.9f).clientTrackingRange(10).updateInterval(2)
                    .build("citadel_citizen"));

    public static final RegistryObject<EntityType<EvilMortyEntity>> EVIL_MORTY = ENTITY_TYPES.register(
            "evil_morty", () -> EntityType.Builder.of(EvilMortyEntity::new, MobCategory.MONSTER)
                    .sized(0.62f, 1.72f).clientTrackingRange(14).updateInterval(1).fireImmune()
                    .build("evil_morty"));

    /** Non-hostile story NPC; intentionally shares Evil Morty's model and texture. */
    public static final RegistryObject<EntityType<EvilMortyEntity>> EVIL_MORTY_ALLY = ENTITY_TYPES.register(
            "evil_morty_ally", () -> EntityType.Builder.of(EvilMortyEntity::new, MobCategory.CREATURE)
                    .sized(0.62f, 1.72f).clientTrackingRange(14).updateInterval(1).fireImmune()
                    .build("evil_morty_ally"));

    public static final RegistryObject<EntityType<FederationAlienEntity>> FEDERATION_ALIEN = ENTITY_TYPES.register(
            "federation_alien",()->EntityType.Builder.of(FederationAlienEntity::new,MobCategory.MONSTER)
                    .sized(.64f,1.95f).clientTrackingRange(12).updateInterval(2)
                    .build("federation_alien"));

    public static final RegistryObject<EntityType<FederationDroneEntity>> FEDERATION_DRONE = ENTITY_TYPES.register(
            "federation_drone", () -> EntityType.Builder.of(FederationDroneEntity::new, MobCategory.MONSTER)
                    .sized(.8f, .8f).clientTrackingRange(14).updateInterval(2)
                    .build("federation_drone"));

    public static final RegistryObject<EntityType<FederationHeavyTrooperEntity>> FEDERATION_HEAVY = ENTITY_TYPES.register(
            "federation_heavy_trooper", () -> EntityType.Builder.of(FederationHeavyTrooperEntity::new, MobCategory.MONSTER)
                    .sized(1.1f, 2.6f).clientTrackingRange(14).updateInterval(2)
                    .build("federation_heavy_trooper"));

    public static final RegistryObject<EntityType<MeeseeksEntity>> MEESEEKS = ENTITY_TYPES.register(
            "meeseeks",()->EntityType.Builder.of(MeeseeksEntity::new,MobCategory.CREATURE)
                    .sized(.62f,1.88f).clientTrackingRange(12).updateInterval(2)
                    .build("meeseeks"));

    /** Rounded technological sphere used to build the visible Finite Curve lattice. */
    public static final RegistryObject<EntityType<CurveNodeEntity>> CURVE_NODE = ENTITY_TYPES.register(
            "curve_node", () -> EntityType.Builder.<CurveNodeEntity>of(CurveNodeEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(4)
                    .build("curve_node"));

    public static final RegistryObject<EntityType<RickShipEntity>> RICK_SHIP = ENTITY_TYPES.register(
            "rick_ship", () -> EntityType.Builder.<RickShipEntity>of(RickShipEntity::new, MobCategory.MISC)
                    .sized(3.8f, 1.65f).fireImmune().clientTrackingRange(16).updateInterval(1)
                    .build("rick_ship"));

    private ModEntityTypes() {
    }

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
