package me.halalcraft.mosque;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class MosqueManager {
    private static final HashMap<String, MosqueArea> mosques = new HashMap<>();

    public static boolean addMosque(String name, Location lecternLocation, Set<Location> woolBlocks) {
        if (mosques.containsKey(name)) return false;
        mosques.put(name, new MosqueArea(name, lecternLocation, woolBlocks));
        return true;
    }

    public static MosqueArea getMosque(String name) {
        return mosques.get(name);
    }

    public static Set<String> getMosqueNames() {
        return mosques.keySet();
    }

    /**
     * Delete a mosque by its lectern location
     */
    public static MosqueArea deleteMosqueByLectern(Location lecternLocation) {
        for (String name : mosques.keySet()) {
            MosqueArea mosque = mosques.get(name);
            if (mosque.getLecternLocation().equals(lecternLocation)) {
                return mosques.remove(name);
            }
        }
        return null;
    }

    /**
     * Find all connected wool blocks starting from the lectern using flood fill
     */
    public static Set<Location> findConnectedWool(Location lecternLocation) {
        Set<Location> wool = new HashSet<>();
        Queue<Location> queue = new LinkedList<>();
        Set<Location> visited = new HashSet<>();

        queue.add(lecternLocation);
        visited.add(lecternLocation);

        while (!queue.isEmpty()) {
            Location current = queue.poll();

            // Check all 6 adjacent blocks
            int[][] directions = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
            for (int[] dir : directions) {
                Location adjacent = current.clone().add(dir[0], dir[1], dir[2]);
                if (!visited.contains(adjacent)) {
                    visited.add(adjacent);
                    Block adjBlock = adjacent.getBlock();
                    if (adjBlock.getType().name().endsWith("WOOL")) {
                        wool.add(adjacent);
                        queue.add(adjacent);
                    }
                }
            }
        }

        return wool;
    }

    public static class MosqueArea {
        private final String name;
        private final Location lecternLocation;
        private final Set<Location> woolBlocks;
        private final Set<Player> players = new HashSet<>();

        public MosqueArea(String name, Location lecternLocation, Set<Location> woolBlocks) {
            this.name = name;
            this.lecternLocation = lecternLocation;
            this.woolBlocks = woolBlocks;
        }

        public String getName() { return name; }
        public Location getLecternLocation() { return lecternLocation; }
        public Set<Location> getWoolBlocks() { return woolBlocks; }
        public Set<Player> getPlayers() { return players; }
        public void addPlayer(Player player) { players.add(player); }
        public void removePlayer(Player player) { players.remove(player); }

        public boolean isInside(Location loc) {
            if (!loc.getWorld().equals(lecternLocation.getWorld())) return false;
            // Check if location is within the bounding box of wool blocks (including above)
            for (Location wool : woolBlocks) {
                // Check the wool block itself and up to 3 blocks above it
                for (int y = 0; y <= 3; y++) {
                    Location checkLoc = wool.clone().add(0, y, 0);
                    if (checkLoc.distance(loc) <= 1.0) return true;
                }
            }
            // Also check lectern location and above
            for (int y = 0; y <= 3; y++) {
                Location checkLoc = lecternLocation.clone().add(0, y, 0);
                if (checkLoc.distance(loc) <= 1.0) return true;
            }
            return false;
        }
    }
    
}
