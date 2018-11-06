package com.shovinus.chopdownupdated.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.Gson;
import com.shovinus.chopdownupdated.ChopDown;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class Config {

	public static String CATEGORY = "General";
	public static String MOD_CATEGORY = "Mod Compatibility";

	public static boolean breakLeaves;
	public static int maxDropsPerTickPerTree;
	public static int maxFallingBlockBeforeManualMove;
	public static String[] allowedPlayers;
	public static String[] ignoreTools;

	public static HashMap<UUID, PersonalConfig> playerConfigs = new HashMap<UUID, PersonalConfig>();
	public static TreeConfiguration[] treeConfigurations;

	public static String[] leaves;
	public static String[] logs;
	public static String[] sharedLeaves;

	public static ModTreeConfigurations mods = new ModTreeConfigurations();

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

	public static void load(FMLPreInitializationEvent event) throws Exception {
		config = new Configuration(event.getSuggestedConfigurationFile(), ChopDown.VERSION);
		if (!config.getDefinedConfigVersion().equals(config.getLoadedConfigVersion())) {
			event.getSuggestedConfigurationFile().renameTo(new File(event.getSuggestedConfigurationFile().getPath() + "_old"));
			
			config = new Configuration(event.getSuggestedConfigurationFile(), ChopDown.VERSION);
		}
		reloadConfig();
	}

	public static void reloadConfig() throws Exception {

		maxDropsPerTickPerTree = config.getInt("maxDropsPerTickPerTree", CATEGORY, 150, 1, 1000000,
				"Maximum number of blocks to drop per tick for each tree thats falling");
		maxFallingBlockBeforeManualMove = config.getInt("maxFallingBlockBeforeManualMove", CATEGORY, 1500, 1, 1000000,
				"If the total blocks in the tree is above this amount instead of creating entities then it will place the blocks directly on the floor, this is for really large trees like the natura Redwood");
		breakLeaves = config.getBoolean("breakLeaves", CATEGORY, false,
				"When you chop a tree down the leaves all fall off and do their drops instead of falling with the tree, this can be better as a) less load and b)The falling of trees gets less messy, you still need to chop the logs but the leaves don't get in the way");
		sharedLeaves = config.getStringList("sharedLeaves", CATEGORY, new String[] { "harvestcraft:beehive:0" },
				"Not necessarily leaves, objects that if seemingly attached to the tree should fall down with it, such as beehives");

		allowedPlayers = config.getStringList("allowedPlayers", CATEGORY,
				new String[] { EntityPlayerMP.class.getName(),
						"micdoodle8.mods.galacticraft.core.entities.player.GCEntityPlayerMP",
						"clayborn.universalremote.hooks.entity.HookedEntityPlayerMP" },
				"List of all the player classes allowed to chop down trees, used to distinguish fake and real players");

		ignoreTools = config.getStringList("ignoreTools", CATEGORY, new String[] { "tconstruct:lumberaxe:.*" },
				"List of tools to ignore chop down on, such as tinkers lumberaxe, any tool that veinmines or similar should be ignored for chopdown");
		
		//Predefined tree configs for mods
		List<String> activeMods = new ArrayList<String>();
		if (config.getBoolean("Vanilla", MOD_CATEGORY, true, "Vanilla"))
			activeMods.add("Vanilla");
		String[] availableMods = { "BiomesOPlenty", "DefiledLands", "ExtraTrees", "Forestry", "IndustrialCraft2",
				"IntegratedDynamics", "Natura", "PamsHarvestCraft", "Plants", "Rustic", "Terra", "Terraqueous",
				"Thaumcraft", "TheBetweenLands", "Traverse", "Tropicraft" };
		for (String mod : availableMods) {
			if (config.getBoolean(mod, MOD_CATEGORY, false, mod))
				activeMods.add(mod);
		}
		
		//Custom configs
		String[] tempTreeConfig = config.getStringList("customTrees", CATEGORY,
				new String[] {},
				"Allows you to add your own custom trees, use the following google sheet to design your own trees more easily (Make a copy): http://bit.ly/treeconfig");
		List<TreeConfiguration> tempTreeConfigurations = new ArrayList<TreeConfiguration>();
		for (String treeConfig : tempTreeConfig) {
			tempTreeConfigurations.add(new Gson().fromJson(treeConfig, TreeConfiguration.class));
		}
		TreeConfiguration[] tempCustomTrees = tempTreeConfigurations.toArray(new TreeConfiguration[tempTreeConfigurations.size()]);
		mods.setCustomTrees(tempCustomTrees);
		
		//Merge trees
		mods.ActivateMods(ConvertListToArray(activeMods));
		treeConfigurations = mods.UnifiedTreeConfigs.toArray(new TreeConfiguration[mods.UnifiedTreeConfigs.size()]);
		GenerateLeavesAndLogs();
		config.save();

	}

	public static boolean MatchesTool(String name) {
		for (String tool : Config.ignoreTools) {
			if (tool.equals(name) || name.matches(tool)) {
				return true;
			}
		}
		return false;
	}

	static String[] MergeArray(String[] a, String[] b) {
		String[] d = a;
		for (String c : b) {
			if (!ArrayUtils.contains(d, c)) {
				d = ArrayUtils.add(d, c);
			}
		}
		return d;
	}

	private static void GenerateLeavesAndLogs() {
		leaves = new String[] {};
		logs = new String[] {};
		for (TreeConfiguration treeConfig : treeConfigurations) {
			leaves = MergeArray(leaves, treeConfig.Leaves());
			logs = MergeArray(logs, ConvertListToArray(treeConfig.Logs()));
		}
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) throws Exception {
		if (event.getModID().equals(ChopDown.MODID)) {
			reloadConfig();
		}
	}

	public static String[] ConvertListToArray(List<String> list) {
		return list.toArray(new String[list.size()]);
	}	
}
