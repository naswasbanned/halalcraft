package me.halalcraft.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.halalcraft.HalalCraft;

public class OpenShopListener implements Listener {

    private static final String GUI_TITLE_BASE = "§6§lOpen Shop Listings";
    private static final int GUI_SIZE = 54; // 6 rows
    private static final int PAGE_SIZE = 45; // entries in first 5 rows
    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 53;

    private final HalalCraft plugin;
    private final NamespacedKey listingKey;

    public OpenShopListener(HalalCraft plugin) {
        this.plugin = plugin;
        this.listingKey = new NamespacedKey(plugin, "openshop_listing_id");
    }

    private static class Listing {
        String id;
        String sellerName;
        UUID sellerUUID;
        int price;
        ItemStack item;
        long createdAt;
    }

    // === COMMAND-SIDE API ===

    public void createListing(Player seller, ItemStack handItem, int price) {
        if (handItem == null || handItem.getType() == Material.AIR) {
            seller.sendMessage("§cYou must hold an item to list it.");
            return;
        }

        List<Listing> all = loadListings();
        int maxListingsPerPlayer = plugin.getConfig().getInt("openshop.max-listings-per-player", 6);
        int currentListings = 0;
        for (Listing l : all) {
            if (l.sellerUUID != null && l.sellerUUID.equals(seller.getUniqueId())) {
                currentListings++;
            }
        }
        if (currentListings >= maxListingsPerPlayer) {
            seller.sendMessage("§cYou already have §e" + currentListings + "§c listings in /openshop. " +
                    "The maximum allowed is §e" + maxListingsPerPlayer + "§c.");
            return;
        }

        ItemStack toList = handItem.clone();
        String id = java.util.UUID.randomUUID().toString();

        FileConfiguration config = plugin.getConfig();
        String base = "openshop.listings." + id;
        config.set(base + ".sellerName", seller.getName());
        config.set(base + ".sellerUUID", seller.getUniqueId().toString());
        config.set(base + ".price", price);
        config.set(base + ".createdAt", System.currentTimeMillis());
        config.set(base + ".item", toList);
        plugin.saveConfig();

        seller.getInventory().setItemInMainHand(null);
        seller.updateInventory();

        seller.sendMessage("§a✓ Your item has been listed for §e" + price + " virtue§a in §f/openshop§a.");

        // Global announcement so players know about the new listing
        String itemName;
        if (toList.hasItemMeta() && toList.getItemMeta().hasDisplayName()) {
            itemName = ChatColor.stripColor(toList.getItemMeta().getDisplayName());
        } else {
            itemName = formatMaterialName(toList.getType());
        }
        int amount = toList.getAmount();
        Bukkit.broadcastMessage("§6[OpenShop] §e" + seller.getName() + " §flisted §a" + amount + "x " + itemName +
                " §ffor §e" + price + " virtue§f. Use §a/openshop §fto view listings.");
    }

    public void openListings(Player viewer, int page) {
        List<Listing> all = loadListings();
        if (all.isEmpty()) {
            viewer.sendMessage("§cThere are no items listed in /openshop right now.");
            return;
        }

        int totalPages = (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = GUI_TITLE_BASE + " §7(Page " + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        for (int i = start; i < end; i++) {
            Listing l = all.get(i);
            int slot = i - start;
            inv.setItem(slot, createListingItem(l));
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§ePrevious Page");
                prev.setItemMeta(meta);
            }
            inv.setItem(SLOT_PREVIOUS, prev);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§cClose");
            close.setItemMeta(closeMeta);
        }
        inv.setItem(SLOT_CLOSE, close);

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eNext Page");
                next.setItemMeta(meta);
            }
            inv.setItem(SLOT_NEXT, next);
        }

        viewer.openInventory(inv);
    }

    // === GUI HANDLING ===

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE_BASE)) {
            return;
        }

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        if (rawSlot >= event.getView().getTopInventory().getSize()) {
            return; // clicked in player inventory
        }

        int page = extractPageFromTitle(title);

        if (rawSlot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (rawSlot == SLOT_PREVIOUS) {
            if (page > 0) {
                openListings(player, page - 1);
            }
            return;
        }
        if (rawSlot == SLOT_NEXT) {
            openListings(player, page + 1);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(listingKey, PersistentDataType.STRING)) {
            return; // not a listing entry
        }

        String id = container.get(listingKey, PersistentDataType.STRING);
        if (id == null || id.isEmpty()) return;

        ClickType clickType = event.getClick();
        handleListingClick(player, id, clickType, page);
    }

    private int extractPageFromTitle(String title) {
        String stripped = ChatColor.stripColor(title);
        int idx = stripped.indexOf("Page ");
        if (idx == -1) return 0;
        int slash = stripped.indexOf('/', idx);
        if (slash == -1) return 0;
        String pagePart = stripped.substring(idx + 5, slash).trim();
        try {
            int page = Integer.parseInt(pagePart);
            return Math.max(0, page - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void handlePurchase(Player buyer, String id) {
        List<Listing> all = loadListings();
        Listing target = null;
        for (Listing l : all) {
            if (l.id.equals(id)) {
                target = l;
                break;
            }
        }

        if (target == null) {
            buyer.sendMessage("§cThis listing has already been sold or removed.");
            buyer.closeInventory();
            return;
        }

        if (target.sellerUUID != null && target.sellerUUID.equals(buyer.getUniqueId())) {
            buyer.sendMessage("§cYou cannot buy your own listing.");
            return;
        }

        int buyerVirtue = plugin.getVirtue(buyer);
        if (buyerVirtue < target.price) {
            buyer.sendMessage("§cYou need " + (target.price - buyerVirtue) + " more virtue to buy this.");
            return;
        }

        // Check inventory space (simple check: at least one empty slot)
        if (buyer.getInventory().firstEmpty() == -1) {
            buyer.sendMessage("§cYour inventory is full.");
            return;
        }

        // Deduct virtue from buyer
        plugin.changeVirtue(buyer, -target.price);

        // Credit virtue to seller (online or offline)
        if (target.sellerUUID != null) {
            Player sellerOnline = Bukkit.getPlayer(target.sellerUUID);
            if (sellerOnline != null && sellerOnline.isOnline()) {
                plugin.changeVirtue(sellerOnline, target.price);
                sellerOnline.sendMessage("§a✓ Your /openshop listing was sold to " + buyer.getName() + " for " + target.price + " virtue.");
            } else {
                int current = plugin.getVirtue(target.sellerUUID);
                plugin.setVirtueByUUID(target.sellerUUID, current + target.price);
            }
        }

        // Give item to buyer
        ItemStack reward = target.item.clone();
        buyer.getInventory().addItem(reward);
        buyer.sendMessage("§a✓ Purchased item from " + target.sellerName + " for " + target.price + " virtue.");

        // Remove listing
        FileConfiguration config = plugin.getConfig();
        config.set("openshop.listings." + target.id, null);
        plugin.saveConfig();

        // Re-open current page to refresh listings
        Bukkit.getScheduler().runTask(plugin, () -> openListings(buyer, 0));
    }

    private void handleListingClick(Player player, String id, ClickType clickType, int page) {
        List<Listing> all = loadListings();
        Listing target = null;
        for (Listing l : all) {
            if (l.id.equals(id)) {
                target = l;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("§cThis listing has already been sold or removed.");
            player.closeInventory();
            return;
        }

        boolean isOwner = target.sellerUUID != null && target.sellerUUID.equals(player.getUniqueId());

        if (isOwner && clickType.isRightClick()) {
            cancelListing(player, target, page);
            return;
        }

        // Only left-click buys; other clicks are ignored for non-owners
        if (clickType.isLeftClick()) {
            handlePurchase(player, id);
        }
    }

    private void cancelListing(Player seller, Listing listing, int page) {
        // Check inventory space
        if (seller.getInventory().firstEmpty() == -1) {
            seller.sendMessage("§cYour inventory is full.");
            return;
        }

        // Remove listing from config
        FileConfiguration config = plugin.getConfig();
        config.set("openshop.listings." + listing.id, null);
        plugin.saveConfig();

        // Return the item
        ItemStack returned = listing.item.clone();
        seller.getInventory().addItem(returned);
        seller.sendMessage("§a✓ Your /openshop listing has been cancelled and the item was returned.");

        // Refresh the current page
        Bukkit.getScheduler().runTask(plugin, () -> openListings(seller, page));
    }

    private ItemStack createListingItem(Listing l) {
        ItemStack icon = l.item.clone();
        if (icon.getType() == Material.AIR) {
            icon = new ItemStack(Material.CHEST);
        }

        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(icon.getType());
        }
        if (meta != null) {
            String baseName;
            if (meta.hasDisplayName()) {
                baseName = meta.getDisplayName();
            } else {
                baseName = ChatColor.RESET + formatMaterialName(icon.getType());
            }

            meta.setDisplayName("§a" + baseName + " §7(§e" + l.price + " virtue§7)");

            List<String> lore = new ArrayList<>();
            lore.add("§7Seller: §b" + l.sellerName);
            lore.add("§7Amount: §f" + icon.getAmount());
            lore.add("§7Price: §e" + l.price + " virtue");
            lore.add(" ");
            lore.add("§aClick to buy this item.");
            meta.setLore(lore);

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(listingKey, PersistentDataType.STRING, l.id);

            icon.setItemMeta(meta);
        }

        return icon;
    }

    private String formatMaterialName(Material mat) {
        String[] parts = mat.name().toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private List<Listing> loadListings() {
        List<Listing> result = new ArrayList<>();
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("openshop.listings")) {
            return result;
        }

        ConfigurationSection section = config.getConfigurationSection("openshop.listings");
        if (section == null) {
            return result;
        }

        for (String id : section.getKeys(false)) {
            String base = "openshop.listings." + id;
            String sellerName = config.getString(base + ".sellerName");
            String sellerUUIDStr = config.getString(base + ".sellerUUID");
            int price = config.getInt(base + ".price", -1);
            long createdAt = config.getLong(base + ".createdAt", 0L);
            ItemStack item = config.getItemStack(base + ".item");

            if (sellerName == null || sellerUUIDStr == null || price <= 0 || item == null) {
                continue;
            }

            UUID sellerUUID;
            try {
                sellerUUID = java.util.UUID.fromString(sellerUUIDStr);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            Listing l = new Listing();
            l.id = id;
            l.sellerName = sellerName;
            l.sellerUUID = sellerUUID;
            l.price = price;
            l.item = item;
            l.createdAt = createdAt;

            result.add(l);
        }

        // Sort newest first
        Collections.sort(result, Comparator.comparingLong((Listing l) -> l.createdAt).reversed());
        return result;
    }
}
