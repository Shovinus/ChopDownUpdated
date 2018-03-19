package com.shovinus.chopdownupdated.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import javax.annotation.Nullable;

import com.shovinus.chopdownupdated.config.TreeConfiguration;
import com.shovinus.chopdownupdated.config.Config;
import com.shovinus.chopdownupdated.config.PersonalConfig;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Tree implements Runnable {

	BlockPos base;
	World world;
	public EntityPlayer player;
	Boolean main = false;
	LinkedList<BlockPos> queue = new LinkedList<BlockPos>();

	HashMap<BlockPos, Integer> estimatedTree = new HashMap<BlockPos, Integer>();
	LinkedList<BlockPos> estimatedTreeQueue = new LinkedList<BlockPos>();

	LinkedList<BlockPos> realisticTree = new LinkedList<BlockPos>();

	HashMap<BlockPos, TreeMovePair> fallingBlocks = new HashMap<BlockPos, TreeMovePair>();

	LinkedList<BlockPos> fallingBlocksList;

	int fallX = 1;
	int fallZ = 0;
	int fallOffset = 0;

	EnumFallAxis axis = EnumFallAxis.X;

	TreeConfiguration config;

	int radius = 8;
	int leafLimit = 7;

	boolean wentUp = false;

	public Boolean finishedCalculation = false;
	public Boolean failedToBuild = false;

	LinkedList<Tree> nearbyTrees = new LinkedList<Tree>();

	/*
	 * Get a tree estimate, used in forests to calculate if leaves should belong to
	 * this tree or the tree we are chopping down
	 */
	public Tree(BlockPos pos, World world) throws Exception {
		initTree(pos, world);
		getPossibleTree();
	}

	/*
	 * Add a tree that can be chopped down, this is one we are targeting to chop as
	 * opposed to one we just want to get an estimate of blocks from
	 */
	public Tree(BlockPos pos, World world, EntityPlayer player) throws Exception {
		main = true;
		this.player = player;
		initTree(pos, world);
		getFallDirection(player);
	}

	public static TreeConfiguration findConfig(World world, BlockPos pos) {
		for (TreeConfiguration treeConfig : Config.treeConfigurations) {
			if (treeConfig.matches(blockName(pos, world))) {
				return treeConfig;
			}
		}
		return null;
	}

	/*
	 * Setup the basic settings of the tree
	 */
	private void initTree(BlockPos pos, World world) throws Exception {
		base = pos;
		this.world = world;
		addEstimateBlock(base, 0);
		this.config = findConfig(world, pos);
		if (this.config == null) {
			System.out.println(blockName(base, world) + " block has no tree configuration");
			throw new Exception("The chopped log type is unknown and not setup");
		}
		this.radius = this.config.Radius();
		this.leafLimit = this.config.Leaf_limit();

	}

	/*
	 * Calculate which direction the tree should fall in
	 */
	private void getFallDirection(EntityPlayer player) {
		Double x = ((base.getX() + 0.5) - player.posX);
		Double z = (base.getZ() + 0.5) - player.posZ;
		Double abX = Math.abs(x);
		Double abZ = Math.abs(z);
		fallX = (int) Math.floor(abX / x);
		fallZ = (int) Math.floor(abZ / z);
		if (abX > abZ) {
			fallZ = 0;
			axis = EnumFallAxis.Z;
		} else {
			fallX = 0;
			axis = EnumFallAxis.X;
		}
	}

	/*
	 * Gets a possible tree, but only if it thinks the trunk is completely cut
	 * through
	 */
	private void getPossibleTree() throws Exception {
		BuilderQueueComparer comp = new BuilderQueueComparer(estimatedTree);
		try {
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
						if (!(log || leaves)) {
							continue;
						}

						boolean logAbove = isWood(inspectPos.add(0, 1, 0));
						int y = inspectPos.getY();
						boolean isTrunk = isTrunk(inspectPos, world, config);
						Boolean yMatch = (y == base.getY());
						if (y > base.getY()) {
							wentUp = true;
						}
						Integer leafStep = getEstimate(blockStep);
						if (leafStep == null) {
							leafStep = 0;
						}

						leafStep = leafStep + (leaves ? stepInc : 0);

						// Don't chop below the chop point, nor if this is the base point, nor if
						// leafStep reached, nor if radius limit reaches, nor if this block is our main
						// block
						if (!(log || leaves) || inspectPos.compareTo(base) == 0 || y < base.getY()
								|| leafStep >= leafLimit || horizontalDistance(base, inspectPos) > radius
								|| !config.matches(blockName(inspectPos, world))) {
							continue;
						}
						// If not directly connected to the tree search down for a base
						if (log && (leafStep > 0 || dy < 0) && !estimatedTree.containsKey(inspectPos) && isTrunk
								&& (Math.abs(inspectPos.getX() - base.getX()) > 1
										|| Math.abs(inspectPos.getZ() - base.getZ()) > 1)

						) {
							// Its the trunk of another tree, check to see if we already have this tree in
							// the list, or add it.
							if (main) {
								Boolean treeFound = false;
								for (Tree tree : nearbyTrees) {
									if (tree.getEstimate(inspectPos) != null && tree.getEstimate(inspectPos) == 0) {
										treeFound = true;
									}
								}
								if (!treeFound) {
									Tree otherTree = new Tree(inspectPos, world);
									nearbyTrees.add(otherTree);
								}
							}
							continue;
						}

						/*
						 * If a log but next to a solid none tree block then fail to chop (avoids 99% of
						 * cases of issues building with logs in houses)
						 * 
						 */
						if (main && log && ((cantDrag(world, inspectPos) && !yMatch)
								|| (yMatch && logAbove && isTrunk && !wentUp)) && leafStep == 0) {
							estimatedTree.clear();
							queue.clear();
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
		}
		} catch (Exception ex) {
			throw ex;
		}
		
	}

	/*
	 * The overall calculation of where the tree should end up, does not actually
	 * drop the blocks, just creates a list of movements needed to be done
	 */
	public void getDropBlocks() throws Exception {
		getPossibleTree();
		getRealisticTree();
		if (Config.lowerLogs) {
			lowerLogs();
		}
		this.finishedCalculation = true;
	}

	/*
	 * Calculate where this block should end up in our fallen tree (pre log in leaf
	 * fall)
	 */
	private BlockPos repositionBlock(BlockPos pos) {
		int y = pos.getY() - base.getY();

		int x = pos.getX() - (base.getX() + fallOffset);
		int z = pos.getZ() - (base.getZ() + fallOffset);

		int changeX = fallZ * z;
		int changeZ = fallX * x;

		int normPosX = (y * fallX);
		int normPosZ = (y * fallZ);

		return pos.add(normPosX - (changeZ * fallX), -(changeX + changeZ), normPosZ - (changeX * fallZ));
	}

	/*
	 * Gets the block that would end up in the position below this one
	 */
	private TreeMovePair getLowerTargetBlock(BlockPos pos) {
		BlockPos lower = pos.add(0, -1, 0);
		if (fallingBlocks.containsKey(lower)) {
			return fallingBlocks.get(lower);
		}
		return null;
	}

	/*
	 * Adds a block to the queue unless the queue already processed the block with
	 * this step value and its not still pending in the queue. Updates the blocks
	 * step value if it is lower than the currently stored value.
	 */
	public void addEstimateBlock(BlockPos pos, int step) {
		if (estimatedTree.containsKey(pos) && estimatedTree.get(pos) <= step) {
			return;
		}
		if (!queue.contains(pos)) {
			queue.add(pos);
		}
		estimatedTree.put(pos, step);
	}

	/*
	 * Get the leaf step value from an estimated tree block
	 */
	private Integer getEstimate(BlockPos pos) {
		return estimatedTree.get(pos);
	}

	@SuppressWarnings("deprecation")
	public static String blockName(BlockPos pos, World world) {
		ItemStack stack = world.getBlockState(pos).getBlock().getItem(world, pos, world.getBlockState(pos));
		ResourceLocation loc = stack.getItem().getRegistryName();
		int damageValue = stack.getItem().getDamage(stack);
		return loc.getResourceDomain() + ":" + loc.getResourcePath() + ":" + String.valueOf(damageValue);
	}

	/*
	 * Checks the blocks in the estimated tree against other trees that were found
	 * to determine if the block more likely belongs to this tree or another
	 */
	private void getRealisticTree() {
		estimatedTreeQueue = new LinkedList<BlockPos>(estimatedTree.keySet());
		LinkedList<BlockPos> realisticTree = new LinkedList<BlockPos>();
		while (!estimatedTreeQueue.isEmpty()) {
			BlockPos from = estimatedTreeQueue.pollFirst();
			Boolean mine = true;
			int leafStep = estimatedTree.get(from);
			double distance = horizontalDistance(base, from);
			if (distance > config.Radius() || leafStep >= config.Leaf_limit()) {
				continue;
			}
			for (Tree otherTree : nearbyTrees) {
				if (otherTree.myBlock(from, distance, leafStep)) {
					mine = false;
					break;
				}
			}
			if (mine && base != from) {
				if (isWood(from) && (from.getY() == base.getY() + 1 || from.getY() == base.getY() + 2)
						&& ((fallZ != 0 && (isWood(from.add(1, 0, 0)) || isWood(from.add(-1, 0, 0))))
								|| (fallX != 0 && (isWood(from.add(0, 0, 1)) || isWood(from.add(0, 0, -1)))))) {
					if (from.getX() * fallX > (fallOffset + base.getX()) * fallX) {
						fallOffset = from.getX() - base.getX();
					} else if (from.getZ() * fallZ > (fallOffset + base.getZ()) * fallZ) {
						fallOffset = from.getZ() - base.getZ();
					}
				}
				realisticTree.add(from);
			}
		}
		while (!realisticTree.isEmpty()) {
			BlockPos from = realisticTree.pollFirst();
			IBlockState state2 = world.getBlockState(from);
			Boolean leaves = state2.getBlock().isLeaves(state2, world, from);
			BlockPos to = repositionBlock(from);
			TreeMovePair pair = new TreeMovePair(from, to, leaves);
			fallingBlocks.put(pair.to, pair);
		}
		fallingBlocksList = new LinkedList<BlockPos>(fallingBlocks.keySet());
		Collections.sort(fallingBlocksList, new AxisComparer(DirectionSort.UP));
	}

	@Override
	public void run() {
		try {
			this.getDropBlocks();
		} catch (Exception e) {
			this.failedToBuild = true;
		}
	}

	/*
	 * Iterates through blocks waiting to drop
	 */
	public boolean dropBlocks() {
		int blocksRemaining = Config.maxDropsPerTickPerTree;
		BlockPos pos;
		while ((pos = fallingBlocksList.pollFirst()) != null) {
			TreeMovePair pair = fallingBlocks.get(pos);
			drop(world, pair.from, pair.to, fallingBlocks.size() > Config.maxFallingBlockBeforeManualMove);
			blocksRemaining--;
			if (blocksRemaining <= 0 && !fallingBlocksList.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Swaps logs with leaves below, creates a better looking tree fall, also
	 * removes some leaves so it looks more like a fallen tree would
	 */
	private void lowerLogs() {
		for (BlockPos pos : fallingBlocksList) {
			TreeMovePair pair = fallingBlocks.get(pos);
			if (!pair.leaves) {
				Boolean movedBlock = true;
				while (movedBlock) {
					movedBlock = false;
					TreeMovePair lowerPair = getLowerTargetBlock(pair.to);
					if (lowerPair != null && lowerPair.leaves && pair.to.getY() > base.getY()
							&& ((isAir(lowerPair.to) || isPassable(lowerPair.to))
									|| !(isAir(pair.to) || isPassable(pair.to)))) {
						BlockPos upperBlock = pair.to;
						pair.to = lowerPair.to;
						lowerPair.to = upperBlock;
						fallingBlocks.put(pair.to, pair);
						fallingBlocks.put(lowerPair.to, lowerPair);
						movedBlock = true;
					}
				}
			}
		}
	}

	/*
	 * Is the block more likely to be yours or mine?
	 */
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

	/*
	 * Checks to see if the block is on the given axis
	 */
	private Boolean isAxis(IBlockState state, IProperty<?> property, String axis) {
		return ((Enum<?>) (state.getProperties().get(property))).name().equalsIgnoreCase(axis);
	}

	/*
	 * Sets the blocks axis by iterating through the property values.
	 */
	private IBlockState setAxis(IBlockState state, IProperty<?> property, String axis) {
		int i = 10;
		while (i > 0 && !isAxis(state, property, axis)) {
			i--;
			state = state.cycleProperty(property);
		}
		return state;
	}

	/*
	 * Trys to rotate the log along the axis given
	 */
	private IBlockState rotateLog(World world, IBlockState state) {
		IProperty<?> foundProp = null;
		for (net.minecraft.block.properties.IProperty<?> prop : state.getProperties().keySet()) {
			if (prop.getName().equals("axis")) {
				foundProp = prop;
			}
		}
		if (foundProp == null) {
			return state;
		}
		if (axis == EnumFallAxis.X) {
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

	/*
	 * Drops a block in the world (basically moves it if it can, does block drop if
	 * it can't, handles falling entity and calculated drop) Also handles debug
	 * configs
	 */
	private void drop(World world, BlockPos pos, BlockPos newPos, Boolean UseSolid) {
		if (!(isWood(pos) || isLeaves(pos))) {
			return;
		}
		PersonalConfig playerConfig = Config.getPlayerConfig(player.getUniqueID());
		if (playerConfig.makeGlass) {
			if (isWood(pos)) {
				world.setBlockState(pos, Blocks.STAINED_GLASS.getStateFromMeta(1));
			} else {
				world.setBlockState(pos, Blocks.STAINED_GLASS.getStateFromMeta(2));
			}
			if (playerConfig.dontDrop) {
				return;
			}
		}
		IBlockState state = rotateLog(world, world.getBlockState(pos));
		if (!(isAir(newPos) || isPassable(newPos)) || (isLeaves(pos) && Config.breakLeaves)) {
			// Do drops at location)
			for (ItemStack stacky : state.getBlock().getDrops(world, pos, state, 0)) {
				EntityItem entityitem = new EntityItem(world, player.posX, player.posY, player.posZ, stacky);
				entityitem.setDefaultPickupDelay();
				world.spawnEntity(entityitem);
			}
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
			return;
		}
		world.setBlockState(pos, Blocks.AIR.getDefaultState());
		if (playerConfig.dontDrop) {
			world.setBlockState(newPos, state);
		} else {
			if (!UseSolid) {
				EntityFallingBlock fallingBlock = new EntityFallingBlock(world, newPos.getX() + 0.5,
						newPos.getY() + 0.5, newPos.getZ() + 0.5, state);
				fallingBlock.setEntityBoundingBox(new AxisAlignedBB(newPos.add(0, 0, 0), newPos.add(1, 1, 1)));
				fallingBlock.fallTime = 1;
				world.spawnEntity(fallingBlock);
			} else {

				while (isAir(newPos.add(0, -1, 0)) && newPos.add(0, -1, 0).getY() > 0) {
					newPos = newPos.add(0, -1, 0);
				}
				world.setBlockState(newPos, state);
			}
		}
	}

	/*
	 * Gets the distance on the x-z plane only
	 */
	private double horizontalDistance(BlockPos pos1, BlockPos pos2) {
		int diffX = Math.abs(pos1.getX() - pos2.getX());
		int diffZ = Math.abs(pos1.getZ() - pos2.getZ());
		return Math.floor(Math.sqrt((Math.pow(diffX, 2) + Math.pow(diffZ, 2))));
	}

	/*
	 * If min vertical logs is 0 it only checks for the log being on a solid block,
	 * otherwise it also checks the log is vertically surrounded by the given number of
	 * blocks, this is useful for some BOP trees that have hollow centres or that
	 * get built floating in water.
	 */
	public static final Boolean isTrunk(BlockPos pos, World world, TreeConfiguration config) {

		// Normal tree check, requires the tree to be sat on a solid block
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

		if (config.Min_vertical_logs() == 0) {
			return false;
		} else {
			// Instead check for at least 4 vertical log blocks above and below
			int below = 0;
			for (int i = 1; i < config.Min_vertical_logs(); i++) {
				if (!isWood(pos.add(0, -i, 0), world)) {
					break;
				}
				below++;
			}
			int above = 0;
			for (int i = 1; i < config.Min_vertical_logs(); i++) {
				if (!isWood(pos.add(0, i, 0), world)) {
					break;
				}
				above++;
			}
			return (1 + below + above) >= config.Min_vertical_logs();
		}
	}

	/*
	 * Is the block touching either air, a tree block or a passable block only on
	 * all 6 sides
	 */
	private static boolean cantDrag(World world, BlockPos pos) {
		if (!isDraggable(world, pos.add(1, 0, 0)) || !isDraggable(world, pos.add(-1, 0, 0))
				|| !isDraggable(world, pos.add(0, 1, 0)) || !isDraggable(world, pos.add(0, -1, 0))
				|| !isDraggable(world, pos.add(0, 0, 1)) || !isDraggable(world, pos.add(0, 0, -1))) {
			return true;
		}
		return false;
	}

	/*
	 * Is this specific block either a tree block, air or a passable block
	 */
	private static boolean isDraggable(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		return state.getBlock().isWood(world, pos) || state.getBlock().isLeaves(state, world, pos)
				|| state.getBlock().isAir(state, world, pos) || state.getBlock().isPassable(world, pos);
	}

	/*
	 * Is the block at this position an air block;
	 */
	private Boolean isAir(BlockPos pos) {
		return world.getBlockState(pos).getBlock().isAir(world.getBlockState(pos), world, pos);
	}

	/*
	 * /* Is the block at this position an air block;
	 */
	private Boolean isPassable(BlockPos pos) {
		return world.getBlockState(pos).getBlock().isPassable(world, pos);
	}

	/*
	 * Is the block at this position a log
	 */
	public boolean isWood(BlockPos pos) {
		return isWood(pos, world);
	}

	/*
	 * Is the block at this position a log
	 */
	public static boolean isWood(BlockPos pos, World world) {
		return world.getBlockState(pos).getBlock().isWood(world, pos);
	}

	/*
	 * Is the block at this position a log
	 */
	public static boolean isLeaves(BlockPos pos, World world) {
		return world.getBlockState(pos).getBlock().isLeaves(world.getBlockState(pos), world, pos);
	}

	/*
	 * Is the block at this position leaves
	 */
	public boolean isLeaves(BlockPos pos) {
		return world.getBlockState(pos).getBlock().isLeaves(world.getBlockState(pos), world, pos);
	}

	/*
	 * Class to house the falling logs and leaves
	 */
	public static class EntityFallingBlock extends net.minecraft.entity.item.EntityFallingBlock {

		EntityFallingBlock(World worldIn, double x, double y, double z, IBlockState fallingBlockState) {
			super(worldIn, x, y, z, fallingBlockState);
		}

		@Nullable
		@Override
		public EntityItem entityDropItem(ItemStack stack, float offsetY) {
			IBlockState state = getBlock();
			// TODO check if this works for none MC leaves
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