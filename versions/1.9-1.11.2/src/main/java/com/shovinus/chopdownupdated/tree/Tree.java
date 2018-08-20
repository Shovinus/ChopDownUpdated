package com.shovinus.chopdownupdated.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

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
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Tree implements Runnable {

	BlockPos base;
	public World world;
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
		while (isLog(pos.add(0,-1,0))) {
			pos = pos.add(0,-1,0);
		}
		base = pos;
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
			if (treeConfig.isLog(blockName(pos, world))) {
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
	public boolean isLog(BlockPos pos) {		
		return isLog(blockName(pos, world));
	}
	private boolean isLog(String name) {
		return config.isLog(name);
	}
	public boolean isLeaf(BlockPos pos) {		
		return isLeaf(blockName(pos, world));
	}
	private boolean isLeaf(String name) {
		return config.isLeaf(name);
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
							String blockName =	blockName(inspectPos, world);
							
						
							boolean log = isLog(blockName);
							boolean leaf = false;
							if(!log) {
								leaf= isLeaf(blockName);
							}
							if(!(log || leaf)) {
								continue;
							}
							
							boolean logAbove = isLog(inspectPos.add(0, 1, 0));
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

							leafStep = leafStep + (leaf ? stepInc : 0);

							// Don't chop below the chop point, nor if this is the base point, nor if
							// leafStep reached, nor if radius limit reaches, nor if this block is our main
							// block
							if (inspectPos.compareTo(base) == 0 || y < base.getY()
									|| leafStep >= leafLimit || horizontalDistance(base, inspectPos) > radius) {
								continue;
							}
							// If not directly connected to the tree search down for a base
							if (log && (leafStep > 0 || dy < 0) && !estimatedTree.containsKey(inspectPos) && isTrunk
									&& (Math.abs(inspectPos.getX() - base.getX()) > config.Trunk_Radius()
											|| Math.abs(inspectPos.getZ() - base.getZ()) > config.Trunk_Radius())

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
							} else if (main && log && (leafStep > 0 || dy < 0) && !estimatedTree.containsKey(inspectPos)
									&& isTrunk && isLog(inspectPos.add(0,1,0))) {
								estimatedTree.clear();
								queue.clear();
								return;
							}

							/*
							 * If a log but next to a solid none tree block then fail to chop (avoids 99% of
							 * cases of issues building with logs in houses)
							 * 
							 */
							if (main && log
									&& ((cantDrag(world, inspectPos,config) && !yMatch) || (yMatch && logAbove && !wentUp))
									&& leafStep == 0) {
								estimatedTree.clear();
								queue.clear();
								return;
							}
							if (!yMatch || !cantDrag(world, inspectPos,config)) {
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
		ItemStack stack = null;
		try {
			stack = world.getBlockState(pos).getBlock().getPickBlock(world.getBlockState(pos), null, world, pos, null);
		} catch (Exception ex){
			try {
			stack = world.getBlockState(pos).getBlock().getItem(world, pos, world.getBlockState(pos));
			}catch(Exception ex2) {			
			}
		}
		if(stack == null) {
			return "unknown, getPickBlock and getItem not set";
		}
		return stackName(stack);
	}
	public static String stackName(ItemStack stack){
		try {
			ResourceLocation loc = stack.getItem().getRegistryName();
			int damageValue = stack.getItem().getDamage(stack);
			return loc.getResourceDomain() + ":" + loc.getResourcePath() + ":" + String.valueOf(damageValue);
		}
		catch (Exception ex) { 
			return "";
		}
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
				if (isLog(from) && (from.getY() == base.getY() + 1 || from.getY() == base.getY() + 2)
						&& ((fallZ != 0 && (isLog(from.add(1, 0, 0)) || isLog(from.add(-1, 0, 0))))
								|| (fallX != 0 && (isLog(from.add(0, 0, 1)) || isLog(from.add(0, 0, -1)))))) {
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
			BlockPos to = repositionBlock(from);
			TreeMovePair pair = new TreeMovePair(from, to, this);
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
		int size = fallingBlocksList.size();
		for (int i = 0; i < size; i++) {
			pos = fallingBlocksList.getFirst();
			TreeMovePair pair = fallingBlocks.get(pos);
			fallingBlocksList.removeFirst();
			if (!drop(pair, fallingBlocks.size() > Config.maxFallingBlockBeforeManualMove)) {
				// not finished moving
				fallingBlocksList.add(pos);
			}
			blocksRemaining--;
			if (blocksRemaining <= 0 && !fallingBlocksList.isEmpty()) {
				return false;
			}
		}
		if(!fallingBlocksList.isEmpty()) {
			return false;
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
	public IBlockState rotateLog(World world, IBlockState state) {
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

	public void dropDrops(BlockPos pos, BlockPos dropPos, IBlockState state) {
		// Do drops at location)
		for (ItemStack stacky : state.getBlock().getDrops(world, pos, state, 0)) {
			EntityItem entityitem = new EntityItem(world, dropPos.getX(), dropPos.getY(), dropPos.getZ(), stacky);
			entityitem.setDefaultPickupDelay();
			world.spawnEntityInWorld(entityitem);
		}
	}

	/*
	 * Drops a block in the world (basically moves it if it can, does block drop if
	 * it can't, handles falling entity and calculated drop) Also handles debug
	 * configs.
	 */
	private boolean drop(TreeMovePair pair, Boolean UseSolid) {
		if (!(isLog(pair.from) || isLeaves(pair.from))) {
			return true;
		}
		PersonalConfig playerConfig = Config.getPlayerConfig(player.getUniqueID());
		// Turn the tree in to glass if set as don't drop;
		if (playerConfig.makeGlass && playerConfig.dontFell) {
			if (isLog(pair.from)) {
				world.setBlockState(pair.from, Blocks.STAINED_GLASS.getStateFromMeta(1));
			} else {
				world.setBlockState(pair.from, Blocks.STAINED_GLASS.getStateFromMeta(2));
			}
			return true;
		}
		// Get the state of the tree block (rotate the log if first time moving)
		IBlockState state = world.getBlockState(pair.from);
		IBlockState originalState = state;
		if (!pair.moved && isLog(pair.from)) {
			state = rotateLog(world, state);
		}
		// If the target block is not passable or the source block is leaves and the
		// config is set to break leaves then do drops and state finished
		if ((!CanMoveTo(pair.to) && !pair.moved) || (isLeaves(pair.from) && Config.breakLeaves)) {
			// Do drops at location
			dropDrops(pair.from, pair.to, state);
			world.setBlockState(pair.from, Blocks.AIR.getDefaultState());
			return true;
		} else if(!CanMoveTo(pair.to)){			
			return true;
		}
		// Can move to this block, set the source block to air, set the from block as to
		// and state that we moved
		world.setBlockState(pair.from, Blocks.AIR.getDefaultState());
		pair.from = pair.to;
		pair.moved = true;

		if (playerConfig.dontFell) {
			pair.move();
		} else {
			if (!UseSolid) {
				if (Config.useFallingEntities) {
					//Use falling entities
					EntityFallingBlock fallingBlock = new EntityFallingBlock(world, pair.to.getX() + 0.5,
							pair.to.getY() + 0.5, pair.to.getZ() + 0.5, state,pair.tile);
					fallingBlock.setEntityBoundingBox(new AxisAlignedBB(pair.to.add(0, 0, 0), pair.to.add(1, 1, 1)));
					fallingBlock.fallTime = 1;
					world.spawnEntityInWorld(fallingBlock);
				} else {
					pair.move();
					pair.to = pair.to.add(0, -1, 0);
					return false;
				}
			} else {
				ManuallyDrop(pair, state);
			}
		}
		return true;
	}

	private void ManuallyDrop(TreeMovePair pair, IBlockState state) {
		// Move large trees to final resting place
		while (CanMoveTo(pair.to.add(0, -1, 0))) {
			pair.to = pair.to.add(0, -1, 0);
		}
		pair.move();
		}

	private boolean CanMoveTo(BlockPos pos) {
		return (isAir(pos) || isPassable(pos)) && pos.getY() > 0;
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
	 * otherwise it also checks the log is vertically surrounded by the given number
	 * of blocks, this is useful for some BOP trees that have hollow centres or that
	 * get built floating in water.
	 */
	public static final Boolean isTrunk(BlockPos pos, World world, TreeConfiguration config) {

		// Normal tree check, requires the tree to be sat on a solid block
		Boolean log = true;
		while (log) {
			pos = pos.add(0, -1, 0);
			if (!config.isLog(blockName(pos, world))) {
				log = false;
				if (!isDraggable(world, pos,config)) {
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
				if (!config.isLog(blockName(pos.add(0, -i, 0), world))) {
					break;
				}
				below++;
			}
			int above = 0;
			for (int i = 1; i < config.Min_vertical_logs(); i++) {
				if (!config.isLog(blockName(pos.add(0, i, 0), world))) {
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
	private static boolean cantDrag(World world, BlockPos pos, TreeConfiguration tree) {
		if (!isDraggable(world, pos.add(1, 0, 0),tree) || !isDraggable(world, pos.add(-1, 0, 0),tree)
				|| !isDraggable(world, pos.add(0, 1, 0),tree) || !isDraggable(world, pos.add(0, -1, 0),tree)
				|| !isDraggable(world, pos.add(0, 0, 1),tree) || !isDraggable(world, pos.add(0, 0, -1),tree)) {
			return true;
		}
		return false;
	}

	/*
	 * Is this specific block either a tree block, air or a passable block
	 */
	private static boolean isDraggable(World world, BlockPos pos, TreeConfiguration tree) {
		
		IBlockState state = world.getBlockState(pos);
		
		if(state.getBlock().isAir(state, world, pos) || state.getBlock().isPassable(world, pos)) {
			return true;
			}
		
		if(tree != null) {
			String name = blockName(pos,world);
			if(tree.isLog(name)||tree.isLeaf(name)){
				return true;
			}
		}
			return isWood(pos,world) || isLeaves(pos,world);	 
	}

	/*
	 * Is the block at this position an air block;
	 */
	public Boolean isAir(BlockPos pos) {
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
	public static boolean isWood(BlockPos pos, World world) {	
		String blockName = blockName(pos,world);
		for(String block : Config.logs) {
			if(block.equals(blockName) || blockName.matches(block)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Is the block at this position a log
	 */
	public static boolean isLeaves(BlockPos pos, World world) {
		return ArrayUtils.contains(Config.leaves, blockName(pos,world));
	}

	/*
	 * Is the block at this position leaves
	 */
	public boolean isLeaves(BlockPos pos) {
		return isLeaves(pos, world);
	}

	/*
	 * Class to house the falling logs and leaves
	 */
	public static class EntityFallingBlock extends net.minecraft.entity.item.EntityFallingBlock {

		EntityFallingBlock(World worldIn, double x, double y, double z, IBlockState fallingBlockState,
				TileEntity tile) {
			super(worldIn, x, y, z, fallingBlockState);
			if(tile != null) {
				tileEntityData = tile.writeToNBT(new NBTTagCompound());
			}
		}
		
		
		@Nullable
		@Override
		public EntityItem entityDropItem(ItemStack stack, float offsetY) {
			
			IBlockState state = getBlock();
			// TODO check if this works for none MC leaves
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