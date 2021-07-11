package net.andrews.mechtour.mapgui.gui;

import java.awt.Font;

import net.andrews.mechtour.mapgui.BitMapImage;
import net.andrews.mechtour.mapgui.MapText;

public class Resources {
    public static MapText back_text = new MapText("Back", new Font("Arial", Font.PLAIN, 30));
    public static MapText close_text = new MapText("Close", new Font("Arial", Font.PLAIN, 30));
    public static BitMapImage banner = new BitMapImage("banner.png").scaledDimensions(-1, 100).setAlphaCutoff(200)
    .bake();

    public static BitMapImage teleport_icon = new BitMapImage("tp_icon.png").scaledDimensions(-1, 100)
    .setAlphaCutoff(250).setColor(29, 113, 173).bake();

    public static MapText teleport_text = new MapText("TP to Tour", new Font("Arial", Font.PLAIN, 40));

    public static BitMapImage waypoints_icon = new BitMapImage("waypoints_icon.png").scaledDimensions(-1, 100)
            .setAlphaCutoff(250).setColor(29, 113, 173).bake();
    public static MapText waypoints_text = new MapText("Waypoints", new Font("Arial", Font.PLAIN, 40));

    public static BitMapImage map_icon = new BitMapImage("map_icon.png").scaledDimensions(-1, 100).setAlphaCutoff(250).setColor(29, 113, 173)
            .bake();
    public static MapText map_text = new MapText("Open Map", new Font("Arial", Font.PLAIN, 40));

    public static BitMapImage about_icon = new BitMapImage("about_icon.png").scaledDimensions(-1, 100).setColor(29, 113, 173)
            .setAlphaCutoff(250).bake();
    public static MapText about_text = new MapText("About", new Font("Arial", Font.PLAIN, 40));
    
    public static MapText about_content = new MapText(
        "Welcome to Mechanists!\n\n" + "We are a Technical Minecraft server currently on 1.16.5\n\n"
                + "Hardware: " + "CPU: Ryzen 5 3600, " + "RAM: 6GB\n" + "Seed: 3671431547008281909\n\n"
                + "Mods We Use:\n" + "- carpet, carpet-addons & carpet-extra\n" + "- lithium\n\n"
                + "Carpet Mod Features:\naccurateBlockPlacement, antiCheatDisabled, ctrlQCraftingFix, flippinCactus, missingTools,\nonePlayerSleeping, optimizedTNT, shulkerSpawningInEndCities, stackableShulkerBoxes,\nxpNoCooldown"
                ,
        new Font("Arial", Font.PLAIN, 20));
    

    public static MapText about_title = new MapText("About", new Font("Arial", Font.PLAIN, 70));

    public static MapText waypoints_title = new MapText("Waypoints", new Font("Arial", Font.PLAIN, 45));

    public static MapText dimension_overworld_title = new MapText("Overworld", new Font("Arial", Font.PLAIN, 20));
    public static MapText dimension_nether_title = new MapText("Nether", new Font("Arial", Font.PLAIN, 20));
    public static MapText dimension_end_title = new MapText("End", new Font("Arial", Font.PLAIN, 20));
    public static MapText uptext = new MapText("▲", new Font("Arial", Font.PLAIN, 20));
    public static MapText downtext = new MapText("▼", new Font("Arial", Font.PLAIN, 20));
       
    public static void noop() {

    }
      

}
