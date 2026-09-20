package com.jhonfx.portalgun.init;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.item.PortalGunItem;
import com.jhonfx.portalgun.item.PortalGunVariant;
import com.jhonfx.portalgun.item.PortalFluidTubeItem;
import com.jhonfx.portalgun.item.CitadelRemoteItem;
import com.jhonfx.portalgun.item.NeuralScannerItem;
import com.jhonfx.portalgun.item.PrimeCommunicatorItem;
import com.jhonfx.portalgun.item.CombatTechItem;
import com.jhonfx.portalgun.item.FederationThreatScannerItem;
import com.jhonfx.portalgun.item.MeeseeksBoxItem;
import com.jhonfx.portalgun.item.RickWeaponItem;
import com.jhonfx.portalgun.entity.PortalColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.ForgeSpawnEggItem;
import com.jhonfx.portalgun.item.BlemflarcksItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PortalGunMod.MOD_ID);

    public static final RegistryObject<Item> CHIP = simple("chip");
    public static final RegistryObject<Item> DARK_MATTER_DUST = simple("dark_matter_dust");
    public static final RegistryObject<Item> PHASE_MATTER = simple("phase_matter");
    public static final RegistryObject<Item> EMPTY_TUBE = simple("empty_tube");
    public static final RegistryObject<Item> BEAKER = simple("beaker");
    public static final RegistryObject<Item> QUANTUM_ISOTOPE = simple("quantum_isotope");
    public static final RegistryObject<Item> GREEN_PORTAL_FLUID = tube("green_portal_fluid", PortalColor.GREEN);
    public static final RegistryObject<Item> BLUE_PORTAL_FLUID = tube("blue_portal_fluid", PortalColor.BLUE);
    public static final RegistryObject<Item> YELLOW_PORTAL_FLUID = tube("yellow_portal_fluid", PortalColor.YELLOW);
    public static final RegistryObject<Item> INTERSPATIAL_SOLUTION = blockItem("interspatial_solution", ModBlocks.INTERSPATIAL_SOLUTION);
    public static final RegistryObject<Item> INTERDIMENSIONAL_SOLUTION = blockItem("interdimensional_solution", ModBlocks.INTERDIMENSIONAL_SOLUTION);
    public static final RegistryObject<Item> EXTRADIMENSIONAL_SOLUTION = blockItem("extradimensional_solution", ModBlocks.EXTRADIMENSIONAL_SOLUTION);
    public static final RegistryObject<Item> OMEGA_DEVICE = blockItem("omega_device", ModBlocks.OMEGA_DEVICE);
    public static final RegistryObject<Item> OMEGA_CORE = blockItem("omega_core", ModBlocks.OMEGA_CORE);
    public static final RegistryObject<Item> OMEGA_EMITTER = blockItem("omega_emitter", ModBlocks.OMEGA_EMITTER);
    public static final RegistryObject<Item> OMEGA_RING = blockItem("omega_ring", ModBlocks.OMEGA_RING);
    public static final RegistryObject<Item> OMEGA_LAB_PANEL = blockItem("omega_lab_panel", ModBlocks.OMEGA_LAB_PANEL);
    public static final RegistryObject<Item> PRIME_LAIR_PANEL = blockItem("prime_lair_panel", ModBlocks.PRIME_LAIR_PANEL);
    public static final RegistryObject<Item> OMEGA_ENERGY_GLASS = blockItem("omega_energy_glass", ModBlocks.OMEGA_ENERGY_GLASS);
    public static final RegistryObject<Item> OMEGA_ALARM_PANEL = blockItem("omega_alarm_panel", ModBlocks.OMEGA_ALARM_PANEL);
    public static final RegistryObject<Item> CITADEL_PANEL = blockItem("citadel_panel", ModBlocks.CITADEL_PANEL);
    public static final RegistryObject<Item> CITADEL_ALLOY = blockItem("citadel_alloy", ModBlocks.CITADEL_ALLOY);
    public static final RegistryObject<Item> CITADEL_GOLD = blockItem("citadel_gold", ModBlocks.CITADEL_GOLD);
    public static final RegistryObject<Item> CITADEL_CIRCUIT = blockItem("citadel_circuit", ModBlocks.CITADEL_CIRCUIT);
    public static final RegistryObject<Item> CITADEL_GLASS = blockItem("citadel_glass", ModBlocks.CITADEL_GLASS);
    public static final RegistryObject<Item> FEDERATION_HULL = blockItem("federation_hull", ModBlocks.FEDERATION_HULL);
    public static final RegistryObject<Item> FEDERATION_FLOOR = blockItem("federation_floor", ModBlocks.FEDERATION_FLOOR);
    public static final RegistryObject<Item> FEDERATION_ENERGY = blockItem("federation_energy", ModBlocks.FEDERATION_ENERGY);
    public static final RegistryObject<Item> FEDERATION_GLASS = blockItem("federation_glass", ModBlocks.FEDERATION_GLASS);
    public static final RegistryObject<Item> FEDERATION_TERMINAL = blockItem("federation_terminal", ModBlocks.FEDERATION_TERMINAL);
    public static final RegistryObject<Item> CITADEL_SHOP = blockItem("citadel_shop", ModBlocks.CITADEL_SHOP);
    public static final RegistryObject<Item> CITADEL_WORKSHOP = blockItem("citadel_workshop", ModBlocks.CITADEL_WORKSHOP);
    public static final RegistryObject<Item> CITADEL_MISSION_TERMINAL = blockItem("citadel_mission_terminal", ModBlocks.CITADEL_MISSION_TERMINAL);
    public static final RegistryObject<Item> NEURAL_SCANNER_BLOCK = blockItem("neural_scanner", ModBlocks.NEURAL_SCANNER);
    public static final RegistryObject<Item> DIMENSIONAL_DRIVE = blockItem("dimensional_drive",ModBlocks.DIMENSIONAL_DRIVE);
    public static final RegistryObject<Item> CURVE_CORE = blockItem("curve_core",ModBlocks.CURVE_CORE);
    public static final RegistryObject<Item> COUNCIL_ARCHIVE = blockItem("council_archive",ModBlocks.COUNCIL_ARCHIVE);
    public static final RegistryObject<Item> CITADEL_CREDIT = simple("citadel_credit");
    public static final RegistryObject<Item> BLEMFLARCKS =
            ITEMS.register("blemflarcks", () -> new BlemflarcksItem(new Item.Properties().stacksTo(999)));
    // ── Arco "Assinatura P-0" ────────────────────────────────────────────
    public static final RegistryObject<Item> P0_SIGNATURE_FRAGMENT = simpleStackOne("p0_signature_fragment");
    public static final RegistryObject<Item> PRIME_TELEPORT_RESIDUE = simpleStackOne("prime_teleport_residue");
    public static final RegistryObject<Item> MULTIVERSAL_SCANNER_UPGRADE_CHIP = ITEMS.register("multiversal_scanner_upgrade_chip",
            () -> new com.jhonfx.portalgun.item.MultiversalScannerUpgradeItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NEURAL_SCANNER = ITEMS.register("neural_scanner_item", () -> new NeuralScannerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> C524_NEURAL_FRAGMENT = simpleStackOne("c524_neural_fragment");
    public static final RegistryObject<Item> IMPOSSIBLE_COMPONENT = simpleStackOne("impossible_component");
    public static final RegistryObject<Item> PRIME_COMMUNICATOR = ITEMS.register("prime_communicator", () -> new PrimeCommunicatorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PRIME_BOMB = simpleStackOne("prime_bomb");
    public static final RegistryObject<Item> RICK_BRAIN = ITEMS.register("rick_brain", () -> new com.jhonfx.portalgun.item.RickBrainItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CURVE_COORDINATES = ITEMS.register("curve_coordinates",()->new com.jhonfx.portalgun.item.CurveCoordinatesItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> INTERDIMENSIONAL_CALIBRATOR = simpleStackOne("interdimensional_calibrator");
    public static final RegistryObject<Item> CURVE_STABILIZER = simpleStackOne("curve_stabilizer");
    public static final RegistryObject<Item> PRIME_SINGULARITY_CORE = simpleStackOne("prime_singularity_core");
    public static final RegistryObject<Item> CYBERNETIC_IMPLANT = tech("cybernetic_implant",CombatTechItem.Tech.IMPLANT,true);
    public static final RegistryObject<Item> ARM_BLASTER = tech("arm_blaster",CombatTechItem.Tech.BLASTER,false);
    public static final RegistryObject<Item> FORCE_FIELD_EMITTER = tech("force_field_emitter",CombatTechItem.Tech.SHIELD,false);
    public static final RegistryObject<Item> COMBAT_JETPACK = tech("combat_jetpack",CombatTechItem.Tech.JETPACK,false);
    public static final RegistryObject<Item> CONTAINMENT_NET = tech("containment_net",CombatTechItem.Tech.NET,false);
    public static final RegistryObject<Item> PHOENIX_MODULE = tech("phoenix_module",CombatTechItem.Tech.PHOENIX,true);
    public static final RegistryObject<Item> REGENERATION_INJECTOR = tech("regeneration_injector",CombatTechItem.Tech.REGENERATOR,true);
    public static final RegistryObject<Item> FEDERATION_THREAT_SCANNER = ITEMS.register("federation_threat_scanner",()->new FederationThreatScannerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FEDERATION_ALIEN_SPAWN_EGG = ITEMS.register("federation_alien_spawn_egg",()->new ForgeSpawnEggItem(ModEntityTypes.FEDERATION_ALIEN,0x4f6427,0xe5c82b,new Item.Properties()));
    public static final RegistryObject<Item> MEESEEKS_BOX = ITEMS.register("meeseeks_box",()->new MeeseeksBoxItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MEESEEKS_SPAWN_EGG = ITEMS.register("meeseeks_spawn_egg",()->new ForgeSpawnEggItem(ModEntityTypes.MEESEEKS,0x28dbea,0xf11a25,new Item.Properties()));
    public static final RegistryObject<Item> CITADEL_REMOTE = ITEMS.register("citadel_remote", () ->
            new CitadelRemoteItem(new Item.Properties().stacksTo(1)));

    // ── Combat Update arsenal ───────────────────────────────────────────────
    public static final RegistryObject<Item> EVIL_MORTY_EYEPATCH = ITEMS.register("evil_morty_eyepatch",()->
            new com.jhonfx.portalgun.item.EvilMortyEyepatchItem(net.minecraft.world.item.ArmorMaterials.NETHERITE,new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LASER_GUN = weapon("laser_gun",RickWeaponItem.Weapon.LASER,640);
    public static final RegistryObject<Item> PLASMA_PISTOL = weapon("plasma_pistol",RickWeaponItem.Weapon.PLASMA_9_GAUGE,420);
    public static final RegistryObject<Item> FREEZE_RAY = weapon("freeze_ray",RickWeaponItem.Weapon.FREEZE_RAY,480);
    public static final RegistryObject<Item> MINDBLOWER_GUN = weapon("mindblower_gun",RickWeaponItem.Weapon.MINDBLOWER,320);
    public static final RegistryObject<Item> NEUTRINO_BOMB = ITEMS.register("neutrino_bomb",()->new RickWeaponItem(new Item.Properties().stacksTo(1),RickWeaponItem.Weapon.NEUTRINO_BOMB));
    public static final RegistryObject<Item> WEAPON_ENERGY_CELL = ITEMS.register("weapon_energy_cell",()->new com.jhonfx.portalgun.item.WeaponEnergyCellItem(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> RICK_LIGHTSABER = ITEMS.register("rick_lightsaber",()->new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.NETHERITE,7,-2.1f,new Item.Properties().durability(980)));
    public static final RegistryObject<Item> PURGE_HELMET = purge("purge_helmet",net.minecraft.world.item.ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> PURGE_CHESTPLATE = purge("purge_chestplate",net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> PURGE_LEGGINGS = purge("purge_leggings",net.minecraft.world.item.ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> PURGE_BOOTS = purge("purge_boots",net.minecraft.world.item.ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> PORTAL_CANNON = ITEMS.register("portal_cannon",()->new com.jhonfx.portalgun.item.PortalCannonItem(new Item.Properties().stacksTo(1).durability(128)));
    public static final RegistryObject<Item> RICK_SHIP = ITEMS.register("rick_ship",()->new com.jhonfx.portalgun.item.RickShipItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> STANDARD_PORTAL_GUN_BASE = simpleStackOne("standard_portal_gun_base");
    public static final RegistryObject<Item> PROTOTYPE_PORTAL_GUN_BASE = simpleStackOne("prototype_portal_gun_base");
    public static final RegistryObject<Item> EVIL_MORTY_PORTAL_GUN_BASE = simpleStackOne("evil_morty_portal_gun_base");
    public static final RegistryObject<Item> PRIME_PORTAL_GUN_BASE = simpleStackOne("prime_portal_gun_base");

    public static final RegistryObject<Item> STANDARD_PORTAL_GUN = gun("standard_portal_gun", PortalGunVariant.STANDARD);
    public static final RegistryObject<Item> PROTOTYPE_PORTAL_GUN = gun("prototype_portal_gun", PortalGunVariant.PROTOTYPE);
    public static final RegistryObject<Item> BLUE_PROTOTYPE_PORTAL_GUN = gun("blue_prototype_portal_gun", PortalGunVariant.BLUE_PROTOTYPE);
    public static final RegistryObject<Item> EVIL_MORTY_PORTAL_GUN = gun("evil_morty_portal_gun", PortalGunVariant.EVIL_MORTY);
    public static final RegistryObject<Item> PRIME_PORTAL_GUN = gun("prime_portal_gun", PortalGunVariant.PRIME);

    public static final RegistryObject<Item> PORTAL_GUN = STANDARD_PORTAL_GUN;

    private static RegistryObject<Item> simple(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> simpleStackOne(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Item> tube(String id, PortalColor color) {
        return ITEMS.register(id, () -> new PortalFluidTubeItem(new Item.Properties().stacksTo(1), color));
    }

    private static RegistryObject<Item> blockItem(String id, RegistryObject<net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> gun(String id, PortalGunVariant variant) {
        return ITEMS.register(id, () -> new PortalGunItem(new Item.Properties().stacksTo(1), variant));
    }
    private static RegistryObject<Item> tech(String id,CombatTechItem.Tech tech,boolean singleUse){
        return ITEMS.register(id,()->{
            Item.Properties properties=new Item.Properties().stacksTo(1);
            if(!singleUse)properties.durability(384);
            return new CombatTechItem(properties,tech);
        });
    }
    private static RegistryObject<Item> weapon(String id,RickWeaponItem.Weapon weapon,int durability){
        return ITEMS.register(id,()->new RickWeaponItem(new Item.Properties().stacksTo(1).durability(durability),weapon));
    }
    private static RegistryObject<Item> purge(String id,net.minecraft.world.item.ArmorItem.Type type){
        return ITEMS.register(id,()->new com.jhonfx.portalgun.item.PurgeSuitItem(net.minecraft.world.item.ArmorMaterials.NETHERITE,type,new Item.Properties().stacksTo(1)));
    }

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
