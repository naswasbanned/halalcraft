package me.halalcraft;

import java.util.AbstractMap;
import java.util.ArrayList;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
import me.halalcraft.listener.OpenShopListener;
import me.halalcraft.listener.PrayListener;
import me.halalcraft.listener.PrayerWarningListener;
import me.halalcraft.listener.TransactionListener;
import me.halalcraft.listener.UpgradeListener;
import me.halalcraft.listener.VirtueShopListener;
import me.halalcraft.listener.VirtueShopSignListener;
import me.halalcraft.mosque.MosqueManager;

public class HalalCraft extends JavaPlugin implements Listener, TabCompleter {

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
       BOSSBAR SYSTEM
       ========================= */
    private BossBar prayerBossBar;

    /* =========================
       AFK SYSTEM
       ========================= */
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final Map<UUID, Location> lastPosition = new HashMap<>();
    private final Set<UUID> afkPlayers = new HashSet<>();
    private static final long AFK_TIMEOUT_MS = 3 * 60 * 1000; // 3 minutes
    private static final long AFK_MIN_MOVE_DISTANCE_SQ = 4; // Must move at least 2 blocks

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
    private OpenShopListener openShopListener;
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
        setupPrayerBossBar();
        startPrayerSystem();
        startAfkDetection();

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

        openShopListener = new OpenShopListener(this);
        Bukkit.getPluginManager().registerEvents(openShopListener, this);

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

        // Register tab-completion for all main commands
        if (getCommand("dua") != null) getCommand("dua").setTabCompleter(this);
        if (getCommand("pray") != null) getCommand("pray").setTabCompleter(this);
        if (getCommand("mosque") != null) getCommand("mosque").setTabCompleter(this);
        if (getCommand("virtue") != null) getCommand("virtue").setTabCompleter(this);
        if (getCommand("challenge") != null) getCommand("challenge").setTabCompleter(this);
        if (getCommand("shop") != null) getCommand("shop").setTabCompleter(this);
        if (getCommand("openshop") != null) getCommand("openshop").setTabCompleter(this);
        if (getCommand("chestshop") != null) getCommand("chestshop").setTabCompleter(this);
        if (getCommand("upgrade") != null) getCommand("upgrade").setTabCompleter(this);
        if (getCommand("halalcraft") != null) getCommand("halalcraft").setTabCompleter(this);

        Bukkit.getLogger().info("§a[HalalCraft] Plugin enabled with Warnings, Challenges, Shop, Upgrade System, Enchantment System, Custom Shop Signs, and Offline Transactions!");
    }

    @Override
    public void onDisable() {
        saveVirtueData();
        if (prayerBossBar != null) {
            prayerBossBar.removeAll();
        }
        playerScoreboards.clear();
        playerObjectives.clear();
    }

    /* =====================================================
     * PRAYER BOSSBAR SYSTEM
     * ===================================================== */
    private void setupPrayerBossBar() {
        prayerBossBar = Bukkit.createBossBar(
            "§e🕌 Loading prayer info...",
            BarColor.GREEN,
            BarStyle.SEGMENTED_6
        );
        prayerBossBar.setVisible(true);
    }

    private void updatePrayerBossBar(long time) {
        if (prayerBossBar == null) return;

        PrayerInfo closest = getClosestPrayer(time);
        String currentPrayer = getCurrentPrayer(time);

        // Determine which prayer window we are in vs next prayer
        String displayName = closest.name;
        long ticksLeft = closest.timeLeft;
        String timeStr = formatTime(ticksLeft);

        // Calculate progress (how far through the wait for next prayer)
        // Each prayer window is ~2000-6000 ticks. We use a max of 6000 for the bar.
        String previousPrayer = getPreviousPrayer(closest.name);
        long windowSize = getWindowSize(previousPrayer, closest.name);
        double progress = 1.0 - ((double) ticksLeft / (double) windowSize);
        progress = Math.max(0.0, Math.min(1.0, progress));

        // Determine bar color based on urgency
        BarColor color;
        if (ticksLeft < 600) { // Less than 30 seconds
            color = BarColor.RED;
        } else if (ticksLeft < 2000) { // Less than ~1.5 min
            color = BarColor.YELLOW;
        } else {
            color = BarColor.GREEN;
        }

        String statusText;
        if (currentPrayer != null) {
            statusText = "§a🕌 Current: §f" + currentPrayer + " §7| §eNext: §f" + displayName + " §7in §b" + timeStr;
        } else {
            statusText = "§e🕌 Next Prayer: §f" + displayName + " §7in §b" + timeStr;
        }

        prayerBossBar.setTitle(statusText);
        prayerBossBar.setColor(color);
        prayerBossBar.setProgress(progress);
    }

    private String getPreviousPrayer(String prayerName) {
        String[] order = {"Subh", "Dzuhr", "Asr", "Maghrib", "Isya"};
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(prayerName)) {
                return order[(i - 1 + order.length) % order.length];
            }
        }
        return "Isya";
    }

    private long getWindowSize(String fromPrayer, String toPrayer) {
        int fromTime = prayerTimes.getOrDefault(fromPrayer, 0);
        int toTime = prayerTimes.getOrDefault(toPrayer, 0);
        long window = toTime - fromTime;
        if (window <= 0) window += 24000;
        return window;
    }

    /* =====================================================
     * AFK DETECTION SYSTEM
     * ===================================================== */
    private void startAfkDetection() {
        // Check every 5 seconds for AFK players
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID uuid = p.getUniqueId();

                // Initialize if new
                lastActivity.putIfAbsent(uuid, now);
                lastPosition.putIfAbsent(uuid, p.getLocation().clone());

                long lastActive = lastActivity.getOrDefault(uuid, now);
                boolean wasAfk = afkPlayers.contains(uuid);

                if ((now - lastActive) >= AFK_TIMEOUT_MS && !wasAfk) {
                    // Player just went AFK
                    afkPlayers.add(uuid);
                    Bukkit.broadcastMessage("§7[AFK] §f" + p.getName() + " §7is now AFK.");
                    p.sendMessage("§7You are now marked as AFK. You are immune to missed prayer penalties.");
                }
            }
        }, 100L, 100L); // Every 5 seconds
    }

    /**
     * Record player activity to reset AFK timer.
     * Only counts real position changes (not just looking around).
     */
    private void recordActivity(Player p) {
        UUID uuid = p.getUniqueId();
        boolean wasAfk = afkPlayers.remove(uuid);
        lastActivity.put(uuid, System.currentTimeMillis());

        if (wasAfk) {
            Bukkit.broadcastMessage("§7[AFK] §f" + p.getName() + " §7is no longer AFK.");
            p.sendMessage("§aYou are no longer AFK.");
        }
    }

    public boolean isAfk(Player p) {
        return afkPlayers.contains(p.getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;

        // Only count REAL movement (not just head rotation)
        if (from.getBlockX() != to.getBlockX() ||
            from.getBlockY() != to.getBlockY() ||
            from.getBlockZ() != to.getBlockZ()) {

            // Anti-exploit: must move at least 2 blocks from last recorded position
            Location lastPos = lastPosition.get(uuid);
            if (lastPos != null && lastPos.getWorld() == to.getWorld()) {
                double distSq = lastPos.distanceSquared(to);
                if (distSq < AFK_MIN_MOVE_DISTANCE_SQ) {
                    return; // Too small a movement (anti-macro)
                }
            }

            lastPosition.put(uuid, to.clone());
            recordActivity(p);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        // Chat counts as activity, but run on main thread
        Bukkit.getScheduler().runTask(this, () -> recordActivity(e.getPlayer()));
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        recordActivity(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            recordActivity(p);
        }
    }

    @EventHandler
    public void onPlayerQuitAfk(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        afkPlayers.remove(uuid);
        lastActivity.remove(uuid);
        lastPosition.remove(uuid);
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

                // Reset all duas on Subh (new day)
                if (duaListener != null) {
                    duaListener.resetAllDuas();
                    duaListener.resetDuaDayTracking();
                    Bukkit.broadcastMessage("§e🕌 Subh has arrived. All duas have been reset.");
                }
            }
            lastWorldTime = time;

            PrayerInfo closest = getClosestPrayer(time);

            // Update BossBar
            updatePrayerBossBar(time);

            for (Player p : Bukkit.getOnlinePlayers()) {
                // Ensure player is added to BossBar
                if (prayerBossBar != null && !prayerBossBar.getPlayers().contains(p)) {
                    prayerBossBar.addPlayer(p);
                }
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
                        // AFK players are immune to missed prayer penalties
                        if (isAfk(p)) {
                            p.sendMessage("§7[AFK] You missed the " + prayerDisplay + " prayer, but no penalty (AFK).");
                            continue;
                        }
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
        
        // Add player to prayer BossBar
        if (prayerBossBar != null) {
            prayerBossBar.addPlayer(p);
        }

        // Initialize AFK tracking
        lastActivity.put(uuid, System.currentTimeMillis());
        lastPosition.put(uuid, p.getLocation().clone());
        
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
        // SAVEVIRTUE (can be used from console too)
        if (cmd.getName().equalsIgnoreCase("savevirtue")) {
            if (!sender.isOp()) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            saveVirtueData();
            sender.sendMessage("§a✓ All virtue data has been saved to disk.");
            Bukkit.getLogger().info("[HalalCraft] Virtue data manually saved by " + sender.getName());
            return true;
        }

        if (!(sender instanceof Player p)) return false;

        // HALALCRAFT INFO
        if (cmd.getName().equalsIgnoreCase("halalcraft")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
                sendPluginInfo(p);
            } else {
                p.sendMessage("§eUsage: /halalcraft info");
            }
            return true;
        }

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

        // OPENSHOP - browse or sell single-item listings
        if (cmd.getName().equalsIgnoreCase("openshop")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("sell")) {
                if (args.length < 2) {
                    p.sendMessage("§eUsage: /openshop sell <price>");
                    return true;
                }

                String priceStr = args[1];
                int price;
                try {
                    price = Integer.parseInt(priceStr);
                    if (price <= 0) {
                        p.sendMessage("§cPrice must be positive.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    p.sendMessage("§cInvalid price: " + priceStr);
                    return true;
                }

                ItemStack hand = p.getInventory().getItemInMainHand();
                openShopListener.createListing(p, hand, price);
                return true;
            }

            // No subcommand: open browser GUI
            openShopListener.openListings(p, 0);
            return true;
        }

        // CHESTSHOP alias - kept for backward compatibility
        if (cmd.getName().equalsIgnoreCase("chestshop")) {
            if (args.length < 2 || !args[0].equalsIgnoreCase("sell")) {
                p.sendMessage("§eUsage: /chestshop sell <price>");
                return true;
            }

            String priceStr = args[1];
            int price;
            try {
                price = Integer.parseInt(priceStr);
                if (price <= 0) {
                    p.sendMessage("§cPrice must be positive.");
                    return true;
                }
            } catch (NumberFormatException e) {
                p.sendMessage("§cInvalid price: " + priceStr);
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            openShopListener.createListing(p, hand, price);
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
     * TAB COMPLETION
     * ===================================================== */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) {
            return Collections.emptyList();
        }

        String name = cmd.getName().toLowerCase();

        // /dua <type|list>
        if (name.equals("dua")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                completions.add("list");
                if (getConfig().contains("dua")) {
                    completions.addAll(getConfig().getConfigurationSection("dua").getKeys(false));
                }
                return partial(completions, args[0]);
            }
            return Collections.emptyList();
        }

        // /pray together|join <player>
        if (name.equals("pray")) {
            if (args.length == 1) {
                return partial(List.of("together", "join"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    names.add(online.getName());
                }
                return partial(names, args[1]);
            }
            return Collections.emptyList();
        }

        // /mosque create <name>
        if (name.equals("mosque")) {
            if (args.length == 1) {
                return partial(List.of("create"), args[0]);
            }
            return Collections.emptyList();
        }

        // /virtue <list|add|give|player>
        if (name.equals("virtue")) {
            if (args.length == 1) {
                return partial(List.of("list", "add", "give"), args[0]);
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("give"))) {
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    names.add(online.getName());
                }
                return partial(names, args[1]);
            }
            return Collections.emptyList();
        }

        // /challenge | /challenge claim <name>
        if (name.equals("challenge")) {
            if (args.length == 1) {
                return partial(List.of("claim"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
                // Let the listener drive actual names later if needed; for now use config if present
                if (getConfig().contains("challenges")) {
                    return partial(new ArrayList<>(getConfig().getConfigurationSection("challenges").getKeys(false)), args[1]);
                }
            }
            return Collections.emptyList();
        }

        // /openshop [sell]
        if (name.equals("openshop")) {
            if (args.length == 1) {
                return partial(List.of("sell"), args[0]);
            }
            return Collections.emptyList();
        }

        // /chestshop sell
        if (name.equals("chestshop")) {
            if (args.length == 1) {
                return partial(List.of("sell"), args[0]);
            }
            return Collections.emptyList();
        }

        // /halalcraft info
        if (name.equals("halalcraft")) {
            if (args.length == 1) {
                return partial(List.of("info"), args[0]);
            }
            return Collections.emptyList();
        }

        // /shop, /upgrade, /savevirtue: no subcommands, so no suggestions
        return Collections.emptyList();
    }

    private List<String> partial(List<String> options, String token) {
        String lower = token == null ? "" : token.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) {
                out.add(opt);
            }
        }
        return out;
    }

    /* =====================================================
     * VIRTUE SYSTEM
     * ===================================================== */
    public void changeVirtue(Player p, int amount) {
        virtue.merge(p.getUniqueId(), amount, Integer::sum);
        // Real-time monitoring: Save to config immediately
        int newValue = virtue.get(p.getUniqueId());
        getConfig().set("virtue." + p.getUniqueId().toString(), newValue);
        saveConfig();
    }

    public void setVirtue(Player p, int amount) {
        virtue.put(p.getUniqueId(), amount);
        // Real-time monitoring: Save to config immediately
        getConfig().set("virtue." + p.getUniqueId().toString(), amount);
        saveConfig();
    }

    private void loadVirtueData() {
        // Load from new virtue path
        if (getConfig().contains("virtue")) {
            for (String k : getConfig().getConfigurationSection("virtue").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(k);
                    virtue.put(uuid, getConfig().getInt("virtue." + k, 0));
                } catch (IllegalArgumentException e) {
                    // Skip non-UUID keys (like "tutorials" or other config sections)
                    getLogger().warning("Skipping non-UUID virtue key: " + k);
                }
            }
        }
        // Migrate old data if exists
        if (getConfig().contains("players")) {
            for (String k : getConfig().getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(k);
                    int oldVirtue = getConfig().getInt("players." + k + ".virtue", 0);
                    virtue.put(uuid, oldVirtue);
                    // Move to new path
                    getConfig().set("virtue." + k, oldVirtue);
                    getConfig().set("players." + k, null);
                } catch (IllegalArgumentException e) {
                    // Skip non-UUID keys
                    getLogger().warning("Skipping non-UUID player key: " + k);
                }
            }
            saveConfig();
        }
    }

    public void saveVirtueData() {
        for (var e : virtue.entrySet()) {
            getConfig().set("virtue." + e.getKey().toString(), e.getValue());
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
        // Get top 5 virtue players from config (both online and offline)
        List<Map.Entry<String, Integer>> topPlayers = new ArrayList<>();
        
        if (getConfig().contains("virtue")) {
            org.bukkit.configuration.ConfigurationSection virtueSection = getConfig().getConfigurationSection("virtue");
            for (String key : virtueSection.getKeys(false)) {
                try {
                    int virtueValue = virtueSection.getInt(key, 0);
                    topPlayers.add(new AbstractMap.SimpleEntry<>(key, virtueValue));
                } catch (Exception e) {
                    // Skip invalid entries
                }
            }
        }
        
        topPlayers.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<Map.Entry<String, Integer>> top5 = topPlayers.stream().limit(5).collect(Collectors.toList());

        if (top5.isEmpty()) {
            p.sendMessage("§cNo virtue data available yet.");
            return;
        }

        // Create inventory with player heads
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 9, "§a§lTop Virtue Players");

        int slot = 0;
        for (Map.Entry<String, Integer> entry : top5) {
            if (slot >= 5) break;

            String uuidStr = entry.getKey();
            int virtueValue = entry.getValue();

            try {
                UUID uuid = UUID.fromString(uuidStr);
                
                // Try to get player name (online or from offline player data)
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
            } catch (Exception e) {
                // Skip invalid UUIDs
            }
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

        // Read virtue from config (persistent storage for offline and online players)
        int virtueValue = getConfig().getInt("virtue." + targetUUID.toString(), 0);
        String displayName = Bukkit.getOfflinePlayer(targetUUID).getName();
        if (displayName == null) displayName = "Unknown";

        p.sendMessage("§a" + displayName + "'s Virtue: §b" + virtueValue);
    }

    /* =====================================================
     * HALALCRAFT INFO COMMAND
     * ===================================================== */
    private void sendPluginInfo(Player p) {
        p.sendMessage("§a§l========== HalalCraft Plugin Info ==========");
        p.sendMessage("");

        // Prayer System
        p.sendMessage("§6§l⛩ PRAYER SYSTEM");
        p.sendMessage("§f  There are 5 daily prayers: §eSubh §f(dawn), §eDzuhr §f(noon), §eAsr §f(afternoon), §eMaghrib §f(sunset), §eIsya §f(night).");
        p.sendMessage("§f  Use §a/pray §fon a §fPrayer Mat or inside a Mosque to pray.");
        p.sendMessage("§f  Each prayer gives §b+" + getConfig().getInt("economy.virtue.pray", 5) + " virtue§f. Missing a prayer costs §c" + getConfig().getInt("economy.virtue.miss-prayer", -5) + " virtue§f.");
        p.sendMessage("§f  On Fridays, Dzuhr becomes §eJumah§f — must be prayed in a mosque for §b+" + getConfig().getInt("friday-prayer.virtue-gain", 150) + " virtue§f.");
        p.sendMessage("§f  Missing Jumah costs §c" + getConfig().getInt("friday-prayer.virtue-penalty-miss", -50) + " virtue§f.");
        p.sendMessage("");

        // Congregational Prayer
        p.sendMessage("§6§l🕌 CONGREGATIONAL PRAYER");
        p.sendMessage("§f  A player in a mosque can start a group prayer with §a/pray together§f.");
        p.sendMessage("§f  Others join with §a/pray join <player>§f, then the leader uses §a/pray start§f.");
        p.sendMessage("§f  Virtue reward is multiplied by the number of participants.");
        p.sendMessage("");

        // Dua System
        p.sendMessage("§6§l📿 DUA SYSTEM");
        p.sendMessage("§f  Say duas before actions with §a/dua <type>§f. List them with §a/dua list§f.");
        p.sendMessage("§f  §eSlaughter dua§f — say before killing farm animals. Without it: §c" + getConfig().getInt("economy.virtue.miss-dua-slaughter", -1) + " virtue§f.");
        p.sendMessage("§f  §eEating dua§f — say before eating. Without it: food is halved & §c" + getConfig().getInt("economy.virtue.miss-dua-eating", -1) + " virtue§f.");
        p.sendMessage("§f  §eSleep dua§f — must be said before sleeping in a bed.");
        p.sendMessage("§f  §eFarming dua§f — say before farming. Lasts 10 minutes.");
        p.sendMessage("§f  §eMining dua§f — say before mining. Lasts 1 minute.");
        p.sendMessage("§f  Eating dua can be said §arepeatedly§f. Some duas are once-per-day.");
        p.sendMessage("§f  All duas §ereset at Subh§f (dawn of each new day).");
        p.sendMessage("");

        // Virtue System
        p.sendMessage("§6§l✨ VIRTUE SYSTEM");
        p.sendMessage("§f  Virtue is the core currency. Earn it by praying, saying duas, and daily challenges.");
        p.sendMessage("§f  Lose it by missing prayers, killing without dua, eating without dua, or using haram enchantments.");
        p.sendMessage("§f  §a/virtue §f— view virtue guide. §a/virtue list §f— leaderboard. §a/virtue <player> §f— check a player.");
        p.sendMessage("§f  §a/virtue give <player> <amount> §f— transfer virtue to another player.");
        p.sendMessage("");

        // Combat System
        p.sendMessage("§6§l⚔ COMBAT SYSTEM");
        p.sendMessage("§f  You cannot attack mobs unprovoked. If a mob attacks you, you get §a" + getConfig().getInt("combat.free-kill-duration-seconds", 120) + "s §ffree combat.");
        p.sendMessage("§f  Attacking without being provoked costs §c" + getConfig().getInt("economy.virtue.illegal-attack", -2) + " virtue§f.");
        p.sendMessage("");

        // Enchantment System
        p.sendMessage("§6§l🔮 ENCHANTMENT SYSTEM");
        p.sendMessage("§f  Enchanting table enchantments are considered §charm (blackmagic)§f.");
        p.sendMessage("§f  Using items with impure enchantments costs §c" + getConfig().getInt("economy.virtue.haram-enchantment-use", -10) + " virtue§f.");
        p.sendMessage("§f  Buy a §dPurification Book §ffrom server shops (e.g. chest shops) to cleanse haram enchantments.");
        p.sendMessage("§f  Use §a/upgrade §fto access §ehalal enchantment upgrades §fpurchased with virtue.");
        p.sendMessage("");

        // Shops & Economy
        p.sendMessage("§6§l🛒 SHOPS & ECONOMY");
        p.sendMessage("§f  §a/shop §f— open the Chest Shop browser to track all active [Sell]/[Buy]/admin shops.");
        p.sendMessage("§f     Choose §aBuy §for §eSell §fthen browse and locate chest shops around the world.");
        p.sendMessage("§f  Players can create [Sell]/[Buy]/[aSell]/[aBuy] signs to trade items for virtue.");
        p.sendMessage("§f  Offline transactions are supported — shops work even when the owner is offline.");
        p.sendMessage("§f  §a/openshop §f— browse global one-item listings from all players.");
        p.sendMessage("§f  §a/openshop sell <price> §f— list the item in your hand for virtue.");
        p.sendMessage("§f  Right-click your own listing in the GUI to cancel and get the item back.");
        p.sendMessage("§f  Max listings per player are configurable via §eopenshop.max-listings-per-player§f in config.yml.");
        p.sendMessage("");

        // Mosque System
        p.sendMessage("§6§l🏛 MOSQUE SYSTEM");
        p.sendMessage("§f  Use §a/mosque create <name> §fto designate a mosque area.");
        p.sendMessage("§f  Praying inside a mosque gives §b+" + getConfig().getInt("mosque.prayer-virtue", 10) + " virtue §f(more than solo prayer).");
        p.sendMessage("§f  Jumah prayer can only be performed in a mosque.");
        p.sendMessage("");

        // Daily Challenges
        p.sendMessage("§6§l🏆 DAILY CHALLENGES");
        p.sendMessage("§f  §a/challenge §f— view today's challenges and your progress.");
        p.sendMessage("§f  §a/challenge claim <name> §f— claim completed challenge rewards.");
        p.sendMessage("§f  Challenges reset daily and offer bonus virtue for completing tasks.");
        p.sendMessage("");

        // Haram Items
        p.sendMessage("§6§l🚫 HARAM RULES");
        p.sendMessage("§f  Pigs are haram — touching one gives infinite slowness (drink milk to cure).");
        p.sendMessage("§f  Drinking potions is haram: §c" + getConfig().getInt("economy.virtue.potion-drinking", -10) + " virtue§f.");
        p.sendMessage("§f  Enchanting tables are flagged as blackmagic when placed.");
        p.sendMessage("");

        // QoL & Admin
        p.sendMessage("§6§l⚙ QUALITY OF LIFE & ADMIN");
        p.sendMessage("§f  Most commands support §atab-completion§f for subcommands and player names.");
        p.sendMessage("§f  §a/savevirtue §f— OP-only command to manually save all virtue data to disk.");
        p.sendMessage("");

        p.sendMessage("§a§l==========================================");
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

            // Process the transaction - check current virtue to prevent going negative
            int buyerCurrentVirtue = getVirtue(player);
            
            if (buyerCurrentVirtue >= virtue) {
                // Buyer has enough virtue - process normally
                changeVirtue(player, -virtue);
                
                player.sendMessage("§a✓ Pending transaction processed:");
                player.sendMessage("§7Purchased " + quantity + " " + itemName + " from §e" + sellerName + "§7 for §b" + virtue + "§7 virtue");
                
                totalVirtue += virtue;
            } else {
                // Insufficient virtue - deduct what they have and cap at 0
                int actualDeduction = buyerCurrentVirtue; // Take all remaining virtue
                changeVirtue(player, -actualDeduction);
                int shortfall = virtue - actualDeduction;
                
                player.sendMessage("§c⚠ Pending transaction processed (INSUFFICIENT FUNDS):");
                player.sendMessage("§7Purchased " + quantity + " " + itemName + " from §e" + sellerName);
                player.sendMessage("§7Required: §b" + virtue + "§7 virtue | Available: §b" + actualDeduction + "§7 virtue");
                player.sendMessage("§cShortfall: §b" + shortfall + "§c virtue - Your shops may be disabled!");
                
                // Note: Seller was already paid immediately when the sale happened
                // So we just log the shortfall
                getLogger().warning("Player " + buyerName + " had insufficient virtue for transaction. Required: " + virtue + ", Had: " + actualDeduction + ", Shortfall: " + shortfall);
                
                totalVirtue += actualDeduction;
            }
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
        // Keep in-memory cache in sync
        virtue.put(uuid, amount);

        // Persist to config (persistent virtue account)
        getConfig().set("virtue." + uuid.toString(), amount);
        saveConfig();
    }

    /**
     * Get virtue for a player by UUID
     */
    public int getVirtue(java.util.UUID uuid) {
        return getConfig().getInt("virtue." + uuid.toString(), 0);
    }

    /**
     * Get virtue for a player by name (works for both online and offline players)
     * This is crucial for checking offline player virtue before allowing shop transactions
     */
    public int getVirtueByName(String playerName) {
        // Try online player first
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            return virtue.getOrDefault(online.getUniqueId(), 0);
        }
        
        // Search offline players
        for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(playerName)) {
                return getVirtue(op.getUniqueId());
            }
        }
        
        return 0; // Player not found
    }
}
