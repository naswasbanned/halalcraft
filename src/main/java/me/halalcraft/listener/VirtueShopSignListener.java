package me.halalcraft.listener;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import me.halalcraft.HalalCraft;

public class VirtueShopSignListener implements Listener {

    private final HalalCraft plugin;

    public VirtueShopSignListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle sign creation for shops
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();

        // Check if player is creating a shop sign
        boolean isNormalSell = lines[0].equalsIgnoreCase("[Sell]");
        boolean isNormalBuy  = lines[0].equalsIgnoreCase("[Buy]");
        boolean isAdminSell  = lines[0].equalsIgnoreCase("[aSell]");
        boolean isAdminBuy   = lines[0].equalsIgnoreCase("[aBuy]");

        if (!(isNormalSell || isNormalBuy || isAdminSell || isAdminBuy)) {
            return;
        }

        boolean isSellSign = isNormalSell || isAdminSell;
        boolean isAdminShop = isAdminSell || isAdminBuy;
        Player player = event.getPlayer();
        Block signBlock = event.getBlock();

        // Only ops can create admin shops
        if (isAdminShop && !player.isOp()) {
            player.sendMessage("§cYou are not allowed to create admin shops.");
            event.setCancelled(true);
            return;
        }
        
        // Validate format
        if (lines[1].isEmpty() || lines[2].isEmpty() || lines[3].isEmpty()) {
            player.sendMessage("§cInvalid shop format! Use: [Sell/Buy], quantity, item name, virtue price");
            event.setCancelled(true);
            return;
        }

        // Parse quantity
        int quantity;
        try {
            quantity = Integer.parseInt(lines[1].trim());
            if (quantity <= 0) {
                player.sendMessage("§cQuantity must be positive!");
                event.setCancelled(true);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid quantity! Must be a number.");
            event.setCancelled(true);
            return;
        }

        // Parse price
        int price;
        try {
            price = Integer.parseInt(lines[3].trim());
            if (price <= 0) {
                player.sendMessage("§cPrice must be positive!");
                event.setCancelled(true);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid price! Must be a number.");
            event.setCancelled(true);
            return;
        }

        // Find chest behind the sign
        Block chestBlock = getChestBehindSign(signBlock);
        if (chestBlock == null || !(chestBlock.getState() instanceof Chest)) {
            player.sendMessage("§cNo chest found behind the sign!");
            event.setCancelled(true);
            return;
        }

        Chest chest = (Chest) chestBlock.getState();
        Inventory chestInv = chest.getInventory();

        // For [Buy] signs, require at least one item in chest as detector
        if (!isSellSign) {
            boolean hasItem = false;
            for (ItemStack item : chestInv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    hasItem = true;
                    break;
                }
            }
            if (!hasItem) {
                player.sendMessage("§cFor [Buy] signs, place at least one item in the chest as a detector!");
                player.sendMessage("§eThe item type will be auto-detected and shown on the sign.");
                event.setCancelled(true);
                return;
            }
            
            // Auto-detect item from chest for [Buy] signs
            ItemStack detectorItem = findMatchingItem(chestInv, "");
            if (detectorItem != null) {
                String itemName = formatItemName(detectorItem.getType().name());
                event.setLine(2, "§f" + itemName);
            }
        }

        // Format the sign with colors
        if (isAdminSell) {
            event.setLine(0, "§l[§cASell§r§l]");
        } else if (isAdminBuy) {
            event.setLine(0, "§l[§cABuy§r§l]");
        } else if (isSellSign) {
            event.setLine(0, "§l[§aSell§r§l]");
        } else {
            event.setLine(0, "§l[§6Buy§r§l]");
        }
        event.setLine(1, "§e" + quantity);
        if (isSellSign) {
            event.setLine(2, "§f" + lines[2]);  // Use user input for sell signs
        }
        event.setLine(3, "§b" + price + " Virtue");

        // Store shop owner in sign metadata
        NamespacedKey ownerKey = new NamespacedKey(plugin, "shop_owner");
        Sign signState = (Sign) event.getBlock().getState();
        String ownerNameToStore = isAdminShop ? "SERVER" : player.getName();
        signState.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, ownerNameToStore);
        signState.update();

        player.sendMessage("§a✓ Shop sign created successfully!");
        if (isAdminShop) {
            player.sendMessage("§7This is an admin shop with infinite stock/virtue.");
        } else if (isSellSign) {
            player.sendMessage("§7Players can now right-click the sign to buy items.");
        } else {
            player.sendMessage("§7Players can now right-click the sign with items to sell to you.");
        }
    }

    /**
     * Handle shop sign purchases
     */
    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();
        String[] lines = sign.getLines();

        // Check if it's a shop sign
        boolean isSellSign = lines[0].toLowerCase().contains("sell");
        boolean isBuySign = lines[0].toLowerCase().contains("buy");
        
        if (!lines[0].contains("[") || (!isSellSign && !isBuySign)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Parse shop data
        int quantity;
        int price;
        String itemName = lines[2].replace("§f", "").trim();

        try {
            quantity = Integer.parseInt(lines[1].replace("§e", "").trim());
            String priceStr = lines[3].replace("§b", "").replace(" Virtue", "").trim();
            price = Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§cShop sign is corrupted!");
            return;
        }

        if (isSellSign) {
            handleSellSignClick(player, sign, block, quantity, price, itemName);
        } else {
            handleBuySignClick(player, sign, block, quantity, price, itemName);
        }
    }

    /**
     * Handle [Sell] sign click - buyer purchases items
     */
    private void handleSellSignClick(Player buyer, Sign sign, Block signBlock, int quantity, int price, String itemName) {
        // Detect admin sell shop by header
        String header = ChatColor.stripColor(sign.getLine(0)).toLowerCase();
        boolean isAdminSell = header.contains("asell");

        // Check buyer has enough virtue
        int buyerVirtue = plugin.getVirtue(buyer);
        if (buyerVirtue < price) {
            buyer.sendMessage("§cYou need " + (price - buyerVirtue) + " more virtue! Current: " + buyerVirtue);
            return;
        }

        // Find chest behind sign
        Block chestBlock = getChestBehindSign(signBlock);
        if (chestBlock == null || !(chestBlock.getState() instanceof Chest)) {
            buyer.sendMessage("§cShop chest not found!");
            return;
        }

        Chest chest = (Chest) chestBlock.getState();
        Inventory chestInv = chest.getInventory();

        // Find matching items in chest (used as template for admin shops)
        ItemStack itemToSell = findMatchingItem(chestInv, itemName);
        if (itemToSell == null) {
            buyer.sendMessage("§cShop is out of stock or misconfigured!");
            return;
        }

        // For normal shops, enforce stock limits; admin shops have infinite stock
        if (!isAdminSell) {
            int availableAmount = countItems(chestInv, itemToSell);
            if (availableAmount < quantity) {
                buyer.sendMessage("§cShop only has " + availableAmount + " items left!");
                return;
            }
        }

        // Check if buyer has inventory space
        if (!hasInventorySpace(buyer, quantity)) {
            buyer.sendMessage("§cYou don't have enough inventory space!");
            return;
        }

        // Process transaction
        // 1. Remove items from chest (normal shops only)
        if (!isAdminSell) {
            removeItems(chestInv, itemToSell, quantity);
        }

        // 2. Deduct virtue from buyer
        plugin.changeVirtue(buyer, -price);

        // 3. Give items to buyer
        ItemStack purchasedItem = itemToSell.clone();
        purchasedItem.setAmount(quantity);
        buyer.getInventory().addItem(purchasedItem);

        // 4. Give virtue to shop owner (if online, non-admin shop)
        String ownerName = getShopOwner(sign);
        if (ownerName != null && !"SERVER".equalsIgnoreCase(ownerName)) {
            Player owner = Bukkit.getPlayerExact(ownerName);
            if (owner != null && owner.isOnline()) {
                plugin.changeVirtue(owner, price);
                owner.sendMessage("§a✓ Sold " + quantity + " " + itemName + " for " + price + " virtue!");
            }
        }

        buyer.sendMessage("§a✓ Purchased " + quantity + " " + itemName + " for " + price + " virtue!");
        plugin.saveVirtueData();
    }

    /**
     * Handle [Buy] sign click - seller sells items to buyer
     */
    private void handleBuySignClick(Player seller, Sign sign, Block signBlock, int quantity, int price, String itemName) {
        // Detect admin buy shop by header
        String header = ChatColor.stripColor(sign.getLine(0)).toLowerCase();
        boolean isAdminBuy = header.contains("abuy");

        // Get the buyer (sign owner)
        String buyerName = getShopOwner(sign);
        if (buyerName == null) {
            seller.sendMessage("§cShop owner not found!");
            return;
        }

        Player buyer = Bukkit.getPlayerExact(buyerName);
        boolean buyerOffline = buyer == null || !buyer.isOnline();

        // For normal shops, check owner's stored virtue; admin buy shops have infinite virtue
        if (!isAdminBuy) {
            int buyerStoredVirtue = plugin.getVirtueByName(buyerName);
            if (buyerStoredVirtue < price) {
                seller.sendMessage("§cShop owner doesn't have enough virtue!");
                seller.sendMessage("§7Required: §b" + price + "§7 | Owner has: §b" + buyerStoredVirtue);
                seller.sendMessage("§e(This shop is temporarily disabled due to insufficient funds)");
                return;
            }
        }

        // Check seller has item in hand
        ItemStack itemInHand = seller.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            seller.sendMessage("§cYou must hold the item you want to sell!");
            return;
        }

        // Check if held item matches the sign
        if (!itemMatchesName(itemInHand, itemName)) {
            seller.sendMessage("§cYou're holding " + formatItemName(itemInHand.getType().name()) + " but this shop wants " + itemName + "!");
            return;
        }

        // Check seller has enough items
        if (itemInHand.getAmount() < quantity) {
            seller.sendMessage("§cYou need " + quantity + " items but only have " + itemInHand.getAmount() + "!");
            return;
        }

        // Find chest behind sign
        Block chestBlock = getChestBehindSign(signBlock);
        if (chestBlock == null || !(chestBlock.getState() instanceof Chest)) {
            seller.sendMessage("§cShop chest not found!");
            return;
        }

        // Get chest and validate space
        Chest chest = (Chest) chestBlock.getState();
        Inventory chestInv = chest.getInventory();

        // For normal shops, ensure chest has space; admin buy shops can overflow into the void
        if (!isAdminBuy) {
            if (!hasChestSpace(chestInv, quantity)) {
                seller.sendMessage("§cShop chest is full!");
                return;
            }
        }

        // Prepare item to transfer
        ItemStack itemToAdd = itemInHand.clone();
        itemToAdd.setAmount(quantity);

        // Remove items from seller's hand FIRST - this must succeed before touching chest
        ItemStack toRemove = itemInHand.clone();
        toRemove.setAmount(quantity);
        seller.getInventory().removeItem(toRemove);
        seller.updateInventory();

        // Now add to chest - get FRESH chest state from block
        // This is critical - we need to reload the chest state to ensure we're working with current data
        Chest freshChest = (Chest) chestBlock.getState();
        Inventory freshChestInv = freshChest.getInventory();

        HashMap<Integer, ItemStack> overflow = freshChestInv.addItem(itemToAdd);

        if (!isAdminBuy && !overflow.isEmpty()) {
            // Some items couldn't fit - return to seller for normal shops
            seller.getInventory().addItem(itemInHand);
            seller.updateInventory();
            seller.sendMessage("§cCouldn't add all items to chest - returned to your inventory!");
            return;
        }

        // All items were handled - save the block state for normal shops
        if (!isAdminBuy) {
            chestBlock.getState().update();
        }

        // Process virtue transaction
        if (isAdminBuy) {
            // Admin buy: server pays virtue, items are removed (optionally stored if space)
            plugin.changeVirtue(seller, price);
            seller.sendMessage("§a✓ Sold " + quantity + " " + itemName + " to the admin shop for " + price + " virtue!");
        } else if (buyerOffline) {
            // Give virtue to seller IMMEDIATELY
            plugin.changeVirtue(seller, price);

            // Deduct from buyer's persistent virtue account (offline-safe)
            int buyerCurrentVirtue = plugin.getVirtueByName(buyerName);
            int newBuyerVirtue = Math.max(0, buyerCurrentVirtue - price);

            java.util.UUID buyerUUID = getPlayerUUID(buyerName);
            if (buyerUUID != null) {
                plugin.setVirtueByUUID(buyerUUID, newBuyerVirtue);
            }

            seller.sendMessage("§a✓ Sold " + quantity + " " + itemName + " for " + price + " virtue!");
            seller.sendMessage("§7(Buyer is offline - virtue deducted from their account)");
        } else {
            // Buyer is online - process immediately
            plugin.changeVirtue(buyer, -price);
            plugin.changeVirtue(seller, price);
            seller.sendMessage("§a✓ Sold " + quantity + " " + itemName + " for " + price + " virtue!");
            buyer.sendMessage("§a✓ Purchased " + quantity + " " + itemName + " for " + price + " virtue!");
        }

        plugin.saveVirtueData();
    }

    /**
     * Prevent players from opening shop chests
     */
    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof Chest)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Chest chest = (Chest) event.getInventory().getHolder();
        Block chestBlock = chest.getBlock();

        // Check if this chest has a shop sign in front
        if (hasShopSign(chestBlock)) {
            String owner = getChestShopOwner(chestBlock);
            if (owner != null && !player.getName().equals(owner) && !player.isOp()) {
                event.setCancelled(true);
                player.sendMessage("§cThis is a shop chest! Only the owner can access it.");
            }
        }
    }

    /**
     * Protect shop signs and chests from being broken by non-owners
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if breaking a shop sign
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            String[] lines = sign.getLines();
            
            if (lines[0].contains("[") && (lines[0].toLowerCase().contains("sell") || lines[0].toLowerCase().contains("buy"))) {
                String owner = getShopOwner(sign);
                if (owner != null && !player.getName().equals(owner) && !player.isOp()) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot break someone else's shop sign!");
                    return;
                }
                player.sendMessage("§eShop sign removed.");
            }
        }

        // Check if breaking a shop chest
        if (block.getState() instanceof Chest) {
            if (hasShopSign(block)) {
                String owner = getChestShopOwner(block);
                if (owner != null && !player.getName().equals(owner) && !player.isOp()) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot break someone else's shop chest!");
                }
            }
        }
    }

    // ===== HELPER METHODS =====

    private Block getChestBehindSign(Block signBlock) {
        // Try the directional facing first
        if (signBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional) signBlock.getBlockData();
            BlockFace facing = directional.getFacing();
            Block chestBlock = signBlock.getRelative(facing.getOppositeFace());
            if (chestBlock.getState() instanceof Chest) {
                return chestBlock;
            }
        }

        // If that doesn't work, check all adjacent blocks
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
            Block adjacent = signBlock.getRelative(face);
            if (adjacent.getState() instanceof Chest) {
                return adjacent;
            }
        }

        return null;
    }

    private ItemStack findMatchingItem(Inventory inv, String itemName) {
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            // If itemName is empty, return the first non-air item (for detector)
            if (itemName.isEmpty()) {
                return item;
            }

            String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                ? item.getItemMeta().getDisplayName() 
                : item.getType().name().replace("_", " ");

            if (displayName.equalsIgnoreCase(itemName) || 
                item.getType().name().replace("_", " ").equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    private int countItems(Inventory inv, ItemStack target) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.isSimilar(target)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Inventory inv, ItemStack target, int amount) {
        int remaining = amount;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.isSimilar(target)) {
                if (item.getAmount() > remaining) {
                    item.setAmount(item.getAmount() - remaining);
                    return;
                } else {
                    remaining -= item.getAmount();
                    inv.setItem(i, null);
                    if (remaining <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private boolean hasInventorySpace(Player player, int amount) {
        int emptySlots = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        return emptySlots > 0;
    }

    private boolean hasShopSign(Block chestBlock) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block relative = chestBlock.getRelative(face);
            if (relative.getState() instanceof Sign) {
                Sign sign = (Sign) relative.getState();
                String[] lines = sign.getLines();
                if (lines[0].contains("[") && (lines[0].toLowerCase().contains("sell") || lines[0].toLowerCase().contains("buy"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getChestShopOwner(Block chestBlock) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block relative = chestBlock.getRelative(face);
            if (relative.getState() instanceof Sign) {
                Sign sign = (Sign) relative.getState();
                String owner = getShopOwner(sign);
                if (owner != null) {
                    return owner;
                }
            }
        }
        return null;
    }

    private String getShopOwner(Sign sign) {
        NamespacedKey ownerKey = new NamespacedKey(plugin, "shop_owner");
        if (sign.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
            return sign.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        }
        return null;
    }

    private boolean itemMatchesName(ItemStack item, String name) {
        String itemDisplayName = formatItemName(item.getType().name());
        return itemDisplayName.equalsIgnoreCase(name);
    }

    private String formatItemName(String materialName) {
        String[] parts = materialName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private boolean hasChestSpace(Inventory inv, int amount) {
        int emptySlots = 0;
        for (ItemStack item : inv.getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        return emptySlots > 0;
    }

    /**
     * Get player UUID by name (works for online and offline players)
     */
    private java.util.UUID getPlayerUUID(String playerName) {
        // Try online player first
        org.bukkit.entity.Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            return online.getUniqueId();
        }
        
        // Search offline players
        for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(playerName)) {
                return op.getUniqueId();
            }
        }
        
        return null; // Player not found
    }
}
