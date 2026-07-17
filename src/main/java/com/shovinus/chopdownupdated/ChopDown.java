package com.shovinus.chopdownupdated;

import com.shovinus.chopdownupdated.command.CDUCommand;
import com.shovinus.chopdownupdated.config.Config;
import com.shovinus.chopdownupdated.config.TreeConfiguration;
import com.shovinus.chopdownupdated.tree.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(ChopDown.MODID)
public class ChopDown {
    public static final String MODID = "chopdownupdated";
    public static final String MODNAME = "ChopDownUpdated";
    public static final String AUTHOR = "Shovinus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    public static final LinkedList<Tree> FALLING_TREES = new LinkedList<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static int tick = 0;

    public ChopDown() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CDUCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel world) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!Tree.isWood(pos, world) || !Config.allowedPlayers.contains(player.getClass().getName())) {
            return;
        }
        if (!player.getMainHandItem().isEmpty() && Config.MatchesTool(Tree.stackName(player.getMainHandItem()))) {
            return;
        }

        TreeConfiguration config = Tree.findConfig(world, pos);
        BlockPos playerStanding = player.blockPosition();
        if (config == null || !Tree.isTrunk(pos, world, config) || !Tree.isWood(pos.above(), world)
                || (playerStanding.getX() == 0 && playerStanding.getZ() == 0)) {
            return;
        }

        for (Tree tree : FALLING_TREES) {
            if (tree.player == player) {
                player.sendSystemMessage(Component.literal("Still chopping down the last tree"));
                event.setCanceled(true);
                return;
            }
        }

        try {
            Tree tree = new Tree(pos, world, player);
            FALLING_TREES.add(tree);
            executor.submit(tree);
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Can't find a tree configuration for this log."));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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

    @SubscribeEvent
    public void clickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel world)) {
            return;
        }
        if (Config.getPlayerConfig(playerId(player)).showBlockName) {
            BlockPos pos = event.getPos();
            player.sendSystemMessage(Component.literal("Block:" + Tree.blockName(pos, world)));
            if (!player.getMainHandItem().isEmpty()) {
                player.sendSystemMessage(Component.literal("Tool:" + Tree.stackName(player.getMainHandItem())));
            }
            player.sendSystemMessage(Component.literal("Player Class:" + player.getClass().getName()));
        }
    }

    public static UUID playerId(ServerPlayer player) {
        try {
            return (UUID) player.getClass().getMethod("getUUID").invoke(player);
        } catch (ReflectiveOperationException ignored) {
            return player.getGameProfile().getId();
        }
    }
}
