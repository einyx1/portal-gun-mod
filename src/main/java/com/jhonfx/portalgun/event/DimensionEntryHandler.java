package com.jhonfx.portalgun.event;

import com.jhonfx.portalgun.citadel.CitadelBuilder;
import com.jhonfx.portalgun.federation.FederationBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Garante que a Cidadela e a Federação sejam construídas assim que QUALQUER
 * jogador entrar nessas dimensões — não importa se chegou via Portal Gun,
 * comando, outro mod de teleporte, ou spawn direto no mundo.
 *
 * Antes, ensureBuilt() só era chamado dentro do fluxo de disparo da Portal Gun
 * (PortalGunItem.spawnDestinationPortal), então qualquer outra forma de entrar
 * na dimensão deixava tudo vazio (sem chão, sem NPCs, sem estruturas).
 */
public final class DimensionEntryHandler {

    private static final ResourceLocation CITADEL_ID    = new ResourceLocation("portalgun", "citadel");
    private static final ResourceLocation FEDERATION_ID = new ResourceLocation("portalgun", "federation");

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ensureForDimension(player, event.getTo().location());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ensureForDimension(player, player.level().dimension().location());
    }

    @SubscribeEvent
    public void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ensureForDimension(player, player.level().dimension().location());
    }

    private void ensureForDimension(ServerPlayer player, ResourceLocation dimId) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (dimId.equals(CITADEL_ID))    CitadelBuilder.ensureBuilt(level);
        if (dimId.equals(FEDERATION_ID)) FederationBuilder.ensureBuilt(level);
        if (dimId.equals(com.jhonfx.portalgun.federation.UnmortrickenFacility.ID))
            com.jhonfx.portalgun.federation.UnmortrickenFacility.ensureBuilt(level);
    }
}
