package net.andrews.mechtour.mapgui.gui;

import net.andrews.mechtour.mapgui.MapGuiHolder;
import net.andrews.mechtour.mapgui.MapRenderer;

public class ScrollBar extends InteractableElement {

    private int totalPages;
    private int currentPage = 0;
    private int hoverPage = -1;

    private int x;
    private int y;
    private int width;
    private int height;

    private SimpleTextButton scrollUpButton;
    private SimpleTextButton scrollDownButton;

    public ScrollBar() {
        scrollUpButton = new SimpleTextButton(Resources.uptext, (byte) 90, (byte) 88, (byte) 29, (byte) 29);

        addInteractableElement(scrollUpButton);

       
        scrollDownButton = new SimpleTextButton(Resources.downtext, (byte) 90, (byte) 88, (byte) 29, (byte) 29);

        addInteractableElement(scrollDownButton);


        scrollUpButton.setClickCallback((boolean i, MapGuiHolder holder) -> {
            decrementPage();
        });
        scrollDownButton.setClickCallback((boolean i, MapGuiHolder holder) -> {
            incrementPage();
        });

    }

    public void setDimensions(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.setInteractionBounds(x, y, x + width, y + height);
    }

    public void decrementPage() {
        if (this.currentPage > 0)
            this.currentPage--;
            setReRenderFlag(true);
    }

    public void incrementPage() {
        if (this.currentPage + 1 < totalPages)
            this.currentPage++;
            setReRenderFlag(true);
    }

    @Override
    public void render(MapGuiHolder holder) {

        if (totalPages <= 1) {
            return;
        }
       
        MapRenderer.fill(holder, x, y, width, height, (byte) 12);
        scrollUpButton.setDimensions(x, y, width, 20);
        scrollDownButton.setDimensions(x, y + height - 20, width, 20);


        scrollUpButton.setTextHidden(currentPage <= 0);
        scrollDownButton.setTextHidden(currentPage + 1 >= totalPages);

        int h = height - 40;
        double barheight = (double)h / (double)totalPages;
        if (hoverPage != -1 && hoverPage != currentPage) {
            MapRenderer.fill(holder, x, y + (int)(hoverPage * barheight) + 20, width, (int) barheight, (byte)88);
        }

        if (currentPage != -1) {
            MapRenderer.fill(holder, x, y + (int)(currentPage * barheight) + 20, width, (int) barheight, (byte)85);
        }

        super.render(holder);

    }

    public int getPageFromPos(int y) {

        int h = height - 40;
        int my = y - this.y;

        if (my <= 20 || my >= height - 20) {
            return -1;
        }

        return (int)(((double)(my - 20) / (double)h) * (double)totalPages);

    }

    public void setTotalPages(int pages) {
        this.totalPages = pages;

        if (this.totalPages > 0) {
        if (this.currentPage >= this.totalPages) {
            this.currentPage = this.totalPages - 1;
        }
        if (this.hoverPage >= this.totalPages) {
            this.hoverPage = this.totalPages - 1;
        }
        }
        setReRenderFlag(true);
    }


    public int getCurrentPage() {

        return currentPage;
    }
    public int getHoverPage() {
        return hoverPage;
    }
    public void setCurrentPage(int page) {
        this.currentPage = page;
        setReRenderFlag(true);
    }



    public void setHoverPage(int page) {
        this.hoverPage = page;
        setReRenderFlag(true);
    }

    @Override
    public void onMousePosChange(MapGuiHolder holder, int newMouseX, int newMouseY, int oldMouseX, int oldMouseY) {
        super.onMousePosChange(holder, newMouseX, newMouseY, oldMouseX, oldMouseY);
        int page = isMouseOver() ? getPageFromPos(newMouseY) : -1;
        if (page != hoverPage) {
            this.setHoverPage(page);
        }

    }

    @Override
    public void onClick(boolean isInteractKey, MapGuiHolder holder) {

        if (hoverPage != -1 && hoverPage != currentPage) {
            this.setCurrentPage(hoverPage);
        }
        super.onClick(isInteractKey, holder);
    }

    public int getDisplayPage() {
        
        return hoverPage == -1 ? currentPage : hoverPage;
    }

}
