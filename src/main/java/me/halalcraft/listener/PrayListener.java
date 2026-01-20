package me.halalcraft.listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.halalcraft.HalalCraft;
import me.halalcraft.mosque.MosqueManager;

public class PrayListener implements Listener {
    private final HalalCraft plugin;
    private final Map<UUID, String> playerInMosque = new HashMap<>();
    
    // Track active congregational prayers: key is player name (who started it)
    // Value also tracks the initiator's UUID
    private final Map<String, CongregationalPrayer> activePrayers = new HashMap<>();

    public PrayListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    /**
     * Inner class to represent a congregational prayer
     */
    private class CongregationalPrayer {
        UUID initiatorUUID; // The player who started the prayer
        String mosqueName;
        Set<UUID> participants; // All players participating
        String prayerName; // Which prayer (Subh, Dzuhr, etc.)

        CongregationalPrayer(UUID initiator, String mosqueName, String prayerName) {
            this.initiatorUUID = initiator;
            this.mosqueName = mosqueName;
            this.prayerName = prayerName;
            this.participants = new HashSet<>();
            this.participants.add(initiator);
        }
    }

    /**
     * Get the current mosque a player is in, if any
     */
    private MosqueManager.MosqueArea getPlayerMosque(Player player) {
        for (String mosqueName : MosqueManager.getMosqueNames()) {
            MosqueManager.MosqueArea mosque = MosqueManager.getMosque(mosqueName);
            if (mosque.isInside(player.getLocation())) {
                return mosque;
            }
        }
        return null;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        MosqueManager.MosqueArea mosque = getPlayerMosque(player);
        String currentMosque = playerInMosque.get(uuid);
        
        if (mosque != null && !mosque.getName().equals(currentMosque)) {
            playerInMosque.put(uuid, mosque.getName());
            String msg = plugin.getConfig().getString("messages.mosque-enter", "§aYou entered the mosque: §e%mosque%");
            player.sendMessage(msg.replace("%mosque%", mosque.getName()));
        } else if (mosque == null && currentMosque != null) {
            playerInMosque.remove(uuid);
            String msg = plugin.getConfig().getString("messages.mosque-leave", "§eYou left the mosque: %mosque%");
            player.sendMessage(msg.replace("%mosque%", currentMosque));
            endCongregationalPrayerIfLeaderLeft(player);
        }
    }

    /**
     * Handle when a player quits - end their congregational prayer if they were leading
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        endCongregationalPrayerIfLeaderLeft(player);
    }

    /**
     * End the congregational prayer if the leader left the mosque or server
     */
    private void endCongregationalPrayerIfLeaderLeft(Player leader) {
        CongregationalPrayer prayer = activePrayers.remove(leader.getName());
        
        if (prayer != null) {
            String msg = plugin.getConfig().getString("messages.congregational-prayer-ended", 
                    "§c%player% left the mosque. The congregational prayer has ended!");
            msg = msg.replace("%player%", leader.getName());
            
            for (UUID participantUUID : prayer.participants) {
                Player participant = Bukkit.getPlayer(participantUUID);
                if (participant != null) {
                    participant.sendMessage(msg);
                }
            }
        }
    }

    @EventHandler
    public void onPrayCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        Player player = event.getPlayer();
        
        // Handle /pray together - start congregational prayer
        if (msg.equalsIgnoreCase("/pray together")) {
            handlePrayTogether(event, player);
        }
        // Handle /pray join <player>
        else if (msg.toLowerCase().startsWith("/pray join ")) {
            handlePrayJoin(event, player, msg);
        }
        // Handle /pray start - complete congregational prayer (leader only)
        else if (msg.equalsIgnoreCase("/pray start")) {
            handlePrayStart(event, player);
        }
        // Handle /pray (without arguments) - available in mosque
        else if (msg.equalsIgnoreCase("/pray")) {
            handlePray(event, player);
        }
    }

    private void handlePrayTogether(PlayerCommandPreprocessEvent event, Player player) {
        MosqueManager.MosqueArea mosque = getPlayerMosque(player);
        if (mosque == null) {
            player.sendMessage(plugin.getConfig().getString("messages.not-in-mosque", "§cYou must be inside a mosque!"));
            return;
        }
        
        String currentPrayer = plugin.getCurrentPrayer(player.getWorld().getTime());
        if (currentPrayer == null) {
            player.sendMessage(plugin.getConfig().getString("messages.pray-no-time", "§cNo active prayer time right now."));
            return;
        }
        
        if (plugin.hasPrayed(player, currentPrayer)) {
            String msg = plugin.getConfig().getString("messages.pray-already-prayed", "§cYou already prayed %prayer% today.");
            player.sendMessage(msg.replace("%prayer%", currentPrayer));
            return;
        }
        
        if (activePrayers.containsKey(player.getName())) {
            player.sendMessage(plugin.getConfig().getString("messages.already-leading-prayer", "§cYou are already leading a congregational prayer!"));
            return;
        }
        
        CongregationalPrayer prayer = new CongregationalPrayer(player.getUniqueId(), mosque.getName(), currentPrayer);
        activePrayers.put(player.getName(), prayer);
        
        String msg = plugin.getConfig().getString("messages.started-congregational-prayer", "§a%player% started a congregational prayer in §e%mosque%");
        msg = msg.replace("%player%", player.getName()).replace("%mosque%", mosque.getName());
        for (Player p : player.getWorld().getNearbyPlayers(player.getLocation(), 64)) {
            p.sendMessage(msg);
        }
        
        event.setCancelled(true);
    }

    private void handlePrayJoin(PlayerCommandPreprocessEvent event, Player player, String msg) {
        String[] parts = msg.split(" ", 3);
        if (parts.length < 3) {
            player.sendMessage("§cUsage: /pray join <player>");
            return;
        }
        
        MosqueManager.MosqueArea mosque = getPlayerMosque(player);
        if (mosque == null) {
            player.sendMessage(plugin.getConfig().getString("messages.not-in-mosque", "§cYou must be inside a mosque!"));
            return;
        }
        
        String targetName = parts[2];
        CongregationalPrayer prayer = activePrayers.get(targetName);
        
        if (prayer == null) {
            player.sendMessage(plugin.getConfig().getString("messages.no-congregational-prayer", "§cThat player is not leading a congregational prayer."));
            return;
        }
        
        if (!prayer.mosqueName.equals(mosque.getName())) {
            player.sendMessage(plugin.getConfig().getString("messages.different-mosque-prayer", "§cThat prayer is in a different mosque."));
            return;
        }
        
        if (plugin.hasPrayed(player, prayer.prayerName)) {
            String msgStr = plugin.getConfig().getString("messages.pray-already-prayed", "§cYou already prayed %prayer% today.");
            player.sendMessage(msgStr.replace("%prayer%", prayer.prayerName));
            return;
        }
        
        prayer.participants.add(player.getUniqueId());
        
        String joinMsg = plugin.getConfig().getString("messages.joined-congregational-prayer", "§aYou joined %player%'s congregational prayer! Waiting for leader to start...");
        player.sendMessage(joinMsg.replace("%player%", targetName));
        
        Player leader = Bukkit.getPlayer(prayer.initiatorUUID);
        if (leader != null) {
            String leaderMsg = plugin.getConfig().getString("messages.player-joined-prayer", "§a%player% joined your congregational prayer! (%count% participants)");
            leader.sendMessage(leaderMsg.replace("%player%", player.getName()).replace("%count%", String.valueOf(prayer.participants.size())));
        }
        
        event.setCancelled(true);
    }

    private void handlePrayStart(PlayerCommandPreprocessEvent event, Player player) {
        CongregationalPrayer prayer = activePrayers.remove(player.getName());
        
        if (prayer == null || !prayer.initiatorUUID.equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfig().getString("messages.not-leading-prayer", "§cYou are not leading a congregational prayer!"));
            if (prayer != null) activePrayers.put(player.getName(), prayer);
            return;
        }
        
        int baseVirtue = plugin.getConfig().getInt("mosque.prayer-virtue", 10);
        int multiplier = prayer.participants.size();
        int totalVirtue = baseVirtue * multiplier;
        
        for (UUID participantUUID : prayer.participants) {
            Player participant = Bukkit.getPlayer(participantUUID);
            if (participant != null) {
                plugin.markPlayerPrayed(participant, prayer.prayerName);
                plugin.changeVirtue(participant, totalVirtue);
                
                String msg = plugin.getConfig().getString("messages.congregational-prayer-complete", "§a%prayer% prayer completed! You earned %virtue% virtue (x%multiplier%).");
                participant.sendMessage(msg.replace("%prayer%", prayer.prayerName).replace("%virtue%", String.valueOf(totalVirtue)).replace("%multiplier%", String.valueOf(multiplier)));
            }
        }
        
        String msg = plugin.getConfig().getString("messages.prayer-started", "§aThe congregational prayer for %prayer% has been completed! All %count% participants earned %virtue% virtue (x%multiplier%).");
        msg = msg.replace("%prayer%", prayer.prayerName).replace("%count%", String.valueOf(multiplier)).replace("%virtue%", String.valueOf(totalVirtue)).replace("%multiplier%", String.valueOf(multiplier));
        
        for (Player p : player.getWorld().getNearbyPlayers(player.getLocation(), 64)) {
            p.sendMessage(msg);
        }
        
        event.setCancelled(true);
    }

    private void handlePray(PlayerCommandPreprocessEvent event, Player player) {
        // Check if standing on prayer mat OR in a mosque
        MosqueManager.MosqueArea mosque = getPlayerMosque(player);
        boolean onPrayerMat = plugin.isStandingOnPrayerMat(player);
        
        if (!onPrayerMat && mosque == null) {
            player.sendMessage("§cYou must stand on a Prayer Mat or be inside a mosque!");
            event.setCancelled(true);
            return;
        }
        
        String currentPrayer = plugin.getCurrentPrayer(player.getWorld().getTime());
        if (currentPrayer == null) {
            player.sendMessage(plugin.getConfig().getString("messages.pray-no-time", "§cNo active prayer time right now."));
            event.setCancelled(true);
            return;
        }
        
        if (plugin.hasPrayed(player, currentPrayer)) {
            String msg = plugin.getConfig().getString("messages.pray-already-prayed", "§cYou already prayed %prayer% today.");
            player.sendMessage(msg.replace("%prayer%", currentPrayer));
            event.setCancelled(true);
            return;
        }
        
        plugin.markPlayerPrayed(player, currentPrayer);
        int virtue = plugin.getConfig().getInt("mosque.prayer-virtue", 10);
        plugin.changeVirtue(player, virtue);
        
        String msg = plugin.getConfig().getString("messages.pray-complete", "§a%prayer% prayer completed! You earned %virtue% virtue.");
        player.sendMessage(msg.replace("%prayer%", currentPrayer).replace("%virtue%", String.valueOf(virtue)));
        event.setCancelled(true);
    }
}