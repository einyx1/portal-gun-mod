package com.jhonfx.portalgun.init;

import com.jhonfx.portalgun.PortalGunMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PortalGunMod.MOD_ID);

    public static final RegistryObject<SoundEvent> FIRE_PORTAL_GUN = register("fire_portal_gun");
    public static final RegistryObject<SoundEvent> PORTAL_SPAWN = register("portal_spawn");
    public static final RegistryObject<SoundEvent> PORTAL_CLOSE = register("portal_close");
    public static final RegistryObject<SoundEvent> PORTAL_ENTER = register("portal_enter");
    public static final RegistryObject<SoundEvent> PORTAL_GUN_ERROR = register("portal_gun_error");
    public static final RegistryObject<SoundEvent> CLICK = register("click");
    public static final RegistryObject<SoundEvent> OPEN_MENU = register("open_menu");
    public static final RegistryObject<SoundEvent> PLUG = register("plug");
    public static final RegistryObject<SoundEvent> UNPLUG = register("unplug");
    public static final RegistryObject<SoundEvent> POWER_OFF = register("power_off");
    public static final RegistryObject<SoundEvent> SELECTION = register("selection");
    public static final RegistryObject<SoundEvent> OMEGA_POWERUP = register("omega_powerup");
    public static final RegistryObject<SoundEvent> OMEGA_ARC = register("omega_arc");
    public static final RegistryObject<SoundEvent> OMEGA_ERASE = register("omega_erase");
    public static final RegistryObject<SoundEvent> OMEGA_COOLDOWN = register("omega_cooldown");
    public static final RegistryObject<SoundEvent> OMEGA_FAILURE = register("omega_failure");
    public static final RegistryObject<SoundEvent> EVIL_MORTY_ESCAPE = register("evil_morty_escape");
    public static final RegistryObject<SoundEvent> WEAPON_LASER = register("weapon_laser");
    public static final RegistryObject<SoundEvent> WEAPON_PLASMA = register("weapon_plasma");
    public static final RegistryObject<SoundEvent> WEAPON_FREEZE = register("weapon_freeze");
    public static final RegistryObject<SoundEvent> WEAPON_MINDBLOWER = register("weapon_mindblower");
    public static final RegistryObject<SoundEvent> WEAPON_NEUTRINO = register("weapon_neutrino");
    public static final RegistryObject<SoundEvent> PURGE_MISSILE = register("purge_missile");
    public static final RegistryObject<SoundEvent> PURGE_FLAME = register("purge_flame");
    public static final RegistryObject<SoundEvent> PURGE_SHOCK = register("purge_shock");
    public static final RegistryObject<SoundEvent> PURGE_BLADE = register("purge_blade");
    public static final RegistryObject<SoundEvent> PURGE_SAW = register("purge_saw");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(PortalGunMod.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
