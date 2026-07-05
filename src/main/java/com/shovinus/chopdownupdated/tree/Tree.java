package com.shovinus.chopdownupdated.tree;

import com.shovinus.chopdownupdated.ChopDown;
import com.shovinus.chopdownupdated.config.Config;
import com.shovinus.chopdownupdated.config.PersonalConfig;
import com.shovinus.chopdownupdated.config.TreeConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

public class Tree implements Runnable {
    BlockPos base;
    public ServerLevel world;
    public Player player;
    boolean main = false;
    LinkedList<BlockPos> queue = new LinkedList<>();

    HashMap<BlockPos, Integer> estimatedTree = new HashMap<>();
    LinkedList<BlockPos> estimatedTreeQueue = new LinkedList<>();
    HashMap<BlockPos, TreeMovePair> fallingBlocks = new HashMap<>();
    LinkedList<BlockPos> fallingBlocksList = new LinkedList<>();

    int fallX = 1;
    int fallZ = 0;
    int fallOffset = 0;

    EnumFallAxis axis = EnumFallAxis.X;
    TreeConfiguration config;
    int radius = 8;
    int leafLimit = 7;
    boolean wentUp = false;

    public boolean finishedCalculation = false;
    public boolean failedToBuild = false;
    LinkedList<Tree> nearbyTrees = new LinkedList<>();

    public Tree(BlockPos pos, ServerLevel world) throws Exception {
        initTree(pos, world);
        while (isLog(pos.below())) {
            pos = pos.below();
        }
        base = pos;
        getPossibleTree();
    }

    public Tree(BlockPos pos, ServerLevel world, Player player) throws Exception {
        main = true;
        this.player = player;
        initTree(pos, world);
        getFallDirection(player);
    }

    public static TreeConfiguration findConfig(ServerLevel world, BlockPos pos) {
        for (TreeConfiguration treeConfig : Config.treeConfigurations) {
            if (treeConfig.isLog(blockName(pos, world))) {
                return treeConfig;
            }
        }
        return null;
    }

    private void initTree(BlockPos pos, ServerLevel world) throws Exception {
        base = pos;
        this.world = world;
        addEstimateBlock(base, 0);
        this.config = findConfig(world, pos);
        if (this.config == null) {
            ChopDown.LOGGER.warn("{} block has no tree configuration", blockName(base, world));
            throw new Exception("The chopped log type is unknown and not setup");
        }
        this.radius = this.config.Radius();
        this.leafLimit = this.config.Leaf_limit();
    }

    private void getFallDirection(Player player) {
        double x = (base.getX() + 0.5) - player.getX();
        double z = (base.getZ() + 0.5) - player.getZ();
        double abX = Math.abs(x);
        double abZ = Math.abs(z);
        fallX = x == 0 ? 0 : (int) Math.floor(abX / x);
        fallZ = z == 0 ? 0 : (int) Math.floor(abZ / z);
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

    private void getPossibleTree() throws Exception {
        BuilderQueueComparer comp = new BuilderQueueComparer(estimatedTree);
        while (!queue.isEmpty()) {
            Collections.sort(queue, comp);
            BlockPos blockStep = queue.pollFirst();
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dz = -1; dz <= 1; ++dz) {
                        int stepInc = dz * dz + dx * dx + dy * dy;
                        BlockPos inspectPos = blockStep.offset(dx, dy, dz);
                        String inspectedBlockName = blockName(inspectPos, world);

                        boolean log = isLog(inspectedBlockName);
                        boolean leaf = !log && isLeaf(inspectedBlockName);
                        if (!(log || leaf)) {
                            continue;
                        }

                        boolean logAbove = isLog(inspectPos.above());
                        int y = inspectPos.getY();
                        boolean isTrunk = isTrunk(inspectPos, world, config);
                        boolean yMatch = y == base.getY();
                        if (y > base.getY()) {
                            wentUp = true;
                        }

                        Integer leafStep = getEstimate(blockStep);
                        leafStep = (leafStep == null ? 0 : leafStep) + (leaf ? stepInc : 0);

                        if (inspectPos.compareTo(base) == 0 || y < base.getY() || leafStep >= leafLimit
                                || horizontalDistance(base, inspectPos) > radius) {
                            continue;
                        }

                        if (log && (leafStep > 0 || dy < 0) && !estimatedTree.containsKey(inspectPos) && isTrunk
                                && (Math.abs(inspectPos.getX() - base.getX()) > config.Trunk_Radius()
                                || Math.abs(inspectPos.getZ() - base.getZ()) > config.Trunk_Radius())) {
                            if (main) {
                                boolean treeFound = false;
                                for (Tree tree : nearbyTrees) {
                                    if (tree.getEstimate(inspectPos) != null && tree.getEstimate(inspectPos) == 0) {
                                        treeFound = true;
                                    }
                                }
                                if (!treeFound) {
                                    nearbyTrees.add(new Tree(inspectPos, world));
                                }
                            }
                            continue;
                        } else if (main && log && (leafStep > 0 || dy < 0) && !estimatedTree.containsKey(inspectPos)
                                && isTrunk && isLog(inspectPos.above())) {
                            estimatedTree.clear();
                            queue.clear();
                            return;
                        }

                        if (main && log && ((cantDrag(world, inspectPos, config) && !yMatch)
                                || (yMatch && logAbove && !wentUp)) && leafStep == 0) {
                            estimatedTree.clear();
                            queue.clear();
                            return;
                        }
                        if (!yMatch || !cantDrag(world, inspectPos, config)) {
                            addEstimateBlock(inspectPos, leafStep);
                        }
                    }
                }
            }
        }
    }

    public void getDropBlocks() throws Exception {
        getPossibleTree();
        getRealisticTree();
        this.finishedCalculation = true;
    }

    private BlockPos repositionBlock(BlockPos pos) {
        int y = pos.getY() - base.getY();
        int x = pos.getX() - (base.getX() + fallOffset);
        int z = pos.getZ() - (base.getZ() + fallOffset);
        int changeX = fallZ * z;
        int changeZ = fallX * x;
        int normPosX = y * fallX;
        int normPosZ = y * fallZ;
        return pos.offset(normPosX - (changeZ * fallX), -(changeX + changeZ), normPosZ - (changeX * fallZ));
    }

    public void addEstimateBlock(BlockPos pos, int step) {
        if (estimatedTree.containsKey(pos) && estimatedTree.get(pos) <= step) {
            return;
        }
        if (!queue.contains(pos)) {
            queue.add(pos);
        }
        estimatedTree.put(pos, step);
    }

    private Integer getEstimate(BlockPos pos) {
        return estimatedTree.get(pos);
    }

    public static String blockName(BlockPos pos, ServerLevel world) {
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock());
        return loc.toString();
    }

    public static String stackName(ItemStack stack) {
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return loc.toString();
    }

    private void getRealisticTree() {
        estimatedTreeQueue = new LinkedList<>(estimatedTree.keySet());
        LinkedList<BlockPos> realisticTree = new LinkedList<>();
        while (!estimatedTreeQueue.isEmpty()) {
            BlockPos from = estimatedTreeQueue.pollFirst();
            boolean mine = true;
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
            if (mine && !base.equals(from)) {
                if (isLog(from) && (from.getY() == base.getY() + 1 || from.getY() == base.getY() + 2)
                        && ((fallZ != 0 && (isLog(from.east()) || isLog(from.west())))
                        || (fallX != 0 && (isLog(from.south()) || isLog(from.north()))))) {
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
        pushLogsThroughPendingLeaves();
        fallingBlocksList = new LinkedList<>(fallingBlocks.keySet());
        Collections.sort(fallingBlocksList, new AxisComparer(DirectionSort.UP));
        breakStackedPendingLeaves();
    }

    @Override
    public void run() {
        try {
            this.getDropBlocks();
        } catch (Exception e) {
            this.failedToBuild = true;
            ChopDown.LOGGER.error("Failed to calculate falling tree", e);
        }
    }

    public boolean dropBlocks() {
        int blocksRemaining = Config.maxDropsPerTickPerTree;
        int size = fallingBlocksList.size();
        for (int i = 0; i < size; i++) {
            BlockPos pos = fallingBlocksList.getFirst();
            TreeMovePair pair = fallingBlocks.get(pos);
            fallingBlocksList.removeFirst();
            if (!drop(pair, fallingBlocks.size() > Config.maxFallingBlockBeforeManualMove)) {
                fallingBlocksList.add(pos);
            }
            blocksRemaining--;
            if (blocksRemaining <= 0 && !fallingBlocksList.isEmpty()) {
                return false;
            }
        }
        return fallingBlocksList.isEmpty();
    }

    public boolean myBlock(BlockPos pos, double yourDistance, int yourStepValue) {
        Integer step = estimatedTree.get(pos);
        if (step == null || step > yourStepValue) {
            return false;
        }
        if (step == yourStepValue) {
            return horizontalDistance(base, pos) < yourDistance;
        }
        return true;
    }

    private boolean isAxis(BlockState state, Property<?> property, String value) {
        return ((Enum<?>) state.getValue(property)).name().equalsIgnoreCase(value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private BlockState cycle(BlockState state, Property<?> property) {
        return state.cycle((Property) property);
    }

    private BlockState setAxis(BlockState state, Property<?> property, String value) {
        int i = 10;
        while (i > 0 && !isAxis(state, property, value)) {
            i--;
            state = cycle(state, property);
        }
        return state;
    }

    public BlockState rotateLog(BlockState state) {
        Property<?> foundProp = null;
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("axis")) {
                foundProp = prop;
                break;
            }
        }
        if (foundProp == null) {
            return state.rotate(world, base, Rotation.CLOCKWISE_90);
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

    public static void dropDrops(BlockPos pos, BlockPos dropPos, BlockState state, ServerLevel world) {
        for (ItemStack stack : Block.getDrops(state, world, pos, world.getBlockEntity(pos))) {
            ItemEntity entityitem = new ItemEntity(world, dropPos.getX(), dropPos.getY(), dropPos.getZ(), stack);
            entityitem.setDefaultPickUpDelay();
            world.addFreshEntity(entityitem);
        }
    }

    private boolean drop(TreeMovePair pair, boolean useSolid) {
        if (!(isLog(pair.from) || isLeaf(pair.from))) {
            return true;
        }
        PersonalConfig playerConfig = Config.getPlayerConfig(player.getUUID());
        if (playerConfig.makeGlass && playerConfig.dontFell) {
            world.setBlock(pair.from, isLog(pair.from) ? Blocks.ORANGE_STAINED_GLASS.defaultBlockState() : Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(), 3);
            return true;
        }

        BlockState state = world.getBlockState(pair.from);
        if (!pair.moved && isLog(pair.from)) {
            state = rotateLog(state);
        }

        if ((!canMoveTo(pair.to, !pair.leaves) && !pair.moved) || (isLeaf(pair.from) && Config.breakLeaves)) {
            dropDrops(pair.from, pair.to, state, world);
            world.setBlock(pair.from, Blocks.AIR.defaultBlockState(), 3);
            return true;
        } else if (!canMoveTo(pair.to, !pair.leaves)) {
            return true;
        }

        world.setBlock(pair.from, Blocks.AIR.defaultBlockState(), 3);
        pair.from = pair.to;
        pair.moved = true;

        if (playerConfig.dontFell || useSolid) {
            manuallyDrop(pair, state);
        } else {
            clearLeafLandingPath(pair);
            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(world, pair.to, state);
            fallingBlock.setHurtsEntities(2.0F, 40);
        }
        return true;
    }

    private void pushLogsThroughPendingLeaves() {
        boolean moved;
        do {
            moved = false;
            LinkedList<BlockPos> logPositions = new LinkedList<>();
            for (TreeMovePair pair : fallingBlocks.values()) {
                if (!pair.leaves) {
                    logPositions.add(pair.to);
                }
            }
            Collections.sort(logPositions, new AxisComparer(DirectionSort.DOWN));

            for (BlockPos pos : logPositions) {
                TreeMovePair pair = fallingBlocks.get(pos);
                if (pair == null || pair.leaves) {
                    continue;
                }
                moved = pushLogThroughPendingLeaves(pair) || moved;
            }
        } while (moved);
    }

    private boolean pushLogThroughPendingLeaves(TreeMovePair logPair) {
        boolean moved = false;
        TreeMovePair leafPair = fallingBlocks.get(fartherFallEdge(logPair.to));
        while (leafPair != null && leafPair.leaves) {
            swapTargets(logPair, leafPair);
            moved = true;
            leafPair = fallingBlocks.get(fartherFallEdge(logPair.to));
        }

        leafPair = fallingBlocks.get(logPair.to.below());
        while (leafPair != null && leafPair.leaves) {
            swapTargets(logPair, leafPair);
            moved = true;
            leafPair = fallingBlocks.get(logPair.to.below());
        }
        return moved;
    }

    private BlockPos fartherFallEdge(BlockPos pos) {
        return pos.offset(fallX, 0, fallZ);
    }

    private void swapTargets(TreeMovePair first, TreeMovePair second) {
        BlockPos firstTo = first.to;
        BlockPos secondTo = second.to;
        fallingBlocks.remove(firstTo);
        fallingBlocks.remove(secondTo);
        first.to = secondTo;
        second.to = firstTo;
        fallingBlocks.put(first.to, first);
        fallingBlocks.put(second.to, second);
    }

    private void clearLeafLandingPath(TreeMovePair pair) {
        BlockPos below = pair.to.below();
        while (below.getY() > world.getMinBuildHeight()) {
            boolean cleared = false;
            if (Tree.isLeaves(below, world)) {
                dropDrops(below, below, world.getBlockState(below), world);
                world.setBlock(below, Blocks.AIR.defaultBlockState(), 3);
                cleared = true;
            }
            if (breakPendingLeaf(below)) {
                cleared = true;
            }
            if (!cleared) {
                return;
            }
            below = below.below();
        }
    }

    private void breakStackedPendingLeaves() {
        LinkedList<BlockPos> leafPositions = new LinkedList<>();
        for (BlockPos pos : fallingBlocksList) {
            TreeMovePair pair = fallingBlocks.get(pos);
            if (pair != null && pair.leaves) {
                leafPositions.add(pos);
            }
        }
        Collections.sort(leafPositions, new AxisComparer(DirectionSort.DOWN));

        for (BlockPos pos : leafPositions) {
            TreeMovePair pair = fallingBlocks.get(pos);
            if (pair == null || !pair.leaves) {
                continue;
            }
            breakPendingLeaf(pos.below());
        }
    }

    private boolean breakPendingLeaf(BlockPos pos) {
        TreeMovePair pair = fallingBlocks.get(pos);
        if (pair == null || !pair.leaves || !fallingBlocksList.remove(pos)) {
            return false;
        }
        fallingBlocks.remove(pos);
        if (Tree.isLeaves(pair.from, world)) {
            dropDrops(pair.from, pair.to, world.getBlockState(pair.from), world);
            world.setBlock(pair.from, Blocks.AIR.defaultBlockState(), 3);
        }
        return true;
    }

    private void breakWorldLeaf(BlockPos pos) {
        if (Tree.isLeaves(pos, world)) {
            dropDrops(pos, pos, world.getBlockState(pos), world);
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void breakLeafAt(BlockPos pos) {
        if (!breakPendingLeaf(pos)) {
            breakWorldLeaf(pos);
        }
    }

    private boolean isPendingLeaf(BlockPos pos) {
        TreeMovePair pair = fallingBlocks.get(pos);
        return pair != null && pair.leaves && fallingBlocksList.contains(pos);
    }

    private void manuallyDrop(TreeMovePair pair, BlockState state) {
        while (canMoveThroughBelow(pair)) {
            pair.to = pair.to.below();
            breakLeafAt(pair.to);
            if (!isAir(pair.to)) {
                dropDrops(pair.from, pair.to, world.getBlockState(pair.to), world);
                world.setBlock(pair.to, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        pair.move();
    }

    private boolean canMoveThroughBelow(TreeMovePair pair) {
        BlockPos below = pair.to.below();
        return canMoveTo(below, !pair.leaves) || Tree.isLeaves(below, world) || isPendingLeaf(below);
    }

    private boolean canMoveTo(BlockPos pos, boolean log) {
        return (isAir(pos) || isPassable(pos) || (log && Tree.isLeaves(pos, world))) && pos.getY() > world.getMinBuildHeight();
    }

    private double horizontalDistance(BlockPos pos1, BlockPos pos2) {
        int diffX = Math.abs(pos1.getX() - pos2.getX());
        int diffZ = Math.abs(pos1.getZ() - pos2.getZ());
        return Math.floor(Math.sqrt(Math.pow(diffX, 2) + Math.pow(diffZ, 2)));
    }

    public static boolean isTrunk(BlockPos pos, ServerLevel world, TreeConfiguration config) {
        boolean log = true;
        BlockPos inspect = pos;
        while (log) {
            inspect = inspect.below();
            if (!config.isLog(blockName(inspect, world))) {
                log = false;
                if (!isDraggable(world, inspect, config)) {
                    return true;
                }
            }
        }

        if (config.Min_vertical_logs() == 0) {
            return false;
        }
        int below = 0;
        for (int i = 1; i < config.Min_vertical_logs(); i++) {
            if (!config.isLog(blockName(pos.below(i), world))) {
                break;
            }
            below++;
        }
        int above = 0;
        for (int i = 1; i < config.Min_vertical_logs(); i++) {
            if (!config.isLog(blockName(pos.above(i), world))) {
                break;
            }
            above++;
        }
        return (1 + below + above) >= config.Min_vertical_logs();
    }

    private static boolean cantDrag(ServerLevel world, BlockPos pos, TreeConfiguration tree) {
        return !isDraggable(world, pos.east(), tree) || !isDraggable(world, pos.west(), tree)
                || !isDraggable(world, pos.above(), tree) || !isDraggable(world, pos.below(), tree)
                || !isDraggable(world, pos.south(), tree) || !isDraggable(world, pos.north(), tree);
    }

    private static boolean isDraggable(ServerLevel world, BlockPos pos, TreeConfiguration tree) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.getCollisionShape(world, pos, CollisionContext.empty()).isEmpty()) {
            return true;
        }

        if (tree != null) {
            String name = blockName(pos, world);
            if (tree.isLog(name) || tree.isLeaf(name)) {
                return true;
            }
        }
        return isWood(pos, world) || isLeaves(pos, world);
    }

    public boolean isAir(BlockPos pos) {
        return world.getBlockState(pos).isAir();
    }

    private boolean isPassable(BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos, CollisionContext.empty()).isEmpty();
    }

    public static boolean isWood(BlockPos pos, ServerLevel world) {
        String blockName = blockName(pos, world);
        for (String block : Config.logs) {
            if (block.equals(blockName) || blockName.matches(block)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLeaves(BlockPos pos, ServerLevel world) {
        String blockName = blockName(pos, world);
        for (String block : Config.leaves) {
            if (block.equals(blockName) || blockName.matches(block)) {
                return true;
            }
        }
        return false;
    }

    public boolean isLeaves(BlockPos pos) {
        return isLeaves(pos, world);
    }
}
