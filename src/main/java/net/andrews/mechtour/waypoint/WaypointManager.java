package net.andrews.mechtour.waypoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.google.gson.Gson;

import net.andrews.mechtour.Utils;
import net.andrews.mechtour.mapgui.gui.WaypointsMenuGui;
import net.fabricmc.loader.api.FabricLoader;

public class WaypointManager {
    
    private ArrayList<Waypoint> waypoints = new ArrayList<>();
    
    private HashSet<WaypointsMenuGui> trackedGuis = new HashSet<>();

    public WaypointManager() {

        loadFromFile();

    }
    
    public void beginTrack(WaypointsMenuGui gui) {
        trackedGuis.add(gui);
    }
    public void stopTrack(WaypointsMenuGui gui) {
        trackedGuis.remove(gui);
    }

    private void updateTracked() {
        for (WaypointsMenuGui gui : trackedGuis) {
            gui.setReRenderFlag(true);
        }
    }

    public void loadFromFile() {
        String str = Utils.readTextFile(FabricLoader.getInstance().getConfigDir().resolve("mechtour/waypoints.json"));
        if (str == null) str = "[]";
        Gson gson = new Gson();
        Waypoint[] wps = gson.fromJson(str,Waypoint[].class);
 
        System.out.println("Found " + wps.length + " waypoints");

        waypoints.clear();
        for (Waypoint waypoint : wps) {
            waypoints.add(waypoint);
        }
        waypointsUpdated();
      
    }


    private void waypointsUpdated() {

        saveToFile();
        updateTracked();
    }
    public void saveToFile() {
        Gson gson = new Gson();
        String json = gson.toJson(waypoints);
 
        Utils.writeTextFile(FabricLoader.getInstance().getConfigDir().resolve("mechtour/waypoints.json"), json);
       
    }

    public ArrayList<Waypoint> getWaypoints() {
        return waypoints;
    }

    public ArrayList<Waypoint> getWaypoints(String dimension) {
        ArrayList<Waypoint> newarr = new ArrayList<>();

        for (Waypoint waypoint : waypoints) {
            if (waypoint.getDimension().equalsIgnoreCase(dimension)) {
                newarr.add(waypoint);
            }
        }
        return newarr;
    }
    public void addWaypoint(int x, int y, int z, String dimension, String name, String iconName) {
        waypoints.add(new Waypoint(x,y,z,dimension,name,iconName));

        waypointsUpdated();
    }


    public Iterable<String> getWaypointNames(String dimension) {

        ArrayList<String> names = new ArrayList<>();
        for (Waypoint waypoint : waypoints) {
            if (dimension.length() == 0 || waypoint.getDimension().equalsIgnoreCase(dimension)) {
                names.add(waypoint.getName());
            }
        }

        return names;
    }


    public int removeWaypoint(String dimension, String name) {

        int count = 0;
        Iterator<Waypoint> it = waypoints.iterator();
        while (it.hasNext()) {
            Waypoint waypoint = it.next();
            if (waypoint.getDimension().equalsIgnoreCase(dimension) && waypoint.getName().equals(name)) {
                it.remove();
                count++;
            }
        }

        waypointsUpdated();
        return count;
    }

    public Waypoint getWaypoint(String dimension, String name) {

        Iterator<Waypoint> it = waypoints.iterator();
        while (it.hasNext()) {
            Waypoint waypoint = it.next();
            if (waypoint.getDimension().equalsIgnoreCase(dimension) && waypoint.getName().equals(name)) {
                return waypoint;
            }
        }

        return null;
    }


}
