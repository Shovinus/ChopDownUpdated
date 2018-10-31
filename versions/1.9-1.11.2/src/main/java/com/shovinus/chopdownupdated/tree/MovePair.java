package com.shovinus.chopdownupdated.tree;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

class TreeMovePair {
	public BlockPos to;
	public BlockPos from;
	public Tree tree;
	public Boolean leaves;
	public TileEntity tile;
	public IBlockState state;
	public Boolean moved = false;

	public TreeMovePair(BlockPos from, BlockPos to, Tree tree) {
		this.from = from;
		this.to = to;
		this.tree = tree;
		leaves = tree.isLeaves(from);
		tile = tree.world.getTileEntity(from);
		state = tree.world.getBlockState(from);
		if (tree.isLog(from)) {
			state = tree.rotateLog(tree.world, state);
		}

	}

		}
	public void move() {
		IBlockState state2 = tree.world.getBlockState(to);
		if (!tree.isAir(to)) {
			Tree.dropDrops(from, to, state2,tree.world);
		}
		tree.world.setBlockState(to, state);
		if (tile != null) {
			NBTTagCompound tileEntityData = tile.writeToNBT(new NBTTagCompound());
			TileEntity tileentity = tree.world.getTileEntity(to);
			if (tileentity != null) {
				NBTTagCompound nbttagcompound = tileentity.writeToNBT(new NBTTagCompound());

				for (String s : tileEntityData.getKeySet()) {
					NBTBase nbtbase = tileEntityData.getTag(s);

					if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s)) {
						nbttagcompound.setTag(s, nbtbase.copy());
					}
				}
				tileentity.readFromNBT(nbttagcompound);
				tileentity.markDirty();
			}
		}
	}
}
