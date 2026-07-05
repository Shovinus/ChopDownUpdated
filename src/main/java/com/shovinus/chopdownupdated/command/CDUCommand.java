package com.shovinus.chopdownupdated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.shovinus.chopdownupdated.config.Config;
import com.shovinus.chopdownupdated.config.PersonalConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class CDUCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("chopdownupdated")
                .requires(source -> source.hasPermission(2));

        registerToggle(root, "makeGlass", cfg -> cfg.makeGlass, (cfg, value) -> cfg.makeGlass = value);
        registerToggle(root, "dontDrop", cfg -> cfg.dontFell, (cfg, value) -> cfg.dontFell = value);
        registerToggle(root, "showBlockName", cfg -> cfg.showBlockName, (cfg, value) -> cfg.showBlockName = value);

        root.then(Commands.literal("breakLeaves")
                .executes(ctx -> setBreakLeaves(ctx.getSource(), !Config.breakLeaves))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> setBreakLeaves(ctx.getSource(), BoolArgumentType.getBool(ctx, "value")))));

        dispatcher.register(root);
        dispatcher.register(Commands.literal("cdu").redirect(root.build()));
    }

    private static void registerToggle(LiteralArgumentBuilder<CommandSourceStack> root, String name,
                                       Function<PersonalConfig, Boolean> getter,
                                       BiConsumer<PersonalConfig, Boolean> setter) {
        root.then(Commands.literal(name)
                .executes(ctx -> setPlayerValue(ctx.getSource(), name, getter, setter, null))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> setPlayerValue(ctx.getSource(), name, getter, setter, BoolArgumentType.getBool(ctx, "value")))));
    }

    private static int setPlayerValue(CommandSourceStack source, String name,
                                      Function<PersonalConfig, Boolean> getter,
                                      BiConsumer<PersonalConfig, Boolean> setter,
                                      Boolean requestedValue) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ex) {
            return 0;
        }
        PersonalConfig config = Config.getPlayerConfig(player.getUUID());
        boolean value = requestedValue == null ? !getter.apply(config) : requestedValue;
        setter.accept(config, value);
        source.sendSuccess(() -> Component.literal(name + (value ? " Enabled" : " Disabled")), false);
        return 1;
    }

    private static int setBreakLeaves(CommandSourceStack source, boolean value) {
        Config.setBreakLeaves(value);
        source.sendSuccess(() -> Component.literal("breakLeaves" + (value ? " Enabled" : " Disabled")), false);
        return 1;
    }
}
