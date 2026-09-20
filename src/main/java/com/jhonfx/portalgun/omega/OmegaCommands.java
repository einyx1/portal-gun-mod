package com.jhonfx.portalgun.omega;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class OmegaCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("omega")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(context -> {
                    var entries = OmegaBanSavedData.get(context.getSource().getLevel()).entries();
                    context.getSource().sendSuccess(() -> Component.literal(entries.isEmpty()
                            ? "Omega: nenhuma entidade apagada."
                            : "Omega: " + String.join(", ", entries.stream().map(ResourceLocation::toString).toList())), false);
                    return entries.size();
                }))
                .then(Commands.literal("restore")
                        .then(Commands.argument("entity_id", StringArgumentType.string()).executes(context -> {
                            ResourceLocation id = ResourceLocation.tryParse(StringArgumentType.getString(context, "entity_id"));
                            if (id == null) {
                                context.getSource().sendFailure(Component.literal("ID de entidade inválido."));
                                return 0;
                            }
                            boolean restored = OmegaBanSavedData.get(context.getSource().getLevel()).restore(id);
                            context.getSource().sendSuccess(() -> Component.literal(restored
                                    ? "Entidade restaurada: " + id : "Essa entidade não estava apagada."), true);
                            return restored ? 1 : 0;
                        }))));
    }

    private OmegaCommands() {}
}
