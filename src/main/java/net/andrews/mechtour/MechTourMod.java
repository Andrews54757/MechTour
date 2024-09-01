package net.andrews.mechtour;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.andrews.mechtour.mapgui.MapGuiHolder;
import net.andrews.mechtour.mapgui.gui.GuideMenuGUI;
import net.andrews.mechtour.mapgui.gui.Resources;
import net.andrews.mechtour.mapgui.gui.WaypointsMenuGui;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class MechTourMod {

    private static String currentGuidePlayer = "";

    private static int guideCooldown = 40;
    private static HashMap<ServerPlayerEntity, MapGuiHolder> guiHolders = new HashMap<>();

    private static HashMap<String, PlayerInfo> playerInfos = new HashMap<>();

    public static WaypointManager waypointManager;

    public static void init() {

        // Initialize resources
        // Resources.noop();
        // WaypointIcons.noop();

    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {

        Configs.loadFromFile();

        Resources.noop();
        WaypointIcons.noop();
        waypointManager = new WaypointManager();

        dispatcher.register(CommandManager.literal("mechtour").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermissionLevel(2);
        }).then(CommandManager.literal("giveitem")
                .then(CommandManager.literal("guide")
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                .executes(MechTourMod::giveGuides))

                        .executes(MechTourMod::giveGuide)))
                .then(CommandManager.literal("config").then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> CommandSource.suggestMatching(Configs.getFields(), b)).then(CommandManager
                                .argument("value", StringArgumentType.greedyString()).executes(MechTourMod::setConfig))
                        .executes(MechTourMod::getConfig)))

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
                                                        .executes(MechTourMod::removeWaypoint)))
                                        .executes(MechTourMod::removeWaypoint2))
                                .then(CommandManager.literal("modifyPos").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                        .executes(MechTourMod::modifyPos)).executes(MechTourMod::modifyPos))
                                .then(CommandManager.literal("modifyIcon").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("icon", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(WaypointIcons.getIconNames(),
                                                b))
                                        .executes(MechTourMod::modifyIcon)))
                                .then(CommandManager.literal("modifyName").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("newname", StringArgumentType.greedyString())
                                        .executes(MechTourMod::modifyName)))
                                .then(CommandManager.literal("modifyColor").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("r", IntegerArgumentType.integer(0, 255))
                                        .then(CommandManager.argument("g", IntegerArgumentType.integer(0, 255))
                                                .then(CommandManager.argument("b", IntegerArgumentType.integer(0, 255))
                                                        .executes(MechTourMod::modifyColor)))))
                                .then(CommandManager.literal("move").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("newindex", IntegerArgumentType.integer())
                                        .executes(MechTourMod::moveCommand)))
                                .then(CommandManager.literal("reload").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.literal("icons").executes(MechTourMod::reloadIconsCommand))
                                        .executes(MechTourMod::reloadCommand)));

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

        dispatcher.register(CommandManager.literal("opengui").executes(MechTourMod::openGuiCommand));

    }

    private static void sendFeedback(CommandContext<ServerCommandSource> ctx, String str, boolean ops) {
        ctx.getSource().sendFeedback(Text.literal(str), ops);
    }

    private static void sendFeedback(CommandContext<ServerCommandSource> ctx, String str) {
        sendFeedback(ctx, str, false);
    }

    private static int openGuiCommand(CommandContext<ServerCommandSource> ctx) {
        try {
            if (Configs.configs.disableGui) {
                sendFeedback(ctx, "Guide gui is disabled!", true);
                return 1;
            }
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                openGuideGUI(player);
            }
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }

    private static int setConfig(CommandContext<ServerCommandSource> ctx) {
        try {

            String name = StringArgumentType.getString(ctx, "name");
            String value = StringArgumentType.getString(ctx, "value");

            if (Configs.setConfig(name, value)) {
                sendFeedback(ctx, "Set " + name + " to:\n" + value, true);
            } else {
                sendFeedback(ctx, "Failed to set " + name + " to " + value, true);
            }

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }

    private static int getConfig(CommandContext<ServerCommandSource> ctx) {
        try {

            String name = StringArgumentType.getString(ctx, "name");

            sendFeedback(ctx, name + " is set to:\n" + Configs.getConfig(name), true);

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

            waypointManager.addWaypoint(pos.getX(), pos.getY(), pos.getZ(), dimension, name, icon);
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
                sendFeedback(ctx,
                        "Removed " + count + " waypoints with the name " + name + " in dimension " + dimension, true);
            }
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }

        return 1;
    }

    private static int removeWaypoint2(CommandContext<ServerCommandSource> ctx) {
        try {
            MapGuiHolder holder = guiHolders.get(ctx.getSource().getPlayer());

            if (holder == null || !holder.isPanelOpen() || holder.getGui() == null
                    || !(holder.getGui() instanceof WaypointsMenuGui)) {
                sendFeedback(ctx, "Waypoint gui is not open!", true);
                return 1;
            }

            WaypointsMenuGui gui = (WaypointsMenuGui) holder.getGui();

            Waypoint waypoint = gui.getWaypoint();

            waypointManager.removeWaypoint(waypoint);
            sendFeedback(ctx,
                    "Removed waypoint with name " + waypoint.getName() + " in dimension " + waypoint.getDimension(),
                    true);

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }
        return 1;
    }

    private static int moveCommand(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            MapGuiHolder holder = guiHolders.get(ctx.getSource().getPlayer());

            if (holder == null || !holder.isPanelOpen() || holder.getGui() == null
                    || !(holder.getGui() instanceof WaypointsMenuGui)) {
                sendFeedback(ctx, "Waypoint gui is not open!", true);
                return 1;
            }

            WaypointsMenuGui gui = (WaypointsMenuGui) holder.getGui();

            Waypoint waypoint = gui.getWaypoint();

            if (waypoint == null) {
                sendFeedback(ctx, "You need to point at the waypoint!", true);
                return 1;
            }
            int newpos = IntegerArgumentType.getInteger(ctx, "newindex");
            waypointManager.moveWaypoint(waypoint, newpos);
            waypointManager.waypointsUpdated();
            sendFeedback(ctx, "Changed index of waypoint " + waypoint.getName() + " in dimension "
                    + waypoint.getDimension() + " to " + newpos + "!", true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }

        return 1;
    }

    private static int modifyPos(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            MapGuiHolder holder = guiHolders.get(ctx.getSource().getPlayer());

            if (holder == null || !holder.isPanelOpen() || holder.getGui() == null
                    || !(holder.getGui() instanceof WaypointsMenuGui)) {
                sendFeedback(ctx, "Waypoint gui is not open!", true);
                return 1;
            }

            WaypointsMenuGui gui = (WaypointsMenuGui) holder.getGui();

            Waypoint waypoint = gui.getWaypoint();

            if (waypoint == null) {
                sendFeedback(ctx, "You need to point at the waypoint!", true);
                return 1;
            }
            Vec3d pos;

            try {
                pos = Vec3ArgumentType.getPosArgument(ctx, "pos").toAbsolutePos(ctx.getSource());
            } catch (Exception e) {
                pos = ctx.getSource().getPosition();
            }

            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();
            
            waypoint.setX(x);
            waypoint.setY(y);
            waypoint.setZ(z);

            waypointManager.waypointsUpdated();
            sendFeedback(ctx, "Changed position of waypoint " + waypoint.getName() + " in dimension "
                    + waypoint.getDimension() + " to " + x + ", " + y + ", " + z + "!", true);
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

            MapGuiHolder holder = guiHolders.get(ctx.getSource().getPlayer());

            if (holder == null || !holder.isPanelOpen() || holder.getGui() == null
                    || !(holder.getGui() instanceof WaypointsMenuGui)) {
                sendFeedback(ctx, "Waypoint gui is not open!", true);
                return 1;
            }

            WaypointsMenuGui gui = (WaypointsMenuGui) holder.getGui();

            Waypoint waypoint = gui.getWaypoint();

            if (waypoint == null) {
                sendFeedback(ctx, "You need to point at the waypoint!", true);
                return 1;
            }
            String icon = StringArgumentType.getString(ctx, "icon");
            waypoint.setIconName(icon);
            waypointManager.waypointsUpdated();
            sendFeedback(ctx, "Changed icon of waypoint " + waypoint.getName() + " in dimension "
                    + waypoint.getDimension() + " to " + icon + "!", true);
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

            MapGuiHolder holder = guiHolders.get(ctx.getSource().getPlayer());

            if (holder == null || !holder.isPanelOpen() || holder.getGui() == null
                    || !(holder.getGui() instanceof WaypointsMenuGui)) {
                sendFeedback(ctx, "Waypoint gui is not open!", true);
                return 1;
            }

            WaypointsMenuGui gui = (WaypointsMenuGui) holder.getGui();

            Waypoint waypoint = gui.getWaypoint();

            if (waypoint == null) {
                sendFeedback(ctx, "You need to point at the waypoint!", true);
                return 1;
            }

            String newname = StringArgumentType.getString(ctx, "newname");
            waypoint.setName(newname);
            waypointManager.waypointsUpdated();
            sendFeedback(ctx, "Changed name of waypoint " + waypoint.getName() + " in dimension "
                    + waypoint.getDimension() + " to " + newname + "!", true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }

        return 1;
    }

    private static int modifyColor(CommandContext<ServerCommandSource> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            if (Configs.configs.disableWaypointEdits) {
                sendFeedback(ctx, "Waypoint editing is disabled!");
                return 1;
            }

            MapGuiHolder holder = guiHolders.get(ctx.getSource().getPlayer());

            if (holder == null || !holder.isPanelOpen() || holder.getGui() == null
                    || !(holder.getGui() instanceof WaypointsMenuGui)) {
                sendFeedback(ctx, "Waypoint gui is not open!", true);
                return 1;
            }

            WaypointsMenuGui gui = (WaypointsMenuGui) holder.getGui();

            Waypoint waypoint = gui.getWaypoint();

            if (waypoint == null) {
                sendFeedback(ctx, "You need to point at the waypoint!", true);
                return 1;
            }

            int r = IntegerArgumentType.getInteger(ctx, "r");
            int g = IntegerArgumentType.getInteger(ctx, "g");
            int b = IntegerArgumentType.getInteger(ctx, "b");
            waypoint.setColor(r, g, b);
            waypointManager.waypointsUpdated();
            sendFeedback(ctx, "Changed color of waypoint " + waypoint.getName() + " in dimension "
                    + waypoint.getDimension() + " to " + r + ", " + g + ", " + b + "!", true);
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
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putBoolean("mechtour_guide_item", true);
        NbtList t = new NbtList();
        t.add(new NbtCompound());
        nbtCompound.put("Enchantments", t);
        NbtCompound displayTag = new NbtCompound();
        NbtList NbtList = new NbtList();
        NbtList.add(NbtString.of("{\"text\":\"Don't Panic\",\"color\":\"dark_green\"}"));
        displayTag.put("Lore", NbtList);
        nbtCompound.put("display", displayTag);
        nbtCompound.putBoolean("LodestoneTracked", false);
        stack.setNbt(nbtCompound);
        stack.setCustomName(Text.literal("\u00A76\u00A7oHitchhikers Guide\u00A7r"));
        stack.setBobbingAnimationTime(1);
        return player.getInventory().insertStack(stack);
    }

    private static boolean canTeleport(ServerPlayerEntity player, ServerPlayerEntity guidePlayer) {

        if (player == null || !player.isAlive()) {
            return false;
        }

        if (guidePlayer == null || !guidePlayer.isAlive()) {
            sendActionBarMessage(player, "There is no tour guide to teleport to!");
            return false;
        }

        if (player.getVehicle() != null) {
            sendActionBarMessage(player, "You are riding something! Teleport aborted!");
            return false;
        }

        if (guidePlayer.getVehicle() != null) {
            sendActionBarMessage(player, "Guide is riding something! Teleport aborted!");
            return false;
        }

        return true;
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

                        ItemStack stack = player.getInventory().getStack(slot);
                        stack.getNbt().getCompound("display").remove("Lore");
                        if (guidePlayer != null && guidePlayer.isAlive()) {
                            NbtCompound posTag = stack.getOrCreateSubNbt("LodestonePos");
                            posTag.putInt("X", (int) guidePlayer.getX());
                            posTag.putInt("Y", (int) guidePlayer.getY());
                            posTag.putInt("Z", (int) guidePlayer.getZ());
                            stack.getNbt().putString("LodestoneDimension",
                                    guidePlayer.getWorld().getRegistryKey().getValue().toString());
                            NbtList NbtList = new NbtList();
                            NbtList.add(NbtString.of("{\"text\":\"Tracking " + guidePlayer.getName().getString()
                                    + "\",\"color\":\"blue\"}"));
                            stack.getNbt().getCompound("display").put("Lore", NbtList);
                        } else {
                            stack.removeSubNbt("LodestonePos");
                            stack.removeSubNbt("LodestoneDimension");
                            NbtList NbtList = new NbtList();
                            NbtList.add(NbtString.of("{\"text\":\"Don't Panic\",\"color\":\"dark_green\"}"));
                            stack.getNbt().getCompound("display").put("Lore", NbtList);
                        }

                        player.getInventory().setStack(slot, stack);
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

        Iterator<PlayerInfo> infos = playerInfos.values().iterator();
        while (infos.hasNext()) {
            PlayerInfo info = infos.next();

            if (info.player.isDisconnected()) {
                infos.remove();
            } else {
                if (info.teleportCooldown > 0)
                    info.teleportCooldown--;

                if (info.clickCooldown > 0)
                    info.clickCooldown--;

                if (info.teleportTimeout > 0) {
                    info.teleportTimeout--;
                    if (info.teleportTimeout == 0) {
                        ServerPlayerEntity guidePlayer = minecraftServer.getPlayerManager()
                                .getPlayer(currentGuidePlayer);
                        ServerPlayerEntity player = minecraftServer.getPlayerManager()
                                .getPlayer(info.player.getGameProfile().getName());

                        if (player != null && player.isAlive()) {
                            if (guidePlayer != null && guidePlayer.isAlive()) {

                                if (guidePlayer.getWorld() == info.world) {

                                    if (canTeleport(player, guidePlayer)) {
                                        sendToOps(player.getServer(), "Teleported " + player.getDisplayName().getString()
                                                + " to guide " + guidePlayer.getDisplayName().getString());

                                        teleportPlayerToPlayer(player, guidePlayer, info.pos);
                                    }

                                } else {
                                    sendActionBarMessage(player, "Guide has changed dimensions! Please try again!");
                                }
                            } else {
                                sendActionBarMessage(player, "There is no tour guide to teleport to!");
                            }
                        }
                    }
                }
            }
        }

    }

    public static List<Integer> getGuideSlots(ServerPlayerEntity player) {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < player.getInventory().main.size(); ++i) {
            ItemStack stack = player.getInventory().main.get(i);
            if (isGuideItem(stack)) {
                list.add(i);
            }
        }
        return list;
    }

    public static boolean isHoldingGuide(ServerPlayerEntity player) {
        if (player.isAlive() && PlayerInventory.isValidHotbarIndex(player.getInventory().selectedSlot)) {
            ItemStack stack = player.getInventory().getStack(player.getInventory().selectedSlot);
            return isGuideItem(stack);
        }
        return false;
    }

    public static boolean isGuideItem(ItemStack stack) {
        return !stack.isEmpty() && stack.hasNbt() && stack.getNbt().contains("mechtour_guide_item");
    }

    public static void onUpdateSelectedSlot(ServerPlayNetworkHandler serverPlayNetworkHandler, int selectedSlot) {

        ServerPlayerEntity player = serverPlayNetworkHandler.player;

        MapGuiHolder holder = guiHolders.get(player);
        if (holder != null) {
            if (holder.onUpdateSelectedSlot(serverPlayNetworkHandler, selectedSlot)) {

                return;
            }
        }

        ItemStack stack = player.getInventory().getStack(selectedSlot);
        if (isGuideItem(stack) && !Configs.configs.disableGuideItem && !Configs.configs.disableGuideHoldMessage) {
            if (Configs.configs.disableGui) {
                if (!Configs.configs.disableWaypoints && !Configs.configs.disableTourTeleport)
                    sendActionBarMessage(player, "Use to teleport to tour");
            } else {
                if (holder == null || !holder.isPanelOpen())
                    sendActionBarMessage(player, "Use to open menu");
            }
        }

    }

    public static void sendActionBarMessage(ServerPlayerEntity player, String str) {

        Utils.sendPacket(player, new OverlayMessageS2CPacket(Text.literal(str)));
    }

    public static void openGuideGUI(ServerPlayerEntity player) {
        ((ThreadExecutor) player.getServer()).execute(() -> {
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

        });
    }

    public static void onInteractClick(ServerPlayerEntity player, CallbackInfoReturnable<ActionResult> ci) {

        PlayerInfo info = getPlayerInfo(player);
        if (info.clickCooldown > 0) {
            return;
        }
        MapGuiHolder holder = guiHolders.get(player);
        if (holder != null && holder.isTrackingPanel()) {
            info.clickCooldown = 6;
            holder.onInteractClick();
            ci.setReturnValue(ActionResult.CONSUME);
            return;
        }

        if (MechTourMod.isHoldingGuide(player) && !Configs.configs.disableGuideItem) {
            info.clickCooldown = 6;
            if (Configs.configs.disableGui) {
                teleportToGuide(player);
            } else {
                MechTourMod.openGuideGUI(player);
            }
            ci.setReturnValue(ActionResult.CONSUME);
        }
    }

    public static void onSwingClick(ServerPlayerEntity player) {

        PlayerInfo info = getPlayerInfo(player);
        if (info.clickCooldown > 0) {
            return;
        }

        MapGuiHolder holder = guiHolders.get(player);
        if (holder != null && holder.isTrackingPanel()) {
            info.clickCooldown = 6;
            holder.onSwingClick();
            return;
        }
    }

    private static void teleportPlayerToPlayer(ServerPlayerEntity player, ServerPlayerEntity target, Vec3d pos) {
        ((ThreadExecutor) player.getServer()).execute(() -> {
            teleportPlayerToPlayerInternal(player, target, pos);
        });

    }

    private static void teleportPlayerToPlayerInternal(ServerPlayerEntity player, ServerPlayerEntity target,
            Vec3d pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        float pitch = target.getPitch(1);
        float yaw = target.getYaw(1);
        ServerWorld world = target.getWorld();
        ChunkPos chunkPos = new ChunkPos(new BlockPos(x, y, z));
        world.getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getId());
        player.stopRiding();
        if (player.isSleeping()) {
            player.wakeUp(true, true);
        }

        if (player.interactionManager.getGameMode() != target.interactionManager.getGameMode()) {
            if (target.interactionManager.getGameMode() == GameMode.SPECTATOR) {
                player.changeGameMode(target.interactionManager.getGameMode());
            }
        }

        if (world == player.getWorld()) {
            Set<PlayerPositionLookS2CPacket.Flag> set = EnumSet.noneOf(PlayerPositionLookS2CPacket.Flag.class);
            player.networkHandler.requestTeleport(x, y, z, yaw, pitch, set);
        } else {
            player.teleport(world, x, y, z, yaw, pitch);
        }

        player.setHeadYaw(yaw);
    }

    public static PlayerInfo getPlayerInfo(ServerPlayerEntity player) {
        PlayerInfo info = playerInfos.get(player.getGameProfile().getName());
        if (info == null) {
            info = new PlayerInfo(player);
            playerInfos.put(player.getGameProfile().getName(), info);
        }
        return info;
    }

    public static void teleportToGuide(ServerPlayerEntity player) {
        ServerPlayerEntity guidePlayer = currentGuidePlayer == null ? null
                : player.getServer().getPlayerManager().getPlayer(currentGuidePlayer);

        if (Configs.configs.disableTourTeleport) {
            sendActionBarMessage(player, "Teleport to tour is disabled!");
            return;
        }

        if (guidePlayer == null || !guidePlayer.isAlive()) {
            sendActionBarMessage(player, "There is no tour guide to teleport to!");
            return;
        }

        PlayerInfo info = getPlayerInfo(player);
        if (info.teleportCooldown > 0) {
            sendActionBarMessage(player,
                    "You must wait " + (info.teleportCooldown / 20) + " seconds to teleport again!");
            return;
        }

        if (!canTeleport(player, guidePlayer)) {
            return;
        }

        info.teleportCooldown = Configs.configs.teleportCooldown;

        info.teleportTimeout = Configs.configs.teleportTimeout;

        info.world = guidePlayer.getWorld();
        info.pos = guidePlayer.getPos();
        sendActionBarMessage(player, "Teleporting to " + guidePlayer.getName().getString() + " in "
                + (Configs.configs.teleportTimeout / 20) + " sec");
        /*
         * sendToOps(player.getServer(), "Teleported " +
         * player.getDisplayName().getString() + " to guide " +
         * guidePlayer.getDisplayName().getString());
         * 
         * teleportPlayerToPlayer(player, guidePlayer);
         */
    }

    public static void sendMapLink(ServerPlayerEntity player) {

        String dimension = "survival%20-%20overworld/survivalday";

        if (player.getWorld().getRegistryKey() == World.OVERWORLD) {
            dimension = "survival%20-%20overworld/survivalday";
        } else if (player.getWorld().getRegistryKey() == World.NETHER) {
            dimension = "survival%20-%20nether/survivalnether";
        } else if (player.getWorld().getRegistryKey() == World.END) {
            dimension = "survival%20-%20end/survivalend";
        }

        String link = Configs.configs.mapUrlBase + "/#/" + player.getBlockPos().getX() + "/64/"
                + player.getBlockPos().getZ() + "/-1/" + dimension;

        player.sendMessage(Text.Serializer.fromJson(
                "{\"text\":\"[Click Me]\",\"color\":\"dark_green\",\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\""
                        + link + "\"}]},\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + link + "\"}}"));
    }

    private static void sendToOps(MinecraftServer server, String message) {
        if (!Configs.configs.broadcastTeleportsToOps)
            return;
        Text text = (Text.literal(message)).formatted(Formatting.GRAY, Formatting.ITALIC);

        if (server.getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
            for (ServerPlayerEntity serverPlayerEntity : server.getPlayerManager().getPlayerList()) {
                if (server.getPlayerManager().isOperator(serverPlayerEntity.getGameProfile())) {
                    serverPlayerEntity.sendMessage(text);
                }
            }
        }

        if (server.getGameRules().getBoolean(GameRules.LOG_ADMIN_COMMANDS)) {
            server.sendMessage(text);
        }

    }

    public static void teleportToWaypoint(ServerPlayerEntity player, Waypoint waypoint, boolean broadcast) {

        if (Configs.configs.disableWaypoints) {
            sendActionBarMessage(player, "Waypoints are disabled!");
            return;
        }

        if (Configs.configs.disableWaypointsTeleport) {
            sendActionBarMessage(player, "Waypoint teleports are disabled!");
            return;
        }

        sendActionBarMessage(player, "Teleporting to " + waypoint.getName());

        ((ThreadExecutor) player.getServer()).execute(() -> {
            teleportToWaypointInternal(player, waypoint, broadcast);
        });
    }

    public static void teleportToWaypointInternal(ServerPlayerEntity player, Waypoint waypoint, boolean broadcast) {

        double x = waypoint.getX();
        double y = waypoint.getY();
        double z = waypoint.getZ();
        float pitch = player.getPitch(1);
        float yaw = player.getYaw(1);
        Identifier identifier = new Identifier(waypoint.getDimension());
        RegistryKey<World> registryKey = RegistryKey.of(RegistryKeys.WORLD, identifier);
        ServerWorld world = player.getServer().getWorld(registryKey);
        ChunkPos chunkPos = new ChunkPos(new BlockPos(x, y, z));
        world.getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getId());
        player.stopRiding();
        if (player.isSleeping()) {
            player.wakeUp(true, true);
        }

        if (world == player.getWorld()) {
            Set<PlayerPositionLookS2CPacket.Flag> set = EnumSet.noneOf(PlayerPositionLookS2CPacket.Flag.class);
            player.networkHandler.requestTeleport(x, y, z, yaw, pitch, set);
        } else {
            player.teleport(world, x, y, z, yaw, pitch);
        }
        if (broadcast)
            sendToOps(player.getServer(), "Teleported " + player.getDisplayName().getString() + " to waypoint "
                    + waypoint.getName() + " in dimension " + waypoint.getDimension());

    }

}
