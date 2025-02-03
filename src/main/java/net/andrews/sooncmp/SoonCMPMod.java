package net.andrews.sooncmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.andrews.sooncmp.mapgui.EphemeralMapGui;
import net.andrews.sooncmp.mapgui.gui.GuideMenuGUI;
import net.andrews.sooncmp.mapgui.gui.Resources;
import net.andrews.sooncmp.mapgui.gui.WaypointsMenuGui;
import net.andrews.sooncmp.slideshow.SlideshowConfig;
import net.andrews.sooncmp.slideshow.SlideshowGUI;
import net.andrews.sooncmp.slideshow.SlideshowManager;
import net.andrews.sooncmp.waypoint.Waypoint;
import net.andrews.sooncmp.waypoint.WaypointIcons;
import net.andrews.sooncmp.waypoint.WaypointManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class SoonCMPMod {
    private static HashMap<ServerPlayerEntity, EphemeralMapGui> guiHolders = new HashMap<>();

    private static HashMap<String, PlayerInfo> playerInfos = new HashMap<>();

    private static ArrayList<SlideshowGUI> slideshowGUIs = new ArrayList<>();

    public static WaypointManager waypointManager;

    public static SlideshowManager slideshowManager;

    public static boolean shouldInit = false;

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
        slideshowManager = new SlideshowManager();
        shouldInit = true;

        dispatcher.register(CommandManager.literal("sooncmp").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermissionLevel(2);
        })
                .then(CommandManager.literal("config").then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> CommandSource.suggestMatching(Configs.getFields(), b)).then(CommandManager
                                .argument("value", StringArgumentType.greedyString()).executes(SoonCMPMod::setConfig))
                        .executes(SoonCMPMod::getConfig)))

        );

        dispatcher
                .register(
                        CommandManager
                                .literal(
                                        "waypoint")
                                .then(CommandManager.literal("list").executes(SoonCMPMod::listWaypoints)
                                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                                .executes(SoonCMPMod::listWaypoints))
                                        .then(CommandManager.literal("all").executes(SoonCMPMod::listAllWaypoints))
                                        .executes(SoonCMPMod::listWaypoints))
                                .then(CommandManager.literal("add").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                                .then(CommandManager.argument("icon", StringArgumentType.word())
                                                        .suggests((c, b) -> CommandSource
                                                                .suggestMatching(WaypointIcons.getIconNames(), b))
                                                        .then(CommandManager
                                                                .argument("name", StringArgumentType.greedyString())
                                                                .executes(SoonCMPMod::addWaypoint)))))
                                        .then(CommandManager.argument("icon", StringArgumentType.word())
                                                .suggests(
                                                        (c, b) -> CommandSource
                                                                .suggestMatching(WaypointIcons.getIconNames(), b))
                                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                                        .executes(SoonCMPMod::addWaypoint)))

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
                                                        .executes(SoonCMPMod::removeWaypoint)))
                                        .executes(SoonCMPMod::removeWaypoint2))
                                .then(CommandManager.literal("modifyPos").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                        .executes(SoonCMPMod::modifyPos)).executes(SoonCMPMod::modifyPos))
                                .then(CommandManager.literal("modifyIcon").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("icon", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(WaypointIcons.getIconNames(),
                                                b))
                                        .executes(SoonCMPMod::modifyIcon)))
                                .then(CommandManager.literal("modifyName").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("newname", StringArgumentType.greedyString())
                                        .executes(SoonCMPMod::modifyName)))
                                .then(CommandManager.literal("modifyColor").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("r", IntegerArgumentType.integer(0, 255))
                                        .then(CommandManager.argument("g", IntegerArgumentType.integer(0, 255))
                                                .then(CommandManager.argument("b", IntegerArgumentType.integer(0, 255))
                                                        .executes(SoonCMPMod::modifyColor)))))
                                .then(CommandManager.literal("move").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.argument("newindex", IntegerArgumentType.integer())
                                        .executes(SoonCMPMod::moveCommand)))
                                .then(CommandManager.literal("reload").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermissionLevel(2);
                                }).then(CommandManager.literal("icons").executes(SoonCMPMod::reloadIconsCommand))
                                        .executes(SoonCMPMod::reloadCommand))
                                .executes(SoonCMPMod::openGuiCommand));

        dispatcher.register(CommandManager.literal("slideshow").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermissionLevel(2);
        }).then(CommandManager.literal("place").then(CommandManager.argument("pos1", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("pos2", BlockPosArgumentType.blockPos())
                        .executes(SoonCMPMod::openSlideshowCommand))))
                .then(CommandManager.literal("remove").executes(SoonCMPMod::removeSlideshowCommand)));

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
                                .executes(SoonCMPMod::tpWaypoint)))));

        // dispatcher.register(CommandManager.literal("opengui").executes(SoonCMPMod::openGuiCommand));

    }

    private static void sendFeedback(CommandContext<ServerCommandSource> ctx, String str, boolean ops) {
        ctx.getSource().sendFeedback(() -> Text.literal(str), ops);
    }

    private static void sendFeedback(CommandContext<ServerCommandSource> ctx, String str) {
        sendFeedback(ctx, str, false);
    }

    private static int openSlideshowCommand(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) {
                sendFeedback(ctx, "You must be a player to use this command!", true);
                return 1;
            }

            BlockPos pos1 = BlockPosArgumentType.getBlockPos(ctx, "pos1");
            BlockPos pos2 = BlockPosArgumentType.getBlockPos(ctx, "pos2");

            BlockPos minPos = BlockPos.min(pos1, pos2);
            BlockPos maxPos = BlockPos.max(pos1, pos2);
            BlockPos diff = maxPos.subtract(minPos);
            if (diff.getX() != 0 && diff.getZ() != 0) {
                sendFeedback(ctx, "You must select a 2D area!", true);
                return 1;
            }

            int width = diff.getX() + diff.getZ() + 1;
            int height = diff.getY() + 1;
            if (width * height > 512) {
                sendFeedback(ctx, "You must select an area smaller than 512 blocks!", true);
                return 1;
            }

            Direction facing = player.getHorizontalFacing().getOpposite();
            SlideshowConfig config = new SlideshowConfig(player.getWorld().getRegistryKey().getValue().getPath(),
                    minPos.getX(), minPos.getY(), minPos.getZ(), width, height, facing.getName());
            if (slideshowManager.hasSlideshow(config)) {
                sendFeedback(ctx, "There is already a slideshow here!", true);
                return 1;
            }
            slideshowManager.addSlideshow(config);
            slideshowGUIs.add(new SlideshowGUI(player.getServer(), config));
            sendFeedback(ctx, "Opened slideshow!", true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }
        return 1;
    }

    private static int removeSlideshowCommand(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) {
                sendFeedback(ctx, "You must be a player to use this command!", true);
                return 1;
            }

            SlideshowGUI gui = null;
            for (SlideshowGUI g : slideshowGUIs) {
                if (!shouldPlayerBeInSlideshow(player, g)) {
                    continue;
                }

                if (g.getMousePosForPlayer(player) != null) {
                    gui = g;
                    break;
                }
            }

            if (gui == null) {
                sendFeedback(ctx, "You must be looking at a slideshow!", true);
                return 1;
            }

            slideshowManager.removeSlideshow(gui.getConfig());
            gui.closePanel();
            slideshowGUIs.remove(gui);
            sendFeedback(ctx, "Removed slideshow!", true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);
        }
        return 1;
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
                pos = Vec3ArgumentType.getPosArgument(ctx, "pos").getPos(ctx.getSource());
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
            EphemeralMapGui holder = guiHolders.get(ctx.getSource().getPlayer());

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

            EphemeralMapGui holder = guiHolders.get(ctx.getSource().getPlayer());

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

            EphemeralMapGui holder = guiHolders.get(ctx.getSource().getPlayer());

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
                pos = Vec3ArgumentType.getPosArgument(ctx, "pos").getPos(ctx.getSource());
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

            EphemeralMapGui holder = guiHolders.get(ctx.getSource().getPlayer());

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

            EphemeralMapGui holder = guiHolders.get(ctx.getSource().getPlayer());

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

            EphemeralMapGui holder = guiHolders.get(ctx.getSource().getPlayer());

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

    public static boolean shouldPlayerBeInSlideshow(ServerPlayerEntity player, SlideshowGUI gui) {
        if (player.isDisconnected() || !player.isAlive())
            return false;
        if (player.getServerWorld() != gui.getPanelWorld())
            return false;
        if (!player.getBlockPos().isWithinDistance(gui.getPanelOpenPos(), 100))
            return false;
        // check if player is in front
        BlockPos playerPos = player.getBlockPos();
        BlockPos panelPos = gui.getPanelOpenPos();
        Direction facing = gui.getPanelFacingSide();

        if (facing == Direction.NORTH) {
            if (playerPos.getZ() > panelPos.getZ())
                return false;
        } else if (facing == Direction.SOUTH) {
            if (playerPos.getZ() < panelPos.getZ())
                return false;
        } else if (facing == Direction.EAST) {
            if (playerPos.getX() < panelPos.getX())
                return false;
        } else if (facing == Direction.WEST) {
            if (playerPos.getX() > panelPos.getX())
                return false;
        }
        return true;
    }

    public static void onBeforeTick(MinecraftServer minecraftServer) {
        if (shouldInit) {
            shouldInit = false;
            slideshowManager.getSlideshows().forEach(config -> {
                slideshowGUIs.add(new SlideshowGUI(minecraftServer, config));
            });
        }
        Iterator<EphemeralMapGui> guis = guiHolders.values().iterator();
        while (guis.hasNext()) {
            EphemeralMapGui gui = guis.next();
            if (gui.shouldRemove()) {
                gui.closeGui();
                gui.closePanel();
                guis.remove();
            } else {
                gui.tick();
            }
        }

        // check if need to add/remove players from slideshows
        slideshowGUIs.forEach(gui -> {
            minecraftServer.getPlayerManager().getPlayerList().forEach(player -> {
                if (shouldPlayerBeInSlideshow(player, gui)) {
                    gui.addPlayer(player);
                } else {
                    gui.removePlayer(player);
                }
            });
        });

        Iterator<SlideshowGUI> slideshows = slideshowGUIs.iterator();
        while (slideshows.hasNext()) {
            SlideshowGUI gui = slideshows.next();
            gui.tick();
        }

        Iterator<PlayerInfo> infos = playerInfos.values().iterator();
        while (infos.hasNext()) {
            PlayerInfo info = infos.next();

            if (info.player.isDisconnected()) {
                slideshowGUIs.forEach(gui -> {
                    gui.removePlayer(info.player);
                });
                infos.remove();
            } else {
                if (info.teleportCooldown > 0)
                    info.teleportCooldown--;
                if (info.clickCooldown > 0)
                    info.clickCooldown--;
            }
        }

    }

    public static void onUpdateSelectedSlot(ServerPlayNetworkHandler serverPlayNetworkHandler, int selectedSlot) {

        ServerPlayerEntity player = serverPlayNetworkHandler.player;

        EphemeralMapGui holder = guiHolders.get(player);
        if (holder != null) {
            if (holder.onUpdateSelectedSlot(serverPlayNetworkHandler, selectedSlot)) {

                return;
            }
        }

        // check slideshow
        for (SlideshowGUI gui : slideshowGUIs) {
            if (shouldPlayerBeInSlideshow(player, gui) && gui.getMousePosForPlayer(player) != null) {
                gui.onUpdateSelectedSlot(serverPlayNetworkHandler, selectedSlot);
            }
        }
    }

    public static void sendActionBarMessage(ServerPlayerEntity player, String str) {

        Utils.sendPacket(player, new OverlayMessageS2CPacket(Text.literal(str)));
    }

    public static void openGuideGUI(ServerPlayerEntity player) {
        ((ThreadExecutor<?>) player.getServer()).execute(() -> {
            EphemeralMapGui holder = guiHolders.get(player);
            if (holder == null) {
                holder = new EphemeralMapGui(player);
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

    public static void onInteractItem(ServerPlayerEntity player, CallbackInfoReturnable<ActionResult> ci) {

        PlayerInfo info = getPlayerInfo(player);

        EphemeralMapGui holder = guiHolders.get(player);
        if (holder != null && holder.isTrackingPanel()) {
            if (info.clickCooldown > 0) {
                info.clickCooldown = 6;
                holder.onInteractClick();
            }
            ci.setReturnValue(ActionResult.CONSUME);
            return;
        }

        for (SlideshowGUI gui : slideshowGUIs) {
            if (shouldPlayerBeInSlideshow(player, gui) && gui.getMousePosForPlayer(player) != null) {
                if (info.clickCooldown > 0) {
                    info.clickCooldown = 6;
                    gui.onInteractClick(player);
                }
                ci.setReturnValue(ActionResult.CONSUME);
                return;
            }
        }
    }

    public static void onBlockBreak(ServerPlayerEntity player, CallbackInfo ci) {
        EphemeralMapGui holder = guiHolders.get(player);
        if (holder != null && holder.isTrackingPanel()) {

            ci.cancel();
            return;
        }

        for (SlideshowGUI gui : slideshowGUIs) {
            if (shouldPlayerBeInSlideshow(player, gui) && gui.getMousePosForPlayer(player) != null) {
                ci.cancel();
                return;
            }
        }
    }

    public static void onSwingClick(ServerPlayerEntity player) {

        PlayerInfo info = getPlayerInfo(player);
        if (info.clickCooldown > 0) {
            return;
        }

        EphemeralMapGui holder = guiHolders.get(player);
        if (holder != null && holder.isTrackingPanel()) {
            info.clickCooldown = 6;
            holder.onSwingClick();
            return;
        }

        for (SlideshowGUI gui : slideshowGUIs) {
            if (shouldPlayerBeInSlideshow(player, gui) && gui.getMousePosForPlayer(player) != null) {
                info.clickCooldown = 6;
                gui.onSwingClick(player);
                return;
            }
        }
    }

    public static PlayerInfo getPlayerInfo(ServerPlayerEntity player) {
        PlayerInfo info = playerInfos.get(player.getGameProfile().getName());
        if (info == null) {
            info = new PlayerInfo(player);
            playerInfos.put(player.getGameProfile().getName(), info);
        }
        return info;
    }

    public static void sendMapLink(ServerPlayerEntity player) {

        String dimension = "New%20World";

        if (player.getWorld().getRegistryKey() == World.OVERWORLD) {
            dimension = "New%20World";
        } else if (player.getWorld().getRegistryKey() == World.NETHER) {
            dimension = "DIM-1";
        } else if (player.getWorld().getRegistryKey() == World.END) {
            dimension = "DIM1";
        }

        String link = Configs.configs.mapUrlBase + "/?worldname=" + dimension + "&mapname=flat&zoom=5&x="
                + player.getBlockPos().getX() + "&y=64&z="
                + player.getBlockPos().getZ();

        player.sendMessage(Text.Serialization.fromJson(
                "{\"text\":\"[Click Me]\",\"color\":\"dark_green\",\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\""
                        + link + "\"}]},\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + link + "\"}}",
                player.getRegistryManager()));
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

        ((ThreadExecutor<?>) player.getServer()).execute(() -> {
            teleportToWaypointInternal(player, waypoint, broadcast);
        });
    }

    public static void teleportToSpawn(ServerPlayerEntity player) {

        sendActionBarMessage(player, "Teleporting to spawn");

        ((ThreadExecutor<?>) player.getServer()).execute(() -> {
            teleportToWaypointInternal(player, new Waypoint(9.5, -43, -6.5, "overworld", "Spawn", "spawn"), false);
        });
    }

    public static void teleportToWaypointInternal(ServerPlayerEntity player, Waypoint waypoint, boolean broadcast) {

        double x = waypoint.getX();
        double y = waypoint.getY();
        double z = waypoint.getZ();
        float pitch = player.getPitch(1);
        float yaw = player.getYaw(1);
        Set<PositionFlag> set = EnumSet.noneOf(PositionFlag.class);
        Identifier identifier = Identifier.of(waypoint.getDimension());
        RegistryKey<World> registryKey = RegistryKey.of(RegistryKeys.WORLD, identifier);
        ServerWorld world = player.getServer().getWorld(registryKey);

        if (world == player.getWorld()) {
            player.networkHandler.requestTeleport(x, y, z, yaw, pitch);
        } else {
            player.teleport(world, x, y, z, set, yaw, pitch, true);
        }
        if (broadcast)
            sendToOps(player.getServer(), "Teleported " + player.getDisplayName().getString() + " to waypoint "
                    + waypoint.getName() + " in dimension " + waypoint.getDimension());

    }

}
