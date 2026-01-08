package net.andrews.sooncmp;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Utils {
    public static net.minecraft.world.phys.AABB createEnclosingAABB(BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        return createAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static net.minecraft.world.phys.AABB createAABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static BlockHitResult raycastBox(Level world, Entity entity, double range, AABB box) {
        Vec3 eyesPos = entity.getEyePosition(1f);
        Vec3 rangedLookRot = entity.getViewVector(1f).scale(range);
        Vec3 lookEndPos = eyesPos.add(rangedLookRot);

        ArrayList<AABB> boxes = new ArrayList<>();
        boxes.add(box);

        return AABB.clip(boxes, eyesPos, lookEndPos, new BlockPos(0, 0, 0));
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void sendPacket(ServerPlayer player, net.minecraft.network.protocol.Packet<?> packet) {
        if (player.hasDisconnected())
            return;
        player.connection.send(packet);
    }

    public static String readTextFile(Path path) {
        try {
            return new String(Files.readAllBytes(path));
        } catch (Exception e) {
            System.out.println("[SoonCMP] Failed to read file " + path);
            return null;
        }
    }

    public static void writeTextFile(Path path, String str) {

        byte[] strToBytes = str.getBytes();
        try {
            Files.write(path, strToBytes);
        } catch (Exception e) {
            System.out.println("[SoonCMP] Failed to write to file " + path);
        }
    }

    public static List<Path> getDirectories(Path path) {
        try {
            return Files.list(path).filter(Files::isDirectory).collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("[SoonCMP] Failed to list directories in " + path);
            return null;
        }
    }

    public static List<Path> getFiles(Path path) {
        try {
            return Files.list(path).filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("[SoonCMP] Failed to list files in " + path);
            return null;
        }
    }

    public static List<Path> sortNatural(List<Path> paths) {
        paths.sort(new NaturalPathComparator());
        return paths;
    }

    public static List<String> sortNaturalString(List<String> strings) {
        strings.sort(new NaturalOrderComparator());
        return strings;
    }

    public static String wordWrap(String str, int maxWidth) {
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

    /**
     * A comparator that implements “natural order” sorting.
     * It breaks each string into a sequence of numeric and non-numeric tokens and
     * compares corresponding tokens numerically (if both tokens are numeric)
     * or lexicographically.
     */
    static class NaturalPathComparator implements Comparator<Path> {
        // Re-use our natural order comparator for string comparisons.
        private final NaturalOrderComparator naturalComparator = new NaturalOrderComparator();

        @Override
        public int compare(Path p1, Path p2) {
            // Compare each name element (segment) of the paths.
            int count1 = p1.getNameCount();
            int count2 = p2.getNameCount();
            int min = Math.min(count1, count2);

            for (int i = 0; i < min; i++) {
                String part1 = p1.getName(i).toString();
                String part2 = p2.getName(i).toString();
                int cmp = naturalComparator.compare(part1, part2);
                if (cmp != 0) {
                    return cmp;
                }
            }

            // If all corresponding segments compare equal, then the shorter path comes
            // first.
            return Integer.compare(count1, count2);
        }
    }

    static class NaturalOrderComparator implements Comparator<String> {
        @Override
        public int compare(String a, String b) {
            int indexA = 0, indexB = 0;
            int lengthA = a.length(), lengthB = b.length();

            while (indexA < lengthA || indexB < lengthB) {
                // If we reached the end of both strings, they are equal.
                if (indexA >= lengthA && indexB >= lengthB) {
                    return 0;
                }
                // If one string ends earlier than the other, it is "smaller".
                if (indexA >= lengthA) {
                    return -1;
                }
                if (indexB >= lengthB) {
                    return 1;
                }

                char charA = a.charAt(indexA);
                char charB = b.charAt(indexB);

                // Check if both tokens are digits.
                if (Character.isDigit(charA) && Character.isDigit(charB)) {
                    // Extract the full number part from each string.
                    int startA = indexA;
                    while (indexA < lengthA && Character.isDigit(a.charAt(indexA))) {
                        indexA++;
                    }
                    int startB = indexB;
                    while (indexB < lengthB && Character.isDigit(b.charAt(indexB))) {
                        indexB++;
                    }
                    String numStrA = a.substring(startA, indexA);
                    String numStrB = b.substring(startB, indexB);

                    // Convert the numeric tokens to BigInteger to handle very large numbers.
                    BigInteger numA = new BigInteger(numStrA);
                    BigInteger numB = new BigInteger(numStrB);
                    int result = numA.compareTo(numB);
                    if (result != 0) {
                        return result;
                    }

                    // If the numeric values are equal, the one with fewer leading zeros should come
                    // first.
                    result = Integer.compare(numStrA.length(), numStrB.length());
                    if (result != 0) {
                        return result;
                    }
                    // Numbers are equal; continue with next token.
                } else {
                    // Compare non-numeric tokens character by character.
                    if (charA != charB) {
                        return charA - charB;
                    }
                    indexA++;
                    indexB++;
                }
            }
            // All tokens compared equal
            return 0;
        }
    }

}
