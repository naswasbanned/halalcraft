package me.halalcraft.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.halalcraft.HalalCraft;

/**
 * Handles the drag-and-drop enchantment book system.
 * Players drag enchantment books onto tools/weapons to apply the enchantment.
 */
public class EnchantmentBookListener implements Listener {
    private HalalCraft plugin;
    private final Map<UUID, Long> lastInteraction = new HashMap<>();
    private static final long INTERACTION_COOLDOWN = 500; // 0.5 seconds

    public EnchantmentBookListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if an item is an upgrade enchantment book
     */
    public boolean isUpgradeBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        return meta.getPersistentDataContainer().has(
            plugin.getUpgradeBookKey(),
            PersistentDataType.STRING
        );
    }

    /**
     * Check if an item is a valid tool/weapon for enchantment
     */
    public boolean isValidTool(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        Material mat = item.getType();
        // Check for tools
        if (mat.name().endsWith("_PICKAXE") || mat.name().endsWith("_AXE") || 
            mat.name().endsWith("_SHOVEL") || mat.name().endsWith("_HOE") ||
            mat.name().endsWith("_SWORD")) {
            return true;
        }
        
        // Check for armor
        if (mat.name().endsWith("_HELMET") || mat.name().endsWith("_CHESTPLATE") ||
            mat.name().endsWith("_LEGGINGS") || mat.name().endsWith("_BOOTS")) {
            return true;
        }
        
        return false;
    }

    /**
     * Extract enchantment info from upgrade book
     */
    public Map<String, Object> getBookEnchantmentInfo(ItemStack book) {
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return null;

        Map<String, Object> info = new HashMap<>();
        String enchantName = meta.getPersistentDataContainer().getOrDefault(
            plugin.getUpgradeEnchantNameKey(),
            PersistentDataType.STRING,
            ""
        );
        int level = meta.getPersistentDataContainer().getOrDefault(
            plugin.getUpgradeEnchantLevelKey(),
            PersistentDataType.INTEGER,
            1
        );

        if (enchantName.isEmpty()) {
            return null;
        }

        try {
            Enchantment enchantment = Enchantment.getByName(enchantName);
            if (enchantment == null) {
                return null;
            }
            info.put("enchantment", enchantment);
            info.put("level", Math.min(level, enchantment.getMaxLevel()));
            info.put("name", enchantName);
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Apply enchantment from book to tool
     */
    public void applyEnchantmentToTool(Player player, ItemStack book, ItemStack tool, InventoryClickEvent event) {
        if (!isUpgradeBook(book) || !isValidTool(tool)) {
            return;
        }

        Map<String, Object> enchantInfo = getBookEnchantmentInfo(book);
        if (enchantInfo == null) {
            player.sendMessage("§c⚠ Error reading enchantment from book!");
            return;
        }

        Enchantment enchantment = (Enchantment) enchantInfo.get("enchantment");
        int level = (int) enchantInfo.get("level");
        String enchantName = (String) enchantInfo.get("name");

        // Remove any existing conflicting enchantments from this book
        ItemMeta toolMeta = tool.getItemMeta();
        if (toolMeta != null) {
            // Remove old version of this enchantment if it exists
            toolMeta.removeEnchant(enchantment);
            tool.setItemMeta(toolMeta);
        }

        // Apply enchantment to tool
        tool.addUnsafeEnchantment(enchantment, level);

        // Mark tool as having purified (upgrade) enchantment
        toolMeta = tool.getItemMeta();
        if (toolMeta != null) {
            toolMeta.getPersistentDataContainer().set(
                plugin.getPurifiedKey(),
                PersistentDataType.BYTE,
                (byte) 1
            );

            // Add lore to show it's a purified enchantment
            java.util.List<String> lore = toolMeta.getLore();
            if (lore == null) {
                lore = new java.util.ArrayList<>();
            }
            
            String purifiedLore = "§a✓ Purified Enchantment";
            if (!lore.contains(purifiedLore)) {
                if (!lore.isEmpty()) {
                    lore.add(""); // Empty line for spacing
                }
                lore.add(purifiedLore);
            }
            toolMeta.setLore(lore);
            tool.setItemMeta(toolMeta);
        }

        // Consume the book by updating cursor
        ItemStack newCursor;
        if (book.getAmount() > 1) {
            newCursor = book.clone();
            newCursor.setAmount(book.getAmount() - 1);
        } else {
            newCursor = new ItemStack(Material.AIR);
        }
        
        // Update the cursor in the event
        event.setCursor(newCursor);

        // Play success sound
        String soundType = plugin.getConfig().getString("upgrade.apply-sound.type", "entity.player.levelup");
        float volume = (float) plugin.getConfig().getDouble("upgrade.aenchapply-sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("upgrade.apply-sound.pitch", 1.0);
        player.playSound(player.getLocation(), soundType, volume, pitch);

        // Success message
        String msg = plugin.getConfig().getString("upgrade.messages.applied-enchantment",
            "§a✓ %enchant% applied to your %tool%!");
        String toolName = tool.getType().name().replace("_", " ").toLowerCase();
        msg = msg.replace("%enchant%", enchantName).replace("%tool%", toolName);
        player.sendMessage(msg);
    }

    /**
     * Right-click on a tool while holding an enchantment book to apply
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        // Check if dragging an upgrade book onto something
        if (cursor == null || cursor.getType() == Material.AIR) {
            return;
        }

        // Check if cursor is an upgrade book
        if (!isUpgradeBook(cursor)) {
            return;
        }

        // Check if clicked item is a valid tool
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (!isValidTool(clicked)) {
            return;
        }

        // Check cooldown
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (lastInteraction.containsKey(uuid) && now - lastInteraction.get(uuid) < INTERACTION_COOLDOWN) {
            return;
        }
        lastInteraction.put(uuid, now);

        // Cancel the normal click event
        event.setCancelled(true);
        
        // Apply enchantment with event reference to update cursor
        applyEnchantmentToTool(player, cursor, clicked, event);
    }
}
