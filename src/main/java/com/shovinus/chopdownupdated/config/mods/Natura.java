package com.shovinus.chopdownupdated.config.mods;

import com.shovinus.chopdownupdated.config.TreeConfiguration;

public class Natura {
	public static TreeConfiguration[] Trees = new TreeConfiguration[] {
			new TreeConfiguration().setLogs("natura:nether_logs:0").setLeaves("natura:nether_leaves:0"),
			new TreeConfiguration().setLogs("natura:nether_logs:1").setLeaves("natura:nether_leaves2:0","natura:nether_leaves2:1", "natura:nether_leaves2:2", "natura:nether_leaves2:10"),
			new TreeConfiguration().setLogs("natura:nether_logs:2").setLeaves("natura:nether_leaves:1"),
			new TreeConfiguration().setLogs("natura:nether_logs2:0", "natura:nether_logs2:1").setLeaves("natura:nether_leaves:0"),
			new TreeConfiguration().setLogs("natura:overworld_logs:0").setLeaves("natura:overworld_leaves:0"),
			new TreeConfiguration().setLogs("natura:overworld_logs:1").setLeaves("natura:overworld_leaves:1"),
			new TreeConfiguration().setLogs("natura:overworld_logs:2").setLeaves("natura:overworld_leaves:2"),
			new TreeConfiguration().setLogs("natura:overworld_logs:3").setLeaves("natura:overworld_leaves:3"),
			new TreeConfiguration().setLogs("natura:overworld_logs2:0").setLeaves("natura:overworld_leaves2:0"),
			new TreeConfiguration().setLogs("natura:overworld_logs2:1").setLeaves("natura:overworld_leaves2:1"),
			new TreeConfiguration().setLogs("natura:overworld_logs2:2").setLeaves("natura:overworld_leaves2:2"),
			new TreeConfiguration().setLogs("natura:overworld_logs2:3").setLeaves("natura:overworld_leaves2:3"),
			new TreeConfiguration(32, 32, 20, 8)
					.setLogs("natura:redwood_logs:0", "natura:redwood_logs:1", "natura:redwood_logs:2")
					.setLeaves("natura:redwood_leaves:0") };
}
