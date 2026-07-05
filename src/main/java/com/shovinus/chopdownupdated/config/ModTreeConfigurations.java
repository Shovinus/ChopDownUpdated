package com.shovinus.chopdownupdated.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shovinus.chopdownupdated.config.mods.Vanilla;

public class ModTreeConfigurations {
	Map<String, TreeConfiguration[]> Mods = new HashMap<String, TreeConfiguration[]>();
	List<TreeConfiguration> UnifiedTreeConfigs = new ArrayList<TreeConfiguration>();

	public void AddMod(String name, TreeConfiguration... trees) {
		Mods.put(name, trees);
	}

	public List<TreeConfiguration> ActivateMods(String[] mods) throws Exception {
		UnifiedTreeConfigs.clear();
		for (String mod : mods) {
			if (Mods.containsKey(mod)) {
				MergeInTrees(Mods.get(mod));
			} else {
				throw new Exception("This mod does not exist:" + mod
						+ " , this should not happen in live, sorry for any inconvience, please report to mod releaser");
			}
		}
		if(Mods.containsKey("_Custom")) {
			MergeInTrees(Mods.get("_Custom"));
		}
		return UnifiedTreeConfigs;
	}

	private void MergeInTrees(TreeConfiguration[] trees) {
		for (TreeConfiguration newTree : trees) {
			if (!compareTrees(newTree)) {
				// Clone to avoid messing up original with possible future merges
				UnifiedTreeConfigs.add(newTree.Clone());
			}
		}
	}

	private boolean compareTrees(TreeConfiguration newTree) {
		for (TreeConfiguration currentTree : UnifiedTreeConfigs) {
			for (String newLog : newTree.Logs()) {
				if (currentTree.Logs().contains(newLog)) {
					// Already have this log in a tree so merge the trees and return
					currentTree.Merge(newTree);
					return true;
				}
			}
		}
		return false;
	}
	public ModTreeConfigurations() {
		Mods.put("Vanilla", Vanilla.Trees);
	}
	public void setCustomTrees(TreeConfiguration[] trees) {
		Mods.put("_Custom", trees);
	}
}
