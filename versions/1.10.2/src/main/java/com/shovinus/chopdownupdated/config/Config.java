package com.shovinus.chopdownupdated.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.shovinus.chopdownupdated.ChopDown;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class Config {

	public static String CATEGORY = "General";

	public static boolean lowerLogs;
	public static boolean dropLeaves;
	public static int maxDropsPerTickPerTree;
	public static int maxFallingBlockBeforeManualMove;
	public static HashMap<UUID, PersonalConfig> playerConfigs = new HashMap<UUID, PersonalConfig>();
	public static TreeConfiguration[] treeConfigurations;

	public static PersonalConfig getPlayerConfig(UUID player) {
		PersonalConfig playerConfig;
		if (playerConfigs.containsKey(player)) {
			playerConfig = Config.playerConfigs.get(player);
		} else {
			playerConfig = new PersonalConfig();
			playerConfigs.put(player, playerConfig);
		}
		return playerConfig;

	}

	public static Configuration config;

	public static void load(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		treeConfigurations = new TreeConfiguration[] {
				new TreeConfiguration(8, 8, "minecraft:log:0", "minecraft:leaves:0", "biomesoplenty:leaves_0:1",
						"biomesoplenty:leaves_3:9", "biomesoplenty:leaves_3:5", "biomesoplenty:leaves_2:3",
						"biomesoplenty:leaves_1:5"),
				new TreeConfiguration(8, 8, "minecraft:log:1", "minecraft:leaves:1"),
				new TreeConfiguration(8, 8, "minecraft:log:2", "minecraft:leaves:2", "biomesoplenty:leaves_0:0"),
				new TreeConfiguration(8, 8, "minecraft:log:3", "minecraft:leaves:3"),
				new TreeConfiguration(8, 8, "minecraft:log2:0", "minecraft:leaves2:0", "biomesoplenty:leaves_0:1"),
				new TreeConfiguration(8, 8, "minecraft:log2:1", "minecraft:leaves2:1", "biomesoplenty:leaves_0:1",
						"biomesoplenty:leaves_1:5"),
				new TreeConfiguration(8, 8, "natura:overworld_logs:0", "natura:overworld_leaves:0"),
				new TreeConfiguration(8, 8, "natura:overworld_logs:1", "natura:overworld_leaves:1"),
				new TreeConfiguration(8, 8, "natura:overworld_logs:2", "natura:overworld_leaves:2"),
				new TreeConfiguration(8, 8, "natura:overworld_logs:3", "natura:overworld_leaves:3"),
				new TreeConfiguration(8, 8, "natura:overworld_logs2:0", "natura:overworld_leaves2:0"),
				new TreeConfiguration(8, 8, "natura:overworld_logs2:1", "natura:overworld_leaves2:1"),
				new TreeConfiguration(8, 8, "natura:overworld_logs2:2", "natura:overworld_leaves2:2"),
				new TreeConfiguration(8, 8, "natura:overworld_logs2:3", "natura:overworld_leaves2:3"),
				new TreeConfiguration(32, 32, "natura:redwood_logs:0", "natura:redwood_logs:1", "natura:redwood_logs:2",
						"natura:redwood_leaves:0"),
				new TreeConfiguration(8, 8, "natura:nether_logs:0", "natura:nether_leaves:0"),
				new TreeConfiguration(8, 8, "natura:nether_logs:1", "natura:nether_leaves2:0",
						"natura:nether_leaves2:1", "natura:nether_leaves2:2"),
				new TreeConfiguration(8, 8, "natura:nether_logs:2", "natura:nether_leaves:2"),
				new TreeConfiguration(8, 8, "natura:nether_logs2:0", "natura:nether_logs2:1", "natura:nether_leaves:0"),
				new TreeConfiguration(8, 8, "biomesoplenty:log_2:5", "biomesoplenty:leaves_4:3"),
				new TreeConfiguration(8, 8, "biomesoplenty:log_1:7", "biomesoplenty:leaves_4:9"),
				new TreeConfiguration(8, 8, "biomesoplenty:log_4:5"),
				new TreeConfiguration(8, 8, "biomesoplenty:log_3:6", "biomesoplenty:leaves_5:6"),
				new TreeConfiguration(8, 8, "biomesoplenty:log_3:4", "biomesoplenty:leaves_3:6"),
				new TreeConfiguration(8, 8, "biomesoplenty:log_0:7", "biomesoplenty:leaves_1:6"),
				new TreeConfiguration(8, 8, "biomesoplenty:log_2:6", "biomesoplenty:leaves_5:4"),
				new TreeConfiguration(8, 8, "biomesoplenty:log_2:4", "biomesoplenty:leaves_4:2"),
				new TreeConfiguration(8, 8, "harvestcraft:pammaple:0", "minecraft:leaves:1"),
				new TreeConfiguration(8, 8, "integrateddynamics:menrilLog:0", "integrateddynamics:menrilFilledLog:0",
						"integrateddynamics:menrilLeaves:0"), };
		reloadConfig();
	}

	private static void reloadConfig() {

		lowerLogs = config.getBoolean("lowerLogs", CATEGORY, true,
				"Whether to move logs down through leaves prior to dropping, makes for a better looking fallen tree but adds a few extra iterations, try setting to false if server not handling well.");
		maxDropsPerTickPerTree = config.getInt("maxDropsPerTickPerTree", CATEGORY, 250, 1, 1000000,
				"Maximum number of blocks to drop per tick for each tree thats falling");
		maxFallingBlockBeforeManualMove = config.getInt("maxFallingBlockBeforeManualMove", CATEGORY, 1000, 1, 1000000,
				"If the total blocks in the tree is above this amount instead of creating entities then it will place the blocks directly on the floor, this is for really large trees like the natura Redwood");

		List<String> defaultTreeConfig = new ArrayList<String>();
		for (TreeConfiguration treeConfig : treeConfigurations) {
			defaultTreeConfig.add(new Gson().toJson(treeConfig));
		}
		String[] tempTreeConfig = config.getStringList("treeConfigurations", CATEGORY,
				defaultTreeConfig.toArray(new String[defaultTreeConfig.size()]),
				"List of possible trees, i.e. spruce log and spruce leaves, this makes felling trees more acurate for mixed trees, it also allows large trees like natura redwoods to be chopped more acurately, normally this tree would get ignored because its leaves reach further than a normal tree and its radius is much wider");
		dropLeaves = config.getBoolean("dropLeaves", CATEGORY, false,
				"When you chop a tree down the leaves all fall off and do their drops instead of falling with the tree, this can be better as a) less load and b)The falling of trees gets less messy, you still need to chop the logs but the leaves don't get in the way");
		List<TreeConfiguration> tempTreeConfigurations = new ArrayList<TreeConfiguration>();
		for (String treeConfig : tempTreeConfig) {
			tempTreeConfigurations.add(new Gson().fromJson(treeConfig, TreeConfiguration.class));
		}
		treeConfigurations = tempTreeConfigurations.toArray(new TreeConfiguration[tempTreeConfigurations.size()]);
		config.save();	
		
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if (event.getModID().equals(ChopDown.MODID)) {
			reloadConfig();
		}
	}

}
