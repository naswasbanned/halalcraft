package me.halalcraft.listener;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.halalcraft.HalalCraft;
import me.halalcraft.mosque.MosqueManager;

public class MosqueListener implements Listener {
    private final HalalCraft plugin;

    public MosqueListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        if (block.getType() == Material.LECTERN) {
            // Check for wool nearby (within 5 blocks)
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    for (int dz = -5; dz <= 5; dz++) {
                        Location loc = block.getLocation().add(dx, dy, dz);
                        Block nearby = loc.getBlock();
                        if (nearby.getType().name().endsWith("WOOL")) {
                            // Prompt for mosque name
                            player.sendMessage("§ePlace wool around the lectern, then type /mosque create <name> to register.");
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if a lectern is being broken
        if (block.getType() == Material.LECTERN) {
            // Check if this lectern is the marker for a mosque
            MosqueManager.MosqueArea mosque = MosqueManager.deleteMosqueByLectern(block.getLocation());
            if (mosque != null) {
                String msgTemplate = plugin.getConfig().getString("messages.mosque-deleted", "§cMosque §e%mosque% §chas been deleted because its lectern was broken.");
                String msg = msgTemplate != null ? msgTemplate.replace("%mosque%", mosque.getName()) : "§cMosque deleted!";
                player.sendMessage(msg);

                // Notify all players in the world
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(msg);
                }
            }
        }
    }

    @EventHandler
    public void onMosqueCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        Player player = event.getPlayer();
        
        if (msg.toLowerCase().startsWith("/mosque create ")) {
            handleMosqueCreate(event, player, msg);
        } else if (msg.equalsIgnoreCase("/mosque list")) {
            handleMosqueList(event, player);
        } else if (msg.equalsIgnoreCase("/mosque locate")) {
            handleMosqueLocate(event, player);
        }
    }

    private void handleMosqueCreate(PlayerCommandPreprocessEvent event, Player player, String msg) {
        String[] parts = msg.split(" ", 3);
        if (parts.length < 3) {
            player.sendMessage("§cUsage: /mosque create <name>");
            return;
        }
        String name = parts[2];
        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.LECTERN) {
            player.sendMessage("§cLook at the lectern to register the mosque.");
            return;
        }
        
        // Find all connected wool blocks using flood fill
        Set<Location> woolBlocks = MosqueManager.findConnectedWool(target.getLocation());
        
        if (woolBlocks.isEmpty()) {
            player.sendMessage("§cNo wool found connected to the lectern.");
            return;
        }
        
        boolean success = MosqueManager.addMosque(name, target.getLocation(), woolBlocks);
        if (success) {
            player.sendMessage("§aMosque '" + name + "' registered! Wool area: " + woolBlocks.size() + " blocks");
        } else {
            player.sendMessage("§cMosque name already exists.");
        }
        event.setCancelled(true);
    }

    private void handleMosqueList(PlayerCommandPreprocessEvent event, Player player) {
        Set<String> mosques = MosqueManager.getMosqueNames();
        if (mosques.isEmpty()) {
            player.sendMessage("§cNo mosques registered.");
        } else {
            player.sendMessage("§a=== Registered Mosques ===");
            int i = 1;
            for (String mosqueName : mosques) {
                MosqueManager.MosqueArea mosque = MosqueManager.getMosque(mosqueName);
                Location loc = mosque.getLecternLocation();
                player.sendMessage("§e" + i + ". §a" + mosqueName + " §f(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
                i++;
            }
        }
        event.setCancelled(true);
    }

    private void handleMosqueLocate(PlayerCommandPreprocessEvent event, Player player) {
        Set<String> mosques = MosqueManager.getMosqueNames();
        if (mosques.isEmpty()) {
            player.sendMessage("§cNo mosques registered.");
            event.setCancelled(true);
            return;
        }
        
        // Find nearest mosque
        MosqueManager.MosqueArea nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (String mosqueName : mosques) {
            MosqueManager.MosqueArea mosque = MosqueManager.getMosque(mosqueName);
            double dist = mosque.getLecternLocation().distance(player.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = mosque;
            }
        }
        
        if (nearest != null) {
            Location loc = nearest.getLecternLocation();
            player.sendMessage("§a=== Nearest Mosque ===");
            player.sendMessage("§eName: §e" + nearest.getName());
            player.sendMessage("§eLocation: §f(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
            player.sendMessage("§eDistance: §f" + String.format("%.1f", nearestDist) + " blocks");
        }
        event.setCancelled(true);
    }
}
