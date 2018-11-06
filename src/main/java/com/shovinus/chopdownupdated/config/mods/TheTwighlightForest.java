package com.shovinus.chopdownupdated.config.mods;

import com.shovinus.chopdownupdated.config.TreeConfiguration;

public class TheTwighlightForest {
	public static TreeConfiguration[] Trees = new TreeConfiguration[] { 
			new TreeConfiguration(20,12,0,1).setLogs("twilightforest:twilight_log:0").setLeaves("twilightforest:twilight_leaves:0","twilightforest:twilight_leaves:2"),
			new TreeConfiguration().setLogs("twilightforest:twilight_log:1").setLeaves("twilightforest:twilight_leaves:1"),
			new TreeConfiguration(9,12,4,5).setLogs("twilightforest:twilight_log:2").setLeaves("twilightforest:twilight_leaves:2"),
			new TreeConfiguration().setLogs("twilightforest:twilight_log:3").setLeaves("twilightforest:dark_leaves:0"),
	};
}
