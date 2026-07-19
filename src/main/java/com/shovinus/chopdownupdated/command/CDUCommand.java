package com.shovinus.chopdownupdated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.shovinus.chopdownupdated.ChopDown;
import com.shovinus.chopdownupdated.config.Config;
import com.shovinus.chopdownupdated.config.PersonalConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class CDUCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = literal("chopdownupdated")
                .requires(CDUCommand::hasPermission);

        registerToggle(root, "makeGlass", cfg -> cfg.makeGlass, (cfg, value) -> cfg.makeGlass = value);
        registerToggle(root, "dontDrop", cfg -> cfg.dontFell, (cfg, value) -> cfg.dontFell = value);
        registerToggle(root, "showBlockName", cfg -> cfg.showBlockName, (cfg, value) -> cfg.showBlockName = value);

        root.then(literal("breakLeaves")
                .executes(ctx -> setBreakLeaves(ctx.getSource(), !Config.breakLeaves))
                .then(argument("value")
                        .executes(ctx -> setBreakLeaves(ctx.getSource(), BoolArgumentType.getBool(ctx, "value")))));

        dispatcher.register(root);
        dispatcher.register(literal("cdu").redirect(root.build()));
    }

    private static void registerToggle(LiteralArgumentBuilder<CommandSourceStack> root, String name,
                                       Function<PersonalConfig, Boolean> getter,
                                       BiConsumer<PersonalConfig, Boolean> setter) {
        root.then(literal(name)
                .executes(ctx -> setPlayerValue(ctx.getSource(), name, getter, setter, null))
                .then(argument("value")
                        .executes(ctx -> setPlayerValue(ctx.getSource(), name, getter, setter, BoolArgumentType.getBool(ctx, "value")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Boolean> argument(String name) {
        return RequiredArgumentBuilder.argument(name, BoolArgumentType.bool());
    }

    private static boolean hasPermission(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
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
        PersonalConfig config = Config.getPlayerConfig(ChopDown.playerId(player));
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
