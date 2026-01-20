package me.halalcraft.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.halalcraft.HalalCraft;

public class DailyChallengeListener implements Listener {

    private final HalalCraft plugin;
    
    // Track progress: UUID -> Challenge Name -> Progress Count
    private final Map<UUID, Map<String, Integer>> challengeProgress = new HashMap<>();
    
    // Track claimed rewards: UUID -> Challenge Name -> Today's Date
    private final Map<UUID, Map<String, Integer>> claimedRewards = new HashMap<>();

    public DailyChallengeListener(HalalCraft plugin) {
        this.plugin = plugin;
        startDailyReset();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        challengeProgress.putIfAbsent(uuid, new HashMap<>());
        claimedRewards.putIfAbsent(uuid, new HashMap<>());
    }

    /**
     * Reset challenges at configured time daily
     */
    private void startDailyReset() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("daily-challenges.enabled", true)) return;

            int currentDay = plugin.getServerDay();
            
            for (UUID uuid : challengeProgress.keySet()) {
                Map<String, Integer> progress = challengeProgress.get(uuid);
                Map<String, Integer> claimed = claimedRewards.get(uuid);
                
                // Reset progress counters
                for (String challengeName : getChallengeNames()) {
                    progress.put(challengeName, 0);
                    
                    // Check if reward was claimed today and reset for new day
                    if (claimed.getOrDefault(challengeName, -1) != currentDay) {
                        claimed.put(challengeName, -1);
                    }
                }
                
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    playResetSound(player);
                }
            }
        }, 0L, 20 * 60 * 60); // Every hour (simplified, should be more precise)
    }

    /**
     * Get all challenge names from config
     */
    private Set<String> getChallengeNames() {
        return plugin.getConfig().getConfigurationSection("daily-challenges.challenges").getKeys(false);
    }

    /**
     * Increment prayer count (called from HalalCraft when prayer completes)
     */
    public void incrementPrayCount(Player player) {
        addChallengeProgress(player, "pray-all-five", 1);
    }

    /**
     * Increment dua count (called from DuaListener)
     */
    public void incrementDuaCount(Player player) {
        addChallengeProgress(player, "say-duas", 1);
    }

    /**
     * Register congregational prayer participation
     */
    public void registerCongregationalPrayer(Player player) {
        addChallengeProgress(player, "congregational", 1);
    }

    /**
     * Track virtue gain for challenge
     */
    public void trackVirtueGain(Player player, int amount) {
        if (amount > 0) {
            addChallengeProgress(player, "virtue-gain", amount);
        }
    }

    /**
     * Add progress to a challenge
     */
    private void addChallengeProgress(Player player, String challengeName, int amount) {
        UUID uuid = player.getUniqueId();
        challengeProgress.putIfAbsent(uuid, new HashMap<>());
        
        Map<String, Integer> progress = challengeProgress.get(uuid);
        int current = progress.getOrDefault(challengeName, 0);
        int required = plugin.getConfig().getInt("daily-challenges.challenges." + challengeName + ".required", 1);
        
        if (current < required) {
            progress.put(challengeName, current + amount);
            int newProgress = progress.get(challengeName);
            
            // Send progress update
            String msg = plugin.getConfig().getString("daily-challenges.messages.progress", "§e%challenge%: %current%/%required%");
            msg = msg.replace("%challenge%", challengeName)
                     .replace("%current%", String.valueOf(newProgress))
                     .replace("%required%", String.valueOf(required));
            player.sendMessage(msg);
            
            // Check if completed
            if (newProgress >= required) {
                String completed = plugin.getConfig().getString("daily-challenges.messages.completed", 
                        "§a✓ Challenge completed: %challenge%! +%virtue% virtue");
                int virtueReward = plugin.getConfig().getInt("daily-challenges.challenges." + challengeName + ".reward-virtue", 0);
                completed = completed.replace("%challenge%", challengeName)
                                    .replace("%virtue%", String.valueOf(virtueReward));
                player.sendMessage(completed);
            }
        }
    }

    /**
     * Claim reward for completed challenge
     */
    public void claimChallengeReward(Player player, String challengeName) {
        UUID uuid = player.getUniqueId();
        int currentDay = plugin.getServerDay();
        
        Map<String, Integer> claimed = claimedRewards.get(uuid);
        if (claimed.getOrDefault(challengeName, -1) == currentDay) {
            String msg = plugin.getConfig().getString("daily-challenges.messages.already-claimed", 
                    "§cYou already claimed this challenge today!");
            player.sendMessage(msg);
            return;
        }
        
        Map<String, Integer> progress = challengeProgress.get(uuid);
        int current = progress.getOrDefault(challengeName, 0);
        int required = plugin.getConfig().getInt("daily-challenges.challenges." + challengeName + ".required", 1);
        
        if (current >= required) {
            int virtueReward = plugin.getConfig().getInt("daily-challenges.challenges." + challengeName + ".reward-virtue", 0);
            plugin.changeVirtue(player, virtueReward);
            claimed.put(challengeName, currentDay);
            
            String msg = plugin.getConfig().getString("daily-challenges.messages.completed", 
                    "§a✓ Challenge completed: %challenge%! +%virtue% virtue");
            msg = msg.replace("%challenge%", challengeName)
                    .replace("%virtue%", String.valueOf(virtueReward));
            player.sendMessage(msg);
        } else {
            String msg = plugin.getConfig().getString("daily-challenges.messages.progress", 
                    "§e%challenge%: %current%/%required%");
            msg = msg.replace("%challenge%", challengeName)
                    .replace("%current%", String.valueOf(current))
                    .replace("%required%", String.valueOf(required));
            player.sendMessage(msg);
        }
    }

    /**
     * View all challenges and progress
     */
    public void showChallenges(Player player) {
        UUID uuid = player.getUniqueId();
        player.sendMessage("§6§l========== DAILY CHALLENGES ==========");
        
        Map<String, Integer> progress = challengeProgress.getOrDefault(uuid, new HashMap<>());
        
        for (String challengeName : getChallengeNames()) {
            String description = plugin.getConfig().getString("daily-challenges.challenges." + challengeName + ".description", "");
            int current = progress.getOrDefault(challengeName, 0);
            int required = plugin.getConfig().getInt("daily-challenges.challenges." + challengeName + ".required", 1);
            int reward = plugin.getConfig().getInt("daily-challenges.challenges." + challengeName + ".reward-virtue", 0);
            
            String status = current >= required ? "§a✓ DONE" : "§e" + current + "/" + required;
            
            player.sendMessage("§f" + challengeName + " " + status);
            player.sendMessage("  §7" + description + " (Reward: §6+" + reward + "§7 virtue)");
        }
        
        player.sendMessage("§6§l========================================");
        player.sendMessage("§eUse §6/challenge claim <name>§e to claim rewards!");
    }

    /**
     * Play reset sound
     */
    private void playResetSound(Player player) {
        if (plugin.getConfig().getBoolean("daily-challenges.reset-sound.enabled", true)) {
            try {
                String soundName = plugin.getConfig().getString("daily-challenges.reset-sound.type", "block.note_block.chime");
                float volume = (float) plugin.getConfig().getDouble("daily-challenges.reset-sound.volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("daily-challenges.reset-sound.pitch", 1.2);
                String properSound = convertSoundName(soundName);
                player.playSound(player.getLocation(), properSound, volume, pitch);
            } catch (Exception e) {
                // Fallback to default sound
                try {
                    player.playSound(player.getLocation(), "block.note_block.chime", 1.0f, 1.2f);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Convert sound name format
     */
    private String convertSoundName(String soundName) {
        if (soundName == null) return "block.note_block.chime";
        if (soundName.contains(".")) return soundName.toLowerCase();
        return soundName.toLowerCase().replace("_", ".");
    }
}
