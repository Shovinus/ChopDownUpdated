package com.ternsip.chopdown;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Mod(   modid = ChopDown.MODID,
        name = ChopDown.MODNAME,
        version = ChopDown.VERSION,
        acceptableRemoteVersions = "*")
public class ChopDown {

    public static final String MODID = "chopdown";
    public static final String MODNAME = "ChopDown";
    public static final String VERSION = "1.0.1";
    public static final String AUTHOR = "Ternsip";

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        BlockPos pos = event.getPos();
        if (!state.getBlock().isWood(world, pos)) {
            return;
        }
        int radius = 16;
        int dirX = Math.max(-1, Math.min(1, pos.getX() - (int)Math.round(event.getPlayer().posX - 0.5)));
        int dirZ = Math.max(-1, Math.min(1, pos.getZ() - (int)Math.round(event.getPlayer().posZ - 0.5)));
        LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
        HashMap<BlockPos, Integer> used = new HashMap<BlockPos, Integer>();
        queue.add(pos);
        int leaf = 5;
        used.put(pos, leaf);
        while (!queue.isEmpty()) {
            BlockPos top = queue.pollFirst();
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        BlockPos nPos = top.add(dx, dy, dz);
                        int step = used.get(top);
                        if (step <= 0 || nPos.distanceSq(pos) > radius * radius) {
                            continue;
                        }
                        IBlockState nState = world.getBlockState(nPos);
                        boolean log = nState.getBlock().isWood(world, nPos);
                        boolean leaves = nState.getBlock().isLeaves(nState, world, nPos);
                        if ((dy >= 0 && step == leaf && log) || leaves) {
                            step = step - (leaves ? 1 : 0);
                            if (!used.containsKey(nPos) || used.get(nPos) < step) {
                                used.put(nPos, step);
                                queue.push(nPos);
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<BlockPos, Integer> entry : used.entrySet()) {
            BlockPos blockPos = entry.getKey();
            if (isDraggable(world, blockPos.add(0, -1, 0))) {
                int oy = blockPos.getY() - pos.getY();
                drop(world, blockPos, blockPos.add(oy * dirX, 0, oy * dirZ));
            }
        }
    }

    private static boolean isDraggable(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return  state.getBlock().isWood(world, pos) ||
                state.getBlock().isLeaves(state, world, pos) ||
                state.getBlock().isAir(state, world, pos) ||
                state.getBlock().isPassable(world, pos);
    }

    private static void drop(World world, BlockPos pos, BlockPos newPos) {
        EntityFallingBlock fallingBlock = new EntityFallingBlock(world, newPos.getX(), newPos.getY(), newPos.getZ(), world.getBlockState(pos));
        fallingBlock.setEntityBoundingBox(new AxisAlignedBB(newPos.add(0, 0, 0), newPos.add(1, 1, 1)));
        fallingBlock.fallTime = 1;
        world.spawnEntity(fallingBlock);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }

    public static class EntityFallingBlock extends net.minecraft.entity.item.EntityFallingBlock {

        EntityFallingBlock(World worldIn, double x, double y, double z, IBlockState fallingBlockState) {
            super(worldIn, x, y, z, fallingBlockState);
        }

        @Nullable
        @Override
        public EntityItem entityDropItem(ItemStack stack, float offsetY) {
            IBlockState state = getBlock();
            if (state != null && state.getBlock() instanceof BlockLeaves) {
                BlockLeaves leaves = (BlockLeaves) state.getBlock();
                for (ItemStack item : leaves.getDrops(world, getPosition(), state, 0)) {
                    super.entityDropItem(item, offsetY);
                }
                return null;
            }
            return super.entityDropItem(stack, offsetY);
        }
    }


}
