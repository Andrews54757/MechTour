package net.andrews.mechtour;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Utils {
    public static net.minecraft.util.math.Box createEnclosingAABB(BlockPos pos1, BlockPos pos2)
    {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        return createAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    public static net.minecraft.util.math.Box createAABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        return new net.minecraft.util.math.Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static BlockHitResult raycastBox(World world, Entity entity, double range, Box box) {
        Vec3d eyesPos = entity.getCameraPosVec(1f);
        Vec3d rangedLookRot = entity.getRotationVec(1f).multiply(range);
        Vec3d lookEndPos = eyesPos.add(rangedLookRot);
        
        ArrayList<Box> boxes = new ArrayList<>();
        boxes.add(box);
        
        return Box.raycast(boxes, eyesPos, lookEndPos, new BlockPos(0,0,0));
    }
    public static Path getConfigDir() {
       return FabricLoader.getInstance().getConfigDir();
    }
    public static void sendPacket(ServerPlayerEntity player, Packet<?> packet) {
        if (player.isDisconnected()) return;
        player.networkHandler.sendPacket(packet);
    }

    public static String readTextFile(Path path)
    {
    try {
       return new String(Files.readAllBytes(path));
    } catch (Exception e) {
        System.out.println("[MechTour] Failed to read file " + path);
        return null;
    }
    }
    public static void writeTextFile(Path path, String str) {

        byte[] strToBytes = str.getBytes();
        try {
        Files.write(path, strToBytes);
        } catch (Exception e) {
            System.out.println("[MechTour] Failed to write to file " + path);
        }
    }

    public static String wordWrap(String str,int maxWidth) {
        String newLineStr = "\n";
        boolean found = false;
        String res = "";
        while (str.length() > maxWidth) {                 
            found = false;
            // Inserts new line at first whitespace of the line
            for (int i = maxWidth - 1; i >= 0; i--) {
                if (testWhite(str.charAt(i))) {
                    res = res + str.substring(0, i) + newLineStr;
                    str = str.substring(i + 1);
                    found = true;
                    break;
                }
            }
            // Inserts new line at maxWidth position, the word is too long to wrap
            if (!found) {
                res += str.substring(0, maxWidth) + newLineStr;
                str = str.substring(maxWidth);
            }
        }
        return res + str;
    }
    private static boolean testWhite(char charAt) {
        return charAt == ' ';
    }
    
}

