package me.halalcraft.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.halalcraft.HalalCraft;

public class PrayerWarningListener implements Listener {

    private final HalalCraft plugin;
    private final Map<UUID, Map<String, Boolean>> warningsSent = new HashMap<>();
    
    public PrayerWarningListener(HalalCraft plugin) {
        this.plugin = plugin;
        startWarningSystem();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        warningsSent.putIfAbsent(uuid, new HashMap<>());
    }

    /**
     * Main warning loop - checks every second if a warning should be sent
     */
    private void startWarningSystem() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("warnings.enabled", true)) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                checkPrayerWarning(player);
            }
        }, 20L, 20L); // Every second
    }

    /**
     * Check if player should receive warning for upcoming prayer
     */
    private void checkPrayerWarning(Player player) {
        long time = player.getWorld().getTime();
        HalalCraft.PrayerInfo closest = plugin.getClosestPrayer(time);
        
        List<Integer> warningTimes = plugin.getConfig().getIntegerList("warnings.times");
        String currentPrayer = plugin.getCurrentPrayer(time);
        
        // Skip if no active prayer
        if (currentPrayer == null || closest == null) return;
        
        // Skip if next prayer is the same as current (already praying)
        if (closest.name.equalsIgnoreCase(currentPrayer)) return;

        UUID uuid = player.getUniqueId();
        warningsSent.putIfAbsent(uuid, new HashMap<>());
        Map<String, Boolean> sentMap = warningsSent.get(uuid);

        // Check each warning time
        for (Integer warningSeconds : warningTimes) {
            long warningTicks = warningSeconds * 20L; // Convert to ticks
            String warningKey = closest.name + "_" + warningSeconds;

            // If time until prayer matches warning time (within 1 second)
            if (closest.ticksUntil >= warningTicks - 10 && closest.ticksUntil <= warningTicks + 10) {
                if (!sentMap.getOrDefault(warningKey, false)) {
                    sendWarning(player, closest.name, warningSeconds);
                    sentMap.put(warningKey, true);
                }
            }
        }

        // Reset warnings when new prayer arrives
        if (closest.ticksUntil > 1000) {
            sentMap.clear();
        }
    }

    /**
     * Send prayer warning with sound effect
     */
    private void sendWarning(Player player, String prayerName, int secondsUntil) {
        String msgKey = secondsUntil == 300 ? "warnings.message-5min" : "warnings.message-1min";
        String msg = plugin.getConfig().getString(msgKey, "§e⏰ §l%prayer% prayer!");
        player.sendMessage(msg.replace("%prayer%", prayerName));

        if (!plugin.getConfig().getBoolean("warnings.sound.enabled", true)) return;
        
        String soundName = plugin.getConfig().getString("warnings.sound.type", "block.note_block.ding");
        float volume = (float) plugin.getConfig().getDouble("warnings.sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("warnings.sound.pitch", 1.0);

        try {
            String properSound = soundName.toLowerCase();
            if (!properSound.contains(".")) {
                properSound = properSound.replace("_", ".");
            }
            player.playSound(player.getLocation(), properSound, SoundCategory.MASTER, volume, pitch);
        } catch (Exception ignored) {
            // Invalid sound, silently ignore
        }
    }

    /**
     * Clear warnings for a prayer (called when prayer time starts)
     */
    public void clearWarningsForPrayer(String prayerName) {
        for (Map<String, Boolean> sentMap : warningsSent.values()) {
            sentMap.entrySet().removeIf(e -> e.getKey().startsWith(prayerName));
        }
    }

    /**
     * Reset all warnings (called on new day)
     */
    public void resetAllWarnings() {
        warningsSent.forEach((uuid, map) -> map.clear());
    }
}
