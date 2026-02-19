package me.halalcraft.listener;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.halalcraft.HalalCraft;

public class VirtueShopListener implements Listener {

    private final HalalCraft plugin;

    private static final String MAIN_MENU_TITLE = "§6§lChest Shops";
    private static final String BUY_LIST_TITLE_BASE = "§6§lChest Shops — Buy";
    private static final String SELL_LIST_TITLE_BASE = "§6§lChest Shops — Sell";

    private static final int LIST_INVENTORY_SIZE = 54; // 6 rows
    private static final int PAGE_SIZE = 45; // first 5 rows for entries

    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;

    private final NamespacedKey shopLocKey;

    private enum BrowseMode {
        BUY,  // player wants to buy items (browse [Sell] shops)
        SELL  // player wants to sell items (browse [Buy] shops)
    }

    private static class ChestShopEntry {
        final BrowseMode mode;
        final String worldName;
        final int signX;
        final int signY;
        final int signZ;
        final String owner;
        final boolean adminShop;
        final int quantity;
        final int price;
        final String itemName;
        final Location chestLocation;

        ChestShopEntry(BrowseMode mode, String worldName, int signX, int signY, int signZ,
                       String owner, boolean adminShop, int quantity, int price,
                       String itemName, Location chestLocation) {
            this.mode = mode;
            this.worldName = worldName;
            this.signX = signX;
            this.signY = signY;
            this.signZ = signZ;
            this.owner = owner;
            this.adminShop = adminShop;
            this.quantity = quantity;
            this.price = price;
            this.itemName = itemName;
            this.chestLocation = chestLocation;
        }
    }

    public VirtueShopListener(HalalCraft plugin) {
        this.plugin = plugin;
        this.shopLocKey = new NamespacedKey(plugin, "shop_browser_sign_loc");
    }

    @EventHandler
    public void onShopCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        Player player = event.getPlayer();

        if (msg.equalsIgnoreCase("/shop")) {
            event.setCancelled(true);
            openMainMenu(player);
        }
    }

    /**
     * Open main /shop GUI where player chooses Buy or Sell browsing.
     */
    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_MENU_TITLE);

        // Buy option (browse [Sell] shops)
        ItemStack buyItem = new ItemStack(Material.EMERALD);
        ItemMeta buyMeta = buyItem.getItemMeta();
        if (buyMeta != null) {
            buyMeta.setDisplayName("§aBrowse Shops to §lBUY");
            List<String> lore = new ArrayList<>();
            lore.add("§7See all chest shops where you");
            lore.add("§7can §abuy §7items with virtue.");
            buyMeta.setLore(lore);
            buyItem.setItemMeta(buyMeta);
        }

        // Sell option (browse [Buy] shops)
        ItemStack sellItem = new ItemStack(Material.CHEST);
        ItemMeta sellMeta = sellItem.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName("§eBrowse Shops to §lSELL");
            List<String> lore = new ArrayList<>();
            lore.add("§7See all chest shops where you");
            lore.add("§7can §esell §7items for virtue.");
            sellMeta.setLore(lore);
            sellItem.setItemMeta(sellMeta);
        }

        inv.setItem(11, buyItem);
        inv.setItem(15, sellItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(MAIN_MENU_TITLE)) {
            event.setCancelled(true);
            handleMainMenuClick(event, player);
            return;
        }

        BrowseMode mode = null;
        if (title.startsWith(BUY_LIST_TITLE_BASE)) {
            mode = BrowseMode.BUY;
        } else if (title.startsWith(SELL_LIST_TITLE_BASE)) {
            mode = BrowseMode.SELL;
        }

        if (mode == null) {
            return; // Not one of our GUIs
        }

        event.setCancelled(true);
        handleListClick(event, player, mode, title);
    }

    private void handleMainMenuClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = ChatColor.stripColor(meta.getDisplayName()).toLowerCase();

        if (name.contains("buy")) {
            openShopList(player, BrowseMode.BUY, 0);
        } else if (name.contains("sell")) {
            openShopList(player, BrowseMode.SELL, 0);
        }
    }

    private void handleListClick(InventoryClickEvent event, Player player, BrowseMode mode, String title) {
        int slot = event.getRawSlot();

        int currentPage = extractPageFromTitle(title);
        if (currentPage < 0) currentPage = 0;

        if (slot == SLOT_PREVIOUS) {
            if (currentPage > 0) {
                openShopList(player, mode, currentPage - 1);
            }
            return;
        }

        if (slot == SLOT_NEXT) {
            openShopList(player, mode, currentPage + 1);
            return;
        }

        if (slot == SLOT_BACK) {
            openMainMenu(player);
            return;
        }

        // Ignore clicks outside the top inventory
        if (slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(shopLocKey, PersistentDataType.STRING)) {
            return; // Not a shop entry
        }

        String locString = container.get(shopLocKey, PersistentDataType.STRING);
        if (locString == null || locString.isEmpty()) return;

        String[] parts = locString.split(",");
        if (parts.length != 4) return;

        String worldName = parts[0];
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(parts[1]);
            y = Integer.parseInt(parts[2]);
            z = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage("§cWorld for this shop is not loaded.");
            return;
        }

        Block signBlock = world.getBlockAt(x, y, z);
        BlockState state = signBlock.getState();
        if (!(state instanceof Sign)) {
            player.sendMessage("§cThis shop no longer exists.");
            return;
        }

        Sign sign = (Sign) state;

        String[] lines = sign.getLines();
        String header = ChatColor.stripColor(lines[0]).toLowerCase();
        boolean isSellSign = header.contains("sell");
        boolean isBuySign = header.contains("buy");

        if (mode == BrowseMode.BUY && !isSellSign) {
            player.sendMessage("§cThis is no longer a sell shop.");
            return;
        }
        if (mode == BrowseMode.SELL && !isBuySign) {
            player.sendMessage("§cThis is no longer a buy shop.");
            return;
        }

        int quantity;
        int price;
        String itemName;
        try {
            quantity = Integer.parseInt(ChatColor.stripColor(lines[1]).trim());
            String priceStr = ChatColor.stripColor(lines[3]).replace(" Virtue", "").trim();
            price = Integer.parseInt(priceStr);
            itemName = ChatColor.stripColor(lines[2]).trim();
        } catch (Exception e) {
            player.sendMessage("§cShop sign is corrupted.");
            return;
        }

        String owner = getShopOwner(sign);
        if (owner == null || owner.isEmpty()) {
            owner = "Unknown";
        }

        Block chestBlock = getChestBehindSign(signBlock);
        Location targetLoc = (chestBlock != null ? chestBlock.getLocation() : signBlock.getLocation());

        if (mode == BrowseMode.BUY) {
            player.sendMessage("§aYou can §lBUY §r§a" + quantity + " " + itemName + " for §e" + price + " virtue");
        } else {
            player.sendMessage("§aYou can §lSELL §r§a" + quantity + " " + itemName + " for §e" + price + " virtue");
        }

        player.sendMessage("§7Shop owner: §b" + owner);
        player.sendMessage("§7Location: §f" + targetLoc.getWorld().getName() + " §7@ §f" +
                targetLoc.getBlockX() + ", " + targetLoc.getBlockY() + ", " + targetLoc.getBlockZ());

        try {
            player.setCompassTarget(targetLoc);
            player.sendMessage("§7Your compass now points to this shop.");
        } catch (Exception ignored) {
        }
    }

    private int extractPageFromTitle(String title) {
        // Title format: "... (Page X/Y)"
        String stripped = ChatColor.stripColor(title);
        int idx = stripped.indexOf("Page ");
        if (idx == -1) return 0;
        int slash = stripped.indexOf('/', idx);
        if (slash == -1) return 0;
        String pagePart = stripped.substring(idx + 5, slash).trim();
        try {
            int page = Integer.parseInt(pagePart);
            return Math.max(0, page - 1); // convert to 0-based
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void openShopList(Player player, BrowseMode mode, int page) {
        List<ChestShopEntry> shops = getAllChestShops(mode);

        if (shops.isEmpty()) {
            if (mode == BrowseMode.BUY) {
                player.sendMessage("§cThere are no active chest shops where you can buy items.");
            } else {
                player.sendMessage("§cThere are no active chest shops where you can sell items.");
            }
            return;
        }

        int totalPages = (shops.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String baseTitle = (mode == BrowseMode.BUY ? BUY_LIST_TITLE_BASE : SELL_LIST_TITLE_BASE);
        String title = baseTitle + " §7(Page " + (page + 1) + "/" + totalPages + ")";

        Inventory inv = Bukkit.createInventory(null, LIST_INVENTORY_SIZE, title);

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, shops.size());

        for (int i = startIndex; i < endIndex; i++) {
            ChestShopEntry entry = shops.get(i);
            int slot = i - startIndex;
            inv.setItem(slot, createShopItem(entry));
        }

        // Navigation controls
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§ePrevious Page");
                prev.setItemMeta(meta);
            }
            inv.setItem(SLOT_PREVIOUS, prev);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cBack to /shop menu");
            back.setItemMeta(backMeta);
        }
        inv.setItem(SLOT_BACK, back);

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eNext Page");
                next.setItemMeta(meta);
            }
            inv.setItem(SLOT_NEXT, next);
        }

        player.openInventory(inv);
    }

    private ItemStack createShopItem(ChestShopEntry entry) {
        Material iconMaterial = resolveMaterialFromItemName(entry.itemName);
        if (iconMaterial == null || iconMaterial == Material.AIR) {
            iconMaterial = Material.CHEST;
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String actionWord = (entry.mode == BrowseMode.BUY ? "BUY" : "SELL");
            meta.setDisplayName("§a" + entry.quantity + "x " + entry.itemName + " §7(" + actionWord + ")");

            List<String> lore = new ArrayList<>();
            lore.add("§7Owner: §b" + entry.owner);
            lore.add("§7Type: " + (entry.mode == BrowseMode.BUY ? "Sell shop (you buy)" : "Buy shop (you sell)"));
            lore.add("§7Price: §e" + entry.price + " virtue");
            if (entry.chestLocation != null) {
                lore.add("§7Coords: §f" + entry.chestLocation.getWorld().getName() + " §7@ §f" +
                        entry.chestLocation.getBlockX() + ", " +
                        entry.chestLocation.getBlockY() + ", " +
                        entry.chestLocation.getBlockZ());
            } else {
                lore.add("§7Coords: §f" + entry.worldName + " §7@ sign " +
                        entry.signX + ", " + entry.signY + ", " + entry.signZ);
            }
            lore.add(" ");
            lore.add("§aClick to track this shop.");
            meta.setLore(lore);

            // Store sign location for lookup on click
            PersistentDataContainer container = meta.getPersistentDataContainer();
            String locString = entry.worldName + "," + entry.signX + "," + entry.signY + "," + entry.signZ;
            container.set(shopLocKey, PersistentDataType.STRING, locString);

            item.setItemMeta(meta);
        }

        return item;
    }

    private List<ChestShopEntry> getAllChestShops(BrowseMode mode) {
        List<ChestShopEntry> result = new ArrayList<>();

        FileConfiguration config = plugin.getConfig();
        if (!config.contains("chestshops")) {
            return result;
        }

        ConfigurationSection section = config.getConfigurationSection("chestshops");
        if (section == null) {
            return result;
        }

        for (String id : section.getKeys(false)) {
            String base = "chestshops." + id;

            String worldName = config.getString(base + ".world");
            String modeStr = config.getString(base + ".mode");
            if (worldName == null || modeStr == null) {
                continue;
            }

            boolean isSellMode = modeStr.equalsIgnoreCase("SELL");
            boolean isBuyMode = modeStr.equalsIgnoreCase("BUY");

            if (mode == BrowseMode.BUY && !isSellMode) {
                continue;
            }
            if (mode == BrowseMode.SELL && !isBuyMode) {
                continue;
            }

            int quantity = config.getInt(base + ".quantity", -1);
            int price = config.getInt(base + ".price", -1);
            String itemName = config.getString(base + ".itemName");
            String owner = config.getString(base + ".owner", "Unknown");
            boolean adminShop = config.getBoolean(base + ".admin", false);

            if (quantity <= 0 || price <= 0 || itemName == null) {
                continue;
            }

            int signX = config.getInt(base + ".signX", Integer.MIN_VALUE);
            int signY = config.getInt(base + ".signY", Integer.MIN_VALUE);
            int signZ = config.getInt(base + ".signZ", Integer.MIN_VALUE);
            if (signX == Integer.MIN_VALUE || signY == Integer.MIN_VALUE || signZ == Integer.MIN_VALUE) {
                continue;
            }

            Location chestLoc = null;
            World world = Bukkit.getWorld(worldName);
            if (world != null && config.contains(base + ".chestX")) {
                int cx = config.getInt(base + ".chestX");
                int cy = config.getInt(base + ".chestY");
                int cz = config.getInt(base + ".chestZ");
                Block chestBlock = world.getBlockAt(cx, cy, cz);
                if (chestBlock.getState() instanceof Chest) {
                    chestLoc = chestBlock.getLocation();
                }
            }

            ChestShopEntry entry = new ChestShopEntry(
                    mode,
                    worldName,
                    signX,
                    signY,
                    signZ,
                    owner,
                    adminShop,
                    quantity,
                    price,
                    itemName,
                    chestLoc
            );

            result.add(entry);
        }

        return result;
    }

    private Material resolveMaterialFromItemName(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return Material.CHEST;
        }

        String key = itemName.trim().toUpperCase().replace(' ', '_');
        Material mat = Material.matchMaterial(key);
        if (mat == null) {
            return Material.CHEST;
        }
        return mat;
    }

    private Block getChestBehindSign(Block signBlock) {
        if (signBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional) signBlock.getBlockData();
            BlockFace facing = directional.getFacing();
            Block chestBlock = signBlock.getRelative(facing.getOppositeFace());
            if (chestBlock.getState() instanceof Chest) {
                return chestBlock;
            }
        }

        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
            Block adjacent = signBlock.getRelative(face);
            if (adjacent.getState() instanceof Chest) {
                return adjacent;
            }
        }

        return null;
    }

    private String getShopOwner(Sign sign) {
        NamespacedKey ownerKey = new NamespacedKey(plugin, "shop_owner");
        PersistentDataContainer container = sign.getPersistentDataContainer();
        if (container.has(ownerKey, PersistentDataType.STRING)) {
            return container.get(ownerKey, PersistentDataType.STRING);
        }
        return null;
    }
}
