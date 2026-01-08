package net.andrews.sooncmp.mapgui.gui;

import net.andrews.sooncmp.Configs;
import net.andrews.sooncmp.mapgui.EphemeralMapGui;
import net.andrews.sooncmp.mapgui.MapRenderer;
import net.minecraft.world.level.Level;

public class AboutPageGUI extends MapGuiBase {

    private static byte nav_fillColor = 5;
    private static byte nav_hoverColor = 4;
    private static byte nav_fillTextColor = 84;
    private static byte nav_hoverTextColor = 87;

    private SimpleTextButton backButton;
    private SimpleTextButton closeButton;

    public AboutPageGUI() {
        backButton = new SimpleTextButton(Resources.back_text, nav_fillColor, nav_hoverColor, nav_fillTextColor,
                nav_hoverTextColor);
        backButton.setClickCallback((boolean isInteract, EphemeralMapGui holder) -> {
            holder.openGui(new GuideMenuGUI());
        });

        closeButton = new SimpleTextButton(Resources.close_text, nav_fillColor, nav_hoverColor, nav_fillTextColor,
                nav_hoverTextColor);
        closeButton.setClickCallback((boolean isInteract, EphemeralMapGui holder) -> {
            holder.closePanel();
        });
        addInteractableElement(backButton);
        addInteractableElement(closeButton);
    }

    @Override
    public void render(EphemeralMapGui holder) {
        // MapRenderer.fill(holder, (byte) 0);
        // banner = new BitMapImage("banner.png").scaledDimensions(-1,
        // 100).setAlphaCutoff(200).bake();

        // MapRenderer.drawImage(holder, Resources.banner, holder.getPanelPixelWidth() /
        // 2 - Resources.banner.getWidth() / 2, 0);

        MapRenderer.fill(holder, 10, 90, holder.getPanelPixelWidth() - 20, holder.getPanelPixelHeight() - 100,
                (byte) 12);

        MapRenderer.drawText(holder, Resources.about_title,
                holder.getPanelPixelWidth() / 2 - Resources.about_title.getWidth() / 2, 0,
                holder.getPlayer().level().dimension().equals(Level.OVERWORLD)
                        ? ((byte) 116)
                        : ((byte) 58));

        MapRenderer.drawText(holder, Configs.aboutTextCache, 20, 100, (byte) 40);

        backButton.setDimensions(10, 10, 100, 50);
        closeButton.setDimensions(holder.getPanelPixelWidth() - 100 - 10, 10, 100, 50);

        super.render(holder);
    }

}