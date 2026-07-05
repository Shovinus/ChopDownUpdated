package com.shovinus.chopdownupdated.config.mods;

import com.shovinus.chopdownupdated.config.TreeConfiguration;

public class Tropicraft {
	public static TreeConfiguration[] Trees = new TreeConfiguration[] {
			new TreeConfiguration(15,14,0,0).setLogs("tropicraft:log:0").setLeaves("tropicraft:leaves:0"),
			new TreeConfiguration().setLogs("tropicraft:log:1").setLeaves("tropicraft:leaves:1"),
			new TreeConfiguration().setLogs("minecraft:log:0").setLeaves("tropicraft:leaves:3","tropicraft:leaves_fruit:0","tropicraft:leaves_fruit:1","tropicraft:leaves_fruit:2","tropicraft:leaves_fruit:3"),
	};

}
