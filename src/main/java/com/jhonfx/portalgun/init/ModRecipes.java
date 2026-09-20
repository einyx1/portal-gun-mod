package com.jhonfx.portalgun.init;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class ModRecipes {
    private ModRecipes() {
    }

    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(new BrewingRecipe(
                    Ingredient.of(ModItems.BEAKER.get()), Ingredient.of(net.minecraft.world.item.Items.AMETHYST_SHARD),
                    new ItemStack(ModItems.QUANTUM_ISOTOPE.get())));
            BrewingRecipeRegistry.addRecipe(new BrewingRecipe(
                    Ingredient.of(ModItems.QUANTUM_ISOTOPE.get()), Ingredient.of(ModItems.PHASE_MATTER.get()),
                    new ItemStack(ModItems.INTERSPATIAL_SOLUTION.get())));
            BrewingRecipeRegistry.addRecipe(new BrewingRecipe(
                    Ingredient.of(ModItems.INTERSPATIAL_SOLUTION.get()), Ingredient.of(net.minecraft.world.item.Items.ENDER_EYE),
                    new ItemStack(ModItems.INTERDIMENSIONAL_SOLUTION.get())));
            BrewingRecipeRegistry.addRecipe(new BrewingRecipe(
                    Ingredient.of(ModItems.INTERDIMENSIONAL_SOLUTION.get()), Ingredient.of(net.minecraft.world.item.Items.RAW_GOLD),
                    new ItemStack(ModItems.EXTRADIMENSIONAL_SOLUTION.get())));
        });
    }
}
