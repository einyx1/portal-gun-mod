package com.jhonfx.portalgun.entity;

import com.jhonfx.portalgun.init.ModEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class CinematicPortal {
    public static final String TRAVELER = "PortalGunCinematicTraveler";
    /** Equipment is the capability, not the entity's name or species. */
    public static boolean canOperate(net.minecraft.world.entity.LivingEntity actor) {
        return actor.getMainHandItem().getItem() instanceof com.jhonfx.portalgun.item.PortalGunItem
                || actor.getOffhandItem().getItem() instanceof com.jhonfx.portalgun.item.PortalGunItem;
    }

    public static boolean activate(ServerLevel level, UUID first, UUID second,
                                   net.minecraft.world.entity.LivingEntity actor) {
        if (!canOperate(actor) || first == null || second == null) return false;
        if (!(level.getEntity(first) instanceof PortalEntity entry)
                || !(level.getEntity(second) instanceof PortalEntity exit)) return false;
        entry.getPersistentData().putUUID(TRAVELER, actor.getUUID());
        exit.getPersistentData().putUUID(TRAVELER, actor.getUUID());
        entry.setLinkedPortal(exit);
        exit.setLinkedPortal(entry);
        return true;
    }
    public static UUID spawn(ServerLevel level,Vec3 position,Direction facing,PortalColor color,float scale){
        PortalEntity portal=ModEntityTypes.PORTAL.get().create(level);if(portal==null)return null;
        portal.setPos(position);portal.setPortalFacing(facing);portal.setColor(color);portal.setPortalScale(scale);
        portal.setLifetimeTicks(20*30);portal.setSafeMode(false);level.addFreshEntity(portal);return portal.getUUID();
    }
    public static void close(ServerLevel level,UUID id){if(id!=null&&level.getEntity(id) instanceof PortalEntity portal)portal.discardWithViews();}
    private CinematicPortal(){}
}
