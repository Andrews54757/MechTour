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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SoonCMPMod {
    private static HashMap<ServerPlayer, EphemeralMapGui> guiHolders = new HashMap<>();

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

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

        Configs.loadFromFile();

        Resources.noop();
        WaypointIcons.noop();
        waypointManager = new WaypointManager();
        slideshowManager = new SlideshowManager();
        shouldInit = true;

        dispatcher.register(Commands.literal("sooncmp").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermission(2);
        })
                .then(Commands.literal("config").then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Configs.getFields(), b)).then(Commands
                                .argument("value", StringArgumentType.greedyString()).executes(SoonCMPMod::setConfig))
                        .executes(SoonCMPMod::getConfig)))

        );

        dispatcher
                .register(
                        Commands
                                .literal(
                                        "waypoint")
                                .then(Commands.literal("list").executes(SoonCMPMod::listWaypoints)
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(SoonCMPMod::listWaypoints))
                                        .then(Commands.literal("all").executes(SoonCMPMod::listAllWaypoints))
                                        .executes(SoonCMPMod::listWaypoints))
                                .then(Commands.literal("add").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermission(2);
                                }).then(Commands.argument("pos", Vec3Argument.vec3())
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .then(Commands.argument("icon", StringArgumentType.word())
                                                        .suggests((c, b) -> SharedSuggestionProvider
                                                                .suggest(WaypointIcons.getIconNames(), b))
                                                        .then(Commands
                                                                .argument("name", StringArgumentType.greedyString())
                                                                .executes(SoonCMPMod::addWaypoint)))))
                                        .then(Commands.argument("icon", StringArgumentType.word())
                                                .suggests(
                                                        (c, b) -> SharedSuggestionProvider
                                                                .suggest(WaypointIcons.getIconNames(), b))
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(SoonCMPMod::addWaypoint)))

                                ).then(Commands.literal("remove").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermission(2);
                                }).then(Commands
                                        .argument("dimension", DimensionArgument.dimension()).then(
                                                Commands
                                                        .argument("name",
                                                                StringArgumentType.greedyString())
                                                        .suggests(
                                                                (c, b) -> SharedSuggestionProvider
                                                                        .suggest(
                                                                                waypointManager.getWaypointNames(
                                                                                        DimensionArgument
                                                                                                .getDimension(c,
                                                                                                        "dimension")
                                                                                                .dimension()
                                                                                                .location().getPath()),
                                                                                b))
                                                        .executes(SoonCMPMod::removeWaypoint)))
                                        .executes(SoonCMPMod::removeWaypoint2))
                                .then(Commands.literal("modifyPos").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermission(2);
                                }).then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(SoonCMPMod::modifyPos)).executes(SoonCMPMod::modifyPos))
                                .then(Commands.literal("modifyIcon").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermission(2);
                                }).then(Commands.argument("icon", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(WaypointIcons.getIconNames(),
                                                b))
                                        .executes(SoonCMPMod::modifyIcon)))
                                .then(Commands.literal("modifyName").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermission(2);
                                }).then(Commands.argument("newname", StringArgumentType.greedyString())
                                        .executes(SoonCMPMod::modifyName)))
                                .then(Commands.literal("modifyColor").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermission(2);
                                }).then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                                                .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                                                        .executes(SoonCMPMod::modifyColor)))))
                                .then(Commands.literal("move").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermission(2);
                                }).then(Commands.argument("newindex", IntegerArgumentType.integer())
                                        .executes(SoonCMPMod::moveCommand)))
                                .then(Commands.literal("reload").requires((serverCommandSource) -> {
                                    return serverCommandSource.hasPermission(2);
                                }).then(Commands.literal("icons").executes(SoonCMPMod::reloadIconsCommand))
                                        .executes(SoonCMPMod::reloadCommand))
                                .executes(SoonCMPMod::openGuiCommand));

        dispatcher.register(Commands.literal("slideshow").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermission(2);
        }).then(Commands.literal("place").then(Commands.argument("pos1", BlockPosArgument.blockPos())
                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                        .executes(SoonCMPMod::openSlideshowCommand))))
                .then(Commands.literal("remove").executes(SoonCMPMod::removeSlideshowCommand)));

        dispatcher.register(Commands.literal("tpw").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermission(2);
        }).then(Commands
                .argument("players",
                        EntityArgument.players())
                .then(Commands
                        .argument("dimension",
                                DimensionArgument.dimension())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((c,
                                        b) -> SharedSuggestionProvider.suggest(waypointManager.getWaypointNames(
                                                DimensionArgument.getDimension(c, "dimension")
                                                        .dimension().location().getPath()),
                                                b))
                                .executes(SoonCMPMod::tpWaypoint)))));

        // dispatcher.register(CommandManager.literal("opengui").executes(SoonCMPMod::openGuiCommand));

    }

    private static void sendFeedback(CommandContext<CommandSourceStack> ctx, String str, boolean ops) {
        ctx.getSource().sendSuccess(() -> Component.literal(str), ops);
    }

    private static void sendFeedback(CommandContext<CommandSourceStack> ctx, String str) {
        sendFeedback(ctx, str, false);
    }

    private static int openSlideshowCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player == null) {
                sendFeedback(ctx, "You must be a player to use this command!", true);
                return 1;
            }

            BlockPos pos1 = BlockPosArgument.getBlockPos(ctx, "pos1");
            BlockPos pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");

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

            Direction facing = player.getDirection().getOpposite();
            SlideshowConfig config = new SlideshowConfig(player.level().dimension().location().getPath(),
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

    private static int removeSlideshowCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayer();
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

    private static int openGuiCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            if (Configs.configs.disableGui) {
                sendFeedback(ctx, "Guide gui is disabled!", true);
                return 1;
            }
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player != null) {
                openGuideGUI(player);
            }

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }

    private static int setConfig(CommandContext<CommandSourceStack> ctx) {
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

    private static int getConfig(CommandContext<CommandSourceStack> ctx) {
        try {

            String name = StringArgumentType.getString(ctx, "name");

            sendFeedback(ctx, name + " is set to:\n" + Configs.getConfig(name), true);

        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }
        return 1;
    }

    private static int listAllWaypoints(CommandContext<CommandSourceStack> ctx) {

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

    private static int reloadCommand(CommandContext<CommandSourceStack> ctx) {

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

    private static int reloadIconsCommand(CommandContext<CommandSourceStack> ctx) {

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

    private static int listWaypoints(CommandContext<CommandSourceStack> ctx) {

        try {
            if (Configs.configs.disableWaypoints) {
                sendFeedback(ctx, "Waypoints are disabled!");
                return 1;
            }
            String dimension;

            try {
                dimension = DimensionArgument.getDimension(ctx, "dimension").dimension().location()
                        .getPath();
            } catch (Exception e) {
                dimension = ctx.getSource().getLevel().dimension().location().getPath();
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

    private static int addWaypoint(CommandContext<CommandSourceStack> ctx) {

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
            Vec3 pos;

            try {
                dimension = DimensionArgument.getDimension(ctx, "dimension").dimension().location()
                        .getPath();
                pos = Vec3Argument.getCoordinates(ctx, "pos").getPosition(ctx.getSource());
            } catch (Exception e) {
                dimension = ctx.getSource().getLevel().dimension().location().getPath();
                pos = ctx.getSource().getPosition();
            }

            String icon = StringArgumentType.getString(ctx, "icon");
            String name = StringArgumentType.getString(ctx, "name");

            waypointManager.addWaypoint(pos.x(), pos.y(), pos.z(), dimension, name, icon);
            sendFeedback(ctx, "Added waypoint " + name, true);
        } catch (Exception e) {
            sendFeedback(ctx, "An error has occured: " + e, true);

        }

        return 1;
    }

    private static int removeWaypoint(CommandContext<CommandSourceStack> ctx) {

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
                dimension = DimensionArgument.getDimension(ctx, "dimension").dimension().location()
                        .getPath();

            } catch (Exception e) {
                dimension = ctx.getSource().getLevel().dimension().location().getPath();
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

    private static int removeWaypoint2(CommandContext<CommandSourceStack> ctx) {
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

    private static int moveCommand(CommandContext<CommandSourceStack> ctx) {

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

    private static int modifyPos(CommandContext<CommandSourceStack> ctx) {

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
            Vec3 pos;

            try {
                pos = Vec3Argument.getCoordinates(ctx, "pos").getPosition(ctx.getSource());
            } catch (Exception e) {
                pos = ctx.getSource().getPosition();
            }

            double x = pos.x();
            double y = pos.y();
            double z = pos.z();

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

    private static int modifyIcon(CommandContext<CommandSourceStack> ctx) {

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

    private static int modifyName(CommandContext<CommandSourceStack> ctx) {

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

    private static int modifyColor(CommandContext<CommandSourceStack> ctx) {

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

    private static int tpWaypoint(CommandContext<CommandSourceStack> ctx) {

        try {

            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");

            String dimension;

            try {
                dimension = DimensionArgument.getDimension(ctx, "dimension").dimension().location()
                        .getPath();

            } catch (Exception e) {
                dimension = ctx.getSource().getLevel().dimension().location().getPath();
            }

            String name = StringArgumentType.getString(ctx, "name");

            Waypoint waypoint = waypointManager.getWaypoint(dimension, name);

            if (waypoint == null) {
                sendFeedback(ctx, "No waypoint " + name + " in dimension " + dimension + " exists!", true);
                return 1;
            }
            int count = 0;

            for (ServerPlayer player : players) {
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

    public static boolean shouldPlayerBeInSlideshow(ServerPlayer player, SlideshowGUI gui) {
        if (player.hasDisconnected() || !player.isAlive())
            return false;
        if (player.serverLevel() != gui.getPanelWorld())
            return false;
        if (!player.blockPosition().closerThan(gui.getPanelOpenPos(), 100))
            return false;
        // check if player is in front
        // BlockPos playerPos = player.getBlockPos();
        // BlockPos panelPos = gui.getPanelOpenPos();
        // Direction facing = gui.getPanelFacingSide();

        // if (facing == Direction.NORTH) {
        //     if (playerPos.getZ() > panelPos.getZ())
        //         return false;
        // } else if (facing == Direction.SOUTH) {
        //     if (playerPos.getZ() < panelPos.getZ())
        //         return false;
        // } else if (facing == Direction.EAST) {
        //     if (playerPos.getX() < panelPos.getX())
        //         return false;
        // } else if (facing == Direction.WEST) {
        //     if (playerPos.getX() > panelPos.getX())
        //         return false;
        // }
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
            minecraftServer.getPlayerList().getPlayers().forEach(player -> {
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

            if (info.player.hasDisconnected()) {
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

    public static void onUpdateSelectedSlot(ServerGamePacketListenerImpl serverPlayNetworkHandler, int selectedSlot) {

        ServerPlayer player = serverPlayNetworkHandler.player;

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

    public static void sendActionBarMessage(ServerPlayer player, String str) {

        Utils.sendPacket(player, new ClientboundSetActionBarTextPacket(Component.literal(str)));
    }

    public static void openGuideGUI(ServerPlayer player) {
        ((BlockableEventLoop<?>) player.getServer()).execute(() -> {
            EphemeralMapGui holder = guiHolders.get(player);
            if (holder == null) {
                holder = new EphemeralMapGui(player);
                holder.openGui(new GuideMenuGUI());
                guiHolders.put(player, holder);
            }

            Direction facing = player.getDirection().getOpposite();
            BlockPos playerPos = player.blockPosition().relative(player.getDirection(), 4);

            if (holder.isPanelOpen()) {
                holder.closePanel();
                sendActionBarMessage(player, "Closed guide menu!");
            } else {

                holder.openPanel(playerPos, facing, 7, 4);
                sendActionBarMessage(player, "Opened guide menu!");
            }

        });
    }

    public static void onInteractItem(ServerPlayer player, CallbackInfoReturnable<InteractionResult> ci) {

        PlayerInfo info = getPlayerInfo(player);

        EphemeralMapGui holder = guiHolders.get(player);
        if (holder != null && holder.isTrackingPanel()) {
            if (info.clickCooldown > 0) {
                info.clickCooldown = 6;
                holder.onInteractClick();
            }
            ci.setReturnValue(InteractionResult.CONSUME);
            return;
        }

        for (SlideshowGUI gui : slideshowGUIs) {
            if (shouldPlayerBeInSlideshow(player, gui) && gui.getMousePosForPlayer(player) != null) {
                if (info.clickCooldown > 0) {
                    info.clickCooldown = 6;
                    gui.onInteractClick(player);
                }
                ci.setReturnValue(InteractionResult.CONSUME);
                return;
            }
        }
    }

    public static void onBlockBreak(ServerPlayer player, CallbackInfo ci) {
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

    public static void onSwingClick(ServerPlayer player) {

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

    public static PlayerInfo getPlayerInfo(ServerPlayer player) {
        PlayerInfo info = playerInfos.get(player.getGameProfile().getName());
        if (info == null) {
            info = new PlayerInfo(player);
            playerInfos.put(player.getGameProfile().getName(), info);
        }
        return info;
    }

    public static void sendMapLink(ServerPlayer player) {

        String dimension = "New%20World";

        if (player.level().dimension() == Level.OVERWORLD) {
            dimension = "New%20World";
        } else if (player.level().dimension() == Level.NETHER) {
            dimension = "DIM-1";
        } else if (player.level().dimension() == Level.END) {
            dimension = "DIM1";
        }

        String link = Configs.configs.mapUrlBase + "/?worldname=" + dimension + "&mapname=flat&zoom=5&x="
                + player.blockPosition().getX() + "&y=64&z="
                + player.blockPosition().getZ();

        player.sendSystemMessage(Component.Serializer.fromJson(
                "{\"text\":\"[Click Me]\",\"color\":\"dark_green\",\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\""
                        + link + "\"}]},\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + link + "\"}}",
                player.registryAccess()));
    }

    private static void sendToOps(MinecraftServer server, String message) {
        if (!Configs.configs.broadcastTeleportsToOps)
            return;
        Component text = (Component.literal(message)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            for (ServerPlayer serverPlayerEntity : server.getPlayerList().getPlayers()) {
                if (server.getPlayerList().isOp(serverPlayerEntity.getGameProfile())) {
                    serverPlayerEntity.sendSystemMessage(text);
                }
            }
        }

        if (server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS)) {
            server.sendSystemMessage(text);
        }

    }

    public static void teleportToWaypoint(ServerPlayer player, Waypoint waypoint, boolean broadcast) {

        if (Configs.configs.disableWaypoints) {
            sendActionBarMessage(player, "Waypoints are disabled!");
            return;
        }

        if (Configs.configs.disableWaypointsTeleport) {
            sendActionBarMessage(player, "Waypoint teleports are disabled!");
            return;
        }

        sendActionBarMessage(player, "Teleporting to " + waypoint.getName());

        ((BlockableEventLoop<?>) player.getServer()).execute(() -> {
            teleportToWaypointInternal(player, waypoint, broadcast);
        });
    }

    public static void teleportToSpawn(ServerPlayer player) {

        sendActionBarMessage(player, "Teleporting to spawn");

        ((BlockableEventLoop<?>) player.getServer()).execute(() -> {
            teleportToWaypointInternal(player, new Waypoint(9.5, -43, -6.5, "overworld", "Spawn", "spawn"), false);
        });
    }

    public static void teleportToWaypointInternal(ServerPlayer player, Waypoint waypoint, boolean broadcast) {

        double x = waypoint.getX();
        double y = waypoint.getY();
        double z = waypoint.getZ();
        float pitch = player.getViewXRot(1);
        float yaw = player.getViewYRot(1);
        Set<Relative> set = EnumSet.noneOf(Relative.class);
        ResourceLocation identifier = ResourceLocation.parse(waypoint.getDimension());
        ResourceKey<Level> registryKey = ResourceKey.create(Registries.DIMENSION, identifier);
        ServerLevel world = player.getServer().getLevel(registryKey);

        if (world == player.level()) {
            player.connection.teleport(x, y, z, yaw, pitch);
        } else {
            player.teleportTo(world, x, y, z, set, yaw, pitch, true);
        }
        if (broadcast)
            sendToOps(player.getServer(), "Teleported " + player.getDisplayName().getString() + " to waypoint "
                    + waypoint.getName() + " in dimension " + waypoint.getDimension());

    }

}
