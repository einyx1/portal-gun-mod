package com.jhonfx.portalgun.compat;

import net.minecraftforge.fml.ModList;

/**
 * OPTIONAL Ad Astra compatibility layer.
 *
 * This mod does NOT hard-depend on Ad Astra. All checks below use
 * ModList.isLoaded(), so the mod works fine standalone.
 *
 * Integration points (to expand once Ad Astra's API is confirmed for your
 * exact mod version — API class names vary between Ad Astra releases):
 *
 *   1. Oxygen: Portal Gun could optionally consume Ad Astra oxygen tanks
 *      instead of vanilla air when traveling through space dimensions.
 *   2. Space dimensions: allow the Portal Gun's dimension picker (see
 *      PortalCommands) to list Ad Astra planets (Moon, Mars, Mercury,
 *      Glacio, Venus) alongside portalgun:federation / portalgun:citadel.
 *   3. Jetpack: if the player already has an Ad Astra jetpack equipped,
 *      the Combat Tech jetpack (see CombatTechItem) could be suppressed
 *      to avoid stacking flight sources — or vice versa.
 *
 * Kept dependency-free on purpose: registry inspection provides stable
 * compatibility across Ad Astra patch versions without linking its internals.
 */
public final class AdAstraCompat {
    private static final String AD_ASTRA_MODID = "ad_astra";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(AD_ASTRA_MODID);
    }

    /**
     * Returns true if the player is wearing an Ad Astra space suit/jetpack.
     * Detects Ad Astra protective equipment by registry id. This is enough to
     * prevent PortalGun flight/space systems from stacking incompatible gear.
     */
    public static boolean hasSpaceSuit(net.minecraft.world.entity.player.Player player) {
        if (!isLoaded()) return false;
        int protectivePieces=0;
        for (var stack : player.getArmorSlots()) {
            var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if(id==null||!id.getNamespace().equals(AD_ASTRA_MODID))continue;
            String path=id.getPath();
            if(path.contains("space_suit")||path.contains("netherite_space")||path.contains("jet_suit"))protectivePieces++;
        }
        return protectivePieces>=3;
    }

    /**
     * Returns the list of Ad Astra dimension IDs to offer in the Portal Gun's
     * dimension picker, if Ad Astra is installed. Empty list otherwise.
     */
    public static java.util.List<String> spaceDimensions() {
        if (!isLoaded()) return java.util.List.of();
        return java.util.List.of(
                "ad_astra:moon",
                "ad_astra:mars",
                "ad_astra:mercury",
                "ad_astra:venus",
                "ad_astra:glacio"
        );
    }

    private AdAstraCompat() {}
}
