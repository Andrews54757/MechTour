package net.andrews.mechtour.mapgui.gui;

import net.andrews.mechtour.Configs;
import net.andrews.mechtour.MechTourMod;
import net.andrews.mechtour.mapgui.MapGuiHolder;
import net.andrews.mechtour.mapgui.MapRenderer;

public class GuideMenuGUI extends MapGuiBase {


    private static byte fillColor = 124;
    private static byte hoverColor = 40;
    private static byte fillTextColor = 84;
    private static byte hoverTextColor = 87;



    private SimpleTextIconButton teleportButton;
    private SimpleTextIconButton waypointsButton;
    private SimpleTextIconButton mapButton;
    private SimpleTextIconButton aboutButton;

    private static byte nav_fillColor = 0;
    private static byte nav_hoverColor = 4;
    private static byte nav_fillTextColor = 0;
    private static byte nav_hoverTextColor = 87;

   // private static MapText back_text = new MapText("Back", new Font("Arial", Font.PLAIN, 30));
  //  private SimpleTextButton backButton;
    private SimpleTextButton closeButton;

    public GuideMenuGUI() {

        teleportButton = new SimpleTextIconButton(Resources.teleport_icon, Resources.teleport_text, fillColor, hoverColor, fillTextColor,
                hoverTextColor);
        waypointsButton = new SimpleTextIconButton(Resources.waypoints_icon, Resources.waypoints_text, fillColor, hoverColor, fillTextColor,
                hoverTextColor);
        mapButton = new SimpleTextIconButton(Resources.map_icon, Resources.map_text, fillColor, hoverColor, fillTextColor, hoverTextColor);
        aboutButton = new SimpleTextIconButton(Resources.about_icon, Resources.about_text, fillColor, hoverColor, fillTextColor,
                hoverTextColor);

        teleportButton.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            MechTourMod.teleportToGuide(holder.getPlayer());
        });

        waypointsButton.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            if (Configs.configs.disableWaypoints) {
                MechTourMod.sendActionBarMessage(holder.getPlayer(), "Waypoints are disabled!");
                return;
            }
            holder.openGui(new WaypointsMenuGui());
        });

        mapButton.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            MechTourMod.sendMapLink(holder.getPlayer());
        });

        aboutButton.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            holder.openGui(new AboutPageGUI());
        });

        addInteractableElement(teleportButton);
        addInteractableElement(waypointsButton);
        addInteractableElement(mapButton);
        addInteractableElement(aboutButton);

       // backButton = new SimpleTextButton(back_text, nav_fillColor, nav_hoverColor, nav_fillTextColor, nav_hoverTextColor);
       // addInteractableElement(backButton);

        closeButton = new SimpleTextButton(Resources.close_text, nav_fillColor, nav_hoverColor, nav_fillTextColor, nav_hoverTextColor);
        closeButton.setClickCallback((boolean isInteract, MapGuiHolder holder) -> {
            holder.closePanel();
        });
        addInteractableElement(closeButton);

     
    }

    @Override
    public void render(MapGuiHolder holder) {
      //  MapRenderer.fill(holder, (byte) 0);
       // banner = new BitMapImage("banner.png").scaledDimensions(-1, 100).setAlphaCutoff(200).bake();

        MapRenderer.drawImage(holder, Resources.banner, holder.getPanelPixelWidth() / 2 - Resources.banner.getWidth() / 2, 0);
        int offsetY = 50;
        int gap = 3;
        int bWidth = 435;
        int bHeight = 200;
        teleportButton.setDimensions(holder.getPanelPixelWidth() / 2 - bWidth - gap,
                holder.getPanelPixelHeight() / 2 - bHeight - gap + offsetY, bWidth, bHeight);
        waypointsButton.setDimensions(holder.getPanelPixelWidth() / 2 + gap,
                holder.getPanelPixelHeight() / 2 - bHeight - gap + offsetY, bWidth, bHeight);
        mapButton.setDimensions(holder.getPanelPixelWidth() / 2 - bWidth - gap,
                holder.getPanelPixelHeight() / 2 + gap + offsetY, bWidth, bHeight);
        aboutButton.setDimensions(holder.getPanelPixelWidth() / 2 + gap,
                holder.getPanelPixelHeight() / 2 + gap + offsetY, bWidth, bHeight);

        //backButton.setDimentions(10, 10, 100, 50);
        closeButton.setDimensions(holder.getPanelPixelWidth() - 100 - 10, 10, 100, 50);

        // MapText text = new MapText("Hello!", new Font("Arial", Font.PLAIN, 100));

        // MapRenderer.drawText(holder, text, 0, 200, (byte)40);

        // System.out.println(mechLogo.getWidth());

        super.render(holder);
    }

    @Override
    public void onMousePosChange(MapGuiHolder holder, int newMouseX, int newMouseY, int oldMouseX, int oldMouseY) {

        super.onMousePosChange(holder, newMouseX, newMouseY, oldMouseX, oldMouseY);

    }

}