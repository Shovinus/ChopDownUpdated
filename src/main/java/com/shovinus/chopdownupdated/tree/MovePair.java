package com.shovinus.chopdownupdated.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

class TreeMovePair {
    public BlockPos to;
    public BlockPos from;
    public final Tree tree;
    public final boolean leaves;
    public final BlockEntity tile;
    public final BlockState state;
    public boolean moved = false;

    TreeMovePair(BlockPos from, BlockPos to, Tree tree) {
        this.from = from;
        this.to = to;
        this.tree = tree;
        this.leaves = tree.isLeaves(from);
        this.tile = tree.world.getBlockEntity(from);
        BlockState originalState = tree.world.getBlockState(from);
        this.state = tree.isLog(from) ? tree.rotateLog(originalState) : originalState;
    }

    public void move() {
        ServerLevel world = tree.world;
        if (!tree.isAir(to)) {
            Tree.dropDrops(from, to, world.getBlockState(to), world);
        }
        world.setBlock(to, state, 3);
        if (tile != null) {
            BlockEntity targetTile = world.getBlockEntity(to);
            if (targetTile != null) {
                CompoundTag tileEntityData = tile.saveWithoutMetadata();
                tileEntityData.remove("x");
                tileEntityData.remove("y");
                tileEntityData.remove("z");
                targetTile.load(tileEntityData);
                targetTile.setChanged();
            }
        }
    }
}
