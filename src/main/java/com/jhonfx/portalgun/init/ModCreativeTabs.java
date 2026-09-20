package com.jhonfx.portalgun.init;

import com.jhonfx.portalgun.PortalGunMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PortalGunMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> PORTAL_GUN_TAB = TABS.register(
            "portal_gun_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.portalgun"))
                    .icon(() -> new ItemStack(ModItems.PORTAL_GUN.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.STANDARD_PORTAL_GUN.get());
                        output.accept(ModItems.PROTOTYPE_PORTAL_GUN.get());
                        output.accept(ModItems.BLUE_PROTOTYPE_PORTAL_GUN.get());
                        output.accept(ModItems.EVIL_MORTY_PORTAL_GUN.get());
                        output.accept(ModItems.PRIME_PORTAL_GUN.get());
                        output.accept(ModItems.STANDARD_PORTAL_GUN_BASE.get());
                        output.accept(ModItems.PROTOTYPE_PORTAL_GUN_BASE.get());
                        output.accept(ModItems.EVIL_MORTY_PORTAL_GUN_BASE.get());
                        output.accept(ModItems.PRIME_PORTAL_GUN_BASE.get());
                        output.accept(ModItems.CHIP.get());
                        output.accept(ModItems.DARK_MATTER_DUST.get());
                        output.accept(ModItems.PHASE_MATTER.get());
                        output.accept(ModItems.EMPTY_TUBE.get());
                        output.accept(ModItems.BEAKER.get());
                        output.accept(ModItems.QUANTUM_ISOTOPE.get());
                        output.accept(ModItems.GREEN_PORTAL_FLUID.get());
                        output.accept(ModItems.BLUE_PORTAL_FLUID.get());
                        output.accept(ModItems.YELLOW_PORTAL_FLUID.get());
                        output.accept(ModItems.INTERSPATIAL_SOLUTION.get());
                        output.accept(ModItems.INTERDIMENSIONAL_SOLUTION.get());
                        output.accept(ModItems.EXTRADIMENSIONAL_SOLUTION.get());
                        output.accept(ModItems.OMEGA_DEVICE.get());
                        output.accept(ModItems.OMEGA_CORE.get());
                        output.accept(ModItems.OMEGA_EMITTER.get());
                        output.accept(ModItems.OMEGA_RING.get());
                        output.accept(ModItems.OMEGA_LAB_PANEL.get());
                        output.accept(ModItems.PRIME_LAIR_PANEL.get());
                        output.accept(ModItems.OMEGA_ENERGY_GLASS.get());
                        output.accept(ModItems.OMEGA_ALARM_PANEL.get());
                        output.accept(ModItems.CITADEL_REMOTE.get());
                        output.accept(ModItems.CITADEL_CREDIT.get());
                        output.accept(ModItems.CITADEL_PANEL.get());
                        output.accept(ModItems.CITADEL_ALLOY.get());
                        output.accept(ModItems.CITADEL_GOLD.get());
                        output.accept(ModItems.CITADEL_CIRCUIT.get());
                        output.accept(ModItems.CITADEL_GLASS.get());
                        output.accept(ModItems.FEDERATION_HULL.get());
                        output.accept(ModItems.FEDERATION_FLOOR.get());
                        output.accept(ModItems.FEDERATION_ENERGY.get());
                        output.accept(ModItems.FEDERATION_GLASS.get());
                        output.accept(ModItems.FEDERATION_TERMINAL.get());
                        output.accept(ModItems.CITADEL_SHOP.get());
                        output.accept(ModItems.CITADEL_WORKSHOP.get());
                        output.accept(ModItems.CITADEL_MISSION_TERMINAL.get());
                        output.accept(ModItems.NEURAL_SCANNER_BLOCK.get());
                        output.accept(ModItems.DIMENSIONAL_DRIVE.get());
                        output.accept(ModItems.CURVE_CORE.get());
                        output.accept(ModItems.COUNCIL_ARCHIVE.get());
                        output.accept(ModItems.NEURAL_SCANNER.get());
                        output.accept(ModItems.C524_NEURAL_FRAGMENT.get());
                        output.accept(ModItems.IMPOSSIBLE_COMPONENT.get());
                        output.accept(ModItems.PRIME_COMMUNICATOR.get());
                        output.accept(ModItems.PRIME_BOMB.get());
                        output.accept(ModItems.RICK_BRAIN.get());
                        output.accept(ModItems.CURVE_COORDINATES.get());
                        output.accept(ModItems.INTERDIMENSIONAL_CALIBRATOR.get());
                        output.accept(ModItems.CURVE_STABILIZER.get());
                        output.accept(ModItems.PRIME_SINGULARITY_CORE.get());
                        output.accept(ModItems.CYBERNETIC_IMPLANT.get());
                        output.accept(ModItems.ARM_BLASTER.get());
                        output.accept(ModItems.FORCE_FIELD_EMITTER.get());
                        output.accept(ModItems.COMBAT_JETPACK.get());
                        output.accept(ModItems.CONTAINMENT_NET.get());
                        output.accept(ModItems.PHOENIX_MODULE.get());
                        output.accept(ModItems.REGENERATION_INJECTOR.get());
                        output.accept(ModItems.FEDERATION_THREAT_SCANNER.get());
                        output.accept(ModItems.FEDERATION_ALIEN_SPAWN_EGG.get());
                        output.accept(ModItems.MEESEEKS_BOX.get());
                        output.accept(ModItems.MEESEEKS_SPAWN_EGG.get());
                        output.accept(ModItems.EVIL_MORTY_EYEPATCH.get());
                        output.accept(ModItems.LASER_GUN.get());
                        output.accept(ModItems.PLASMA_PISTOL.get());
                        output.accept(ModItems.FREEZE_RAY.get());
                        output.accept(ModItems.MINDBLOWER_GUN.get());
                        output.accept(ModItems.NEUTRINO_BOMB.get());
                        output.accept(ModItems.WEAPON_ENERGY_CELL.get());
                        output.accept(ModItems.RICK_LIGHTSABER.get());
                        output.accept(ModItems.PURGE_HELMET.get());
                        output.accept(ModItems.PURGE_CHESTPLATE.get());
                        output.accept(ModItems.PURGE_LEGGINGS.get());
                        output.accept(ModItems.PURGE_BOOTS.get());
                        output.accept(ModItems.PORTAL_CANNON.get());
                        output.accept(ModItems.RICK_SHIP.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
