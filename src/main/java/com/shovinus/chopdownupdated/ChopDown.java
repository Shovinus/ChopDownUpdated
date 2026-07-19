package com.shovinus.chopdownupdated;

import com.shovinus.chopdownupdated.command.CDUCommand;
import com.shovinus.chopdownupdated.config.Config;
import com.shovinus.chopdownupdated.config.TreeConfiguration;
import com.shovinus.chopdownupdated.tree.Tree;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChopDown implements ModInitializer {
    public static final String MODID = "chopdownupdated";
    public static final String MODNAME = "ChopDownUpdated";
    public static final String AUTHOR = "Shovinus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    public static final LinkedList<Tree> FALLING_TREES = new LinkedList<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static int tick = 0;

    @Override
    public void onInitialize() {
        Config.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CDUCommand.register(dispatcher));
        PlayerBlockBreakEvents.BEFORE.register(this::onBlockBreak);
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
        AttackBlockCallback.EVENT.register(this::clickBlock);
    }

    private boolean onBlockBreak(net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player sourcePlayer,
                                 BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                                 net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel serverWorld) || !(sourcePlayer instanceof ServerPlayer player)) {
            return true;
        }
        if (!Tree.isWood(pos, serverWorld) || !isAllowedPlayer(player)) {
            return true;
        }
        if (!player.getMainHandItem().isEmpty() && Config.MatchesTool(Tree.stackName(player.getMainHandItem()))) {
            return true;
        }

        TreeConfiguration config = Tree.findConfig(serverWorld, pos);
        BlockPos playerStanding = player.blockPosition();
        if (config == null || !Tree.isTrunk(pos, serverWorld, config) || !Tree.isWood(pos.above(), serverWorld)
                || (playerStanding.getX() == 0 && playerStanding.getZ() == 0)) {
            return true;
        }

        for (Tree tree : FALLING_TREES) {
            if (tree.player == player) {
                player.sendSystemMessage(Component.literal("Still chopping down the last tree"));
                return false;
            }
        }

        try {
            Tree tree = new Tree(pos, serverWorld, player);
            FALLING_TREES.add(tree);
            executor.submit(tree);
        } catch (Exception ex) {
            player.sendSystemMessage(Component.literal("Can't find a tree configuration for this log."));
        }
        return true;
    }

    private static boolean isAllowedPlayer(ServerPlayer player) {
        return Config.allowedPlayers.contains(player.getClass().getName())
                || Config.allowedPlayers.contains("net.minecraft.server.level.ServerPlayer");
    }

    private void onTick(MinecraftServer server) {
        try {
            tick++;
            boolean throttledTick = tick % 4 == 0;
            if (throttledTick) {
                tick = 0;
            }

            Iterator<Tree> iterator = FALLING_TREES.iterator();
            while (iterator.hasNext()) {
                Tree tree = iterator.next();
                if (tree.failedToBuild) {
                    iterator.remove();
                } else if (tree.finishedCalculation && (!tree.startedDropping || throttledTick) && tree.dropBlocks()) {
                    iterator.remove();
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error while continuing to chop trees", ex);
        }
    }

    private InteractionResult clickBlock(net.minecraft.world.entity.player.Player sourcePlayer,
                                         net.minecraft.world.level.Level world,
                                         net.minecraft.world.InteractionHand hand, BlockPos pos,
                                         net.minecraft.core.Direction direction) {
        if (sourcePlayer instanceof ServerPlayer player && world instanceof ServerLevel serverWorld
                && Config.getPlayerConfig(playerId(player)).showBlockName) {
            player.sendSystemMessage(Component.literal("Block:" + Tree.blockName(pos, serverWorld)));
            if (!player.getMainHandItem().isEmpty()) {
                player.sendSystemMessage(Component.literal("Tool:" + Tree.stackName(player.getMainHandItem())));
            }
            player.sendSystemMessage(Component.literal("Player Class:" + player.getClass().getName()));
        }
        return InteractionResult.PASS;
    }

    public static UUID playerId(ServerPlayer player) {
        return player.getUUID();
    }
}
