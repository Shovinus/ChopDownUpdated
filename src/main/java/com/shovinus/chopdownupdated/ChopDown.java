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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;
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
        registerConfigEventHandler();
        registerToForgeEventBus();
    }

    private void registerConfigEventHandler() {
        try {
            Class<?> contextClass = Class.forName("net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext");
            Object context = contextClass.getMethod("get").invoke(null);

            try {
                Object modEventBus = contextClass.getMethod("getModEventBus").invoke(context);
                Method addListener = modEventBus.getClass().getMethod("addListener", Consumer.class);
                addListener.invoke(modEventBus, (Consumer<Object>) Config::onConfigLoad);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            Object modBusGroup = contextClass.getMethod("getModBusGroup").invoke(context);
            addModernConfigListener("net.minecraftforge.fml.event.config.ModConfigEvent$Loading", modBusGroup);
            addModernConfigListener("net.minecraftforge.fml.event.config.ModConfigEvent$Reloading", modBusGroup);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Unable to register Chop Down config reload listener; using current config values", e);
            Config.reloadConfig();
        }
    }

    private void addModernConfigListener(String eventClassName, Object modBusGroup) throws ReflectiveOperationException {
        Class<?> eventClass = Class.forName(eventClassName);
        Class<?> busGroupClass = Class.forName("net.minecraftforge.eventbus.api.bus.BusGroup");
        Object eventBus = eventClass.getMethod("getBus", busGroupClass).invoke(null, modBusGroup);
        Method addListener = eventBus.getClass().getMethod("addListener", Consumer.class);
        addListener.invoke(eventBus, (Consumer<Object>) Config::onConfigLoad);
    }

    private void registerToForgeEventBus() {
        try {
            Object eventBus = MinecraftForge.class.getField("EVENT_BUS").get(null);
            if ("net.minecraftforge.common.EventBusMigrationHelper".equals(eventBus.getClass().getName())) {
                registerModernEventHandlers();
            } else {
                Method register = eventBus.getClass().getMethod("register", Object.class);
                register.invoke(eventBus, this);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to register Chop Down event handlers", e);
        }
    }

    private void registerModernEventHandlers() throws ReflectiveOperationException {
        addModernListener("net.minecraftforge.event.RegisterCommandsEvent", event ->
                onCommandRegister((RegisterCommandsEvent) event));
        addModernListener("net.minecraftforge.event.level.BlockEvent$BreakEvent", event ->
                onBlockBreak((BlockEvent.BreakEvent) event));
        addModernListener("net.minecraftforge.event.TickEvent$ServerTickEvent$Post", this::onModernServerTick);
        addModernListener("net.minecraftforge.event.entity.player.PlayerInteractEvent$LeftClickBlock", event ->
                clickBlock((PlayerInteractEvent.LeftClickBlock) event));
    }

    private void addModernListener(String eventClassName, Consumer<Object> listener) throws ReflectiveOperationException {
        Class<?> eventClass = Class.forName(eventClassName);
        Field busField = eventClass.getField("BUS");
        Object bus = busField.get(null);
        Method addListener = bus.getClass().getMethod("addListener", Consumer.class);
        addListener.invoke(bus, listener);
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
        onModernServerTick(event);
    }

    private void onModernServerTick(Object event) {
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
        if (Config.getPlayerConfig(player.getUUID()).showBlockName) {
            BlockPos pos = event.getPos();
            player.sendSystemMessage(Component.literal("Block:" + Tree.blockName(pos, world)));
            if (!player.getMainHandItem().isEmpty()) {
                player.sendSystemMessage(Component.literal("Tool:" + Tree.stackName(player.getMainHandItem())));
            }
            player.sendSystemMessage(Component.literal("Player Class:" + player.getClass().getName()));
        }
    }
}
