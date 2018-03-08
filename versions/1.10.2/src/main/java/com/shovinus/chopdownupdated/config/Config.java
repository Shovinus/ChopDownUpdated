package com.shovinus.chopdownupdated.config;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import com.google.gson.Gson;
import com.shovinus.chopdownupdated.ChopDown;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.config.Configuration;

public class Config {

	public boolean lowerLogs;
	public int maxDropsPerTickPerTree;
	public int maxFallingBlockBeforeManualMove;
	public HashMap<UUID,PersonalConfig> playerConfigs = new HashMap<UUID,PersonalConfig>();
	public TreeConfiguration[] treeConfigurations = new TreeConfiguration[] {
			new TreeConfiguration(8, 8, "minecraft:log:0", "minecraft:leaves:0", "biomesoplenty:leaves_3:9", "biomesoplenty:leaves_2:3", "biomesoplenty:leaves_0:1", "biomesoplenty:leaves_1:5"),
			new TreeConfiguration(8, 8, "minecraft:log:1", "minecraft:leaves:1"),
			new TreeConfiguration(8, 8, "minecraft:log:2", "minecraft:leaves:2", "biomesoplenty:leaves_0:0"),
			new TreeConfiguration(8, 8, "minecraft:log:3", "minecraft:leaves:3"),
			new TreeConfiguration(8, 8, "minecraft:log2:0", "minecraft:leaves2:0", "biomesoplenty:leaves_0:1"),
			new TreeConfiguration(8, 8, "minecraft:log2:1", "minecraft:leaves2:1"),
			new TreeConfiguration(8, 8, "natura:overworld_logs:0", "natura:overworld_leaves:0"),
			new TreeConfiguration(8, 8, "natura:overworld_logs:1", "natura:overworld_leaves:1"),
			new TreeConfiguration(8, 8, "natura:overworld_logs:2", "natura:overworld_leaves:2"),
			new TreeConfiguration(8, 8, "natura:overworld_logs:3", "natura:overworld_leaves:3"),
			new TreeConfiguration(8, 8, "natura:overworld_logs2:0", "natura:overworld_leaves2:0"),
			new TreeConfiguration(8, 8, "natura:overworld_logs2:1", "natura:overworld_leaves2:1"),
			new TreeConfiguration(8, 8, "natura:overworld_logs2:2", "natura:overworld_leaves2:2"),
			new TreeConfiguration(8, 8, "natura:overworld_logs2:3", "natura:overworld_leaves2:3"),
			new TreeConfiguration(32, 32, "natura:redwood_logs:0", "natura:redwood_logs:1", "natura:redwood_logs:2","natura:redwood_leaves:0"),
			new TreeConfiguration(8, 8, "natura:nether_logs:0", "natura:nether_leaves:0"),
			new TreeConfiguration(8, 8, "natura:nether_logs:1", "natura:nether_leaves2:0", "natura:nether_leaves2:1","natura:nether_leaves2:2"),
			new TreeConfiguration(8, 8, "natura:nether_logs:2", "natura:nether_leaves:2"),
			new TreeConfiguration(8, 8, "natura:nether_logs2:0", "natura:nether_logs2:1", "natura:nether_leaves:0"),
			new TreeConfiguration(8, 8, "biomesoplenty:log_2:5", "biomesoplenty:leaves_4:3"),
			new TreeConfiguration(8, 8, "biomesoplenty:log_1:7", "biomesoplenty:leaves_4:9"),
			new TreeConfiguration(8, 8, "biomesoplenty:log_4:5"),
			new TreeConfiguration(8, 8, "biomesoplenty:log_3:6", "biomesoplenty:leaves_5:6"),
			new TreeConfiguration(8, 8, "biomesoplenty:log_3:4", "biomesoplenty:leaves_3:6"),
			new TreeConfiguration(8, 8, "biomesoplenty:log_0:7", "biomesoplenty:leaves_1:6"),
			new TreeConfiguration(8, 8, "biomesoplenty:log_2:6", "biomesoplenty:leaves_5:4"),
			new TreeConfiguration(8, 8, "harvestcraft:pammaple:0", "minecraft:leaves:1"),
			new TreeConfiguration(8, 8, "integrateddynamics:menrilLog:0", "integrateddynamics:menrilFilledLog:0","integrateddynamics:menrilLeaves:0"),
	};
	
	public PersonalConfig getPlayerConfig(UUID player) {
		PersonalConfig playerConfig;        
        if(playerConfigs.containsKey(player)) {
        	playerConfig = ChopDown.config.playerConfigs.get(player);
        } else {
        	playerConfig = new PersonalConfig();
        	playerConfigs.put(player, playerConfig);
        }
        return playerConfig;
	}
	
	public static Configuration config;

	public Config(File file) {
		
		config = new Configuration(file);
		file.getAbsolutePath();
		String tempTreeConfig = "";
		try {
			lowerLogs = config.getBoolean("lowerLogs", "Optimisation", true,
					"Whether to move logs down through leaves prior to dropping, makes for a better looking fallen tree but adds a few extra iterations, try setting to false if server not handling well.");
			maxDropsPerTickPerTree = config.getInt("maxDropsPerTickPerTree", "Optimisation", 250, 1, 1000000,
					"Maximum number of blocks to drop per tick for each tree thats falling");
			maxFallingBlockBeforeManualMove = config.getInt("maxFallingBlockBeforeManualMove", "Optimisation", 1000, 1,
					1000000,
					"If the total blocks in the tree is above this amount instead of creating entities then it will place the blocks directly on the floor, this is for really large trees like the natura Redwood");
			tempTreeConfig = config.getString("treeConfigurations", "Trees", new Gson().toJson(treeConfigurations),
					"List of possible trees, i.e. spruce log and spruce leaves, this makes felling trees more acurate for mixed trees");
			} catch (Exception e) {
			System.out.println("Error loading config, returning to default variables.");
		} finally {
			config.save();
		}
		treeConfigurations = new Gson().fromJson(tempTreeConfig, TreeConfiguration[].class);
	}

	public class TreeConfiguration {
		public int Radius() {
			return radius;
		}

		public int Leaf_limit() {
			return leaf_limit;
		}

		public String[] getBlocks() {
			return blocks;
		}

		private int radius;
		private int leaf_limit;
		private String[] blocks;

		public TreeConfiguration(int radius, int leaf_limit, String... blocks) {
			this.radius = radius;
			this.leaf_limit = leaf_limit;
			this.blocks = blocks;
		}
		
		
		public boolean matches(String name) {
			for(String block : blocks) {
				if(block.equals(name)) {
					return true;
				}
			}
			return false;
    	}

	}
}
