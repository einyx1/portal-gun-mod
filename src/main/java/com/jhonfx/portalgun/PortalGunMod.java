package com.jhonfx.portalgun;

import com.jhonfx.portalgun.event.PortalTickHandler;
import com.jhonfx.portalgun.omega.OmegaDeviceHandler;
import com.jhonfx.portalgun.init.ModCreativeTabs;
import com.jhonfx.portalgun.init.ModEntityTypes;
import com.jhonfx.portalgun.init.ModItems;
import com.jhonfx.portalgun.init.ModBlocks;
import com.jhonfx.portalgun.init.ModBlockEntityTypes;
import com.jhonfx.portalgun.init.ModSounds;
import com.jhonfx.portalgun.init.ModRecipes;
import com.jhonfx.portalgun.network.ModNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import com.jhonfx.portalgun.entity.RickPrimeEntity;
import com.jhonfx.portalgun.entity.DianeRobotEntity;
import com.jhonfx.portalgun.entity.CitadelNpcEntity;
import com.jhonfx.portalgun.entity.EvilMortyEntity;
import com.jhonfx.portalgun.entity.FederationAlienEntity;
import com.jhonfx.portalgun.entity.MeeseeksEntity;
import com.jhonfx.portalgun.citadel.CitadelMissions;
import com.jhonfx.portalgun.prologue.PrologueHandler;
import com.jhonfx.portalgun.curve.CurveHandler;
import com.jhonfx.portalgun.event.CombatTechHandler;
import com.jhonfx.portalgun.command.PortalCommands;
import com.jhonfx.portalgun.federation.FederationThreatHandler;
import com.jhonfx.portalgun.federation.FederationPrisonEvent;

/**
 * Port em Java (Forge) do addon Bedrock "Portal Gun" de JhonFX.
 * Reimplementa armas, modelos, menu, carga, modos de pareamento, receitas,
 * portais animados e teleporte a partir dos assets e scripts originais.
 */
@Mod(PortalGunMod.MOD_ID)
public class PortalGunMod {

    public static final String MOD_ID = "portalgun";

    public PortalGunMod() {
        GeckoLib.initialize();
        ModNetwork.register();
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ── Server config (Meeseeks limits, griefing, guard cap, etc.) ────────
        ModLoadingContext.get().registerConfig(Type.SERVER, ModConfig.SERVER_SPEC);

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(ModRecipes::setup);
        modEventBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(ModEntityTypes.RICK_PRIME.get(), RickPrimeEntity.createAttributes().build());
            event.put(ModEntityTypes.DIANE_ROBOT.get(), DianeRobotEntity.createAttributes().build());
            event.put(ModEntityTypes.CITADEL_CITIZEN.get(), CitadelNpcEntity.createAttributes().build());
            event.put(ModEntityTypes.EVIL_MORTY.get(), EvilMortyEntity.createAttributes().build());
            event.put(ModEntityTypes.EVIL_MORTY_ALLY.get(), EvilMortyEntity.createAttributes().build());
            event.put(ModEntityTypes.FEDERATION_ALIEN.get(), FederationAlienEntity.createAttributes().build());
            event.put(ModEntityTypes.FEDERATION_DRONE.get(), com.jhonfx.portalgun.entity.FederationDroneEntity.createAttributes().build());
            event.put(ModEntityTypes.FEDERATION_HEAVY.get(), com.jhonfx.portalgun.entity.FederationHeavyTrooperEntity.createAttributes().build());
            event.put(ModEntityTypes.MEESEEKS.get(), MeeseeksEntity.createAttributes().build());
            // CURVE_NODE is a MISC-category decorative entity — no attributes needed.
        });

        MinecraftForge.EVENT_BUS.register(new PortalTickHandler());
        MinecraftForge.EVENT_BUS.register(new OmegaDeviceHandler());
        MinecraftForge.EVENT_BUS.register(new CitadelMissions());
        MinecraftForge.EVENT_BUS.register(new PrologueHandler());
        MinecraftForge.EVENT_BUS.register(new CurveHandler());
        MinecraftForge.EVENT_BUS.register(new CombatTechHandler());
        MinecraftForge.EVENT_BUS.register(new PortalCommands());
        MinecraftForge.EVENT_BUS.register(new FederationThreatHandler());
        MinecraftForge.EVENT_BUS.register(new FederationPrisonEvent());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.event.DimensionEntryHandler());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.federation.FederationSignalArc());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.federation.UnmortrickenFacility());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.federation.FederationPostgameHandler());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.federation.FederationVisualUpgrade());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.event.CinematicSessionGuard());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.citadel.CitadelRuinsCleanup());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.citadel.CitadelV4Layout());
        MinecraftForge.EVENT_BUS.register(new com.jhonfx.portalgun.compat.SpaceIntegrationHandler());
    }
}
