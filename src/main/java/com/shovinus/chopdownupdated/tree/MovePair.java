package com.shovinus.chopdownupdated.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;

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
                copyTileData(world, targetTile);
            }
        }
    }

    private void copyTileData(ServerLevel world, BlockEntity targetTile) {
        try {
            CompoundTag tileEntityData = saveTileData(world);
            tileEntityData.remove("x");
            tileEntityData.remove("y");
            tileEntityData.remove("z");
            loadTileData(world, targetTile, tileEntityData);
            targetTile.setChanged();
        } catch (ReflectiveOperationException ex) {
            // Block entity serialization changed in 1.20.5. A failed copy must not stop the tree move.
        }
    }

    private CompoundTag saveTileData(ServerLevel world) throws ReflectiveOperationException {
        Method method = findMethod("saveWithoutMetadata");
        return method.getParameterCount() == 0
                ? (CompoundTag) method.invoke(tile)
                : (CompoundTag) method.invoke(tile, world.registryAccess());
    }

    private void loadTileData(ServerLevel world, BlockEntity targetTile, CompoundTag data)
            throws ReflectiveOperationException {
        Method method;
        try {
            method = findMethod("load");
        } catch (NoSuchMethodException ex) {
            method = findMethod("loadWithComponents");
        }
        if (method.getParameterCount() == 1) {
            method.invoke(targetTile, data);
        } else {
            method.invoke(targetTile, data, world.registryAccess());
        }
    }

    private Method findMethod(String name) throws NoSuchMethodException {
        for (Method method : BlockEntity.class.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }
}
