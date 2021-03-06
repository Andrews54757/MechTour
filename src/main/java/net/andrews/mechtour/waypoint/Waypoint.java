package net.andrews.mechtour.waypoint;

import net.andrews.mechtour.Utils;
import net.andrews.mechtour.mapgui.BitMapImage;
import net.andrews.mechtour.mapgui.MapText;
import net.andrews.mechtour.waypoint.WaypointIcons.Icon;

import java.awt.Font;

public class Waypoint {

    String name;
    String dimension;
    String icon;
    double x;
    double y;
    double z;
    int r = 83;
    int g = 134;
    int b = 184;

    transient MapText cachedText = null;

    transient MapText cachedPosText = null;

    public Waypoint(double x, double y, double z, String dimension, String name, String iconName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = name;
        this.icon = iconName;
        this.dimension = dimension;
    }

    public static String scrubNameRegex(String input) {
        input = input.replace(",", "");
        input = input.replace("[", "");
        input = input.replace("]", "");
        return input;
    }

    public String voxelmap_string() {
        return String.format("[name:%s, x:%s, y:%s, z:%s, dim:minecraft:%s, icon:%s]", scrubNameRegex(name),
                Integer.valueOf((int)getX()), Integer.valueOf((int)getY()), Integer.valueOf((int)getZ()), getDimension(),
                getIconName());
    }

    public String getName() {
        return name;
    }

    public BitMapImage getIcon() {
        Icon ic = WaypointIcons.getIconByName(icon);
        if (ic != null) {
            return ic.getImage(r, g, b);
        }
        return null;
    }

    public MapText getTextIcon() {
        if (cachedText == null) {
            cachedText = new MapText(Utils.wordWrap(this.getName(), 18),
                    new Font("Arial", Font.PLAIN, this.getName().length() > 11 ? (this.getName().length() > 14 ? 20 : 23) : 25));
        }
        return cachedText;
    }

    public MapText getPosText() {
        if (cachedPosText == null) {
            cachedPosText = new MapText("TP > " + (int)this.getX() + ", " + (int)this.getY() + ", " + (int)this.getZ() + "",
                    new Font("Arial", Font.PLAIN, 45));
        }
        return cachedPosText;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getDimension() {
        return dimension;
    }

    public String getIconName() {
        return icon;
    }

    public void setName(String name) {
        this.name = name;
        this.cachedText = null;
    }

    public void setX(double x) {
        this.x = x;
        this.cachedPosText = null;
    }

    public void setY(double y) {
        this.y = y;
        this.cachedPosText = null;
    }

    public void setZ(double z) {
        this.z = z;
        this.cachedPosText = null;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public void setIconName(String iconName) {
        this.icon = iconName;
    }

    public void setColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }
}
