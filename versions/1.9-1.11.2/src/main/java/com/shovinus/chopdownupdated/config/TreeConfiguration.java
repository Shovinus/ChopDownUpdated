package com.shovinus.chopdownupdated.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class TreeConfiguration {
	/*
	 * The horizontal radius from the trunk to check for tree members
	 */
	public int Radius() {
		return radius == 0 ? 9 : radius;
	}

	/*
	 * Maximum steps from a log a leaf can be
	 */
	public int Leaf_limit() {
		return leaf_limit == 0 ? 12 : leaf_limit;
	}

	/*
	 * Maximum steps from a log a leaf can be
	 */
	public int Trunk_Radius() {
		return trunk_radius == 0 ? 1 : trunk_radius;
	}

	public int Min_vertical_logs() {
		return min_vertical_logs;
	}

	private int radius = 9;
	private int leaf_limit = 12;
	private int trunk_radius = 1;
	private int min_vertical_logs = 0;
	private List<String> logs;
	private List<String> leaves;
	private String[] leaves_merged;
	private String[] blocks = null;

	public TreeConfiguration() {
	}
	public TreeConfiguration(int radius, int leaf_limit, int min_logs, int trunk_radius, String[] logs,
			String[] leaves) {
		this.radius = radius;
		this.leaf_limit = leaf_limit;
		this.trunk_radius = trunk_radius;
		this.logs = new ArrayList<String>(Arrays.asList(logs));
		this.leaves = new ArrayList<String>(Arrays.asList(leaves));;
		this.min_vertical_logs = min_logs;
	}
	public TreeConfiguration(int radius, int leaf_limit, int min_logs, int trunk_radius) {
		this.radius = radius;
		this.leaf_limit = leaf_limit;
		this.trunk_radius = trunk_radius;
		this.min_vertical_logs = min_logs;
	}
	public TreeConfiguration setLogs(String... logs) {
		this.logs = new ArrayList<String>(Arrays.asList(logs));
		return this;
	}
	public TreeConfiguration setLeaves(String... leaves) {
		this.leaves = new ArrayList<String>(Arrays.asList(leaves));
		return this;
	}

	public boolean isLog(String name) {
		for (String block : logs) {
			if (block.equals(name) || name.matches(block)) {
				return true;
			}
		}
		return false;
	}

	public boolean isLeaf(String name) {
		for (String block : Leaves()) {
			if (block.equals(name) || name.matches(block)) {
				return true;
			}
		}
		return false;
	}

	public List<String> Logs() {
		return logs;
	}

	//Gets all leaves after merging the shared leaves (beehives etc)
	public String[] Leaves() {
		if (leaves_merged == null) {
			leaves_merged = Config.MergeArray(Config.ConvertListToArray(leaves), Config.sharedLeaves);
		}
		return leaves_merged;
	}
	//Gets all blocks associated with this tree
	public String[] Blocks() {
		if (blocks == null) {
			blocks =ArrayUtils.addAll(Config.ConvertListToArray(logs), Leaves());
		}
		return blocks;
	}

	public void Merge(TreeConfiguration newTree) {
		// TODO Auto-generated method stub
		for (String log : newTree.Logs()) {
			if (!logs.contains(log)) {
				logs.add(log);
			}
		}
		for (String leaf : newTree.Leaves()) {
			if (!leaves.contains(leaf)) {
				leaves.add(leaf);
			}
		}
		leaves_merged = null;
	}
	public TreeConfiguration Clone() {
		return new TreeConfiguration(radius,leaf_limit,min_vertical_logs,trunk_radius,Config.ConvertListToArray(logs),Config.ConvertListToArray(leaves));
	}
}
