package net.andrews.mechtour;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.andrews.mechtour.mapgui.MapGuiHolder;
import net.andrews.mechtour.mapgui.gui.GuideMenuGUI;
import net.andrews.mechtour.mapgui.gui.Resources;
import net.andrews.mechtour.waypoint.Waypoint;
import net.andrews.mechtour.waypoint.WaypointIcons;
import net.andrews.mechtour.waypoint.WaypointManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class MechTourMod {

    private static String currentGuidePlayer = "";

    private static int guideCooldown = 40;
    private static HashMap<ServerPlayerEntity, MapGuiHolder> guiHolders = new HashMap<>();

    public static WaypointManager waypointManager;

    public static void init() {

      

        // Initialize resources
        // Resources.noop();
        // WaypointIcons.noop();

    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {

        Resources.noop();
        WaypointIcons.noop();
        waypointManager = new WaypointManager();

        Configs.loadFromFile();

        dispatcher.register(CommandManager.literal("mechtour").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermissionLevel(2);
        }).then(CommandManager.literal("giveitem")
                .then(CommandManager.literal("guide")
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                .executes(MechTourMod::giveGuides))

                        .executes(MechTourMod::giveGuide)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> CommandSource.suggestMatching(Configs.getFields(), b))
                                .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                        .executes(MechTourMod::setConfig))))

        );

        dispatcher.register(CommandManager.literal("guideset").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermissionLevel(2);
        }).then(CommandManager.argument("player", EntityArgumentType.player()).executes(MechTourMod::setGuide))
                .executes(MechTourMod::setGuide));

        dispatcher.register(CommandManager.literal("guidestop").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermissionLevel(2);
        }).executes(MechTourMod::stopGuide));

        dispatcher
                .register(
                        CommandManager
                                .literal(
                                        "waypoint")
                                .then(CommandManager.literal("list").executes(MechTourMod::listWaypoints)
                                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                                .executes(MechTourMod::listWaypoints))
                                        .then(CommandManager.literal("all").executes(MechTourMod::listAllWaypoints))
                                        .executes(MechTourMod::listWaypoints))
                                .then(CommandManager.literal("add").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                                .then(CommandManager.argument("icon", StringArgumentType.word())
                                                        .suggests((c, b) -> CommandSource
                                                                .suggestMatching(WaypointIcons.getIconNames(), b))
                                                        .then(CommandManager
                                                                .argument("name", StringArgumentType.greedyString())
                                                                .executes(MechTourMod::addWaypoint)))))
                                        .then(CommandManager.argument("icon", StringArgumentType.word())
                                                .suggests(
                                                        (c, b) -> CommandSource
                                                                .suggestMatching(WaypointIcons.getIconNames(), b))
                                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                                        .executes(MechTourMod::addWaypoint)))

                                ).then(CommandManager.literal("remove").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager
                                        .argument("dimension", DimensionArgumentType.dimension()).then(
                                                CommandManager
                                                        .argument("name",
                                                                StringArgumentType.greedyString())
                                                        .suggests(
                                                                (c, b) -> CommandSource
                                                                        .suggestMatching(
                                                                                waypointManager.getWaypointNames(
                                                                                        DimensionArgumentType
                                                                                                .getDimensionArgument(c,
                                                                                                        "dimension")
                                                                                                .getRegistryKey()
                                                                                                .getValue().getPath()),
                                                                                b))
                                                        .executes(MechTourMod::removeWaypoint))))
                                .then(CommandManager.literal("modifyIcon").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager
                                        .argument("dimension", DimensionArgumentType.dimension()).then(
                                                CommandManager
                                                        .argument("name",
                                                                StringArgumentType.string())
                                                        .suggests(
                                                                (c, b) -> CommandSource
                                                                        .suggestMatching(
                                                                                waypointManager.getWaypointNames(
                                                                                        DimensionArgumentType
                                                                                                .getDimensionArgument(c,
                                                                                                        "dimension")
                                                                                                .getRegistryKey()
                                                                                                .getValue().getPath()),
                                                                                b))
                                                        .then(CommandManager.argument("icon", StringArgumentType.word())
                                                                .suggests((c, b) -> CommandSource.suggestMatching(
                                                                        WaypointIcons.getIconNames(), b))
                                                                .executes(MechTourMod::modifyIcon)))))
                                .then(CommandManager.literal("modifyName").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager
                                        .argument("dimension", DimensionArgumentType.dimension()).then(
                                                CommandManager
                                                        .argument("name",
                                                                StringArgumentType.string())
                                                        .suggests(
                                                                (c, b) -> CommandSource
                                                                        .suggestMatching(
                                                                                waypointManager.getWaypointNames(
                                                                                        DimensionArgumentType
                                                                                                .getDimensionArgument(c,
                                                                                                        "dimension")
                                                                                                .getRegistryKey()
                                                                                                .getValue().getPath()),
                                                                                b))
                                                        .then(CommandManager
                                                                .argument("newname", StringArgumentType.greedyString())
                                                                .suggests((c, b) -> CommandSource.suggestMatching(
                                                                        WaypointIcons.getIconNames(), b))
                                                                .executes(MechTourMod::modifyName)))))
                                .then(CommandManager.literal("reload").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.literal("icons").executes(MechTourMod::reloadIconsCommand)).executes(MechTourMod::reloadCommand)));

        dispatcher.register(CommandManager.literal("tpw").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermissionLevel(2);
        }).then(CommandManager
                .argument("players",
                        EntityArgumentType.players())
                .then(CommandManager
                        .argument("dimension",
                                DimensionArgumentType.dimension())
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .suggests((c,
                                        b) -> CommandSource.suggestMatching(waypointManager.getWaypointNames(
                                                DimensionArgumentType.getDimensionArgument(c, "dimension")
                                                        .getRegistryKey().getValue().getPath()),
                                                b))
                                .executes(MechTourMod::tpWaypoint)))));
    }

    private static void sendFeedback(CommandContext<ServerCommandSource> ctx, String str, boolean ops) {
        ctx.getSource().sendFeedback(new LiteralText(str), ops);
    }

    private static void sendFeedback(CommandContext<ServerCommandSource> ctx, String str) {
        sendFeedback(ctx, str, false);
    }

    private static int setConfig(CommandContext<ServerCommandSource> ctx) {
        try {

            String name = StringArgumentType.getString(ctx, "name");
            String value = StringArgumentType.getString(ctx, "value");

            if (Configs.setConfig(name, value)) {
                sendFeedback(ctx, "Set " + name + " to " + value, true);
            } else {
                sendFeedback(ctx, "Failed to set " + name + " to " + value, true);
            }

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }

    private static int setGuide(CommandContext<ServerCommandSource> ctx) {
        try {

            ServerPlayerEntity player;
            try {
                player = EntityArgumentType.getPlayer(ctx, "player");
            } catch (Exception e) {
                player = ctx.getSource().getPlayer();
            }

            currentGuidePlayer = player.getGameProfile().getName().toString();
            guideCooldown = 0;
            sendFeedback(ctx, currentGuidePlayer + " is the tour guide now!", true);

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }

    private static int stopGuide(CommandContext<ServerCommandSource> ctx) {
        try {

            currentGuidePlayer = null;
            guideCooldown = 0;
            sendFeedback(ctx, "Guide unset", true);

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }

    private static int listAllWaypoints(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }

            ArrayList<Waypoint> waypoints = waypointManager.getWaypoints();
            if (waypoints.size() == 0) {
                sendFeedback(ctx, "There are no waypoints");
            } else {
                for (Waypoint waypoint : waypoints) {
                    sendFeedback(ctx, waypoint.voxelmap_string());
                }
            }
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }

    private static int reloadCommand(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            waypointManager.loadFromFile();
            sendFeedback(ctx, "Reloaded waypoints!", true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }


    private static int reloadIconsCommand(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            WaypointIcons.load();
            waypointManager.loadFromFile();
            sendFeedback(ctx, "Reloaded waypoints and icons", true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }
        return 1;
    }

    private static int listWaypoints(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            String dimension;

            try {
                dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue()
                        .getPath();
            } catch (Exception e) {
                dimension = ctx.getSource().getWorld().getRegistryKey().getValue().getPath();
            }

            ArrayList<Waypoint> waypoints = waypointManager.getWaypoints(dimension);

            if (waypoints.size() == 0) {
                sendFeedback(ctx, "There are no waypoints in dimension " + dimension);
            } else {
                for (Waypoint waypoint : waypoints) {
                    sendFeedback(ctx, waypoint.voxelmap_string());
                }
            }

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e);

        }
        return 1;
    }

    private static int addWaypoint(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            String dimension;
            Vec3d pos;

            try {
                dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue()
                        .getPath();
                pos = Vec3ArgumentType.getPosArgument(ctx, "pos").toAbsolutePos(ctx.getSource());
            } catch (Exception e) {
                dimension = ctx.getSource().getWorld().getRegistryKey().getValue().getPath();
                pos = ctx.getSource().getPosition();
            }

            String icon = StringArgumentType.getString(ctx, "icon");
            String name = StringArgumentType.getString(ctx, "name");

            waypointManager.addWaypoint((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), dimension, name, icon);
            sendFeedback(ctx, "Added waypoint " + name, true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }

        return 1;
    }

    private static int removeWaypoint(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            String dimension;

            try {
                dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue()
                        .getPath();

            } catch (Exception e) {
                dimension = ctx.getSource().getWorld().getRegistryKey().getValue().getPath();
            }

            String name = StringArgumentType.getString(ctx, "name");

            int count = waypointManager.removeWaypoint(dimension, name);
            if (count == 0) {
                sendFeedback(ctx, "There is no waypoint with name " + name + " in dimension " + dimension, true);
            } else {
                sendFeedback(ctx, "Removed " + count + " waypoint", true);
            }
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }

        return 1;
    }

    private static int modifyIcon(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            String dimension;

            try {
                dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue()
                        .getPath();

            } catch (Exception e) {
                dimension = ctx.getSource().getWorld().getRegistryKey().getValue().getPath();
            }

            String name = StringArgumentType.getString(ctx, "name");
            Waypoint waypoint = waypointManager.getWaypoint(dimension, name);
            if (waypoint == null) {
                sendFeedback(ctx, "No waypoint " + name + " in dimension " + dimension + " exists!", true);
                return 1;
            }
            String icon = StringArgumentType.getString(ctx, "icon");
            waypoint.setIconName(icon);
            waypointManager.waypointsUpdated();
            sendFeedback(ctx, "Changed icon of waypoint " + name + " in dimension " + dimension + " to " + icon + "!",
                    true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }

        return 1;
    }

    private static int modifyName(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            String dimension;

            try {
                dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue()
                        .getPath();

            } catch (Exception e) {
                dimension = ctx.getSource().getWorld().getRegistryKey().getValue().getPath();
            }

            String name = StringArgumentType.getString(ctx, "name");
            Waypoint waypoint = waypointManager.getWaypoint(dimension, name);
            if (waypoint == null) {
                sendFeedback(ctx, "No waypoint " + name + " in dimension " + dimension + " exists!", true);
                return 1;
            }
            String newname = StringArgumentType.getString(ctx, "newname");
            waypoint.setName(newname);
            waypointManager.waypointsUpdated();
            sendFeedback(ctx,
                    "Changed name of waypoint " + name + " in dimension " + dimension + " to " + newname + "!", true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }

        return 1;
    }

    private static int giveGuide(CommandContext<ServerCommandSource> ctx) {

        ServerPlayerEntity player = null;
        try {
            player = ctx.getSource().getPlayer();
        } catch (Exception e) {
            sendFeedback(ctx, "Player must run command");
            return 1;
        }

        if (giveGuideItemToPlayer(player)) {
            sendFeedback(ctx, "Gave guide item to player", true);
        } else {
            sendFeedback(ctx, "Failed to give guide item to player", true);
        }
        return 1;
    }

    private static int giveGuides(CommandContext<ServerCommandSource> ctx) {

        try {

            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "players");
            int count = 0;
            for (ServerPlayerEntity player : players) {
                giveGuideItemToPlayer(player);
                count++;
            }

            sendFeedback(ctx, "Gave guide items to " + count + " players", true);

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }
        return 1;
    }

    private static int tpWaypoint(CommandContext<ServerCommandSource> ctx) {

        try {

            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "players");

            String dimension;

            try {
                dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue()
                        .getPath();

            } catch (Exception e) {
                dimension = ctx.getSource().getWorld().getRegistryKey().getValue().getPath();
            }

            String name = StringArgumentType.getString(ctx, "name");

            Waypoint waypoint = waypointManager.getWaypoint(dimension, name);

            if (waypoint == null) {
                sendFeedback(ctx, "No waypoint " + name + " in dimension " + dimension + " exists!", true);
                return 1;
            }
            int count = 0;

            for (ServerPlayerEntity player : players) {
                teleportToWaypoint(player, waypoint, false);
                count++;
            }

            sendFeedback(ctx, "Teleported " + count + " players to waypoint " + name + " in dimension " + dimension,
                    true);

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }
        return 1;
    }

    public static boolean giveGuideItemToPlayer(ServerPlayerEntity player) {

        ItemStack stack = new ItemStack(Items.COMPASS);
        CompoundTag nbtCompound = new CompoundTag();
        nbtCompound.putBoolean("mechtour_guide_item", true);
        ListTag t = new ListTag();
        t.add(new CompoundTag());
        nbtCompound.put("Enchantments", t);
        CompoundTag displayTag = new CompoundTag();
        ListTag listTag = new ListTag();
        listTag.add(StringTag.of("{\"text\":\"Don't Panic\",\"color\":\"dark_green\"}"));
        displayTag.put("Lore", listTag);
        nbtCompound.put("display", displayTag);
        nbtCompound.putBoolean("LodestoneTracked", false);
        stack.setTag(nbtCompound);
        stack.setCustomName(new LiteralText("\u00A76\u00A7oHitchhikers Guide\u00A7r"));
        stack.setCooldown(1);
        return player.inventory.insertStack(stack);
    }

    public static void onBeforeTick(MinecraftServer minecraftServer) {
        if (guideCooldown > 0) {
            guideCooldown--;
        } else {
            guideCooldown = 40;

            ServerPlayerEntity guidePlayer = currentGuidePlayer == null ? null
                    : minecraftServer.getPlayerManager().getPlayer(currentGuidePlayer);
            List<ServerPlayerEntity> players = minecraftServer.getPlayerManager().getPlayerList();
            for (ServerPlayerEntity player : players) {
                if (player.isAlive()) {
                    List<Integer> slots = getGuideSlots(player);
                    for (int slot : slots) {

                        ItemStack stack = player.inventory.getStack(slot);
                        stack.getTag().getCompound("display").remove("Lore");
                        if (guidePlayer != null && guidePlayer.isAlive()) {
                            CompoundTag posTag = stack.getOrCreateSubTag("LodestonePos");
                            posTag.putInt("X", (int) guidePlayer.getX());
                            posTag.putInt("Y", (int) guidePlayer.getY());
                            posTag.putInt("Z", (int) guidePlayer.getZ());
                            stack.getTag().putString("LodestoneDimension",
                                    guidePlayer.getServerWorld().getRegistryKey().getValue().toString());
                            ListTag listTag = new ListTag();
                            listTag.add(StringTag.of("{\"text\":\"Tracking " + guidePlayer.getName().asString()
                                    + "\",\"color\":\"blue\"}"));
                            stack.getTag().getCompound("display").put("Lore", listTag);
                        } else {
                            stack.removeSubTag("LodestonePos");
                            stack.removeSubTag("LodestoneDimension");
                            ListTag listTag = new ListTag();
                            listTag.add(StringTag.of("{\"text\":\"Don't Panic\",\"color\":\"dark_green\"}"));
                            stack.getTag().getCompound("display").put("Lore", listTag);
                        }

                        player.inventory.setStack(slot, stack);
                    }

                }
            }

        }

        Iterator<MapGuiHolder> guis = guiHolders.values().iterator();
        while (guis.hasNext()) {
            MapGuiHolder gui = guis.next();
            if (gui.shouldRemove()) {
                gui.closeGui();
                gui.closePanel();
                guis.remove();
            } else {
                gui.tick();
            }
        }
    }

    public static List<Integer> getGuideSlots(ServerPlayerEntity player) {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < player.inventory.main.size(); ++i) {
            ItemStack stack = player.inventory.main.get(i);
            if (isGuideItem(stack)) {
                list.add(i);
            }
        }
        return list;
    }

    public static boolean isHoldingGuide(ServerPlayerEntity player) {
        if (player.isAlive() && PlayerInventory.isValidHotbarIndex(player.inventory.selectedSlot)) {
            ItemStack stack = player.inventory.getStack(player.inventory.selectedSlot);
            return isGuideItem(stack);
        }
        return false;
    }

    public static boolean isGuideItem(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().contains("mechtour_guide_item");
    }


    public static void onUpdateSelectedSlot(ServerPlayNetworkHandler serverPlayNetworkHandler, int selectedSlot) {

        ServerPlayerEntity player = serverPlayNetworkHandler.player;
        ItemStack stack = player.inventory.getStack(selectedSlot);
        if (isGuideItem(stack)) {
            if (Configs.configs.vanillaMode) {
                sendActionBarMessage(player, "Use to teleport to tour");
            } else {
                sendActionBarMessage(player, "Use to open menu");
            }
        }

    }

    public static void sendActionBarMessage(ServerPlayerEntity player, String str) {
        Utils.sendPacket(player, new TitleS2CPacket(Action.ACTIONBAR, new LiteralText(str), 0, 20 * 4, 20));
    }

    public static void openGuideGUI(ServerPlayerEntity player) {
        MapGuiHolder holder = guiHolders.get(player);
        if (holder == null) {
            holder = new MapGuiHolder(player);
            holder.openGui(new GuideMenuGUI());
            guiHolders.put(player, holder);
        }

        Direction facing = player.getHorizontalFacing().getOpposite();
        BlockPos playerPos = player.getBlockPos().offset(player.getHorizontalFacing(), 4);

        if (holder.isPanelOpen()) {
            holder.closePanel();
            sendActionBarMessage(player, "Closed guide menu!");
        } else {

            holder.openPanel(playerPos, facing, 7, 4);
            sendActionBarMessage(player, "Opened guide menu!");
        }

    }

    public static void onInteractClick(ServerPlayerEntity player, CallbackInfoReturnable<ActionResult> ci) {
        MapGuiHolder holder = guiHolders.get(player);
        if (holder != null && holder.isTrackingPanel()) {
            holder.onInteractClick();
            ci.setReturnValue(ActionResult.CONSUME);
            return;
        }

        if (MechTourMod.isHoldingGuide(player)) {

            if (Configs.configs.vanillaMode) {
                teleportToGuide(player);
            } else {
                MechTourMod.openGuideGUI(player);
            }
            ci.setReturnValue(ActionResult.CONSUME);
        }
    }

    public static void onSwingClick(ServerPlayerEntity player) {
        MapGuiHolder holder = guiHolders.get(player);
        if (holder != null && holder.isTrackingPanel()) {
            holder.onSwingClick();
            return;
        }
    }

    private static void teleportPlayerToPlayer(ServerPlayerEntity player, ServerPlayerEntity target) {
        Vec3d pos = target.getPos();
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        float pitch = target.getPitch(1);
        float yaw = target.getYaw(1);
        ServerWorld world = target.getServerWorld();
        ChunkPos chunkPos = new ChunkPos(new BlockPos(x, y, z));
        world.getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getEntityId());
        player.stopRiding();
        if (player.isSleeping()) {
            player.wakeUp(true, true);
        }

        if (player.interactionManager.getGameMode() != target.interactionManager.getGameMode()) {
            if (target.interactionManager.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(target.interactionManager.getGameMode());
            }
        }

        if (world == player.getServerWorld()) {
            Set<PlayerPositionLookS2CPacket.Flag> set = EnumSet.noneOf(PlayerPositionLookS2CPacket.Flag.class);
            player.networkHandler.teleportRequest(x, y, z, yaw, pitch, set);
        } else {
            player.teleport(world, x, y, z, yaw, pitch);
        }

        player.setHeadYaw(yaw);
    }

    public static void teleportToGuide(ServerPlayerEntity player) {
        ServerPlayerEntity guidePlayer = currentGuidePlayer == null ? null
                : player.getServer().getPlayerManager().getPlayer(currentGuidePlayer);

        if (guidePlayer == null || !guidePlayer.isAlive()) {
            sendActionBarMessage(player, "There is no tour guide to teleport to!");
            return;
        }

        sendActionBarMessage(player, "Teleporting to " + guidePlayer.getName().asString());
        sendToOps(player.getServer(), "Teleported " + player.getDisplayName().asString() + " to guide "
                + guidePlayer.getDisplayName().asString());

        teleportPlayerToPlayer(player, guidePlayer);
    }

    public static void sendMapLink(ServerPlayerEntity player) {

        String dimension = "survival%20-%20overworld/survivalday";

        if (player.getServerWorld().getRegistryKey() == RegistryKey.of(Registry.DIMENSION,
                DimensionType.OVERWORLD_ID)) {
            dimension = "survival%20-%20overworld/survivalday";
        } else if (player.getServerWorld().getRegistryKey() == RegistryKey.of(Registry.DIMENSION,
                DimensionType.THE_NETHER_ID)) {
            dimension = "survival%20-%20nether/survivalnether";
        } else if (player.getServerWorld().getRegistryKey() == RegistryKey.of(Registry.DIMENSION,
                DimensionType.THE_END_ID)) {
            dimension = "survival%20-%20end/survivalend";
        }

        String link = "http://mechanists.org/maps/#/" + player.getBlockPos().getX() + "/64/"
                + player.getBlockPos().getZ() + "/-1/" + dimension;

        player.sendSystemMessage(Text.Serializer.fromJson(
                "{\"text\":\"[Click Me]\",\"color\":\"dark_green\",\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\""
                        + link + "\"}]},\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + link + "\"}}"),
                Util.NIL_UUID);
    }

    private static void sendToOps(MinecraftServer server, String message) {
        Text text = (new LiteralText(message)).formatted(new Formatting[] { Formatting.GRAY, Formatting.ITALIC });

        if (server.getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
            Iterator<ServerPlayerEntity> var3 = server.getPlayerManager().getPlayerList().iterator();
            while (var3.hasNext()) {
                ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) var3.next();
                if (server.getPlayerManager().isOperator(serverPlayerEntity.getGameProfile())) {
                    serverPlayerEntity.sendSystemMessage(text, Util.NIL_UUID);
                }
            }
        }

        if (server.getGameRules().getBoolean(GameRules.LOG_ADMIN_COMMANDS)) {
            server.sendSystemMessage(text, Util.NIL_UUID);
        }

    }

    public static void teleportToWaypoint(ServerPlayerEntity player, Waypoint waypoint, boolean broadcast) {

        if (Configs.configs.disableWaypoints) {
            sendActionBarMessage(player, "Waypoints are disabled!");
            return;
        }

        if (Configs.configs.vanillaMode) {
            sendActionBarMessage(player, "Waypoint teleport is disabled!");
            return;
        }

        sendActionBarMessage(player, "Teleporting to " + waypoint.getName());

        double x = (double) waypoint.getX() + 0.5;
        double y = (double) waypoint.getY() + 0.5;
        double z = (double) waypoint.getZ() + 0.5;
        float pitch = player.getPitch(1);
        float yaw = player.getYaw(1);
        Identifier identifier = new Identifier(waypoint.getDimension());
        RegistryKey<World> registryKey = RegistryKey.of(Registry.DIMENSION, identifier);
        ServerWorld world = player.getServer().getWorld(registryKey);
        ChunkPos chunkPos = new ChunkPos(new BlockPos(x, y, z));
        world.getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getEntityId());
        player.stopRiding();
        if (player.isSleeping()) {
            player.wakeUp(true, true);
        }

        if (world == player.getServerWorld()) {
            Set<PlayerPositionLookS2CPacket.Flag> set = EnumSet.noneOf(PlayerPositionLookS2CPacket.Flag.class);
            player.networkHandler.teleportRequest(x, y, z, yaw, pitch, set);
        } else {
            player.teleport(world, x, y, z, yaw, pitch);
        }
        if (broadcast)
            sendToOps(player.getServer(), "Teleported " + player.getDisplayName().asString() + " to waypoint "
                    + waypoint.getName() + " in dimension " + waypoint.getDimension());

    }

}
