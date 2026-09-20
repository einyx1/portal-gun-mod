package com.jhonfx.portalgun.item;

import com.jhonfx.portalgun.entity.PortalColor;

/**
 * PORTAL GUN HIERARCHY
 *
 * Tier 1 — PROTOTYPE / BLUE_PROTOTYPE
 *   Obtained early. Green spatial fluid. Short range.
 *   Green → Overworld + Cidadela, após o incidente Prime.
 *   Blue  → coordenadas locais e estruturas do Overworld.
 *   Cannot cross Finite Curve. No saved destinations in normal gameplay.
 *
 * Tier 2 — STANDARD
 *   Crafted from Rick Brain + components. Full interdimensional travel inside Curve.
 *   Saved destinations, waypoints, structure search. Cannot cross Curve boundary.
 *
 * Tier 3 — EVIL_MORTY
 *   Stolen/dropped by Evil Morty. Yellow fluid. Bypasses Citadel restrictions.
 *   Crosses Finite Curve. No biometrics. Unstable charge (burns faster).
 *
 * Tier 4 — PRIME
 *   Built from Omega components. Rick Prime's personal weapon.
 *   Full unrestricted travel, biometric lock, max power.
 *   Crosses Curve boundary. Half fluid cost.
 */
public enum PortalGunVariant {
    PROTOTYPE(     "prototype_portal_gun",      "prototype_portal_gun",      "prototype_portal_gun_discharged",      PortalColor.GREEN),
    BLUE_PROTOTYPE("prototype_portal_gun",      "blue_prototype_portal_gun", "prototype_portal_gun_discharged",      PortalColor.BLUE),
    STANDARD(      "standard_portal_gun",       "standard_portal_gun",       "standard_portal_gun_discharged",       PortalColor.GREEN),
    EVIL_MORTY(    "evil_morty_portal_gun",     "evil_morty_portal_gun",     "evil_morty_portal_gun_discharged",     PortalColor.YELLOW),
    PRIME(         "prime_portal_gun",          "prime_portal_gun",          "prime_portal_gun_discharged",          PortalColor.GREEN);

    private final String model;
    private final String texture;
    private final String dischargedTexture;
    private final PortalColor color;

    PortalGunVariant(String model, String texture, String dischargedTexture, PortalColor color) {
        this.model = model;
        this.texture = texture;
        this.dischargedTexture = dischargedTexture;
        this.color = color;
    }

    public String model()            { return model; }
    public String texture()          { return texture; }
    public String dischargedTexture(){ return dischargedTexture; }
    public PortalColor color()       { return color; }

    // ── Tier classification ───────────────────────────────────────────────────
    public int tier() {
        return switch (this) {
            case PROTOTYPE, BLUE_PROTOTYPE -> 1;
            case STANDARD                  -> 2;
            case EVIL_MORTY                -> 3;
            case PRIME                     -> 4;
        };
    }

    // ── Capability flags ──────────────────────────────────────────────────────

    /** True for green and blue prototype — early-game, limited range */
    public boolean isPrototype() { return this == PROTOTYPE || this == BLUE_PROTOTYPE; }

    /**
     * Prototypes use the simplified coordinates UI (citadel-only / fed-only buttons).
     * All other tiers show the full dimension picker.
     */
    public boolean isCitadelOnly() { return isPrototype(); }

    /** Prototypes retain only their fixed Citadel destination; higher tiers save arbitrary destinations. */
    public boolean supportsSavedDestinations() { return this != BLUE_PROTOTYPE; }

    /**
     * Can this gun cross the Finite Curve boundary (enter dimensions outside the Curve)?
     * Tier 1-2: NO.  Tier 3-4: YES.
     */
    public boolean crossesFiniteCurve() { return this == EVIL_MORTY || this == PRIME; }

    /**
     * Biometric lock — only the registered owner can fire.
     * Only Rick Prime's gun has this.
     */
    public boolean hasBiometrics() { return this == PRIME; }

    /**
     * Bypass Citadel access restrictions (Citadel won't lock out the shooter).
     * Evil Morty's gun bypasses Citadel security.
     */
    public boolean bypassesRickRestrictions() { return this == EVIL_MORTY; }

    /**
     * Can this gun open portals in the floor/ceiling (vertical portals)?
     * Tier 3+ can.
     */
    public boolean supportsVerticalPortals() { return this == EVIL_MORTY || this == PRIME; }

    /**
     * Can this gun target structures in other dimensions when locating?
     * Standard and above.
     */
    public boolean supportsStructureSearch() { return this == PROTOTYPE || tier() >= 2; }

    /** Fluid cost multiplier. Lower = cheaper per shot. */
    public float fluidMultiplier() {
        return switch (this) {
            case PROTOTYPE, BLUE_PROTOTYPE -> 2.75f;  // expensive — early game
            case STANDARD                  -> 1.00f;  // baseline
            case EVIL_MORTY                -> 1.40f;  // unstable, burns faster
            case PRIME                     -> 0.45f;  // extremely efficient
        };
    }

    /** Max portal scale this variant can open. */
    public float maxPortalScale() {
        return switch (this) {
            case PROTOTYPE, BLUE_PROTOTYPE -> 0.65f;
            case STANDARD                  -> 1.00f;
            case EVIL_MORTY                -> 1.20f;
            case PRIME                     -> 1.60f;
        };
    }

    /** Charge capacity (max fluid stored). */
    public int maxCharge() {
        return switch (this) {
            case PROTOTYPE, BLUE_PROTOTYPE -> 600;
            case STANDARD                  -> 1000;
            case EVIL_MORTY                -> 1000;
            case PRIME                     -> 2000;
        };
    }

    /** Technology key for lore / scanner display */
    public String technologyKey() {
        return switch (this) {
            case PROTOTYPE      -> "spatial_prototype_green";
            case BLUE_PROTOTYPE -> "spatial_prototype_blue";
            case STANDARD                  -> "rick_reality";
            case EVIL_MORTY                -> "unrestricted_multiversal";
            case PRIME                     -> "prime_multiversal";
        };
    }

    /** Human-readable tier label for tooltips */
    public String tierLabel() {
        return switch (tier()) {
            case 1 -> "§7[TIER 1 — PROTOTYPE]";
            case 2 -> "§a[TIER 2 — STANDARD]";
            case 3 -> "§e[TIER 3 — UNRESTRICTED]";
            case 4 -> "§c[TIER 4 — PRIME]";
            default -> "";
        };
    }
}
