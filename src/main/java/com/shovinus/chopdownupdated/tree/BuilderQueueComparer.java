package com.shovinus.chopdownupdated.tree;

import java.util.Comparator;
import java.util.HashMap;

import net.minecraft.util.math.BlockPos;

public class BuilderQueueComparer implements Comparator<BlockPos> {
	private HashMap<BlockPos, Integer> map;

	public BuilderQueueComparer(HashMap<BlockPos, Integer> map) {
		this.map = map;
	}

	@Override
	public int compare(BlockPos x, BlockPos y) {

		int a = map.get(x);
		int b = map.get(y);
		return a < b ? -1 : a > b ? 1 : 0;

	}
}
