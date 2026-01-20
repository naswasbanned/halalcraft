package me.halalcraft.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.halalcraft.HalalCraft;

public class VirtueShopListener implements Listener {

    private final HalalCraft plugin;
    private final String SHOP_ID = "halalcraft_virtue_shop";

    public VirtueShopListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShopCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        Player player = event.getPlayer();
        
        if (msg.equalsIgnoreCase("/shop")) {
            event.setCancelled(true);
            openShop(player);
        }
    }

    /**
     * Open virtue shop GUI for player
     */
    public void openShop(Player player) {
        if (!plugin.getConfig().getBoolean("shop.enabled", true)) {
            player.sendMessage("§cThe shop is currently disabled!");
            return;
        }

        String title = plugin.getConfig().getString("shop.title", "§6§lVirtue Shop");
        Set<String> itemKeys = plugin.getConfig().getConfigurationSection("shop.items").getKeys(false);
        int size = Math.min(((itemKeys.size() + 8) / 9) * 9, 54); // Size in multiples of 9, max 54

        Inventory shop = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (String itemKey : itemKeys) {
            String configPath = "shop.items." + itemKey;
            String displayName = plugin.getConfig().getString(configPath + ".display-name", itemKey);
            String description = plugin.getConfig().getString(configPath + ".description", "");
            String materialName = plugin.getConfig().getString(configPath + ".material", "DIAMOND");
            int cost = plugin.getConfig().getInt(configPath + ".cost", 100);
            int amount = plugin.getConfig().getInt(configPath + ".amount", 1);

            Material material;
            try {
                material = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[HalalCraft] Invalid material in shop: " + materialName);
                continue;
            }

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(displayName);
                
                List<String> lore = new ArrayList<>();
                lore.add("§7" + description);
                lore.add(" ");
                lore.add("§6Cost: §e" + cost + " virtue");
                lore.add("§6Quantity: §e" + amount);
                lore.add(" ");
                lore.add("§aClick to purchase");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            // Store item key in item's display name temporarily for identification
            shop.setItem(slot, item);
            slot++;

            if (slot >= size) break;
        }

        player.openInventory(shop);
        
        // Play open sound
        if (plugin.getConfig().getBoolean("shop.open-sound.enabled", true)) {
            try {
                String soundName = plugin.getConfig().getString("shop.open-sound.type", "block.chest.open");
                float volume = (float) plugin.getConfig().getDouble("shop.open-sound.volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("shop.open-sound.pitch", 1.0);
                String properSound = convertSoundName(soundName);
                player.playSound(player.getLocation(), properSound, SoundCategory.MASTER, volume, pitch);
            } catch (Exception e) {
                try {
                    player.playSound(player.getLocation(), "block.chest.open", SoundCategory.MASTER, 1.0f, 1.0f);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is the shop inventory by checking the title
        String title = event.getView().getTitle();
        if (!title.contains("Virtue Shop")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Find which item was clicked
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        
        String itemName = meta.getDisplayName();
        String itemKey = findItemKeyByName(itemName);
        
        if (itemKey == null) return;

        purchaseItem(player, itemKey);
    }

    /**
     * Purchase item from shop
     */
    private void purchaseItem(Player player, String itemKey) {
        String configPath = "shop.items." + itemKey;
        int cost = plugin.getConfig().getInt(configPath + ".cost", 100);
        String materialName = plugin.getConfig().getString(configPath + ".material", "DIAMOND");
        int amount = plugin.getConfig().getInt(configPath + ".amount", 1);
        String displayName = plugin.getConfig().getString(configPath + ".display-name", itemKey);

        int playerVirtue = plugin.getVirtue(player);

        // Check sufficient virtue
        if (playerVirtue < cost) {
            String msg = plugin.getConfig().getString("shop.messages.insufficient-virtue", 
                    "§cYou need %needed% more virtue! You have: %current%");
            msg = msg.replace("%needed%", String.valueOf(cost - playerVirtue))
                    .replace("%current%", String.valueOf(playerVirtue));
            player.sendMessage(msg);
            
            playDenySound(player);
            return;
        }

        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            String msg = plugin.getConfig().getString("shop.messages.inventory-full", "§cYour inventory is full!");
            player.sendMessage(msg);
            playDenySound(player);
            return;
        }

        // Deduct virtue
        plugin.changeVirtue(player, -cost);

        // Give item
        Material material = Material.valueOf(materialName);
        ItemStack item = new ItemStack(material, amount);
        player.getInventory().addItem(item);

        // Success message
        String msg = plugin.getConfig().getString("shop.messages.purchase-success", 
                "§a✓ Purchased %item% for %cost% virtue!");
        msg = msg.replace("%item%", displayName)
                .replace("%cost%", String.valueOf(cost));
        player.sendMessage(msg);

        playBuySound(player);
    }

    /**
     * Find item key by display name
     */
    private String findItemKeyByName(String displayName) {
        Set<String> itemKeys = plugin.getConfig().getConfigurationSection("shop.items").getKeys(false);
        for (String itemKey : itemKeys) {
            String configName = plugin.getConfig().getString("shop.items." + itemKey + ".display-name", itemKey);
            if (configName.equals(displayName)) {
                return itemKey;
            }
        }
        return null;
    }

    /**
     * Play purchase success sound
     */
    private void playBuySound(Player player) {
        if (plugin.getConfig().getBoolean("shop.buy-sound.enabled", true)) {
            try {
                String soundName = plugin.getConfig().getString("shop.buy-sound.type", "entity.item.pickup");
                float volume = (float) plugin.getConfig().getDouble("shop.buy-sound.volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("shop.buy-sound.pitch", 1.2);
                String properSound = convertSoundName(soundName);
                player.playSound(player.getLocation(), properSound, SoundCategory.MASTER, volume, pitch);
            } catch (Exception e) {
                try {
                    player.playSound(player.getLocation(), "entity.item.pickup", SoundCategory.MASTER, 1.0f, 1.2f);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Play purchase deny sound
     */
    private void playDenySound(Player player) {
        if (plugin.getConfig().getBoolean("shop.deny-sound.enabled", true)) {
            try {
                String soundName = plugin.getConfig().getString("shop.deny-sound.type", "block.anvil.place");
                float volume = (float) plugin.getConfig().getDouble("shop.deny-sound.volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("shop.deny-sound.pitch", 0.5);
                String properSound = convertSoundName(soundName);
                player.playSound(player.getLocation(), properSound, SoundCategory.MASTER, volume, pitch);
            } catch (Exception e) {
                try {
                    player.playSound(player.getLocation(), "block.anvil.place", SoundCategory.MASTER, 1.0f, 0.5f);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Convert sound name format
     */
    private String convertSoundName(String soundName) {
        if (soundName == null) return "block.note_block.ding";
        if (soundName.contains(".")) return soundName.toLowerCase();
        return soundName.toLowerCase().replace("_", ".");
    }
}
