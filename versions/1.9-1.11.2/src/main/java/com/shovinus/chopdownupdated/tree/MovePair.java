package com.shovinus.chopdownupdated.tree;

import net.minecraft.util.math.BlockPos;

class TreeMovePair {
		public BlockPos to;
		public BlockPos from;
		public Boolean leaves;

		public TreeMovePair(BlockPos from, BlockPos to, Boolean leaves) {
			this.from = from;
			this.to = to;
			this.leaves = leaves;
		}
	}
