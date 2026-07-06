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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CDUCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("chopdownupdated")
                .requires(source -> hasGamemasterPermission(source));

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
        sendSuccess(source, Component.literal(name + (value ? " Enabled" : " Disabled")));
        return 1;
    }

    private static int setBreakLeaves(CommandSourceStack source, boolean value) {
        Config.setBreakLeaves(value);
        sendSuccess(source, Component.literal("breakLeaves" + (value ? " Enabled" : " Disabled")));
        return 1;
    }

    private static void sendSuccess(CommandSourceStack source, Component message) {
        try {
            Method method = CommandSourceStack.class.getMethod("sendSuccess", Supplier.class, boolean.class);
            method.invoke(source, (Supplier<Component>) () -> message, false);
        } catch (ReflectiveOperationException ex) {
            try {
                Method method = CommandSourceStack.class.getMethod("sendSuccess", Component.class, boolean.class);
                method.invoke(source, message, false);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static boolean hasGamemasterPermission(CommandSourceStack source) {
        try {
            Method method = source.getClass().getMethod("hasPermission", int.class);
            return (Boolean) method.invoke(source, 2);
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method permissionsMethod = source.getClass().getMethod("permissions");
            Object permissionSet = permissionsMethod.invoke(source);
            Class<?> permissionsClass = Class.forName("net.minecraft.server.permissions.Permissions");
            Field gamemasterField = permissionsClass.getField("COMMANDS_GAMEMASTER");
            Object gamemasterPermission = gamemasterField.get(null);
            Method hasPermission = permissionSet.getClass()
                    .getMethod("hasPermission", Class.forName("net.minecraft.server.permissions.Permission"));
            return (Boolean) hasPermission.invoke(permissionSet, gamemasterPermission);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
