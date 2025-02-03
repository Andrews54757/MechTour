package net.andrews.sooncmp.slideshow.gui;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.datafixers.util.Pair;

import net.andrews.sooncmp.SoonCMPMod;
import net.andrews.sooncmp.mapgui.BitMapImage;
import net.andrews.sooncmp.mapgui.color.MapColors;
import net.andrews.sooncmp.mapgui.gui.Resources;
import net.andrews.sooncmp.slideshow.SlideshowGUI;
import net.minecraft.server.network.ServerPlayerEntity;
import net.andrews.sooncmp.slideshow.MapRenderer;

public class PresentationGui extends MapGuiBase {
    private static byte nav_fillColor = 5;
    private static byte nav_hoverColor = 4;
    private static byte nav_fillTextColor = 84;
    private static byte nav_hoverTextColor = 87;
    private SimpleTextButton backButton;

    private String presentation;
    private List<String> slides;
    private int slideNumber = 0;
    private ConcurrentHashMap<Integer, BitMapImage> cachedImages = new ConcurrentHashMap<>();
    private boolean failed = false;
    private ThreadedImageLoader imageLoader = null;

    public PresentationGui(String presentation, SlideshowGUI holder) {
        this.presentation = presentation;
        backButton = new SimpleTextButton(Resources.back_text, nav_fillColor, nav_hoverColor, nav_fillTextColor,
                nav_hoverTextColor);
        backButton.setClickCallback((ServerPlayerEntity player, Pair<Integer, Integer> mousepos, boolean isInteract,
                SlideshowGUI holder2) -> {
            holder.openGui(new SlideshowMenuGui());
        });

        loadSlides();
        setSlideNumber(holder, 0);
    }

    public void loadSlides() {
        this.slides = SoonCMPMod.slideshowManager.getSlides(presentation);
    }

    public void setSlideNumber(SlideshowGUI holder, int slideNumber) {
        this.slideNumber = slideNumber;
        // remove any slides from cache with a distance > 10
        this.setReRenderFlag(true);
        this.cachedImages.entrySet().removeIf(entry -> Math.abs(entry.getKey() - slideNumber) > 10);

        if (imageLoader == null) {
            loadNextImage(holder.getPanelPixelWidth(), holder.getPanelPixelHeight());
        }
    }

    public void loadNextImage(int holderWidth, int holderHeight) {
        if (imageLoader != null || failed || slides.size() == 0) {
            return;
        }

        // load 8 images forward
        for (int i = 0; i < 8; i++) {
            int slideNumber = this.slideNumber + i;
            if (slideNumber >= slides.size()) {
                break;
            }
            if (cachedImages.containsKey(slideNumber)) {
                continue;
            }

            loadSlide(holderWidth, holderHeight, slideNumber);
            break;
        }

        // load 2 image backwards
        for (int i = 0; i < 2; i++) {
            int slideNumber = this.slideNumber - i;
            if (slideNumber < 0) {
                break;
            }
            if (cachedImages.containsKey(slideNumber)) {
                continue;
            }

            loadSlide(holderWidth, holderHeight, slideNumber);
            break;
        }
    }

    public void loadSlide(int holderWidth, int holderHeight, int i) {
        if (imageLoader != null || i < 0 || i >= slides.size()) {
            return;
        }

        imageLoader = new ThreadedImageLoader(this, "presentations/" + presentation + "/" + slides.get(i), i,
                holderWidth,
                holderHeight);
        imageLoader.start();
    }

    public void loadedSlide(int holderWidth, int holderHeight) {
        if (imageLoader == null) {
            return;
        }
        BitMapImage cachedImage = imageLoader.getImage();
        int slideNumberLoaded = imageLoader.getSlideNumber();
        if (cachedImage == null) {
            failed = true;
        } else {
            cachedImages.put(slideNumberLoaded, cachedImage);
            imageLoader = null;
            loadNextImage(holderWidth, holderHeight);
        }

        if (slideNumberLoaded == slideNumber) {
            this.setReRenderFlag(true);
        }
    }

    @Override
    public void onClose(SlideshowGUI holder) {
        failed = true;
        if (imageLoader != null) {
            imageLoader.interrupt();
            imageLoader = null;
        }
        super.onClose(holder);
    }

    @Override
    public void render(SlideshowGUI holder) {
        // Clear the map
        MapRenderer.fill(holder, (byte) 0);
        backButton.setDimensions(10, 10, 100, 50);

        int holderWidth = holder.getPanelPixelWidth();
        int holderHeight = holder.getPanelPixelHeight();

        if (this.slides == null) {
            failed = true;
        }

        BitMapImage cachedImage = cachedImages.get(slideNumber);

        if (cachedImage != null) {
            int width = cachedImage.getWidth();
            int height = cachedImage.getHeight();
            MapRenderer.drawImage(holder, cachedImage, holderWidth / 2 - width / 2, holderHeight / 2 - height / 2);
        } else {
            MapRenderer.fill(holder, (byte) (MapColors.SNOW + MapColors.BASE_COLOR));
            if (failed) {
                MapRenderer.drawText(holder, Resources.slideshow_text_failed,
                        holderWidth / 2 - Resources.slideshow_text_failed.getWidth() / 2,
                        holderHeight / 2 - Resources.slideshow_text_failed.getHeight() / 2, (byte) 7);
            } else {
                if (slides.size() > 0) {
                    MapRenderer.drawText(holder, Resources.slideshow_text_loading,
                            holderWidth / 2 - Resources.slideshow_text_loading.getWidth() / 2,
                            holderHeight / 2 - Resources.slideshow_text_loading.getHeight() / 2, (byte) 7);
                } else {
                    MapRenderer.drawText(holder, Resources.slideshow_text_no_slides,
                            holderWidth / 2 - Resources.slideshow_text_no_slides.getWidth() / 2,
                            holderHeight / 2 - Resources.slideshow_text_no_slides.getHeight() / 2, (byte) 7);
                }
            }

        }

        if (backButton.isMouseOver() || cachedImage == null) {
            backButton.render(holder);
        }
        backButton.setReRenderFlag(false);

        super.render(holder);
    }

    @Override
    public void onMousePosChange(SlideshowGUI holder, List<Pair<Integer, Integer>> old_positions_looking_at,
            List<Pair<Integer, Integer>> positions_looking_at) {

        backButton.onMousePosChange(holder, old_positions_looking_at, positions_looking_at);
        super.onMousePosChange(holder, old_positions_looking_at, positions_looking_at);
    }

    @Override
    public boolean shouldReRender(SlideshowGUI holder) {
        if (backButton.shouldReRender(holder)) {
            return true;
        }

        return super.shouldReRender(holder);
    }

    @Override
    public void onClick(ServerPlayerEntity player, Pair<Integer, Integer> mousepos, boolean isInteractKey,
            SlideshowGUI holder) {

        if (backButton.isMouseOnElement(mousepos.getFirst(), mousepos.getSecond())) {
            backButton.onClick(player, mousepos, isInteractKey, holder);
            return;
        }

        // check if mousepos first
        if (mousepos.getFirst() < holder.getPanelPixelWidth() / 2) { // backwards
            if (slideNumber > 0) {
                setSlideNumber(holder, slideNumber - 1);
            }
        } else { // forwards
            if (slideNumber < slides.size() - 1) {
                setSlideNumber(holder, slideNumber + 1);
            }
        }

        super.onClick(player, mousepos, isInteractKey, holder);

    }

    static class ThreadedImageLoader extends Thread {
        private String path;
        private BitMapImage image;
        private int holderWidth;
        private int holderHeight;
        private int slideNumber;
        private PresentationGui gui;

        public ThreadedImageLoader(PresentationGui gui, String path, int slideNumber, int holderWidth,
                int holderHeight) {
            this.path = path;
            this.holderWidth = holderWidth;
            this.holderHeight = holderHeight;
            this.gui = gui;
            this.slideNumber = slideNumber;
        }

        @Override
        public void run() {
            image = new BitMapImage(path);
            if (image.hasImage()) {
                int width = image.getUnprocessedWidth();
                int height = image.getUnprocessedHeight();

                double aspectRatioUnprocessed = (double) width / (double) height;
                double aspectRatioHolder = (double) holderWidth / (double) holderHeight;

                if (aspectRatioUnprocessed > aspectRatioHolder) {
                    // width is the limiting factor
                    image.scaledDimensions(holderWidth, -1);
                } else {
                    // height is the limiting factor
                    image.scaledDimensions(-1, holderHeight);
                }
                image.bake();
            }

            gui.loadedSlide(holderWidth, holderHeight);
        }

        public BitMapImage getImage() {
            return image;
        }

        public int getSlideNumber() {
            return slideNumber;
        }
    }

}