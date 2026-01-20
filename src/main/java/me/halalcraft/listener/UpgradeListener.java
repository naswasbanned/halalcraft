package me.halalcraft.listener;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.halalcraft.HalalCraft;

public class UpgradeListener implements Listener {
    private HalalCraft plugin;
    private static final String UPGRADE_BOOK_KEY = "upgrade_enchantment_book";
    private static final String ENCHANT_NAME_KEY = "enchant_name";
    private static final String ENCHANT_LEVEL_KEY = "enchant_level";
    private static final String CATEGORY_KEY = "upgrade_category";

    public UpgradeListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    public void showUpgradeGUI(Player p) {
        String title = plugin.getUpgradeConfig().getString("upgrade.title", "§6§lEnchantment Shop");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Play open sound
        String soundType = plugin.getUpgradeConfig().getString("upgrade.open-sound.type", "block.chest.open");
        float volume = (float) plugin.getUpgradeConfig().getDouble("upgrade.open-sound.volume", 1.0);
        float pitch = (float) plugin.getUpgradeConfig().getDouble("upgrade.open-sound.pitch", 1.0);
        p.playSound(p.getLocation(), convertSoundName(soundType), volume, pitch);

        // Load categories from config
        var categories = plugin.getUpgradeConfig().getConfigurationSection("upgrade.categories");
        if (categories == null) return;

        // Position categories in the middle (slots 10-14 for a 27-slot inventory)
        int[] middleSlots = {10, 11, 12, 13, 14};
        int slotIndex = 0;
        
        for (String categoryKey : categories.getKeys(false)) {
            if (slotIndex >= middleSlots.length) break;

            String displayName = plugin.getUpgradeConfig().getString("upgrade.categories." + categoryKey + ".display-name", "Category");
            String materialName = plugin.getUpgradeConfig().getString("upgrade.categories." + categoryKey + ".material", "DIAMOND_SWORD");
            
            try {
                Material material = Material.valueOf(materialName);
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(displayName);
                    List<String> lore = new ArrayList<>();
                    lore.add("§eClick to view enchantments");
                    
                    // Tag this item as a category selector
                    meta.getPersistentDataContainer().set(
                        plugin.getUpgradeBookKey(),
                        PersistentDataType.STRING,
                        categoryKey
                    );
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(middleSlots[slotIndex], item);
            } catch (IllegalArgumentException e) {
                // Invalid material, skip
            }
            slotIndex++;
        }

        p.openInventory(inv);
    }

    /**
     * Show enchantments for a specific category
     */
    private void showCategoryEnchantments(Player p, String categoryKey) {
        String categoryName = plugin.getUpgradeConfig().getString("upgrade.categories." + categoryKey + ".display-name", "Enchantments");
        String title = "§6" + categoryName;
        
        var enchantments = plugin.getUpgradeConfig().getConfigurationSection("upgrade.categories." + categoryKey + ".enchantments");
        if (enchantments == null) return;

        int size = Math.min(((enchantments.getKeys(false).size() + 8) / 9) * 9, 54); // Size in multiples of 9, max 54
        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (String enchantKey : enchantments.getKeys(false)) {
            if (slot >= size - 1) break; // Leave space for back button

            String displayName = plugin.getUpgradeConfig().getString("upgrade.categories." + categoryKey + ".enchantments." + enchantKey + ".display-name", "Enchantment");
            String description = plugin.getUpgradeConfig().getString("upgrade.categories." + categoryKey + ".enchantments." + enchantKey + ".description", "");
            String enchantName = plugin.getUpgradeConfig().getString("upgrade.categories." + categoryKey + ".enchantments." + enchantKey + ".enchantment", "");
            int level = plugin.getUpgradeConfig().getInt("upgrade.categories." + categoryKey + ".enchantments." + enchantKey + ".level", 1);
            int cost = plugin.getUpgradeConfig().getInt("upgrade.categories." + categoryKey + ".enchantments." + enchantKey + ".cost", 100);

            ItemStack book = createEnchantmentBook(displayName, description, enchantName, level, cost);
            inv.setItem(slot, book);
            slot++;
        }

        // Add back button at bottom right
        inv.setItem(size - 1, createBackButton());

        p.openInventory(inv);
    }

    private ItemStack createEnchantmentBook(String displayName, String description, String enchantName, int level, int cost) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add("§f" + description);
            lore.add("§7");
            lore.add("§eCost: §b" + cost + " virtue");
            lore.add("§7");
            lore.add("§a✓ Purified Enchantment");
            lore.add("§7");
            lore.add("§eUsage: Drag & drop");
            lore.add("§ethis book onto your");
            lore.add("§etools to enchant them");
            meta.setLore(lore);

            // NBT tags for enchantment info
            meta.getPersistentDataContainer().set(
                plugin.getUpgradeBookKey(),
                PersistentDataType.STRING,
                "true"
            );
            meta.getPersistentDataContainer().set(
                plugin.getUpgradeEnchantNameKey(),
                PersistentDataType.STRING,
                enchantName
            );
            meta.getPersistentDataContainer().set(
                plugin.getUpgradeEnchantLevelKey(),
                PersistentDataType.INTEGER,
                level
            );
            meta.getPersistentDataContainer().set(
                plugin.getUpgradeCostKey(),
                PersistentDataType.INTEGER,
                cost
            );

            book.setItemMeta(meta);
        }
        return book;
    }

    /**
     * Create a back button item
     */
    private ItemStack createBackButton() {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c« Back");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to go back");
            meta.setLore(lore);
            
            // Mark as back button with special NBT tag
            meta.getPersistentDataContainer().set(
                plugin.getUpgradeBookKey(),
                PersistentDataType.STRING,
                "BACK_BUTTON"
            );
            backButton.setItemMeta(meta);
        }
        return backButton;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player p = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        // Check if in main shop GUI (showing categories)
        if (title.equals(plugin.getUpgradeConfig().getString("upgrade.title", "§6§lEnchantment Shop"))) {
            event.setCancelled(true);
            
            // Check if it's a category selector
            String categoryKey = meta.getPersistentDataContainer().getOrDefault(
                plugin.getUpgradeBookKey(),
                PersistentDataType.STRING,
                ""
            );
            
            if (!categoryKey.isEmpty() && plugin.getUpgradeConfig().contains("upgrade.categories." + categoryKey)) {
                // It's a category - show its enchantments
                showCategoryEnchantments(p, categoryKey);
                return;
            }
        }

        // Check if in category enchantment GUI (showing enchantments from a category)
        if (title.startsWith("§6")) {
            event.setCancelled(true);
            
            // Check if it's an upgrade book
            if (meta.getPersistentDataContainer().has(plugin.getUpgradeBookKey(), PersistentDataType.STRING)) {
                String bookValue = meta.getPersistentDataContainer().get(
                    plugin.getUpgradeBookKey(),
                    PersistentDataType.STRING
                );
                
                // Check if it's the back button
                if (bookValue.equals("BACK_BUTTON")) {
                    showUpgradeGUI(p);
                    return;
                }
                
                // If it's a category key (not "true"), go back to categories
                if (!bookValue.equals("true")) {
                    showUpgradeGUI(p);
                    return;
                }
                
                // Otherwise it's a real enchantment book to purchase
                int cost = meta.getPersistentDataContainer().getOrDefault(plugin.getUpgradeCostKey(), PersistentDataType.INTEGER, 0);
                String enchantName = meta.getPersistentDataContainer().getOrDefault(plugin.getUpgradeEnchantNameKey(), PersistentDataType.STRING, "");

                int playerVirtue = plugin.getVirtue(p);

                if (playerVirtue < cost) {
                    int needed = cost - playerVirtue;
                    String msg = plugin.getUpgradeConfig().getString("upgrade.messages.insufficient-virtue", "§cYou need %needed% more virtue!");
                    p.sendMessage(msg.replace("%needed%", String.valueOf(needed)).replace("%current%", String.valueOf(playerVirtue)));
                    
                    String denySound = plugin.getUpgradeConfig().getString("upgrade.deny-sound.type", "block.anvil.place");
                    float volume = (float) plugin.getUpgradeConfig().getDouble("upgrade.deny-sound.volume", 1.0);
                    float pitch = (float) plugin.getUpgradeConfig().getDouble("upgrade.deny-sound.pitch", 0.5);
                    p.playSound(p.getLocation(), convertSoundName(denySound), volume, pitch);
                    return;
                }

                // Check inventory space
                if (p.getInventory().firstEmpty() == -1) {
                    p.sendMessage(plugin.getUpgradeConfig().getString("upgrade.messages.inventory-full", "§cYour inventory is full!"));
                    p.playSound(p.getLocation(), convertSoundName(plugin.getUpgradeConfig().getString("upgrade.deny-sound.type", "block.anvil.place")), 1.0f, 0.5f);
                    return;
                }

                // Give book and deduct virtue
                ItemStack book = new ItemStack(clicked);
                p.getInventory().addItem(book);
                plugin.changeVirtue(p, -cost);

                String msg = plugin.getUpgradeConfig().getString("upgrade.messages.purchased-book", "§a✓ Received %enchant%");
                p.sendMessage(msg.replace("%enchant%", clicked.getItemMeta().getDisplayName()).replace("%cost%", String.valueOf(cost)));

                String buySound = plugin.getUpgradeConfig().getString("upgrade.buy-sound.type", "entity.item.pickup");
                float buyVolume = (float) plugin.getUpgradeConfig().getDouble("upgrade.buy-sound.volume", 1.0);
                float buyPitch = (float) plugin.getUpgradeConfig().getDouble("upgrade.buy-sound.pitch", 1.2);
                p.playSound(p.getLocation(), convertSoundName(buySound), buyVolume, buyPitch);

                p.closeInventory();
            }
        }
    }

    private String convertSoundName(String configName) {
        try {
            return configName;
        } catch (Exception e) {
            return "block.note_block.ding";
        }
    }
}
