package net.andrews.mechtour;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import net.fabricmc.loader.api.FabricLoader;

public class Configs {
    
    public boolean disableWaypointEdits = false;
    public boolean disableWaypoints = false;
    public boolean vanillaMode = false;

    @Expose(serialize = false) 
    public static Configs configs;
  
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
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) return false;
        field.setAccessible(true);
        if (field.getType().getName().equals("boolean")) {
            value = Boolean.valueOf((String)value);
        }
        field.set(configs, value);
        saveToFile();
            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }


    }

    public static void loadFromFile() {
        String str = Utils.readTextFile(FabricLoader.getInstance().getConfigDir().resolve("mechtour/config.json"));

        if (str == null) {
            str = "{}";
        }
        Gson gson = new Gson();
        configs = gson.fromJson(str,Configs.class);
        saveToFile();
        System.out.println("Loaded configs");
    }


    public static void saveToFile() {
        Gson gson = new Gson();
        String json = gson.toJson(configs);
 
        Path path = FabricLoader.getInstance().getConfigDir().resolve("mechtour/config.json");
        if (Files.notExists(path.getParent()))
        {
            try {
                Files.createDirectories(path.getParent());
            } catch (Exception e) {
                System.out.println("[MechTour] " + e);
            }
        }

        Utils.writeTextFile(path, json);
       
    }

}
