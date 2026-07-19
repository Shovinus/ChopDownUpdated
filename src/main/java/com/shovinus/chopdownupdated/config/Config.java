package com.shovinus.chopdownupdated.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shovinus.chopdownupdated.ChopDown;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("chopdownupdated.json");

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

    private static ConfigData data = new ConfigData();

    public static void initialize() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null) {
                    data = loaded;
                }
            } catch (Exception ex) {
                ChopDown.LOGGER.error("Unable to read {}; using defaults", CONFIG_PATH, ex);
            }
        } else {
            save();
        }
        reloadConfig();
    }

    public static PersonalConfig getPlayerConfig(UUID player) {
        return playerConfigs.computeIfAbsent(player, id -> new PersonalConfig());
    }

    public static void reloadConfig() {
        data.validate();
        maxDropsPerTickPerTree = data.maxDropsPerTickPerTree;
        maxFallingBlockBeforeManualMove = data.maxFallingBlockBeforeManualMove;
        breakLeaves = data.breakLeaves;
        sharedLeaves = List.copyOf(data.sharedLeaves);
        allowedPlayers = List.copyOf(data.allowedPlayers);
        ignoreTools = List.copyOf(data.ignoreTools);

        List<String> activeMods = new ArrayList<>();
        if (data.vanilla) {
            activeMods.add("Vanilla");
        }

        List<TreeConfiguration> customTrees = new ArrayList<>();
        for (String treeConfig : data.customTrees) {
            try {
                customTrees.add(GSON.fromJson(treeConfig, TreeConfiguration.class));
            } catch (Exception ex) {
                ChopDown.LOGGER.warn("Ignoring invalid custom tree configuration", ex);
            }
        }
        mods.setCustomTrees(customTrees.toArray(new TreeConfiguration[0]));

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
        data.breakLeaves = value;
        save();
        reloadConfig();
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ex) {
            ChopDown.LOGGER.error("Unable to save {}", CONFIG_PATH, ex);
        }
    }

    public static String[] ConvertListToArray(List<String> list) {
        return list.toArray(new String[0]);
    }

    private static final class ConfigData {
        int maxDropsPerTickPerTree = 150;
        int maxFallingBlockBeforeManualMove = 1500;
        boolean breakLeaves = false;
        List<String> sharedLeaves = new ArrayList<>(List.of("minecraft:bee_nest", "minecraft:beehive"));
        List<String> allowedPlayers = new ArrayList<>(List.of("net.minecraft.server.level.ServerPlayer"));
        List<String> ignoreTools = new ArrayList<>();
        boolean vanilla = true;
        List<String> customTrees = new ArrayList<>();

        void validate() {
            maxDropsPerTickPerTree = Math.max(1, maxDropsPerTickPerTree);
            maxFallingBlockBeforeManualMove = Math.max(1, maxFallingBlockBeforeManualMove);
            if (sharedLeaves == null) sharedLeaves = new ArrayList<>();
            if (allowedPlayers == null) allowedPlayers = new ArrayList<>();
            if (ignoreTools == null) ignoreTools = new ArrayList<>();
            if (customTrees == null) customTrees = new ArrayList<>();
        }
    }
}
