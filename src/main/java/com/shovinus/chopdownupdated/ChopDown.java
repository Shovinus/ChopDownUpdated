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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Mod(ChopDown.MODID)
public class ChopDown {
    public static final String MODID = "chopdownupdated";
    public static final String MODNAME = "ChopDownUpdated";
    public static final String AUTHOR = "Shovinus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    public static final LinkedList<Tree> FALLING_TREES = new LinkedList<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static int tick = 0;

    public ChopDown(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        Config.registerReloadListener(context);
        registerForgeListener(RegisterCommandsEvent.class, event -> onCommandRegister((RegisterCommandsEvent) event));
        registerForgeListener(BlockEvent.BreakEvent.class, event -> onBlockBreak((BlockEvent.BreakEvent) event));
        registerForgeListener(TickEvent.ServerTickEvent.Post.class, event -> onTick((TickEvent.ServerTickEvent.Post) event));
        registerForgeListener(PlayerInteractEvent.LeftClickBlock.class, event -> clickBlock((PlayerInteractEvent.LeftClickBlock) event));
    }

    public void onCommandRegister(RegisterCommandsEvent event) {
        CDUCommand.register(event.getDispatcher());
    }

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
                cancelEvent(event);
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

    public void onTick(TickEvent.ServerTickEvent.Post event) {
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
            try {
                return (UUID) player.getGameProfile().getClass().getMethod("id").invoke(player.getGameProfile());
            } catch (ReflectiveOperationException accessorMissing) {
                try {
                    return (UUID) player.getGameProfile().getClass().getMethod("getId").invoke(player.getGameProfile());
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("Unable to read the player's UUID", ex);
                }
            }
        }
    }

    private static void registerForgeListener(Class<?> eventClass, Consumer<Object> listener) {
        try {
            // EventBus 7 (Forge 60) exposes a typed static bus on each event class.
            Object eventBus = eventClass.getField("BUS").get(null);
            eventBus.getClass().getMethod("addListener", Consumer.class).invoke(eventBus, listener);
        } catch (NoSuchFieldException newApiMissing) {
            try {
                // EventBus 6 (Forge 53-59) uses the global Forge event bus.
                Object eventBus = MinecraftForge.EVENT_BUS;
                Class<?> priorityClass = Class.forName("net.minecraftforge.eventbus.api.EventPriority");
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object normalPriority = Enum.valueOf((Class<? extends Enum>) priorityClass.asSubclass(Enum.class), "NORMAL");
                eventBus.getClass().getMethod("addListener", priorityClass, boolean.class, Class.class, Consumer.class)
                        .invoke(eventBus, normalPriority, false, eventClass, listener);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Unable to register Forge listener for " + eventClass.getName(), ex);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to register Forge listener for " + eventClass.getName(), ex);
        }
    }

    private static void cancelEvent(BlockEvent.BreakEvent event) {
        try {
            // EventBus 6 cancellation API.
            event.getClass().getMethod("setCanceled", boolean.class).invoke(event, true);
        } catch (NoSuchMethodException oldApiMissing) {
            try {
                // EventBus 7 result API.
                Class<?> resultClass = Class.forName("net.minecraftforge.common.util.Result");
                Object deny = resultClass.getField("DENY").get(null);
                event.getClass().getMethod("setResult", resultClass).invoke(event, deny);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Unable to cancel the block break event", ex);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to cancel the block break event", ex);
        }
    }
}
