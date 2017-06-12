package com.ternsip.chopdown;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedList;
import java.util.Random;

@Mod(   modid = ChopDown.MODID,
        name = ChopDown.MODNAME,
        version = ChopDown.VERSION,
        acceptableRemoteVersions = "*")
public class ChopDown {

    public static final String MODID = "chopdown";
    public static final String MODNAME = "ChopDown";
    public static final String VERSION = "1.0.0";
    public static final String AUTHOR = "Ternsip";

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    @SuppressWarnings({"ConstantConditions"})
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        if (state.getBlock() != Blocks.LOG && state.getBlock() != Blocks.LOG2) {
            return;
        }
        BlockPos pos = event.getPos();
        int radius = 16;
        int leaf = 3;
        Random random = new Random(System.currentTimeMillis());
        int dirX = random.nextInt(3) - 1;
        int dirZ = random.nextInt(3) - 1;
        LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
        queue.add(pos);
        while (!queue.isEmpty()) {
            BlockPos top = queue.pollFirst();
            for (int dx = -leaf; dx <= leaf; ++dx) {
                for (int dy = -leaf; dy <= leaf; ++dy) {
                    for (int dz = -leaf; dz <= leaf; ++dz) {
                        BlockPos nPos = top.add(dx, dy, dz);
                        double distSq = nPos.distanceSq(pos);
                        if (distSq > radius * radius || distSq < 1) {
                            continue;
                        }
                        IBlockState nState = world.getBlockState(nPos);
                        int oy = nPos.getY() - pos.getY();
                        if ((dx <= 1 && dx >= -1 && dy <= 1 && dy >= 0 && dz <= 1 && dz >= -1) && (nState.getBlock() == Blocks.LOG || nState.getBlock() == Blocks.LOG2)) {
                            drop(world, nPos, nPos.add(oy * dirX, 0, oy * dirZ));
                            queue.push(nPos);
                        }
                        if (nState.getBlock() == Blocks.LEAVES || nState.getBlock() == Blocks.LEAVES2) {
                            drop(world, nPos, nPos.add(oy * dirX, 0, oy * dirZ));
                        }
                    }
                }
            }
        }
    }

    private static void drop(World world, BlockPos pos, BlockPos newPos) {
        EntityFallingBlock ef = new EntityFallingBlock(world, newPos.getX(), newPos.getY(), newPos.getZ(), world.getBlockState(pos));
        ef.setEntityBoundingBox(new AxisAlignedBB(newPos.add(0, 0, 0), newPos.add(1, 1, 1)));
        ef.fallTime = 1;
        world.spawnEntityInWorld(ef);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }

}
