package com.ternsip.chopdown;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockLog.EnumAxis;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

@Mod(modid = ChopDown.MODID, name = ChopDown.MODNAME, version = ChopDown.VERSION, acceptableRemoteVersions = "*")
public class ChopDown {

	public static final String MODID = "chopdown";
	public static final String MODNAME = "ChopDownUpdated";
	public static final String VERSION = "1b.1.0";
	public static final String AUTHOR = "Shovinus";// Original Author Ternsip, not sure if this does naming though
	public static LinkedList<Tree> FallingTrees = new LinkedList<Tree>();

	@EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	public static class Tree {
		BlockPos base;
		World world;
		Boolean main = false;
		LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
		HashMap<BlockPos, Integer> used = new HashMap<BlockPos, Integer>();
		HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> estimatedTree = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>();
		LinkedList<BlockPos> logs = new LinkedList<BlockPos>();
		LinkedList<BlockPos> realisticTree = new LinkedList<BlockPos>();
		int fallX = 1;
		int fallZ = 1;
		int radius = 16;
		int leafLimit = 5;

		LinkedList<Tree> lOtherTrees = new LinkedList<Tree>();

		private void BuildTree(BlockPos pos, World world) {
			base = pos;
			this.world = world;
			AddEstimateBlock(base, 0);
			GetPossibleTree();
		}

		public Tree(BlockPos pos, World world) {
			BuildTree(pos, world);

		}

		private BlockPos reposBlock(BlockPos pos) {
			int y = pos.getY() - base.getY();

			int x = pos.getX() - base.getX();
			int z = pos.getZ() - base.getZ();			

			int changeX =  fallZ * z;
			int changeZ =  fallX * x;
			;

			int normPosX = (y * fallX);
			int normPosZ = (y * fallZ);

			return pos.add(normPosX - (changeZ * fallX), -(changeX + changeZ) + 1, normPosZ - (changeX * fallZ));
		}

		public Tree(BlockPos pos, World world, EntityPlayer player) {
			main = true;
			BuildTree(pos, world);
			GetFallDirection(player);
		}

		private void GetFallDirection(EntityPlayer player) {
			Double x = ((base.getX() + 0.5) - player.posX);
			Double z = (base.getZ() + 0.5) - player.posZ;
			Double abX = Math.abs(x);
			Double abZ = Math.abs(z);
			fallX = (int) Math.floor(abX / x);
			fallZ = (int) Math.floor(abZ / z);
			if (abX > abZ) {
				fallZ = 0;
			} else {
				fallX = 0;
			}
		}

		public void Fell() {
			LinkedList<MovePair> dropBlocks = new LinkedList<MovePair>();
			for (BlockPos blockPos : GetRealisticTree()) {
				Thread.yield();
				if (!base.equals(blockPos)) {
					IBlockState state2 = world.getBlockState(blockPos);
					Random rand = new Random();
					if (state2.getBlock().isLeaves(state2, world, blockPos) && rand.nextInt(2) == -1) {
						BlockLeaves blockLeaves = (BlockLeaves) state2.getBlock();
						world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
						blockLeaves.dropBlockAsItem(world, blockPos, state2, 0);
					} else {
						dropBlocks.add(new MovePair(blockPos, reposBlock(blockPos)));
					}
				}
			}
			Collections.sort(dropBlocks, new BlockPosYComparer());
			Boolean movedLog = true;
			while (movedLog) {
				movedLog = false;
				for (MovePair pair : dropBlocks) {
					Thread.yield();
					if (IsWood(pair.from, world)) {
						MovePair lowerPair = GetLowerTargetBlock(pair.to, dropBlocks);
						if (lowerPair != null && !IsWood(lowerPair.from, world)) {
							BlockPos upperBlock = pair.from;
							pair.from = lowerPair.from;
							lowerPair.from = upperBlock;
							movedLog = true;
						}
					}
				}
			}
			Collections.sort(dropBlocks, new BlockPosYComparer());
			for (MovePair pair : dropBlocks) {
				Drop(world, pair.from, pair.to,false);
			}

		}

		private MovePair GetLowerTargetBlock(BlockPos pos, LinkedList<MovePair> pairs) {
			for (MovePair pair : pairs) {
				if (pair.to.equals(pos.add(0, -1, 0))) {
					return pair;
				}
			}
			return null;
		}

		private class MovePair {
			public BlockPos from;
			public BlockPos to;

			public MovePair(BlockPos from, BlockPos to) {
				this.from = from;
				this.to = to;
			}
		}

		public Boolean HasEstimateBlock(BlockPos pos) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			return estimatedTree.containsKey(x) && estimatedTree.get(x).containsKey(y)
					&& estimatedTree.get(x).get(y).containsKey(z);
		}

		public int GetEstimateBlock(BlockPos pos) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			if (HasEstimateBlock(pos)) {
				return estimatedTree.get(x).get(y).get(z);
			}
			return leafLimit + 1;
		}

		public void AddEstimateBlock(BlockPos pos, int step) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			queue.add(pos);
			used.put(pos, step);
			HashMap<Integer, HashMap<Integer, Integer>> yHolder;
			if (estimatedTree.containsKey(x)) {
				yHolder = estimatedTree.get(x);
			} else {
				yHolder = new HashMap<Integer, HashMap<Integer, Integer>>();
				estimatedTree.put(x, yHolder);
			}
			HashMap<Integer, Integer> zHolder;
			if (yHolder.containsKey(y)) {
				zHolder = yHolder.get(y);
			} else {
				zHolder = new HashMap<Integer, Integer>();
				yHolder.put(y, zHolder);
			}
			zHolder.put(z, step);

		}

		public class BlockPosYComparer implements Comparator<MovePair> {
			@Override
			public int compare(MovePair x, MovePair y) {
				int a = x.to.getY();
				int b = y.to.getY();
				return a < b ? -1 : a > b ? 1 : 0;
			}
		}

		public LinkedList<BlockPos> GetRealisticTree() {
			for (BlockPos myBlock : used.keySet()) {
				Boolean mine = true;
				for (Tree otherTree : lOtherTrees) {
					if (otherTree.MyBlock(myBlock, PlaneDistance(base, myBlock), GetEstimateBlock(myBlock))) {
						mine = false;
						break;
					}
					Thread.yield();
				}
				if (mine) {
					realisticTree.add(myBlock);
				}
			}
			return realisticTree;
		}

		// Is the block closer to my trunk or yours?
		public Boolean MyBlock(BlockPos pos, double yourDistance, int yourStepValue) {
			if (GetEstimateBlock(pos) > yourStepValue) {
				return false;
			}
			if (GetEstimateBlock(pos) == yourStepValue) {
				return PlaneDistance(base, pos) < yourDistance;
			}
			return true;
		}
		private enum EnumFallAxis{
			X,Z
		};
		private void RotateLog(World world, BlockPos pos, EnumFallAxis Axis) {
			IProperty<?> foundProp = null;
			IBlockState state = world.getBlockState(pos);
			for (net.minecraft.block.properties.IProperty<?> prop : state.getProperties().keySet())
	        {
	            if (prop.getName().equals("axis"))
	            {
	            	foundProp= prop;
	            }
	        }
			
			if (foundProp == null) {
				return;
			}
			BlockLog.EnumAxis currAxis =(BlockLog.EnumAxis)state.getProperties().get(foundProp);
			if(Axis == EnumFallAxis.X) {
				if(currAxis == EnumAxis.Y) {
					world.setBlockState(pos, state.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Z));
				}else if(currAxis == EnumAxis.Z) {
					world.setBlockState(pos, state.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y));
				}
			} else {
				if(currAxis == EnumAxis.Y) {
					world.setBlockState(pos, state.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.X));
				}else if(currAxis == EnumAxis.X) {
					world.setBlockState(pos, state.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y));
				}
			}
		}
		private void Drop(World world, BlockPos pos, BlockPos newPos, Boolean UseSolid) {
			IBlockState state = world.getBlockState(pos);
			if(!UseSolid) {
				RotateLog(world,pos,fallZ == 0?EnumFallAxis.Z:EnumFallAxis.X);
				EntityFallingBlock fallingBlock = new EntityFallingBlock(world, newPos.getX() + 0.5, newPos.getY() + 0.5,
						newPos.getZ() + 0.5, state);
				fallingBlock.setEntityBoundingBox(new AxisAlignedBB(newPos.add(0, 0, 0), newPos.add(1, 1, 1)));
				fallingBlock.fallTime = 1;
				//world.spawnEntityInWorld(fallingBlock);
				world.setBlockState(pos, Blocks.AIR.getDefaultState());		
				world.setBlockState(newPos, state);			
				Thread.yield();
				
			} else {
				
			}
			
			
		}

		private double PlaneDistance(BlockPos pos1, BlockPos pos2) {
			int diffX = Math.abs(pos1.getX() - pos2.getX());
			int diffZ = Math.abs(pos1.getZ() - pos2.getZ());
			return Math.floor(Math.sqrt((Math.pow(diffX, 2) + Math.pow(diffZ, 2))));
		}

		public static final Boolean OnSolid(BlockPos pos, World world) {
			Boolean log = true;
			while (log) {
				pos = pos.add(0, -1, 0);
				IBlockState yState = world.getBlockState(pos);
				if (!yState.getBlock().isWood(world, pos)) {
					log = false;
					if (!isDraggable(world, pos)) {
						return true;
					}
				}
			}
			return false;
		}

		private static boolean cantDrag(World world, BlockPos pos) {
			if (!isDraggable(world, pos.add(1, 0, 0)) || !isDraggable(world, pos.add(-1, 0, 0))
					|| !isDraggable(world, pos.add(0, 1, 0)) || !isDraggable(world, pos.add(0, -1, 0))
					|| !isDraggable(world, pos.add(0, 0, 1)) || !isDraggable(world, pos.add(0, 0, -1))) {
				return true;
			}
			return false;
		}

		private static boolean isDraggable(World world, BlockPos pos) {
			IBlockState state = world.getBlockState(pos);
			return state.getBlock().isWood(world, pos) || state.getBlock().isLeaves(state, world, pos)
					|| state.getBlock().isAir(state, world, pos) || state.getBlock().isPassable(world, pos);
		}

		private void GetPossibleTree() {
			while (!queue.isEmpty()) {
				BlockPos blockStep = queue.pollFirst();

				for (int dy = -1; dy <= 1; ++dy) {
					for (int dx = -1; dx <= 1; ++dx) {
						for (int dz = -1; dz <= 1; ++dz) {
							BlockPos inspectPos = blockStep.add(dx, dy, dz);
							int leafStep = used.get(blockStep);

							if (inspectPos.getY() < base.getY() || leafStep == leafLimit
									|| PlaneDistance(base, inspectPos) > radius || inspectPos.compareTo(base) == 0) {
								continue;
							}
							IBlockState nState = world.getBlockState(inspectPos);
							boolean log = nState.getBlock().isWood(world, inspectPos);
							boolean leaves = nState.getBlock().isLeaves(nState, world, inspectPos);
							// If not directly connected to the tree search down for a base
							if (log && (leafStep > 0 || dy < 0) && !HasEstimateBlock(inspectPos)) {

								// Its the trunk of another tree, check to see if we already have this tree in
								// the list, or add it.
								if (OnSolid(inspectPos, world)) {
									if (main) {
										Boolean treeFound = false;
										for (Tree tree : lOtherTrees) {
											if (tree.GetEstimateBlock(inspectPos) == 0) {
												treeFound = true;
											}
										}
										if (!treeFound) {
											Tree otherTree = new Tree(inspectPos, world);
											lOtherTrees.add(otherTree);
										}
									}
									continue;
								}
							}

							if (log || leaves) {
								leafStep = leafStep + (leaves ? 1 : 0);
								if (!used.containsKey(inspectPos) || used.get(inspectPos) > leafStep) {
									Boolean yMatch = inspectPos.getY() == base.getY();
									Boolean logAbove = world.getBlockState(inspectPos.add(0, 1, 0)).getBlock()
											.isWood(world, inspectPos.add(0, 1, 0));
									// if a log but next to a solid none tree block then fail to chop (avoids 99% of
									// cases of issues building with logs in houses)
									if (log && ((cantDrag(world, inspectPos) && !yMatch) || (yMatch && logAbove))
											&& leafStep == 0) {
										estimatedTree = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>();
										return;
									}
									if (!yMatch || !cantDrag(world, inspectPos)) {
										AddEstimateBlock(inspectPos, leafStep);
									}
								}
							}
						}
					}
				}
			}
		}

		public static boolean IsWood(BlockPos pos, World world) {
			return world.getBlockState(pos).getBlock().isWood(world, pos);
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
					for (ItemStack item : leaves.getDrops(worldObj, getPosition(), state, 0)) {
						super.entityDropItem(item, offsetY);
					}
					return null;
				}
				return super.entityDropItem(stack, offsetY);
			}
		}
	}

	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent event) {
		World world = event.getWorld();
		BlockPos pos = event.getPos();
		if (!Tree.IsWood(pos, world) || !Tree.OnSolid(pos, world) || !Tree.IsWood(pos.add(0, 1, 0), world)
				|| !Tree.IsWood(pos.add(0, 2, 0), world)) {
			return;
		}
		Tree tree = new Tree(pos, world, event.getPlayer());
		tree.Fell();

	}

}
