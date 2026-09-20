package com.jhonfx.portalgun.init;

import com.jhonfx.portalgun.PortalGunMod;
import com.jhonfx.portalgun.block.entity.PortalSolutionBlockEntity;
import com.jhonfx.portalgun.block.entity.OmegaMachineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PortalGunMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<PortalSolutionBlockEntity>> PORTAL_SOLUTION =
            BLOCK_ENTITY_TYPES.register("portal_solution", () -> BlockEntityType.Builder.of(
                    PortalSolutionBlockEntity::new,
                    ModBlocks.INTERSPATIAL_SOLUTION.get(),
                    ModBlocks.INTERDIMENSIONAL_SOLUTION.get(),
                    ModBlocks.EXTRADIMENSIONAL_SOLUTION.get()).build(null));

    public static final RegistryObject<BlockEntityType<OmegaMachineBlockEntity>> OMEGA_MACHINE =
            BLOCK_ENTITY_TYPES.register("omega_machine", () -> BlockEntityType.Builder.of(
                    OmegaMachineBlockEntity::new,
                    ModBlocks.OMEGA_DEVICE.get(), ModBlocks.OMEGA_CORE.get(),
                    ModBlocks.OMEGA_EMITTER.get(), ModBlocks.OMEGA_RING.get()).build(null));

    private ModBlockEntityTypes() {}
}
