package net.andrews.sooncmp.slideshow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.andrews.sooncmp.Utils;
import net.fabricmc.loader.api.FabricLoader;

public class SlideshowManager {

    private ArrayList<SlideshowConfig> slideshows = new ArrayList<>();

    public SlideshowManager() {
        loadFromFile();
    }

    public void loadFromFile() {
        String str = Utils.readTextFile(FabricLoader.getInstance().getConfigDir().resolve("sooncmp/slideshow_placements.json"));
        if (str == null)
            str = "[]";
        Gson gson = new Gson();
        SlideshowConfig[] wps = gson.fromJson(str, SlideshowConfig[].class);

        System.out.println("Found " + wps.length + " slideshow placements");

        slideshows.clear();
        for (SlideshowConfig placement : wps) {
            slideshows.add(placement);
        }
        slideshowsUpdated();

    }

    public List<String> getPresentationList() {
        // scan the /presentations folder for folders
        List<String> out = new ArrayList<>();
        Utils.getDirectories(FabricLoader.getInstance().getConfigDir().resolve("sooncmp/presentations")).forEach((path) -> {
            out.add(path.getFileName().toString());
        });
        return out;
    }

    public List<String> getSlides(String presentation) {
        // scan the /presentations/presentation folder for files
        List<String> out = new ArrayList<>();
        Utils.getFiles(FabricLoader.getInstance().getConfigDir().resolve("sooncmp/presentations/" + presentation)).forEach((path) -> {
            // check if it ends with a .png or a .jpg
            if (path.getFileName().toString().endsWith(".png") || path.getFileName().toString().endsWith(".jpg")) {
                out.add(path.getFileName().toString());
            }
        });

        List<String> sorted = Utils.sortNaturalString(out);

        return sorted;
    }

    public void slideshowsUpdated() {

        saveToFile();
    }

    public void saveToFile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(slideshows);

        Utils.writeTextFile(FabricLoader.getInstance().getConfigDir().resolve("sooncmp/slideshow_placements.json"), json);
    }

    public ArrayList<SlideshowConfig> getSlideshows() {
        return slideshows;
    }

    public boolean hasSlideshow(SlideshowConfig placement) {
        for (SlideshowConfig p : slideshows) {
            if (p.equals(placement)) {
                return true;
            }
        }
        return false;
    }

    public void addSlideshow(SlideshowConfig placement) {
        slideshows.add(placement);
        slideshowsUpdated();
    }

    public int removeSlideshow(SlideshowConfig placement) {

        int count = 0;
        Iterator<SlideshowConfig> it = slideshows.iterator();
        while (it.hasNext()) {
            SlideshowConfig placement2 = it.next();
            if (placement2.equals(placement)) {
                it.remove();
                count++;
            }
        }

        slideshowsUpdated();
        return count;
    }



}
