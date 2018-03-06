package com.shovinus.chopdown.tree;

import java.util.Comparator;

import com.shovinus.chopdown.tree.DirectionSort;

import net.minecraft.util.math.BlockPos;

public class AxisComparer implements Comparator<BlockPos> {
		private DirectionSort sort;

		public AxisComparer(DirectionSort sort) {
			this.sort = sort;
		}

		@Override
		public int compare(BlockPos x, BlockPos y) {
			int a = 0, b = 0;
			switch (sort) {
			case NORTH:
				a = x.getX();
				b = y.getX();
				break;
			case SOUTH:
				a = y.getX();
				b = x.getX();
				break;
			case UP:
				a = x.getY();
				b = y.getY();
				break;
			case DOWN:
				a = y.getY();
				b = x.getY();
				break;
			case EAST:
				a = x.getZ();
				b = y.getZ();
				break;
			case WEST:
				a = y.getZ();
				b = x.getZ();
				break;
			}
			return a < b ? -1 : a > b ? 1 : 0;
		}
	}
