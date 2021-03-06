package net.andrews.mechtour;

import java.awt.Font;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.andrews.mechtour.mapgui.MapText;
import net.fabricmc.loader.api.FabricLoader;

public class Configs {

    public boolean disableWaypointEdits = false;
    public boolean disableWaypoints = false;
    public boolean disableWaypointsTeleport = false;
    public boolean disableTourTeleport = false;
    public boolean disableGui = false;
    public boolean disableGuideItem = false;
    public boolean disableGuideHoldMessage = false;
    public boolean broadcastTeleportsToOps = true;
    public int teleportCooldown = 20*15;
    public int teleportTimeout = 20;
  //  public boolean fastColorMatch = true;
    
    public String mapUrlBase = "http://mechanists.org/maps";
    public String aboutText = "Welcome to Mechanists!\n\nWe are a Technical Minecraft server currently on 1.17.1\n\nHardware: CPU: Ryzen 5 3600, RAM: 6GB\nSeed: 3671431547008281909\n\nMods We Use:\n- carpet, carpet-addons \u0026 carpet-extra\n- lithium\n\nCarpet Mod Features:\naccurateBlockPlacement, antiCheatDisabled, ctrlQCraftingFix, flippinCactus,\nmissingTools, onePlayerSleeping, optimizedTNT, shulkerSpawningInEndCities,\nstackableShulkerBoxes, xpNoCooldown";
    
    public transient static Configs configs;

    public transient static MapText aboutTextCache;

    public static Iterable<String> getFields() {
        ArrayList<String> out = new ArrayList<>();

        Field[] fields = configs.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                out.add(field.getName());
            }
        }
        return out;

    }

    public static boolean setConfig(String name, Object value) {
        try {
            Field field = configs.getClass().getDeclaredField(name);
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                return false;
            field.setAccessible(true);
            String tname = field.getType().getName();
            if (tname.equals("boolean")) {
                value = Boolean.valueOf((String) value);
            } else
            if (tname.equals("int")) {
                value = Integer.valueOf((String) value);
            }
            field.set(configs, value);
            setupCaches();
            saveToFile();
            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }

    }

    public static Object getConfig(String name) {
        try {
            Field field = configs.getClass().getDeclaredField(name);
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                return null;
            field.setAccessible(true);
            return field.get(configs);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }        
    }

    public static void loadFromFile() {
        String str = Utils.readTextFile(FabricLoader.getInstance().getConfigDir().resolve("mechtour/config.json"));

        if (str == null) {
            str = "{}";
        }
        Gson gson = new Gson();
        configs = gson.fromJson(str, Configs.class);
        setupCaches();
        saveToFile();
        System.out.println("Loaded configs");
    }

    public static void saveToFile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(configs);

        Path path = FabricLoader.getInstance().getConfigDir().resolve("mechtour/config.json");
        if (Files.notExists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (Exception e) {
                System.out.println("[MechTour] " + e);
            }
        }

        Utils.writeTextFile(path, json);
    }

    public static void setupCaches() {
        aboutTextCache = new MapText(Configs.configs.aboutText, new Font("Arial", Font.PLAIN, 20));
    }
}
