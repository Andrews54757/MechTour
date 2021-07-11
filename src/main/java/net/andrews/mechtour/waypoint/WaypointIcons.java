package net.andrews.mechtour.waypoint;

import net.andrews.mechtour.Utils;

import java.util.HashMap;

import com.google.gson.Gson;
import net.andrews.mechtour.mapgui.BitMapImage;
import net.fabricmc.loader.api.FabricLoader;

public class WaypointIcons {

    private static HashMap<String, Icon> icons = new HashMap<>();;

    static {
        load();
    }

    public static void load() {
       
       String str = Utils.readTextFile(FabricLoader.getInstance().getConfigDir().resolve("mechtour/waypoint_icons.json"));
       if (str == null) str = "[]";
       Gson gson = new Gson();
       Icon[] ic = gson.fromJson(str,Icon[].class);
       
       
       System.out.println("Found " + ic.length + " icons");
       icons.clear();
       for (int i = 0; i < ic.length; i++) {
           icons.put(ic[i].getName(), ic[i]);
           ic[i].prepareImage();
       }
    }


    public static Icon getIconByName(String name) {
        return icons.get(name);
    }
    public static Iterable<String> getIconNames() {
        return icons.keySet();
    }
    public static class Icon {
        String name;
        String file;
        boolean colored = true;
        private BitMapImage image;

        public void prepareImage() {
            image = new BitMapImage("waypoint_icons/" + file).scaledDimensions(-1, 80).setAlphaCutoff(200);
            if (!getColored()) {
                image.setColor(83,134,184);
            }
            image.bake();
        }
        public String getName() {
            return name;
        }

        public BitMapImage getImage() {
            return image;
        }

        public String getFile() {
            return file;
        }
        
        public boolean getColored() {
            return colored;
        }

        public void setColored(boolean colored) {
            this.colored = colored;
        }
        public void setFile(String file) {
            this.file = file;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    public static void noop() {
        
    }
}
