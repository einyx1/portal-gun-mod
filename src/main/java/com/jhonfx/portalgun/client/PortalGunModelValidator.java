package com.jhonfx.portalgun.client;

import com.jhonfx.portalgun.PortalGunMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

/** Loads every ported Bedrock mesh during resource reload so malformed assets fail early. */
@Mod.EventBusSubscriber(modid = PortalGunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PortalGunModelValidator {
    private static final String[] MODELS = {
            "standard_portal_gun", "prototype_portal_gun",
            "evil_morty_portal_gun", "prime_portal_gun",
            "travel_solution_full", "travel_solution_mid", "travel_solution_low"
    };

    private PortalGunModelValidator() {}

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
                for (String model : MODELS) {
                    var raw = FileLoader.loadModelFile(
                            new ResourceLocation(PortalGunMod.MOD_ID, "geo/" + model + ".geo.json"), manager);
                    BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));
                }
                FileLoader.loadAnimationsFile(
                        new ResourceLocation(PortalGunMod.MOD_ID, "animations/portal_gun.animation.json"), manager);
                return null;
            }

            @Override
            protected void apply(Void unused, ResourceManager manager, ProfilerFiller profiler) {}
        });
    }
}
