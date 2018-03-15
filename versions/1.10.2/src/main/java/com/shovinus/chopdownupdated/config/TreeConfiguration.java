package com.shovinus.chopdownupdated.config;

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
