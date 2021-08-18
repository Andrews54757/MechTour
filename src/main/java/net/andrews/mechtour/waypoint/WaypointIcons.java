package net.andrews.mechtour.waypoint;

import net.andrews.mechtour.Utils;

import java.util.HashMap;

import com.google.gson.Gson;
import net.andrews.mechtour.mapgui.BitMapImage;
import net.fabricmc.loader.api.FabricLoader;

public class WaypointIcons {

    private static HashMap<String, Icon> icons = new HashMap<>();

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
        boolean matchSlow = false;
        boolean isFuzzy = false;

        private transient HashMap<Integer, BitMapImage> images = new HashMap<>();

        public String getName() {
            return name;
        }

        public BitMapImage getImage(int r, int g, int b) {

            int index = 0;
            if (!colored) {
                index = r << 16 | g << 8 | b;
            }

            BitMapImage image = images.get(index);
            if (image == null) {
                image = new BitMapImage("waypoint_icons/" + file).scaledDimensions(-1, 80).setAlphaCutoff(isFuzzy() ? 200 : 20);
                if (!getColored()) {
                    image.setColor(r,g,b);
                }
                image.setMatchSlow(matchSlow);
                image.bake();

                images.put(index, image);
            }
            return image;
        }

        public String getFile() {
            return file;
        }
        
        public boolean getColored() {
            return colored;
        }

        public boolean isFuzzy() {
            return isFuzzy;
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

        public void setFuzzy(boolean isFuzzy) {
            this.isFuzzy = isFuzzy;
        }
    }

    public static void noop() {
        
    }
}
