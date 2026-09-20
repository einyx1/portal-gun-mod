package com.jhonfx.portalgun;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * PortalGun Mod — server-side config.
 * Register via: ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModConfig.SERVER_SPEC);
 */
public final class ModConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.IntValue    MEESEEKS_MAX;
    public static final ForgeConfigSpec.IntValue    MEESEEKS_RANGE;
    public static final ForgeConfigSpec.BooleanValue MEESEEKS_GRIEFING;
    public static final ForgeConfigSpec.IntValue    MEESEEKS_LIFETIME_MINUTES;
    public static final ForgeConfigSpec.BooleanValue PORTAL_FLOOR_SHOTS;
    public static final ForgeConfigSpec.BooleanValue FEDERATION_WANTED_SYSTEM;
    public static final ForgeConfigSpec.IntValue    CITADEL_GUARD_CAP;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Meeseeks settings").push("meeseeks");
        MEESEEKS_MAX = b.comment("Max Meeseeks that can be active per player at once.")
                .defineInRange("max_per_player", 5, 1, 20);
        MEESEEKS_RANGE = b.comment("Radius (blocks) in which Meeseeks search for items/threats.")
                .defineInRange("search_range", 32, 8, 64);
        MEESEEKS_GRIEFING = b.comment("Allow Meeseeks to break/place blocks (MINE, BUILD orders).")
                .define("allow_griefing", true);
        MEESEEKS_LIFETIME_MINUTES = b.comment("Minutes before a Meeseeks enters crisis mode.")
                .defineInRange("lifetime_minutes", 8, 1, 60);
        b.pop();

        b.comment("Portal Gun settings").push("portal_gun");
        PORTAL_FLOOR_SHOTS = b.comment("Allow Tier 3+ guns to open portals in floors/ceilings.")
                .define("allow_floor_shots", true);
        b.pop();

        b.comment("Federation settings").push("federation");
        FEDERATION_WANTED_SYSTEM = b.comment("Enable the Federation wanted level system.")
                .define("wanted_system", true);
        b.pop();

        b.comment("Citadel settings").push("citadel");
        CITADEL_GUARD_CAP = b.comment("Max guards spawned in the Citadel.")
                .defineInRange("guard_cap", 4, 0, 16);
        b.pop();

        SERVER_SPEC = b.build();
    }

    private ModConfig() {}
}
