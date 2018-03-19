package com.shovinus.chopdownupdated.config;

public class TreeConfiguration {
	/*
	 * The horizontal radius from the trunk to check for tree members
	 */
	public int Radius() {
		return radius == 0?8:radius;
	}
	/*
	 * Maximum steps from a log a leaf can be
	 */
	public int Leaf_limit() {
		return leaf_limit == 0?8:leaf_limit;
	}
	public int Min_vertical_logs() {
		return min_vertical_logs;
	}

	public String[] getBlocks() {
		return blocks;
	}
	
	private int radius = 8;
	private int leaf_limit = 8;
	private int min_vertical_logs = 0;
	private String[] blocks;

	public TreeConfiguration(int radius, int leaf_limit, int min_logs,String... blocks) {
		this.radius = radius;
		this.leaf_limit = leaf_limit;
		this.blocks = blocks;
		this.min_vertical_logs = min_logs;
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
