package me.halalcraft;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import me.halalcraft.listener.DailyChallengeListener;
import me.halalcraft.listener.DuaListener;
import me.halalcraft.listener.EnchantmentBookListener;
import me.halalcraft.listener.ImpureEnchantmentPenaltyListener;
import me.halalcraft.listener.IslamicCombatRule;
import me.halalcraft.listener.MosqueListener;
import me.halalcraft.listener.PrayListener;
import me.halalcraft.listener.PrayerWarningListener;
import me.halalcraft.listener.TransactionListener;
import me.halalcraft.listener.UpgradeListener;
import me.halalcraft.listener.VirtueShopListener;
import me.halalcraft.listener.VirtueShopSignListener;
import me.halalcraft.mosque.MosqueManager;

public class HalalCraft extends JavaPlugin implements Listener {

    /* =========================
       NBT KEYS FOR ITEM TAGGING
       ========================= */
    private final NamespacedKey HARAM_KEY = new NamespacedKey(this, "haram_enchantment");
    private final NamespacedKey PURIFIED_KEY = new NamespacedKey(this, "purified_enchantment");
    private final NamespacedKey TABLE_ENCHANTED_KEY = new NamespacedKey(this, "table_enchanted");
    private final NamespacedKey UPGRADE_BOOK_KEY = new NamespacedKey(this, "upgrade_enchantment_book");
    private final NamespacedKey ENCHANT_NAME_KEY = new NamespacedKey(this, "enchant_name");
    private final NamespacedKey ENCHANT_LEVEL_KEY = new NamespacedKey(this, "enchant_level");
    private final NamespacedKey UPGRADE_COST_KEY = new NamespacedKey(this, "upgrade_cost");

    /* =========================
       SCOREBOARD (per-player)
       ========================= */
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, Objective> playerObjectives = new HashMap<>();

    /* =========================
       PRAYER SYSTEM
       ========================= */
    private final LinkedHashMap<String, Integer> prayerTimes = new LinkedHashMap<>();
    private final Map<String, Boolean> announcedToday = new HashMap<>();
    private final Map<String, Boolean> prayerEnded = new HashMap<>();
    private final Map<UUID, Set<String>> prayedToday = new HashMap<>();

    /* =========================
       VIRTUE SYSTEM
       ========================= */
    private final Map<UUID, Integer> virtue = new HashMap<>();

    /* =========================
       DAY SYSTEM
       ========================= */
    private int serverDay = 1;
    private long lastWorldTime = 0;

    /* =========================
       DUA SYSTEM
       ========================= */
    private DuaListener duaListener;

    /* =========================
       MOSQUE SYSTEM
       ========================= */
    private MosqueListener mosqueListener;
    private PrayListener prayListener;

    /* =========================
       COMBAT SYSTEM
       ========================= */
    private IslamicCombatRule combatRule;

    /* =========================
       NEW FEATURES SYSTEM
       ========================= */
    private PrayerWarningListener warningListener;
    private DailyChallengeListener challengeListener;
    private VirtueShopListener shopListener;
    private UpgradeListener upgradeListener;
    private EnchantmentBookListener enchantmentBookListener;
    private ImpureEnchantmentPenaltyListener impureEnchantmentListener;
    private VirtueShopSignListener virtueShopSignListener;
    private FileConfiguration upgradeConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadUpgradeConfig();
        loadVirtueData();
        setupPrayerTimes();
        startPrayerSystem();

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);

        combatRule = new IslamicCombatRule(this);
        Bukkit.getPluginManager().registerEvents(combatRule, this);

        duaListener = new DuaListener(this);
        Bukkit.getPluginManager().registerEvents(duaListener, this);

        mosqueListener = new MosqueListener(this);
        Bukkit.getPluginManager().registerEvents(mosqueListener, this);

        prayListener = new PrayListener(this);
        Bukkit.getPluginManager().registerEvents(prayListener, this);

        // New features
        warningListener = new PrayerWarningListener(this);
        Bukkit.getPluginManager().registerEvents(warningListener, this);

        challengeListener = new DailyChallengeListener(this);
        Bukkit.getPluginManager().registerEvents(challengeListener, this);

        shopListener = new VirtueShopListener(this);
        Bukkit.getPluginManager().registerEvents(shopListener, this);

        upgradeListener = new UpgradeListener(this);
        Bukkit.getPluginManager().registerEvents(upgradeListener, this);

        enchantmentBookListener = new EnchantmentBookListener(this);
        Bukkit.getPluginManager().registerEvents(enchantmentBookListener, this);

        impureEnchantmentListener = new ImpureEnchantmentPenaltyListener(this);
        Bukkit.getPluginManager().registerEvents(impureEnchantmentListener, this);

        virtueShopSignListener = new VirtueShopSignListener(this);
        Bukkit.getPluginManager().registerEvents(virtueShopSignListener, this);

        // Register transaction listener for offline player shop purchases
        TransactionListener transactionListener = new TransactionListener(this);
        Bukkit.getPluginManager().registerEvents(transactionListener, this);

        Bukkit.getLogger().info("§a[HalalCraft] Plugin enabled with Warnings, Challenges, Shop, Upgrade System, Enchantment System, Custom Shop Signs, and Offline Transactions!");
    }

    @Override
    public void onDisable() {
        saveVirtueData();
        playerScoreboards.clear();
        playerObjectives.clear();
    }

    /* =====================================================
     * PRAYER SYSTEM
     * ===================================================== */
    private void setupPrayerTimes() {
        prayerTimes.put("Subh", 0);
        prayerTimes.put("Dzuhr", 6000);
        prayerTimes.put("Asr", 9000);
        prayerTimes.put("Maghrib", 12000);
        prayerTimes.put("Isya", 14000);

        for (String p : prayerTimes.keySet()) {
            announcedToday.put(p, false);
            prayerEnded.put(p, false);
        }
    }

    /* =====================================================
     * SCOREBOARD (per-player)
     * ===================================================== */
    private void setupScoreboardForPlayer(Player p) {
        UUID uuid = p.getUniqueId();
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard sb = manager.getNewScoreboard();
        Objective obj = sb.registerNewObjective("halalcraft", "dummy", "§a🕌 GNC Halal Craft");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        playerScoreboards.put(uuid, sb);
        playerObjectives.put(uuid, obj);
        p.setScoreboard(sb);
    }

    public void updateScoreboard(Player p, PrayerInfo closestPrayer) {
        UUID uuid = p.getUniqueId();
        Scoreboard sb = playerScoreboards.get(uuid);
        Objective obj = playerObjectives.get(uuid);

        // Create if doesn't exist
        if (sb == null || obj == null) {
            setupScoreboardForPlayer(p);
            sb = playerScoreboards.get(uuid);
            obj = playerObjectives.get(uuid);
        }

        // Remove all entries from objective
        for (String entry : sb.getEntries()) {
            sb.resetScores(entry);
        }
        
        String currentPrayer = getCurrentPrayer(p.getWorld().getTime());
        boolean prayed = currentPrayer != null && hasPrayed(p, currentPrayer);
        String status = currentPrayer == null ? "§7No Active Prayer" : (prayed ? "§aAlready Prayed" : "§cNot Prayed");

        // Player info
        obj.getScore("§fName: §a" + p.getName()).setScore(10);
        obj.getScore("§fVirtue: §b" + virtue.getOrDefault(uuid, 0)).setScore(9);
        obj.getScore("§fDay: §e" + serverDay + " (" + getDayName(serverDay) + ")").setScore(8);

        // Prayer info
        obj.getScore(" ").setScore(7);
        obj.getScore("§eClosest Prayer").setScore(6);
        obj.getScore("§f" + closestPrayer.name + " : §a" + formatTime(closestPrayer.timeLeft)).setScore(5);
        obj.getScore("§fStatus: " + status).setScore(4);

        // Combat info
        if (combatRule.isFreeKillActive(p)) {
            long msLeft = combatRule.getFreeKillTimeLeft(p);
            String timeLeft = formatTime(msLeft / 50); // ms -> ticks
            obj.getScore(" ").setScore(3);
            obj.getScore("§eFree Kill Active").setScore(2);
            obj.getScore("§fCombat: §a" + timeLeft).setScore(1);
        }

        // Dua info
        Map<String, Long> activeDua = duaListener.getActiveDua(p);
        if (!activeDua.isEmpty()) {
            obj.getScore(" ").setScore(0);
            obj.getScore("§eDua Active").setScore(-1);
            int score = -2;
            for (var entry : activeDua.entrySet()) {
                long msLeft = entry.getValue() - System.currentTimeMillis();
                if (msLeft <= 0) continue;
                String timeLeft = formatTime(msLeft / 50);
                obj.getScore("§f" + entry.getKey() + " : §a" + timeLeft).setScore(score--);
            }
        }

        // Challenge info
        if (challengeListener != null) {
            obj.getScore("  ").setScore(-10);
            obj.getScore("§eDaily Challenges").setScore(-11);
            obj.getScore("§fUse: /challenge").setScore(-12);
        }
    }

    /* =====================================================
     * MAIN LOOP
     * ===================================================== */
    private void startPrayerSystem() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            World world = Bukkit.getWorlds().get(0);
            long time = world.getTime();

            // New day detection
            if (time < lastWorldTime) {
                serverDay++;
                prayedToday.clear();
                prayerEnded.replaceAll((k, v) -> false);
                announcedToday.replaceAll((k, v) -> false);

                Bukkit.broadcastMessage("§e☀ A new day has begun. Day " + serverDay + " (" + getDayName(serverDay) + ")");
            }
            lastWorldTime = time;

            PrayerInfo closest = getClosestPrayer(time);

            for (Player p : Bukkit.getOnlinePlayers()) {
                updateScoreboard(p, closest);
            }

            checkPrayerAnnouncement(time);
            checkMissedPrayers(time);
        }, 0L, 20L);
    }

    /* =====================================================
     * PRAYER LOGIC
     * ===================================================== */
    public PrayerInfo getClosestPrayer(long time) {
        String name = null;
        long diffMin = Long.MAX_VALUE;

        for (var e : prayerTimes.entrySet()) {
            long d = e.getValue() - time;
            if (d < 0) d += 24000;
            if (d < diffMin) {
                diffMin = d;
                name = e.getKey();
            }
        }
        return new PrayerInfo(name, diffMin);
    }

    public String getCurrentPrayer(long time) {
        String current = null;
        for (var e : prayerTimes.entrySet()) {
            if (time >= e.getValue()) current = e.getKey();
        }
        
        // Check if today is Friday and current prayer is Dzuhr
        if (current != null && current.equals("Dzuhr") && isFriday()) {
            return "Jumah";
        }
        
        return current;
    }

    private boolean isFriday() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
    }

    public boolean hasPrayed(Player p, String prayer) {
        return prayedToday.getOrDefault(p.getUniqueId(), Collections.emptySet()).contains(prayer);
    }

    public void markPlayerPrayed(Player p, String prayer) {
        prayedToday.putIfAbsent(p.getUniqueId(), new HashSet<>());
        prayedToday.get(p.getUniqueId()).add(prayer);
    }

    private void checkPrayerAnnouncement(long time) {
        for (var e : prayerTimes.entrySet()) {
            if (Math.abs(time - e.getValue()) < 20 && !announcedToday.get(e.getKey())) {
                String prayerName = e.getKey();
                
                // Check if this is Dzuhr on Friday (becomes Jumah)
                if (prayerName.equals("Dzuhr") && isFriday()) {
                    Bukkit.broadcastMessage(getConfig().getString("friday-prayer.announcement", "§a🕌 It is time for the Friday congregational prayer (Jumah)!"));
                    
                    // Send title popup to all players
                    String title = getConfig().getString("friday-prayer.title", "§a§lJumah Prayer Time");
                    String subtitle = getConfig().getString("friday-prayer.subtitle", "§fGather at the mosque for the Friday congregational prayer");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(title, subtitle, 10, 70, 20);
                    }
                } else {
                    Bukkit.broadcastMessage("§a🕌 It is time for §e" + prayerName + " §aprayer.");
                }
                
                announcedToday.put(e.getKey(), true);
            }
        }
    }

    private void checkMissedPrayers(long time) {
        for (var e : prayerTimes.entrySet()) {
            String prayer = e.getKey();
            int t = e.getValue();

            if (time > t + 600 && !prayerEnded.get(prayer)) {
                int penalty;
                String prayerDisplay = prayer;
                
                // Check if this is Dzuhr on Friday (becomes Jumah)
                if (prayer.equals("Dzuhr") && isFriday()) {
                    penalty = getConfig().getInt("friday-prayer.virtue-penalty-miss", -50);
                    prayerDisplay = "Jumah";
                } else {
                    penalty = getConfig().getInt("economy.virtue.miss-prayer", -5);
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!hasPrayed(p, prayerDisplay)) {
                        changeVirtue(p, penalty);
                        p.sendMessage("§c⚠ You missed the " + prayerDisplay + " prayer. Virtue " + penalty);
                    }
                }
                prayerEnded.put(prayer, true);
                saveVirtueData();
            }
        }
    }



    /* =====================================================
     * PLAYER JOIN
     * ===================================================== */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        virtue.putIfAbsent(uuid, 0);

        // Create scoreboard for this player
        setupScoreboardForPlayer(p);
        
        // Update scoreboard immediately
        PrayerInfo closest = getClosestPrayer(Bukkit.getWorlds().get(0).getTime());
        updateScoreboard(p, closest);

        if (!p.hasPlayedBefore()) {
            p.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
            p.getInventory().addItem(createPrayerMat(), createPrayerMat());
        }
    }

    private ItemStack createPrayerMat() {
        ItemStack i = new ItemStack(Material.WHITE_CARPET);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§fPrayer Mat");
        i.setItemMeta(m);
        return i;
    }

    public boolean isStandingOnPrayerMat(Player p) {
        Block feet = p.getLocation().getBlock();
        Block below = feet.getRelative(0, -1, 0);
        return feet.getType() == Material.WHITE_CARPET || below.getType() == Material.WHITE_CARPET;
    }

    private boolean isInMosque(Player p) {
        for (String mosqueName : MosqueManager.getMosqueNames()) {
            MosqueManager.MosqueArea mosque = MosqueManager.getMosque(mosqueName);
            if (mosque.isInside(p.getLocation())) {
                return true;
            }
        }
        return false;
    }

    /* =====================================================
     * COMMANDS
     * ===================================================== */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return false;

        // DUAs
        if (cmd.getName().equalsIgnoreCase("dua")) {
            if (args.length == 0) {
                p.sendMessage("§eUsage: /dua <list|type>");
                return true;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("list")) {
                if (!getConfig().contains("dua")) {
                    p.sendMessage("§cNo duas configured.");
                    return true;
                }
                p.sendMessage("§aAvailable duas:");
                for (String k : getConfig().getConfigurationSection("dua").getKeys(false)) {
                    p.sendMessage(" - §f" + k);
                }
                return true;
            }

            String type = sub;
            boolean ok = duaListener.registerDua(p, type);
            if (ok) {
                int dur = getConfig().getInt("dua." + type + ".duration-seconds", getConfig().getInt("dua.default-duration-seconds", 300));
                p.sendMessage("§aYou have said the dua: " + type + " for " + dur + " seconds.");
            }
            return true;
        }

        // PRAYER
        if (cmd.getName().equalsIgnoreCase("pray")) {
            String prayer = getCurrentPrayer(p.getWorld().getTime());
            if (prayer == null) {
                p.sendMessage("§cIt is not prayer time.");
                return true;
            }

            // Jumah (Friday) prayers must be in a mosque
            boolean isJumah = prayer.equals("Jumah");
            if (isJumah) {
                if (!isInMosque(p)) {
                    p.sendMessage(getConfig().getString("messages.jumah-mosque-only", "§cJumah prayer can only be performed in a mosque!"));
                    return true;
                }
            } else {
                // Regular prayers: check if standing on prayer mat OR in a mosque
                if (!isStandingOnPrayerMat(p) && !isInMosque(p)) {
                    p.sendMessage("§cYou must stand on a Prayer Mat or be inside a mosque.");
                    return true;
                }
            }

            prayedToday.putIfAbsent(p.getUniqueId(), new HashSet<>());
            if (!prayedToday.get(p.getUniqueId()).add(prayer)) {
                p.sendMessage("§eYou already prayed " + prayer);
                return true;
            }

            // Determine virtue gain (Jumah gives 150, regular prayers give 5)
            int gain;
            if (isJumah) {
                gain = getConfig().getInt("friday-prayer.virtue-gain", 150);
            } else {
                gain = getConfig().getInt("economy.virtue.pray", 5);
            }
            
            changeVirtue(p, gain);
            
            // Track for daily challenges
            if (challengeListener != null) {
                challengeListener.incrementPrayCount(p);
                challengeListener.trackVirtueGain(p, gain);
            }
            
            saveVirtueData();

            p.sendMessage("§a🕌 Prayer " + prayer + " Done, Masyallah");

            updateScoreboard(p, getClosestPrayer(p.getWorld().getTime()));
            return true;
        }

        // VIRTUE
        if (cmd.getName().equalsIgnoreCase("virtue")) {
            if (args.length == 0) {
                handleVirtueInfo(p);
                return true;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("list")) {
                handleVirtueList(p);
                return true;
            }

            if (sub.equals("add")) {
                handleVirtueAdd(p, args);
                return true;
            }

            if (sub.equals("give")) {
                handleVirtueGivePlayer(p, args);
                return true;
            }

            // /virtue <player>
            String playerName = args[0];
            handleVirtuePlayer(p, playerName);
            return true;
        }

        // CHALLENGES
        if (cmd.getName().equalsIgnoreCase("challenge")) {
            if (args.length == 0) {
                challengeListener.showChallenges(p);
                return true;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("claim")) {
                if (args.length < 2) {
                    p.sendMessage("§eUsage: /challenge claim <name>");
                    return true;
                }
                String challengeName = args[1].toLowerCase();
                challengeListener.claimChallengeReward(p, challengeName);
                return true;
            }
            return true;
        }

        // UPGRADE
        if (cmd.getName().equalsIgnoreCase("upgrade")) {
            upgradeListener.showUpgradeGUI(p);
            return true;
        }

        return false;
    }

    /* =====================================================
     * VIRTUE SYSTEM
     * ===================================================== */
    public void changeVirtue(Player p, int amount) {
        virtue.merge(p.getUniqueId(), amount, Integer::sum);
    }

    public void setVirtue(Player p, int amount) {
        virtue.put(p.getUniqueId(), amount);
    }

    private void loadVirtueData() {
        if (!getConfig().contains("players")) return;
        for (String k : getConfig().getConfigurationSection("players").getKeys(false)) {
            virtue.put(UUID.fromString(k), getConfig().getInt("players." + k + ".virtue", 0));
        }
    }

    public void saveVirtueData() {
        for (var e : virtue.entrySet()) {
            getConfig().set("players." + e.getKey() + ".virtue", e.getValue());
        }
        saveConfig();
    }

    /* =====================================================
     * HELPERS
     * ===================================================== */
    private String formatTime(long ticks) {
        long s = ticks / 20;
        return (s / 60) + "m " + (s % 60) + "s";
    }

    private String getDayName(int d) {
        return switch ((d - 1) % 7) {
            case 0 -> "Monday";
            case 1 -> "Tuesday";
            case 2 -> "Wednesday";
            case 3 -> "Thursday";
            case 4 -> "Friday";
            case 5 -> "Saturday";
            default -> "Sunday";
        };
    }

    public IslamicCombatRule getCombatRule() {
        return combatRule;
    }

    public DuaListener getDuaListener() {
        return duaListener;
    }

    public int getServerDay() {
        return serverDay;
    }

    /* =====================================================
     * VIRTUE COMMANDS
     * ===================================================== */
    private void handleVirtueInfo(Player p) {
        p.sendMessage("§a§l=== Virtue Guide ===");
        if (getConfig().contains("virtue.tutorials")) {
            for (String tutorial : getConfig().getStringList("virtue.tutorials")) {
                p.sendMessage(tutorial);
            }
        }
    }

    private void handleVirtueList(Player p) {
        // Get top 5 virtue players (including offline)
        List<Map.Entry<UUID, Integer>> topPlayers = virtue.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        if (topPlayers.isEmpty()) {
            p.sendMessage("§cNo virtue data available yet.");
            return;
        }

        // Create inventory with player heads
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 9, "§a§lTop Virtue Players");

        int slot = 0;
        for (Map.Entry<UUID, Integer> entry : topPlayers) {
            if (slot >= 5) break;

            UUID uuid = entry.getKey();
            int virtueValue = entry.getValue();

            // Try to get player name (online or from config)
            String playerName = null;
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() != null) {
                playerName = offlinePlayer.getName();
            }

            if (playerName == null) {
                playerName = "Unknown";
            }

            // Create player head item
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(offlinePlayer);
                skullMeta.setDisplayName("§e" + playerName);
                skullMeta.setLore(java.util.List.of("§fVirtue: §b" + virtueValue));
                head.setItemMeta(skullMeta);
            }

            inv.setItem(slot, head);
            slot++;
        }

        p.openInventory(inv);
    }

    private void handleVirtueAdd(Player p, String[] args) {
        if (!p.isOp()) {
            p.sendMessage(getConfig().getString("messages.permission-denied", "§cYou don't have permission"));
            return;
        }

        if (args.length < 3) {
            p.sendMessage("§cUsage: /virtue add <player> <amount>");
            return;
        }

        String targetName = args[1];
        String amountStr = args[2];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            p.sendMessage("§cPlayer not found: " + targetName);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            p.sendMessage("§cInvalid amount: " + amountStr);
            return;
        }

        changeVirtue(target, amount);
        p.sendMessage("§a✓ Added " + amount + " virtue to " + target.getName() + " (Total: " + getVirtue(target) + ")");
        target.sendMessage(String.format(getConfig().getString("messages.virtue-admin-given", "§aOP gave you %d virtue!"), amount));
    }

    private void handleVirtueGivePlayer(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage("§cUsage: /virtue give <player> <amount>");
            return;
        }

        String targetName = args[1];
        String amountStr = args[2];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            p.sendMessage("§cPlayer not found: " + targetName);
            return;
        }

        if (target.equals(p)) {
            p.sendMessage("§cYou cannot give virtue to yourself!");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                p.sendMessage("§cAmount must be positive!");
                return;
            }
        } catch (NumberFormatException e) {
            p.sendMessage("§cInvalid amount: " + amountStr);
            return;
        }

        int senderVirtue = getVirtue(p);
        if (senderVirtue < amount) {
            p.sendMessage("§cYou don't have enough virtue! (Have: " + senderVirtue + ", Need: " + amount + ")");
            return;
        }

        // Transfer virtue from sender to target
        changeVirtue(p, -amount);
        changeVirtue(target, amount);
        saveVirtueData();

        p.sendMessage("§a✓ Gave " + amount + " virtue to " + target.getName() + " (Remaining: " + getVirtue(p) + ")");
        target.sendMessage("§a✓ Received " + amount + " virtue from " + p.getName() + " (Total: " + getVirtue(target) + ")");
    }

    private void handleVirtueGive(Player p, String[] args) {
        if (!p.isOp()) {
            p.sendMessage(getConfig().getString("messages.permission-denied", "§cYou don't have permission"));
            return;
        }

        if (args.length < 3) {
            p.sendMessage("§cUsage: /virtue give <player> <amount>");
            return;
        }

        String targetName = args[1];
        String amountStr = args[2];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            p.sendMessage("§cPlayer not found: " + targetName);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            p.sendMessage("§cInvalid amount: " + amountStr);
            return;
        }

        changeVirtue(target, amount);
        p.sendMessage("§a✓ Given " + amount + " virtue to " + target.getName() + " (Total: " + getVirtue(target) + ")");
        target.sendMessage(String.format(getConfig().getString("messages.virtue-admin-given", "§aOP gave you %d virtue!"), amount));
    }

    private void handleVirtuePlayer(Player p, String playerName) {
        // Search for player by name (online first, then offline)
        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID = null;

        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            // Search offline players
            for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null && op.getName().equalsIgnoreCase(playerName)) {
                    targetUUID = op.getUniqueId();
                    break;
                }
            }
        }

        if (targetUUID == null) {
            p.sendMessage("§cPlayer not found: " + playerName);
            return;
        }

        int virtueValue = virtue.getOrDefault(targetUUID, 0);
        String displayName = Bukkit.getOfflinePlayer(targetUUID).getName();
        if (displayName == null) displayName = "Unknown";

        p.sendMessage("§a=== " + displayName + " Virtue Info ===");
        p.sendMessage("§fVirtue: §b" + virtueValue);
        p.sendMessage("§fStatus: " + (Bukkit.getPlayer(targetUUID) != null ? "§aOnline" : "§7Offline"));
    }

    /* =====================================================
     * PRAYER INFO CLASS
     * ===================================================== */
    public static class PrayerInfo {
        public String name;
        public long timeLeft;
        public long ticksUntil;
        
        public PrayerInfo(String n, long t) {
            name = n;
            timeLeft = t;
            ticksUntil = t;
        }
    }

    /* =====================================================
     * PUBLIC ACCESSORS FOR LISTENERS
     * ===================================================== */
    public int getVirtue(Player p) {
        return virtue.getOrDefault(p.getUniqueId(), 0);
    }

    public DailyChallengeListener getChallengeListener() {
        return challengeListener;
    }

    public NamespacedKey getHaramKey() {
        return HARAM_KEY;
    }

    public NamespacedKey getPurifiedKey() {
        return PURIFIED_KEY;
    }

    public NamespacedKey getUpgradeBookKey() {
        return UPGRADE_BOOK_KEY;
    }

    public NamespacedKey getUpgradeEnchantNameKey() {
        return ENCHANT_NAME_KEY;
    }

    public NamespacedKey getUpgradeEnchantLevelKey() {
        return ENCHANT_LEVEL_KEY;
    }

    public NamespacedKey getUpgradeCostKey() {
        return UPGRADE_COST_KEY;
    }

    public NamespacedKey getPurifiedEnchantmentKey() {
        return PURIFIED_KEY;
    }

    /**
     * Load upgrade.yml configuration
     */
    private void loadUpgradeConfig() {
        java.io.File upgradeFile = new java.io.File(getDataFolder(), "upgrade.yml");
        if (!upgradeFile.exists()) {
            saveResource("upgrade.yml", false);
        }
        upgradeConfig = YamlConfiguration.loadConfiguration(upgradeFile);
    }

    /**
     * Get the upgrade configuration
     */
    public FileConfiguration getUpgradeConfig() {
        return upgradeConfig;
    }

    /**
     * Record a pending transaction for an offline player
     */
    public void recordPendingTransaction(String buyerName, String sellerName, int virtue, String itemName, int quantity) {
        FileConfiguration config = getConfig();
        
        // Get or create pending transactions list
        java.util.List<java.util.Map<String, Object>> transactions = (java.util.List) config.getList("pending-transactions", new java.util.ArrayList<>());
        
        // Create transaction record
        java.util.Map<String, Object> transaction = new java.util.HashMap<>();
        transaction.put("buyer", buyerName);
        transaction.put("seller", sellerName);
        transaction.put("virtue", virtue);
        transaction.put("item", itemName);
        transaction.put("quantity", quantity);
        transaction.put("timestamp", System.currentTimeMillis());
        
        transactions.add(transaction);
        config.set("pending-transactions", transactions);
        saveConfig();
    }

    /**
     * Process pending transactions for a player when they join
     */
    public void processPendingTransactions(Player player) {
        FileConfiguration config = getConfig();
        java.util.List<java.util.Map<String, Object>> transactions = (java.util.List) config.getList("pending-transactions", new java.util.ArrayList<>());
        
        if (transactions.isEmpty()) {
            return;
        }

        java.util.List<java.util.Map<String, Object>> completedTransactions = new java.util.ArrayList<>();
        int totalVirtue = 0;

        for (java.util.Map<String, Object> trans : transactions) {
            String buyerName = (String) trans.get("buyer");
            
            // Only process transactions for this player
            if (!buyerName.equalsIgnoreCase(player.getName())) {
                continue;
            }

            String sellerName = (String) trans.get("seller");
            int virtue = (Integer) trans.get("virtue");
            String itemName = (String) trans.get("item");
            int quantity = (Integer) trans.get("quantity");
            long timestamp = (Long) trans.get("timestamp");

            // Security: Check if transaction is not too old (older than 30 days)
            long ageInDays = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24);
            if (ageInDays > 30) {
                getLogger().warning("Pending transaction for " + buyerName + " is too old (" + ageInDays + " days), skipping: " + trans);
                completedTransactions.add(trans);
                continue;
            }

            // Security: Verify seller still exists (basic check)
            OfflinePlayer offlineSeller = Bukkit.getOfflinePlayer(sellerName);
            if (!offlineSeller.hasPlayedBefore()) {
                getLogger().warning("Seller " + sellerName + " does not exist for transaction: " + trans);
                completedTransactions.add(trans);
                continue;
            }

            // Process the transaction
            changeVirtue(player, -virtue);
            
            // Give virtue to seller (whether online or not)
            OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerName);
            int sellerCurrentVirtue = getVirtue(seller.getUniqueId());
            setVirtueByUUID(seller.getUniqueId(), sellerCurrentVirtue + virtue);

            // Notify player
            player.sendMessage("§a✓ Pending transaction processed:");
            player.sendMessage("§7Purchased " + quantity + " " + itemName + " from §e" + sellerName + "§7 for §b" + virtue + "§7 virtue");

            totalVirtue += virtue;
            completedTransactions.add(trans);
        }

        // Remove completed transactions
        for (java.util.Map<String, Object> completed : completedTransactions) {
            transactions.remove(completed);
        }

        config.set("pending-transactions", transactions);
        saveConfig();
        saveVirtueData();

        if (totalVirtue > 0) {
            player.sendMessage("§aTotal pending virtue processed: §b" + totalVirtue);
        }
    }

    /**
     * Set virtue for a player by UUID
     */
    public void setVirtueByUUID(java.util.UUID uuid, int amount) {
        getConfig().set("virtue." + uuid.toString(), amount);
        saveConfig();
    }

    /**
     * Get virtue for a player by UUID
     */
    public int getVirtue(java.util.UUID uuid) {
        return getConfig().getInt("virtue." + uuid.toString(), 0);
    }
}
