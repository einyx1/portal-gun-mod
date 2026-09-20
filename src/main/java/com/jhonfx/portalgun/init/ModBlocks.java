package com.jhonfx.portalgun.init;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.block.PortalSolutionBlock;
import com.jhonfx.portalgun.block.OmegaDeviceBlock;
import com.jhonfx.portalgun.block.OmegaComponent;
import com.jhonfx.portalgun.block.OmegaComponentBlock;
import com.jhonfx.portalgun.block.CitadelServiceBlock;
import com.jhonfx.portalgun.entity.PortalColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PortalGunMod.MOD_ID);
    public static final RegistryObject<Block> INTERSPATIAL_SOLUTION = solution("interspatial_solution", PortalColor.BLUE);
    public static final RegistryObject<Block> INTERDIMENSIONAL_SOLUTION = solution("interdimensional_solution", PortalColor.GREEN);
    public static final RegistryObject<Block> EXTRADIMENSIONAL_SOLUTION = solution("extradimensional_solution", PortalColor.YELLOW);
    public static final RegistryObject<Block> OMEGA_DEVICE = BLOCKS.register("omega_device", () ->
            new OmegaDeviceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(12.0f, 1200.0f).lightLevel(state -> state.getValue(OmegaDeviceBlock.ACTIVE) ? 12 : 5).noOcclusion()));
    public static final RegistryObject<Block> OMEGA_CORE = omegaComponent("omega_core", OmegaComponent.CORE, 14);
    public static final RegistryObject<Block> OMEGA_EMITTER = omegaComponent("omega_emitter", OmegaComponent.EMITTER, 10);
    public static final RegistryObject<Block> OMEGA_RING = omegaComponent("omega_ring", OmegaComponent.RING, 9);
    public static final RegistryObject<Block> OMEGA_LAB_PANEL = BLOCKS.register("omega_lab_panel", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(10, 1200)));
    public static final RegistryObject<Block> PRIME_LAIR_PANEL = BLOCKS.register("prime_lair_panel", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(14, 1200)
                    .lightLevel(state -> 4)));
    public static final RegistryObject<Block> OMEGA_ENERGY_GLASS = BLOCKS.register("omega_energy_glass", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(8, 1200)
                    .lightLevel(state -> 13).noOcclusion()));
    public static final RegistryObject<Block> OMEGA_ALARM_PANEL = BLOCKS.register("omega_alarm_panel", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(10, 1200)
                    .lightLevel(state -> 10)));
    public static final RegistryObject<Block> CITADEL_PANEL = BLOCKS.register("citadel_panel", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(6, 30)));
    public static final RegistryObject<Block> CITADEL_ALLOY = BLOCKS.register("citadel_alloy", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(8, 50)));
    public static final RegistryObject<Block> CITADEL_GOLD = BLOCKS.register("citadel_gold", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(7, 40)));
    public static final RegistryObject<Block> CITADEL_CIRCUIT = BLOCKS.register("citadel_circuit", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(7, 40).lightLevel(s -> 13)));
    public static final RegistryObject<Block> CITADEL_GLASS = BLOCKS.register("citadel_glass", () ->
            new GlassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(3, 20)
                    .lightLevel(s -> 5).noOcclusion()));
    public static final RegistryObject<Block> FEDERATION_HULL = BLOCKS.register("federation_hull", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(9, 80)));
    public static final RegistryObject<Block> FEDERATION_FLOOR = BLOCKS.register("federation_floor", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(8, 70)));
    public static final RegistryObject<Block> FEDERATION_ENERGY = BLOCKS.register("federation_energy", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(8, 80).lightLevel(s -> 13)));
    public static final RegistryObject<Block> FEDERATION_GLASS = BLOCKS.register("federation_glass", () ->
            new GlassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(5, 45)
                    .lightLevel(s -> 5).noOcclusion()));
    public static final RegistryObject<Block> FEDERATION_TERMINAL = BLOCKS.register("federation_terminal", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(9, 90).lightLevel(s -> 8)));
    public static final RegistryObject<Block> CITADEL_SHOP = BLOCKS.register("citadel_shop", () ->
            new CitadelServiceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(8, 50).lightLevel(s -> 8),
                    CitadelServiceBlock.Service.SHOP));
    public static final RegistryObject<Block> CITADEL_WORKSHOP = BLOCKS.register("citadel_workshop", () ->
            new CitadelServiceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(8, 50).lightLevel(s -> 11),
                    CitadelServiceBlock.Service.WORKSHOP));
    public static final RegistryObject<Block> CITADEL_MISSION_TERMINAL = BLOCKS.register("citadel_mission_terminal", () ->
            new CitadelServiceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(8,50).lightLevel(s -> 10),
                    CitadelServiceBlock.Service.MISSION));
    public static final RegistryObject<Block> NEURAL_SCANNER = BLOCKS.register("neural_scanner", () ->
            new CitadelServiceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(10,60).lightLevel(s -> 12),
                    CitadelServiceBlock.Service.SCANNER));
    public static final RegistryObject<Block> DIMENSIONAL_DRIVE = BLOCKS.register("dimensional_drive", () ->
            new CitadelServiceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(12,100).lightLevel(s->10),CitadelServiceBlock.Service.DRIVE));
    public static final RegistryObject<Block> CURVE_CORE = BLOCKS.register("curve_core", () ->
            new CitadelServiceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(18,1200).lightLevel(s->14),CitadelServiceBlock.Service.CURVE));
    public static final RegistryObject<Block> COUNCIL_ARCHIVE = BLOCKS.register("council_archive", () ->
            new CitadelServiceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(10,80).lightLevel(s->7),CitadelServiceBlock.Service.ARCHIVE));

    private static RegistryObject<Block> omegaComponent(String id, OmegaComponent component, int light) {
        return BLOCKS.register(id, () -> new OmegaComponentBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK).strength(14.0f, 1200.0f)
                .lightLevel(state -> state.getValue(OmegaDeviceBlock.ACTIVE) ? light : 3).noOcclusion(), component));
    }

    private static RegistryObject<Block> solution(String id, PortalColor color) {
        return BLOCKS.register(id, () -> new PortalSolutionBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.35f).lightLevel(state -> 7).noOcclusion(), color));
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
    private ModBlocks() {}
}
