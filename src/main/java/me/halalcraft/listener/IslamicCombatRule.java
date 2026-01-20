package me.halalcraft.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.halalcraft.HalalCraft;

public class IslamicCombatRule implements Listener {

    private final HalalCraft plugin;

    // Player UUID -> timestamp when mob first hit player
    private final Map<UUID, Long> freeKillWindow = new HashMap<>();

    // Prevent message spam
    private final Map<UUID, Boolean> notified = new HashMap<>();

    // Track which enchanted items have been warned about
    private final Map<String, Boolean> haramItemsWarned = new HashMap<>();

    // free kill duration is configurable in seconds under 'combat.free-kill-duration-seconds'

    public IslamicCombatRule(HalalCraft plugin) {
        this.plugin = plugin;
        startEnchantmentDetection();
    }

    /**
     * Detect haram enchanted items and tag them
     */
    private void startEnchantmentDetection() {
        if (!plugin.getConfig().getBoolean("enchantment-haram.enabled", true)) return;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkPlayerEnchantedItems(player);
            }
        }, 0L, plugin.getConfig().getLong("enchantment-haram.check-interval-ticks", 20L));
    }

    /**
     * Check if player is holding or has equipped enchanted items
     */
    private void checkPlayerEnchantedItems(Player player) {
        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isHaramEnchanted(mainHand)) {
            markAsHaram(mainHand);
        }

        // Check off hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isHaramEnchanted(offHand)) {
            markAsHaram(offHand);
        }

        // Check armor
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (isHaramEnchanted(armor)) {
                markAsHaram(armor);
            }
        }
    }

    /**
     * Check if item has enchantments and is not already marked as purified
     */
    private boolean isHaramEnchanted(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getEnchantments().isEmpty()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        // Don't mark upgrade books as haram
        if (meta.getPersistentDataContainer().has(plugin.getUpgradeBookKey(), PersistentDataType.STRING)) {
            return false;
        }

        // Don't mark purified items (from upgrade system) as haram
        if (meta.getPersistentDataContainer().has(plugin.getPurifiedKey(), PersistentDataType.BYTE)) {
            return false;
        }

        // Mark all enchanted items as haram (except upgrade books and purified items)
        return true;
    }

    /**
     * Mark item with NBT tag as haram
     */
    private void markAsHaram(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Check if already marked
        if (meta.getPersistentDataContainer().has(plugin.getHaramKey(), PersistentDataType.BYTE)) {
            return; // Already marked
        }

        // Mark as haram with NBT tag
        meta.getPersistentDataContainer().set(plugin.getHaramKey(), PersistentDataType.BYTE, (byte) 1);

        // Add lore to show it's unpurified
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        String haramLore = plugin.getConfig().getString("messages.haram-enchantment-detected", 
                "§c⚠ Unpurified Enchantment");
        if (!lore.contains(haramLore)) {
            lore.add(""); // Empty line for spacing
            lore.add(haramLore);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /* =====================================================
     * MOB → PLAYER (start free kill)
     * ===================================================== */
    @EventHandler
    public void onMobHitPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        LivingEntity damager = null;

        // Direct attack from a monster
        if (e.getDamager() instanceof Monster) {
            damager = (Monster) e.getDamager();
        }
        // Projectile from a monster (e.g., skeleton arrow)
        else if (e.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Monster) {
                damager = (Monster) projectile.getShooter();
            }
        }

        if (damager == null) return;

        Player player = (Player) e.getEntity();
        UUID id = player.getUniqueId();

        long now = System.currentTimeMillis();

        // Start / refresh free kill window
        freeKillWindow.put(id, now);

        // Notify only once per window
        if (!notified.getOrDefault(id, false)) {
            long seconds = plugin.getConfig().getLong("combat.free-kill-duration-seconds", 120);
            String msg = plugin.getConfig().getString("messages.free-kill", "§aYou were attacked! Free combat allowed for %s seconds.");
            player.sendMessage(String.format(msg, seconds));
            notified.put(id, true);
        }
    }

    /* =====================================================
     * PLAYER → MOB (virtue loss per hit if NOT attacked)
     * ===================================================== */
    @EventHandler
    public void onPlayerHitMob(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof Monster)) return;

        Player player = (Player) e.getDamager();
        UUID id = player.getUniqueId();

        long now = System.currentTimeMillis();
        Long start = freeKillWindow.get(id);

        boolean freeKillActive = start != null && (now - start) <= (plugin.getConfig().getLong("combat.free-kill-duration-seconds", 120) * 1000L);

        if (!freeKillActive) {
            int penalty = plugin.getConfig().getInt("economy.virtue.illegal-attack", -2);
            plugin.changeVirtue(player, penalty);
            String msg = plugin.getConfig().getString("messages.illegal-attack", "§cYou attacked a mob without being attacked first. Virtue -%d");
            player.sendMessage(String.format(msg, Math.abs(penalty)));
        }
    }

    /* =====================================================
     * PLAYER → NON-HOSTILE MOBS (horses, cats, etc)
     * ===================================================== */
    @EventHandler
    public void onPlayerHitNonHostileMob(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (e.getEntity() instanceof Monster) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) e.getEntity();
        
        // Check if it's a non-hostile, non-farm mob (horses, cats, parrots, etc.)
        if (entity instanceof Horse || entity instanceof Donkey || entity instanceof Cat || entity instanceof Parrot) {
            Player player = (Player) e.getDamager();
            int penalty = plugin.getConfig().getInt("economy.virtue.hit-non-hostile-mob", -10);
            plugin.changeVirtue(player, penalty);
            String msg = plugin.getConfig().getString("messages.hit-non-hostile-mob", "§cYou hurt an innocent animal. Virtue -%d");
            player.sendMessage(String.format(msg, Math.abs(penalty)));
        }
    }

    /* =====================================================
     * FARM ANIMAL DROPS (slaughter dua check)
     * ===================================================== */
    @EventHandler
    public void onFarmAnimalDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        
        // Check if it's a farm animal
        if (!(entity instanceof Cow || entity instanceof Pig || entity instanceof Sheep || entity instanceof Chicken || entity instanceof Rabbit)) {
            return;
        }
        
        // Get the killer
        Player killer = entity.getKiller();
        if (killer == null) return;
        
        // Check if player has active slaughter dua
        DuaListener duaListener = plugin.getDuaListener();
        if (duaListener == null) return;
        
        if (!duaListener.hasActiveDua(killer, "slaughter")) {
            e.getDrops().clear();
            killer.sendMessage(plugin.getConfig().getString("messages.farm-animal-no-dua", "§cYou must say the slaughter dua before killing farm animals."));
        }
    }

    /* =====================================================
     * ENCHANTMENT USE (blackmagic penalty)
     * ===================================================== */
    @EventHandler
    public void onPlayerAttackWithEnchantedItem(EntityDamageByEntityEvent e) {
        // DISABLED - Uses new ImpureEnchantmentPenaltyListener system
        return;
    }

    /* =====================================================
     * POTION DRINKING (alcohol/intoxication penalty)
     * ===================================================== */
    @EventHandler
    public void onPlayerDrinkPotion(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        
        // Check if it's a potion
        if (item.getType().name().contains("POTION")) {
            int penalty = plugin.getConfig().getInt("economy.virtue.potion-drinking", -10);
            plugin.changeVirtue(player, penalty);
            String msg = plugin.getConfig().getString("messages.potion-drinking", "§cDrinking potions is haram! Virtue -%d");
            player.sendMessage(String.format(msg, Math.abs(penalty)));
            
            // Apply nausea effect
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    PotionEffectType.NAUSEA,
                    plugin.getConfig().getInt("effects.nausea-duration-seconds", 30) * 20,
                    0
            ));
        }
    }

    /* =====================================================
     * ENCHANTING TABLE PLACEMENT WARNING
     * ===================================================== */
    @EventHandler
    public void onEnchantingTablePlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() == Material.ENCHANTING_TABLE) {
            Player player = e.getPlayer();
            String warning = plugin.getConfig().getString("messages.enchanting-table-warning", 
                    "§c⚠ WARNING: %player% has placed an enchanting table! Blackmagic is forbidden in Islam!");
            warning = warning.replace("%player%", player.getName());
            Bukkit.broadcastMessage(warning);
        }
    }

    /* =====================================================
     * PIG HARAM SYSTEM (Slowness infinite)
     * ===================================================== */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        
        // Check if player has pork in inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.PORKCHOP || item.getType() == Material.COOKED_PORKCHOP)) {
                applyPigHaramEffect(player);
                return;
            }
        }
        
        // Check if player is near a pig
        Location loc = player.getLocation();
        for (LivingEntity entity : loc.getNearbyLivingEntities(2.0, 2.0, 2.0)) {
            if (entity instanceof Pig) {
                applyPigHaramEffect(player);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerHitPig(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof Pig)) return;
        
        Player player = (Player) e.getDamager();
        applyPigHaramEffect(player);
    }

    private void applyPigHaramEffect(Player player) {
        // Only apply if player doesn't have slowness already or if it needs renewal
        if (!player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1), true);
            player.sendMessage(plugin.getConfig().getString("messages.pig-haram", 
                    "§c§lYou touched a pig! Infinite slowness applied. Drink milk to cure!"));
        }
    }

    @EventHandler
    public void onPlayerDrinkMilk(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        if (e.getItem().getType() == Material.MILK_BUCKET) {
            // Milk removes all potion effects
            player.sendMessage(plugin.getConfig().getString("messages.milk-cure", 
                    "§aThe milk has purified you!"));
        }
    }

    /* =====================================================
     * HELPERS (Scoreboard & Reset)
     * ===================================================== */
    public boolean isFreeKillActive(Player p) {
        return getFreeKillTimeLeft(p) > 0;
    }

    public long getFreeKillTimeLeft(Player p) {
        Long start = freeKillWindow.get(p.getUniqueId());
        if (start == null) return 0;

        long durationMs = plugin.getConfig().getLong("combat.free-kill-duration-seconds", 120) * 1000L;
        long left = durationMs - (System.currentTimeMillis() - start);
        if (left <= 0) {
            resetCombatWindow(p);
            return 0;
        }
        return left;
    }

    public void resetCombatWindow(Player p) {
        freeKillWindow.remove(p.getUniqueId());
        notified.remove(p.getUniqueId());
    }
}
