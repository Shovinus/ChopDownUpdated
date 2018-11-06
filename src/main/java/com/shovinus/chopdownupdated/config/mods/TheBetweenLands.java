package com.shovinus.chopdownupdated.config.mods;

import com.shovinus.chopdownupdated.config.TreeConfiguration;

public class TheBetweenLands {
	public static TreeConfiguration[] Trees = new TreeConfiguration[] { 
			new TreeConfiguration(32,20,15,5).setLogs("thebetweenlands:log_weedwood:.*","thebetweenlands:weedwood:.*","thebetweenlands:log_rotten_bark:0","thebetweenlands:dentrothyst:0").setLeaves("thebetweenlands:leaves_weedwood_tree:.*","thebetweenlands:shelf_fungus:0"),
			new TreeConfiguration().setLogs("thebetweenlands:log_sap:0").setLeaves("thebetweenlands:leaves_sap_tree:0"),
			new TreeConfiguration().setLogs("thebetweenlands:log_nibbletwig:.*").setLeaves("thebetweenlands:leaves_nibbletwig_tree:.*"),
			new TreeConfiguration().setLogs("thebetweenlands:log_rubber:.*").setLeaves("thebetweenlands:leaves_rubber_tree:.*"),
			new TreeConfiguration(9,12,5,3).setLogs("thebetweenlands:log_hearthgrove:.*").setLeaves("thebetweenlands:leaves_hearthgrove_tree:.*"),
			new TreeConfiguration().setLogs("thebetweenlands:bulb_capped_mushroom_stalk:.*").setLeaves("thebetweenlands:bulb_capped_mushroom_cap:.*"),
			new TreeConfiguration().setLogs("thebetweenlands:log_spirit_tree:.*").setLeaves("thebetweenlands:leaves_spirit_tree_top:.*","thebetweenlands:leaves_spirit_tree_middle:.*","thebetweenlands:leaves_spirit_tree_bottom:.*"),};
}
