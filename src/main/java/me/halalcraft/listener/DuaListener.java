package me.halalcraft.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.halalcraft.HalalCraft;

public class DuaListener implements Listener {

    private final HalalCraft plugin;

    // Map<Player UUID, Map<Dua Name, Expiry Timestamp>>
    private final Map<UUID, Map<String, Long>> activeDuas = new HashMap<>();

    // Duration is configurable per-dua in config.yml

    public DuaListener(HalalCraft plugin) {
        this.plugin = plugin;

        // Scheduler to remove expired duas automatically
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (UUID playerId : activeDuas.keySet()) {
                Map<String, Long> duaMap = activeDuas.get(playerId);
                duaMap.entrySet().removeIf(entry -> entry.getValue() <= now);
            }
        }, 20L, 20L); // every second
    }

    /**
     * Register any dua for a player
     */
    public boolean registerDua(Player player, String name) {
        if (name == null) return false;
        name = name.toLowerCase();

        // Validate dua exists in config
        if (!plugin.getConfig().contains("dua." + name)) {
            player.sendMessage("§cUnknown dua: " + name);
            return false;
        }

        // once-per-day enforcement
        boolean oncePerDay = plugin.getConfig().getBoolean("dua." + name + ".once-per-day", false);
        if (oncePerDay) {
            String dayPath = "players." + player.getUniqueId() + ".duas." + name + ".day";
            int lastDay = plugin.getConfig().getInt(dayPath, Integer.MIN_VALUE);
            if (lastDay == plugin.getServerDay()) {
                String already = plugin.getConfig().getString("messages.dua-already-said", "§cYou can only say the %dua% dua once per day.");
                player.sendMessage(already.replace("%dua%", name));
                return false;
            }
            plugin.getConfig().set(dayPath, plugin.getServerDay());
            plugin.saveConfig();

            int virtueGain = plugin.getConfig().getInt("dua." + name + ".virtue-on-say", 1);
            if (virtueGain != 0) plugin.changeVirtue(player, virtueGain);
        }

        activeDuas.putIfAbsent(player.getUniqueId(), new HashMap<>());
        int durationSeconds = plugin.getConfig().getInt("dua." + name + ".duration-seconds",
                plugin.getConfig().getInt("dua.default-duration-seconds", 300));
        long expiry = System.currentTimeMillis() + durationSeconds * 1000L;
        activeDuas.get(player.getUniqueId()).put(name, expiry);

        String text = plugin.getConfig().getString("dua." + name + ".text", "{player} said " + name);
        text = text.replace("{player}", player.getName()).replace("%player%", player.getName());

        // If this dua is configured to broadcast (e.g., slaughter), broadcast; otherwise send to player
        boolean broadcast = plugin.getConfig().getBoolean("dua." + name + ".broadcast", name.equalsIgnoreCase("slaughter"));
        if (broadcast) Bukkit.broadcastMessage(text);
        else player.sendMessage(text);

        return true;
    }

    /**
     * Check if a player has a specific active dua
     */
    public boolean hasActiveDua(Player player, String name) {
        if (name == null) return false;
        name = name.toLowerCase();
        Map<String, Long> map = activeDuas.get(player.getUniqueId());
        if (map == null) return false;
        Long expiry = map.get(name);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    /**
     * Get all active duas for a player
     */
    public Map<String, Long> getActiveDua(Player player) {
        return activeDuas.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    /**
     * Reset all active duas (e.g., new day)
     */
    public void resetAllDuas() {
        activeDuas.clear();
    }

    /**
     * Prevent sleeping if `sleep` dua is required and not active.
     */
    @EventHandler
    public void onPlayerBedEnter(org.bukkit.event.player.PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        String duaName = "sleep";

        if (!plugin.getConfig().contains("dua." + duaName)) return;
        boolean required = plugin.getConfig().getBoolean("dua." + duaName + ".required", true);
        if (!required) return;

        boolean hasDua = hasActiveDua(player, duaName);
        if (!hasDua) {
            String msg = plugin.getConfig().getString("messages.need-dua", "§cYou must say the %dua% dua before doing that.");
            player.sendMessage(msg.replace("%dua%", duaName));
            event.setCancelled(true);
        }
    }

    /**
     * Clear sleep dua when player leaves the bed (wakes up).
     */
    @EventHandler
    public void onPlayerBedLeave(org.bukkit.event.player.PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        Map<String, Long> map = activeDuas.get(player.getUniqueId());
        if (map == null || map.isEmpty()) return;

        // Clear all active duas for the player when they sleep/wake
        map.clear();
        player.sendMessage(plugin.getConfig().getString("messages.duas-cleared", "§eAll your duas have been cleared."));
    }

    /**
     * Detect /dua <type> command typed directly
     */
    @EventHandler
    public void onDuaCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String raw = message.trim();
        if (raw.toLowerCase().startsWith("/dua")) {
            String[] parts = raw.split(" ", 2);
            String arg = parts.length > 1 ? parts[1].trim() : "";

            // allow /dua and /dua list to proceed to the normal command handler
            if (arg.isEmpty() || arg.equalsIgnoreCase("list")) return;

            Player player = event.getPlayer();
            String duaName = arg.toLowerCase(); // extract name after "/dua "
            boolean ok = registerDua(player, duaName);
            if (ok) {
                int dur = plugin.getConfig().getInt("dua." + duaName + ".duration-seconds",
                    plugin.getConfig().getInt("dua.default-duration-seconds", 300));
                String msg = plugin.getConfig().getString("messages.dua-said",
                    "§aYou have said the %dua% dua. It will last %duration% seconds.");
                msg = msg.replace("%dua%", duaName).replace("%duration%", String.valueOf(dur));
                player.sendMessage(msg);
            }
            event.setCancelled(true);
        }
    }

    /**
     * Detect player killing a farm animal (for slaughter dua)
     */
    @EventHandler
    public void onEntityKilled(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.getKiller() instanceof Player)) return;

        Player player = entity.getKiller();

        boolean isFarmAnimal = entity instanceof Cow
                || entity instanceof Sheep
                || entity instanceof Pig
                || entity instanceof Chicken
                || entity instanceof Rabbit;

        if (!isFarmAnimal) return;

        // Penalize if player does NOT have slaughter dua active
        if (!hasActiveDua(player, "slaughter")) {
            int penalty = plugin.getConfig().getInt("economy.virtue.miss-dua-slaughter", -1);
            plugin.changeVirtue(player, penalty);
            player.sendMessage("§cYou killed an animal without saying the slaughter dua! Virtue -" + Math.abs(penalty));
        }
    }

    /**
      * Handle eating: if player eats without saying the 'eating' dua reduce food benefits and apply virtue penalty.
      */
     @EventHandler
     public void onPlayerItemConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
         Player player = event.getPlayer();

         // If they have the eating dua active, do nothing special
         if (hasActiveDua(player, "eating")) return;

         // Capture old values before the consumption takes effect
         int oldFood = player.getFoodLevel();
         float oldSat = player.getSaturation();

         Bukkit.getScheduler().runTaskLater(plugin, () -> {
             int newFood = player.getFoodLevel();
             float newSat = player.getSaturation();

             int delta = newFood - oldFood;
             float deltaSat = newSat - oldSat;

             if (delta > 0) {
                 int reduce = delta / 2; // cut the food gain (half)
                 int adjusted = Math.max(0, newFood - reduce);
                 player.setFoodLevel(Math.min(20, adjusted));
             }

             if (deltaSat > 0f) {
                 float reduceSat = deltaSat / 2f;
                 float adjustedSat = Math.max(0f, newSat - reduceSat);
                 player.setSaturation(Math.min(player.getFoodLevel(), adjustedSat));
             }

             int penalty = plugin.getConfig().getInt("economy.virtue.miss-dua-eating", -1);
             plugin.changeVirtue(player, penalty);

             String msg = plugin.getConfig().getString("messages.eating-missed", "§cYou ate without saying the dua. Virtue -%d");
             player.sendMessage(String.format(msg, Math.abs(penalty)));

             String cutMsg = plugin.getConfig().getString("messages.eating-cut", "§eYour food was less effective because you didn't say the dua.");
             player.sendMessage(cutMsg);
         }, 1L);
     }
}
