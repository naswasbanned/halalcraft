package me.halalcraft.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.halalcraft.HalalCraft;

/**
 * Handles pending transactions for players when they join
 * Ensures offline shop purchases are credited when player comes back online
 */
public class TransactionListener implements Listener {

    private final HalalCraft plugin;

    public TransactionListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    /**
     * Process pending transactions when player joins
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Schedule processing on next tick to ensure player is fully loaded
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            plugin.processPendingTransactions(player);
        }, 1L);
    }
}
