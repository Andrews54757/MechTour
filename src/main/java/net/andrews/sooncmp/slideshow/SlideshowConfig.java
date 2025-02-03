package net.andrews.sooncmp.slideshow;

import net.andrews.sooncmp.Utils;
import net.andrews.sooncmp.mapgui.BitMapImage;
import net.andrews.sooncmp.mapgui.MapText;
import net.andrews.sooncmp.waypoint.WaypointIcons.Icon;

import java.awt.Font;

public class SlideshowConfig {

    public String dimension;
    public int x;
    public int y;
    public int z;
    public int width;
    public int height;
    public String direction;


    public SlideshowConfig(String dimension, int x, int y, int z, int width, int height, String direction) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.direction = direction;
    }

    public boolean equals(SlideshowConfig other) {
        return this.dimension.equals(other.dimension) && this.x == other.x && this.y == other.y && this.z == other.z
                && this.width == other.width && this.height == other.height && this.direction.equals(other.direction);
    }

}
