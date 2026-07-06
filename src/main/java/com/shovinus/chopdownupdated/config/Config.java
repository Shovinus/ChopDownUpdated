package com.shovinus.chopdownupdated.config;

import com.google.gson.Gson;
import com.shovinus.chopdownupdated.ChopDown;
import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Config {
    public static final String CATEGORY = "General";
    public static final String MOD_CATEGORY = "Mod Compatibility";

    public static boolean breakLeaves;
    public static int maxDropsPerTickPerTree;
    public static int maxFallingBlockBeforeManualMove;
    public static List<? extends String> allowedPlayers;
    public static List<? extends String> ignoreTools;
    public static List<? extends String> sharedLeaves;

    public static final HashMap<UUID, PersonalConfig> playerConfigs = new HashMap<>();
    public static TreeConfiguration[] treeConfigurations = new TreeConfiguration[0];

    public static String[] leaves = new String[0];
    public static String[] logs = new String[0];

    public static ModTreeConfigurations mods = new ModTreeConfigurations();

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue MAX_DROPS_PER_TICK_PER_TREE;
    private static final ForgeConfigSpec.IntValue MAX_FALLING_BLOCK_BEFORE_MANUAL_MOVE;
    private static final ForgeConfigSpec.BooleanValue BREAK_LEAVES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHARED_LEAVES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_PLAYERS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORE_TOOLS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_TREES;
    private static final ForgeConfigSpec.BooleanValue VANILLA;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push(CATEGORY);
        MAX_DROPS_PER_TICK_PER_TREE = builder.comment("Maximum number of blocks to drop per tick for each falling tree.")
                .defineInRange("maxDropsPerTickPerTree", 150, 1, 1_000_000);
        MAX_FALLING_BLOCK_BEFORE_MANUAL_MOVE = builder.comment("Trees above this block count are placed directly instead of spawned as falling entities.")
                .defineInRange("maxFallingBlockBeforeManualMove", 1500, 1, 1_000_000);
        BREAK_LEAVES = builder.comment("Leaves break and drop instead of falling with the logs.")
                .define("breakLeaves", false);
        SHARED_LEAVES = builder.comment("Extra block ids or regexes that should fall with attached trees.")
                .defineListAllowEmpty(List.of("sharedLeaves"), () -> List.of("minecraft:bee_nest", "minecraft:beehive"), value -> value instanceof String);
        ALLOWED_PLAYERS = builder.comment("Player implementation classes allowed to trigger tree falling.")
                .defineListAllowEmpty(List.of("allowedPlayers"), () -> List.of("net.minecraft.server.level.ServerPlayer"), value -> value instanceof String);
        IGNORE_TOOLS = builder.comment("Tool item ids or regexes that should not trigger Chop Down.")
                .defineListAllowEmpty(List.of("ignoreTools"), List::of, value -> value instanceof String);
        builder.pop();

        builder.push(MOD_CATEGORY);
        VANILLA = builder.comment("Vanilla Minecraft trees.").define("Vanilla", true);
        CUSTOM_TREES = builder.comment("Custom tree definitions as JSON TreeConfiguration objects.")
                .defineListAllowEmpty(List.of("customTrees"), List::of, value -> value instanceof String);
        builder.pop();

        SPEC = builder.build();
    }

    public static PersonalConfig getPlayerConfig(UUID player) {
        return playerConfigs.computeIfAbsent(player, id -> new PersonalConfig());
    }

    public static void reloadConfig() {
        maxDropsPerTickPerTree = MAX_DROPS_PER_TICK_PER_TREE.get();
        maxFallingBlockBeforeManualMove = MAX_FALLING_BLOCK_BEFORE_MANUAL_MOVE.get();
        breakLeaves = BREAK_LEAVES.get();
        sharedLeaves = SHARED_LEAVES.get();
        allowedPlayers = ALLOWED_PLAYERS.get();
        ignoreTools = IGNORE_TOOLS.get();

        List<String> activeMods = new ArrayList<>();
        if (VANILLA.get()) {
            activeMods.add("Vanilla");
        }

        List<TreeConfiguration> tempTreeConfigurations = new ArrayList<>();
        for (String treeConfig : CUSTOM_TREES.get()) {
            tempTreeConfigurations.add(new Gson().fromJson(treeConfig, TreeConfiguration.class));
        }
        mods.setCustomTrees(tempTreeConfigurations.toArray(new TreeConfiguration[0]));

        try {
            mods.ActivateMods(ConvertListToArray(activeMods));
            treeConfigurations = mods.UnifiedTreeConfigs.toArray(new TreeConfiguration[0]);
            GenerateLeavesAndLogs();
        } catch (Exception ex) {
            ChopDown.LOGGER.error("Unable to load Chop Down tree configuration", ex);
        }
    }

    public static boolean MatchesTool(String name) {
        for (String tool : ignoreTools) {
            if (tool.equals(name) || name.matches(tool)) {
                return true;
            }
        }
        return false;
    }

    static String[] MergeArray(String[] a, List<? extends String> b) {
        String[] d = a;
        for (String c : b) {
            d = addIfMissing(d, c);
        }
        return d;
    }

    static String[] MergeArray(String[] a, String[] b) {
        String[] d = a;
        for (String c : b) {
            d = addIfMissing(d, c);
        }
        return d;
    }

    private static String[] addIfMissing(String[] values, String value) {
        for (String existing : values) {
            if (existing.equals(value)) {
                return values;
            }
        }
        String[] next = new String[values.length + 1];
        System.arraycopy(values, 0, next, 0, values.length);
        next[values.length] = value;
        return next;
    }

    private static void GenerateLeavesAndLogs() {
        leaves = new String[0];
        logs = new String[0];
        for (TreeConfiguration treeConfig : treeConfigurations) {
            leaves = MergeArray(leaves, treeConfig.Leaves());
            logs = MergeArray(logs, ConvertListToArray(treeConfig.Logs()));
        }
    }

    public static void setBreakLeaves(boolean value) {
        BREAK_LEAVES.set(value);
        BREAK_LEAVES.save();
        reloadConfig();
    }

    public static void onConfigLoad(Object event) {
        if (configSpec(event) == SPEC) {
            reloadConfig();
        }
    }

    private static Object configSpec(Object event) {
        try {
            Object config = event.getClass().getMethod("getConfig").invoke(event);
            Method getSpec = config.getClass().getMethod("getSpec");
            return getSpec.invoke(config);
        } catch (ReflectiveOperationException e) {
            ChopDown.LOGGER.warn("Unable to inspect Chop Down config event", e);
            return null;
        }
    }

    public static String[] ConvertListToArray(List<String> list) {
        return list.toArray(new String[0]);
    }
}
