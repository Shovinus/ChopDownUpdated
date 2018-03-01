package com.shovinus.chopdown;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import javax.annotation.Nullable;

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
		private enum EnumFallAxis {
			X, Z
		};

		private enum DirectionSort {
			NORTH, SOUTH, UP, DOWN, EAST, WEST
		}

		BlockPos base;
		World world;
		public EntityPlayer player;
		Boolean main = false;
		Long tick = (long) 0;
		LinkedList<BlockPos> queue = new LinkedList<BlockPos>();

		HashMap<BlockPos, Integer> estimatedTree = new HashMap<BlockPos, Integer>();
		LinkedList<BlockPos> estimatedTreeQueue = new LinkedList<BlockPos>();
		LinkedList<BlockPos> realisticTree = new LinkedList<BlockPos>();
		HashMap<BlockPos, MovePair> fallingBlocks = new HashMap<BlockPos, MovePair>();
		LinkedList<BlockPos> sortFallingBlocksList;
		LinkedList<BlockPos> dropfallingBlocksList;
		int fallX = 1;
		int fallZ = 1;
		int radius = 8;
		int leafLimit = 7;

		boolean wentUp = false;
		public Boolean finishedEstimate = false;
		public Boolean finishedRealistic = false;
		public Boolean finishedMoves = false;
		public Boolean finishedFell = false;

		public int iNumberOfBlockProcesses;
		int iResetNumberOfBlockProcesses = 9000;
		LinkedList<Tree> lOtherTrees = new LinkedList<Tree>();

		private void BuildTree(BlockPos pos, World world) {
			base = pos;
			this.world = world;
			addEstimateBlock(base, 0);
			tick = System.nanoTime();
			getPossibleTree();
		}

		public Tree(BlockPos pos, World world, int iRemainingBlockOps) {
			iNumberOfBlockProcesses = iRemainingBlockOps;
			BuildTree(pos, world);

		}

		public Tree(BlockPos pos, World world, EntityPlayer player) {
			main = true;
			iNumberOfBlockProcesses = iResetNumberOfBlockProcesses;
			this.player = player;
			BuildTree(pos, world);
			getFallDirection(player);
			fell();
		}

		public void ContinueFelling() {
			iNumberOfBlockProcesses = iResetNumberOfBlockProcesses;
			continueBuildingOtherTrees();
			getPossibleTree();
			fell();
		}

		public int ContinueEstimating(int iNumberOfBlockProcesses) {
			this.iNumberOfBlockProcesses = iNumberOfBlockProcesses;
			getPossibleTree();
			return this.iNumberOfBlockProcesses;
		}

		private void continueBuildingOtherTrees() {
			for (Tree tree : lOtherTrees) {
				if (!tree.finishedEstimate) {
					iNumberOfBlockProcesses = tree.ContinueEstimating(iNumberOfBlockProcesses);
				}
			}
		}

		private void getFallDirection(EntityPlayer player) {
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

		public class QueueComparer implements Comparator<BlockPos> {
			private HashMap<BlockPos, Integer> map;

			public QueueComparer(HashMap<BlockPos, Integer> map) {
				this.map = map;
			}

			@Override
			public int compare(BlockPos x, BlockPos y) {

				int a = map.get(x);
				int b = map.get(y);
				return a < b ? -1 : a > b ? 1 : 0;

			}
		}

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

		private void getPossibleTree() {
			QueueComparer comp = new QueueComparer(estimatedTree);
			while (!queue.isEmpty()) {
				Collections.sort(queue, comp);
				BlockPos blockStep = queue.pollFirst();	
				for (int dy = -1; dy <= 1; ++dy) {
					for (int dx = -1; dx <= 1; ++dx) {
						for (int dz = -1; dz <= 1; ++dz) {							
							int dzA = dz * dz, dxA = dx * dx, dyA = dy * dy;
							int stepInc = (dzA + dxA + dyA);
							BlockPos inspectPos = blockStep.add(dx, dy, dz);
							boolean log = isWood(inspectPos);
							boolean leaves = isLeaves(inspectPos);
							boolean logAbove = isWood(inspectPos.add(0, 1, 0));
							int y = inspectPos.getY();
							boolean onSolid = onSolid(inspectPos, world);
							Boolean yMatch = (y == base.getY());
							if(y > base.getY()) {
								wentUp = true;
							}
							Integer leafStep = estimatedTree.get(blockStep);
							if (leafStep == null) {
								leafStep = 0;
							}

							leafStep = leafStep + (leaves ? stepInc : 0);
							// Don't chop below the chop point, nor if this is the base point, nor if
							// leafStep reached, nor if radius limit reaches, nor if this block is our main
							// block
							if (!(log || leaves) || inspectPos.compareTo(base) == 0 || y < base.getY()
									|| leafStep >= leafLimit || horizontalDistance(base, inspectPos) > radius

							) {
								continue;
							}

							// If not directly connected to the tree search down for a base
							if (log && (leafStep > 0 || dy < 0) && !estimatedTree.containsKey(inspectPos)
									&& onSolid
									&& !(inspectPos.getX() == base.getX() && inspectPos.getZ() == base.getZ())) {
								// Its the trunk of another tree, check to see if we already have this tree in
								// the list, or add it.
								if (main) {
									Boolean treeFound = false;
									for (Tree tree : lOtherTrees) {
										if (tree.getEstimate(inspectPos) != null && tree.getEstimate(inspectPos) == 0) {
											treeFound = true;
										}
									}
									if (!treeFound) {
										Tree otherTree = new Tree(inspectPos, world, this.iNumberOfBlockProcesses);
										this.iNumberOfBlockProcesses = otherTree.iNumberOfBlockProcesses;
										lOtherTrees.add(otherTree);
									}
								}
								continue;
							}

							// if a log but next to a solid none tree block then fail to chop (avoids 99% of
							// cases of issues building with logs in houses)
							if (log && ((cantDrag(world, inspectPos) && !yMatch) || (yMatch && logAbove && onSolid && !wentUp))
									&& leafStep == 0) {
								estimatedTree.clear();
								queue.clear();
								finishedEstimate = true;
								return;
							}
							if (!yMatch || !cantDrag(world, inspectPos)) {
								addEstimateBlock(inspectPos, leafStep);
							} else {
								continue;
							}

						}
					}
				}
				if (shouldReturn(27)) {
					return;
				}
			}
			if (queue.isEmpty() && !finishedEstimate) {
				estimatedTreeQueue = new LinkedList<BlockPos>(estimatedTree.keySet());
				finishedEstimate = true;
			}
		}

		public void fell() {
			if (!finishedEstimate) {
				return;
			}
			getRealisticTree();
			if (!finishedRealistic) {
				return;
			}
			while (!realisticTree.isEmpty()) {
				BlockPos blockPos = realisticTree.pollFirst();
				IBlockState state2 = world.getBlockState(blockPos);
				Boolean leaves = state2.getBlock().isLeaves(state2, world, blockPos);
				BlockPos to = repositionBlock(blockPos);
				MovePair pair = new MovePair(rotateLog(world, state2, fallZ == 0 ? EnumFallAxis.Z : EnumFallAxis.X), to,
						leaves);
				fallingBlocks.put(pair.to, pair);
				world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
				if (shouldReturn(1)) {
					return;
				}
			}
			if (realisticTree.isEmpty() && dropfallingBlocksList == null) {
				dropfallingBlocksList = new LinkedList<BlockPos>(fallingBlocks.keySet());
				Collections.sort(dropfallingBlocksList, new AxisComparer(DirectionSort.UP));
				sortFallingBlocksList = new LinkedList<BlockPos>(dropfallingBlocksList);
			}
			while (!sortFallingBlocksList.isEmpty()) {
				BlockPos pos = sortFallingBlocksList.pollFirst();
				MovePair pair = fallingBlocks.get(pos);
				int moves = 1;
				if (!pair.leaves) {
					Boolean movedBlock = true;
					while (movedBlock) {
						moves++;
						movedBlock = false;
						MovePair lowerPair = getLowerTargetBlock(pair.to);
						if (lowerPair != null && lowerPair.leaves) {
							BlockPos upperBlock = pair.to;
							pair.to = lowerPair.to;
							lowerPair.to = upperBlock;
							fallingBlocks.put(pair.to, pair);
							fallingBlocks.put(lowerPair.to, lowerPair);
							movedBlock = true;
						}
					}
				}
				if (shouldReturn(moves)) {
					return;
				}
			}
			while (!dropfallingBlocksList.isEmpty()) {
				BlockPos pos = dropfallingBlocksList.pollFirst();
				MovePair pair = fallingBlocks.get(pos);
				drop(world, pair.from, pair.to, fallingBlocks.size() > 1000);
				if (shouldReturn(1)) {
					return;
				}
			}
			this.finishedFell = true;
		}

		private Boolean shouldReturn(int moves) {
			iNumberOfBlockProcesses -= moves;
			return iNumberOfBlockProcesses <= 0;
		}

		private BlockPos repositionBlock(BlockPos pos) {
			int y = pos.getY() - base.getY();

			int x = pos.getX() - base.getX();
			int z = pos.getZ() - base.getZ();

			int changeX = fallZ * z;
			int changeZ = fallX * x;

			int normPosX = (y * fallX);
			int normPosZ = (y * fallZ);

			return pos.add(normPosX - (changeZ * fallX), -(changeX + changeZ) + 1, normPosZ - (changeX * fallZ));
		}

		private MovePair getLowerTargetBlock(BlockPos pos) {
			BlockPos lower = pos.add(0, -1, 0);
			if (fallingBlocks.containsKey(lower)) {
				return fallingBlocks.get(lower);
			}
			return null;
		}

		private class MovePair {
			public BlockPos to;
			public IBlockState from;
			public Boolean leaves;

			public MovePair(IBlockState from, BlockPos to, Boolean leaves) {
				this.from = from;
				this.to = to;
				this.leaves = leaves;
			}
		}

		public void addEstimateBlock(BlockPos pos, int step) {
			if (estimatedTree.containsKey(pos) && estimatedTree.get(pos) <= step) {
				return;
			}
			queue.add(pos);
			estimatedTree.put(pos, step);
		}

		public Integer getEstimate(BlockPos pos) {
			return estimatedTree.get(pos);
		}

		public void getRealisticTree() {
			while (!estimatedTreeQueue.isEmpty()) {
				if (shouldReturn(1)) {
					return;
				}
				BlockPos myBlock = estimatedTreeQueue.pollFirst();
				Boolean mine = true;
				for (Tree otherTree : lOtherTrees) {
					if (otherTree.myBlock(myBlock, horizontalDistance(base, myBlock), estimatedTree.get(myBlock))) {
						mine = false;
						break;
					}
				}
				if (mine && base != myBlock) {
					realisticTree.add(myBlock);
				}

			}
			finishedRealistic = true;
		}

		// Is the block closer to my trunk or yours?
		public Boolean myBlock(BlockPos pos, double yourDistance, int yourStepValue) {
			// TODO check if block type matches main types

			Integer step = estimatedTree.get(pos);
			if (step == null || step > yourStepValue) {
				return false;
			}
			if (step == yourStepValue) {
				return horizontalDistance(base, pos) < yourDistance;
			}
			return true;
		}

		private Boolean isAxis(IBlockState state, IProperty<?> property, String axis) {
			return ((Enum<?>) (state.getProperties().get(property))).name().equalsIgnoreCase(axis);
		}

		private IBlockState setAxis(IBlockState state, IProperty<?> property, String axis) {
			int i = 10;
			while (i > 0 && !isAxis(state, property, axis)) {
				i--;
				state = state.cycleProperty(property);
			}
			return state;
		}

		private IBlockState rotateLog(World world, IBlockState state, EnumFallAxis Axis) {
			IProperty<?> foundProp = null;
			for (net.minecraft.block.properties.IProperty<?> prop : state.getProperties().keySet()) {
				if (prop.getName().equals("axis")) {
					foundProp = prop;
				}
			}
			if (foundProp == null) {
				return state;
			}
			if (Axis == EnumFallAxis.X) {
				if (isAxis(state, foundProp, "Y")) {
					state = setAxis(state, foundProp, "Z");
				} else if (isAxis(state, foundProp, "Z")) {
					state = setAxis(state, foundProp, "Y");
				}
			} else {
				if (isAxis(state, foundProp, "Y")) {
					state = setAxis(state, foundProp, "X");
				} else if (isAxis(state, foundProp, "X")) {
					state = setAxis(state, foundProp, "Y");
				}
			}
			return state;
		}

		private void drop(World world, IBlockState state, BlockPos newPos, Boolean UseSolid) {
			if (!UseSolid || !isAir(newPos)) {
				EntityFallingBlock fallingBlock = new EntityFallingBlock(world, newPos.getX() + 0.5,
						newPos.getY() + 0.5, newPos.getZ() + 0.5, state);
				fallingBlock.setEntityBoundingBox(new AxisAlignedBB(newPos.add(0, 0, 0), newPos.add(1, 1, 1)));
				fallingBlock.fallTime = 1;
				world.spawnEntityInWorld(fallingBlock);
			} else {
				while (isAir(newPos.add(0, -1, 0)) && newPos.add(0, -1, 0).getY() > 0) {
					newPos = newPos.add(0, -1, 0);
				}
				world.setBlockState(newPos, state);
			}
		}

		private double horizontalDistance(BlockPos pos1, BlockPos pos2) {
			int diffX = Math.abs(pos1.getX() - pos2.getX());
			int diffZ = Math.abs(pos1.getZ() - pos2.getZ());
			return Math.floor(Math.sqrt((Math.pow(diffX, 2) + Math.pow(diffZ, 2))));
		}

		public static final Boolean onSolid(BlockPos pos, World world) {
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

		private Boolean isAir(BlockPos pos) {
			return world.getBlockState(pos).getBlock().isAir(world.getBlockState(pos), world, pos);
		}

		public boolean isWood(BlockPos pos) {
			return isWood(pos, world);
		}

		public static boolean isWood(BlockPos pos, World world) {
			return world.getBlockState(pos).getBlock().isWood(world, pos);
		}

		public boolean isLeaves(BlockPos pos) {
			return world.getBlockState(pos).getBlock().isLeaves(world.getBlockState(pos), world, pos);
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
		if (!Tree.isWood(pos, world) || !Tree.onSolid(pos, world) || !Tree.isWood(pos.add(0, 1, 0), world)
				|| !Tree.isWood(pos.add(0, 2, 0), world)) {
			return;
		}
		for (Tree tree : FallingTrees) {
			if (tree.player == event.getPlayer()) {
				tree.player.addChatComponentMessage(new TextComponentString("Still chopping down the last tree"));
				event.setCanceled(true);
				return;
			}
		}
		Tree tree = new Tree(pos, world, event.getPlayer());
		if (!tree.finishedFell) {
			FallingTrees.add(tree);
		}
	}

	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		try {
			for (Tree tree : FallingTrees) {
				if (tree.finishedFell) {
					FallingTrees.remove(tree);
					continue;
				}
				tree.ContinueFelling();
			}
		} catch (Exception ex) {
			System.out.println("Error while continuing to chop trees");
		}
	}
}
