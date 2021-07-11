package net.andrews.mechtour.waypoint;

import net.andrews.mechtour.Utils;
import net.andrews.mechtour.mapgui.MapText;
import com.google.gson.annotations.Expose;

import java.awt.Font;


public class Waypoint {

    int x;
    int y;
    int z;
    String iconName;
    String name;
    String dimension;

    @Expose(serialize = false) 
    WaypointIcons.Icon cachedIcon = null;

    @Expose(serialize = false) 
    MapText cachedText = null;

    @Expose(serialize = false) 
    MapText cachedPosText = null;

    public Waypoint(int x, int y, int z, String dimension, String name, String iconName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = name;
        this.iconName = iconName;
        this.dimension = dimension;
    }

    public static String scrubNameRegex(String input) {
        input = input.replace(",", "");
        input = input.replace("[", "");
        input = input.replace("]", "");
        return input;
    }

    public String voxelmap_string() {
        return String.format("[name:%s, x:%s, y:%s, z:%s, dim:%s, icon:%s]", scrubNameRegex(name), Integer.valueOf(getX()), Integer.valueOf(getY()), Integer.valueOf(getZ()), getDimension(), getIconName());
    }

    public String getName() {
        return name;
    }

    public WaypointIcons.Icon getIcon() {
        if (cachedIcon == null) {
            cachedIcon = WaypointIcons.getIconByName(iconName);
        }
        return cachedIcon;
    }
    public MapText getTextIcon() {
        if (cachedText == null) {
            cachedText = new MapText(Utils.wordWrap(this.getName(), 14), new Font("Arial", Font.PLAIN, 25));
        }
        return cachedText;
    }

    public MapText getPosText() {
        if (cachedPosText == null) {
            cachedPosText = new MapText("(" + this.getX() + ", " + this.getY() + ", " + this.getZ() + ")", new Font("Arial", Font.PLAIN, 45));
        }
        return cachedPosText;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getDimension() {
        return dimension;
    }

    public String getIconName() {
        return iconName;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }
    public void setZ(int z) {
        this.z = z;
    }
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }
    public void setIconName(String iconName) {
        this.iconName = iconName;
    }



}

