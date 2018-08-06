package com.shovinus.chopdownupdated.config;

import org.apache.commons.lang3.ArrayUtils;

public class TreeConfiguration {
	/*
	 * The horizontal radius from the trunk to check for tree members
	 */
	public int Radius() {
		return radius == 0?9:radius;
	}
	/*
	 * Maximum steps from a log a leaf can be
	 */
	public int Leaf_limit() {
		return leaf_limit == 0?12:leaf_limit;
	}
	/*
	 * Maximum steps from a log a leaf can be
	 */
	public int Trunk_Radius() {
		return trunk_radius == 0?1:trunk_radius;
	}
	public int Min_vertical_logs() {
		return min_vertical_logs;
	}



	private int radius = 9;
	private int leaf_limit = 12;
	private int trunk_radius = 1;
	private int min_vertical_logs = 0;
	private String[] logs;
	private String[] leaves;
	private String[] leaves_merged;
	private String[] blocks = null;

	public TreeConfiguration(int radius, int leaf_limit, int min_logs, int trunk_radius, String[] logs, String[] leaves) {
		this.radius = radius;
		this.leaf_limit = leaf_limit;
		this.trunk_radius = trunk_radius;
		this.logs = logs;
		this.leaves = leaves;
		this.min_vertical_logs = min_logs;
	}		

	public boolean isLog(String name) {
		for(String block : logs) {
			if(block.equals(name) || name.matches(block)) {
				return true;
			}
		}
		return false;
	}
	public boolean isLeaf(String name) {
		for(String block : Leaves()) {
			if(block.equals(name) || name.matches(block)) {
				return true;
			}
	}
	return false;
	}
	public String[] Logs() {
		return logs;
	}
	public String[] Leaves() {
		if(leaves_merged == null) {
			leaves_merged = Config.MergeArray(leaves,Config.sharedLeaves);
		}
		return leaves_merged;
	}
	public String[] Blocks() {
		if(blocks == null) {
			blocks = (String[]) ArrayUtils.addAll(logs,Leaves());
		}
		return blocks;
	}
}
