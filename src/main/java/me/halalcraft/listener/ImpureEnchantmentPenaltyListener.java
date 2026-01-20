package me.halalcraft.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.halalcraft.HalalCraft;

/**
 * Tracks and penalizes the use of impure (haram) enchantments.
 * When a player uses an item with unpurified enchantments from the enchanting table,
 * they lose virtue and are warned.
 */
public class ImpureEnchantmentPenaltyListener implements Listener {
    private HalalCraft plugin;
    private final Map<UUID, Long> lastPenalty = new HashMap<>();
    private static final long PENALTY_COOLDOWN = 1000; // 1 second between penalties

    public ImpureEnchantmentPenaltyListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if an item has haram (unpurified) enchantments
     */
    private boolean hasHaramEnchantments(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // Check if marked as haram
        if (meta.getPersistentDataContainer().has(plugin.getHaramKey(), PersistentDataType.BYTE)) {
            return true;
        }

        // Check if it's NOT marked as purified but HAS enchantments
        // (this catches enchantments from enchanting table)
        if (!meta.getPersistentDataContainer().has(plugin.getPurifiedKey(), PersistentDataType.BYTE) &&
            !item.getEnchantments().isEmpty()) {
            // Check if it's NOT an upgrade book (those are allowed)
            if (!meta.getPersistentDataContainer().has(plugin.getUpgradeBookKey(), PersistentDataType.STRING)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Apply virtue penalty for using impure enchantment
     */
    private void applyPenalty(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Apply cooldown to prevent spam
        if (lastPenalty.containsKey(uuid) && now - lastPenalty.get(uuid) < PENALTY_COOLDOWN) {
            return;
        }
        lastPenalty.put(uuid, now);

        // Get penalty amount
        int penalty = plugin.getConfig().getInt("economy.virtue.haram-enchantment-use", -10);
        String penaltyMsg = plugin.getConfig().getString("messages.blackmagic", "§cUsing enchantments is forbidden! Virtue -%d");
        penaltyMsg = penaltyMsg.replace("%d", String.valueOf(Math.abs(penalty)));

        // Apply penalty
        plugin.changeVirtue(player, penalty);
        player.sendMessage(penaltyMsg);

        // Play warning sound
        String soundType = plugin.getConfig().getString("upgrade.deny-sound.type", "block.anvil.place");
        float volume = (float) plugin.getConfig().getDouble("upgrade.deny-sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("upgrade.deny-sound.pitch", 0.5);
        player.playSound(player.getLocation(), soundType, volume, pitch);

        // Save virtue data
        plugin.saveVirtueData();
    }

    /**
     * Detect when player damages an entity with an impure enchanted item
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (hasHaramEnchantments(weapon)) {
            applyPenalty(player, weapon);
        }
    }

    /**
     * Detect when player breaks a block with an impure enchanted item
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (hasHaramEnchantments(tool)) {
            applyPenalty(player, tool);
        }
    }
}
