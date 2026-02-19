# HalalCraft Plugin - Feature Verification

## ✅ All 8 Core Features Implemented & Verified

### 1. **Prayer & Prayer Time System** ✅
**Location:** `HalalCraft.java` (Lines 50-250)

**Features:**
- **5 Daily Prayers Setup:**
  - Subh: 0 ticks (sunrise)
  - Dzuhr: 6000 ticks (noon)
  - Asr: 9000 ticks (afternoon)
  - Maghrib: 12000 ticks (sunset)
  - Isya: 14000 ticks (night)

- **Prayer Announcement System:**
  - Automatic announcements when prayer time begins
  - Prevents duplicate announcements with `announcedToday` tracking
  - Configurable messages in `config.yml`

- **Prayer Completion:**
  - `/pray` command completes current prayer
  - Awards configurable virtue (+5 per prayer)
  - Tracks who prayed with `prayedToday` map
  - Prevents double praying same day

- **Missed Prayer Tracking:**
  - `checkMissedPrayers()` applies -5 virtue penalty for missed prayers
  - Special Friday prayer (Jumah) applies -50 virtue penalty if missed

- **Per-Player Scoreboard:**
  - Real-time prayer timer showing time until next prayer
  - Distance to next prayer in ticks
  - Current prayer name
  - Player's virtue balance

### 2. **Dua System** ✅
**Location:** `DuaListener.java` (Lines 1-250)

**Features:**
- **5 Configurable Duas:**
  1. **Slaughter Dua** - Before killing farm animals
  2. **Sleep Dua** - Required before sleeping (bed access blocked without it)
  3. **Eating Dua** - Before eating food (food reduced by 50% without it)
  4. **Farming Dua** - Before farming (once per day, +1 virtue)
  5. **Mining Dua** - Before mining (once per day, +1 virtue)

- **Dua Management:**
  - `/dua <name>` - Say a dua
  - `/dua list` - View all available duas
  - Validation against config.yml
  - Auto-expiry tracking with millisecond precision

- **Once-Per-Day System:**
  - Stored in `config.yml` under `players.<UUID>.duas.<name>.day`
  - Resets each day automatically
  - Virtue reward (+1) on successful once-per-day dua

- **Sleep Dua Special:**
  - Prevents bed access without sleep dua
  - `onPlayerBedEnter()` blocks sleep if dua not said
  - `onPlayerBedLeave()` **clears ALL duas** when player wakes up

- **Eating Dua Special:**
  - `onPlayerItemConsume()` checks for eating dua
  - Reduces food/saturation by 50% if missing
  - Applies -1 virtue penalty
  - Prevents overeating without prayer

### 3. **Mosque System** ✅
**Location:** `MosqueListener.java` (Lines 1-161), `MosqueManager.java` (Lines 1-114)

**Features:**
- **Mosque Creation:**
  - `/mosque create <name>` command
  - Lectern + wool blocks define mosque area
  - Flood fill algorithm finds all connected wool blocks

- **Mosque Detection:**
  - `isInside(Location)` checks if location is within mosque bounds
  - Per-player mosque tracking in `playerInMosque` map

- **Mosque Movement:**
  - `onPlayerMove()` detects entry/exit
  - Messages: "§aYou entered the mosque: §e%mosque%"
  - Messages: "§eYou left the mosque: %mosque%"

- **Mosque Deletion:**
  - Breaking lectern deletes entire mosque
  - Broadcasts to all players
  - Removes all data from `MosqueManager`

- **Prayer Virtue Bonus:**
  - Completing prayer in mosque awards extra virtue (+10 base)
  - Congregational prayers multiply by number of participants

### 4. **Haram Items System** ✅
**Location:** `IslamicCombatRule.java` (Lines 160-260)

#### **A. Pig (Pork) Haram**
- **Detection:**
  - `onPlayerMove()` checks inventory for porkchop
  - Detects pigs within 2-block radius
  - Triggers on collision with pig entity

- **Effect:**
  - Applies infinite Slowness effect (Speed 2)
  - Message: "§c§lYou touched a pig! Infinite slowness applied. Drink milk to cure!"
  - Only applies once per player

- **Cure:**
  - Drinking milk bucket removes all potion effects
  - Message: "§aThe milk has purified you!"

#### **B. Potion Drinking Haram**
- **Detection:**
  - `onPlayerDrinkPotion()` in `PlayerItemConsumeEvent`
  - Checks if item type contains "POTION"

- **Effects:**
  - Applies -10 virtue penalty
  - Adds Nausea potion effect (30 seconds configurable)
  - Message: "§cDrinking potions is haram! Virtue -%d"

- **Config:**
  - `economy.virtue.potion-drinking: -10`
  - `effects.nausea-duration-seconds: 30`

#### **C. Enchantment (Blackmagic) Haram**
- **Detection:**
  - `onPlayerAttackWithEnchantedItem()` checks main hand item
  - Detects enchantments with `item.getEnchantments().isEmpty()`

- **Effects:**
  - Applies -20 virtue penalty per attack with enchanted item
  - Message: "§cUsing enchantments is forbidden! Virtue -%d"

- **Warning System:**
  - `onEnchantingTablePlace()` broadcasts warning when table placed
  - Message: "§c⚠ WARNING: %player% has placed an enchanting table!"

- **Config:**
  - `economy.virtue.blackmagic: -20`

### 5. **Virtue System** ✅
**Location:** `HalalCraft.java` (Lines 600-656)

**Features:**
- **Virtue Tracking:**
  - Per-player virtue stored in `virtue<UUID, Integer>` map
  - Persisted to disk with `saveVirtueData()`
  - Loaded on plugin startup

- **Virtue Commands:**
  - `/virtue` - Show personal virtue score
  - `/virtue list` - Top 5 players leaderboard with GUI (player head items)
  - `/virtue <player>` - View other player's virtue

- **Virtue Changes:**
  - `changeVirtue(Player, int)` modifies and displays changes
  - Broadcasts virtue changes: "§a%player% gained %d virtue!"
  - Negative changes broadcast: "§c%player% lost %d virtue!"

- **Virtue Sources:**
  - Prayer: +5 per regular prayer, +150 for Jumah
  - Once-per-day duas: +1 each (farming, mining, eating)
  - Congregational prayers: +10 × number of participants
  - Slaughter dua: -0 (avoids penalty)

- **Virtue Penalties:**
  - Illegal mob attack: -2
  - Missed prayer: -5 per prayer
  - Missed Jumah: -50
  - Eating without dua: -1
  - Slaughter without dua: -1
  - Blackmagic (enchanted items): -20
  - Potion drinking: -10

- **Leaderboard GUI:**
  - Shows top 5 virtue players
  - Uses player head items with lore showing virtue
  - Click-friendly inventory interface

### 6. **Congregational Prayer System** ✅
**Location:** `PrayListener.java` (Lines 1-331)

**Features:**
- **Prayer Initiation:**
  - `/pray together` - Start congregational prayer in current mosque
  - Requires player to be in mosque
  - Creates `CongregationalPrayer` instance tracking initiator

- **Prayer Joining:**
  - `/pray join <player>` - Join another player's congregational prayer
  - Checks if target is leading prayer
  - Checks if both players in same mosque
  - Message: "§aYou joined %player%'s congregational prayer! Waiting for leader to start..."

- **Prayer Completion:**
  - `/pray start` - Leader completes congregational prayer
  - Marks all participants as prayed
  - Virtues multiplied: Base 10 × number of participants
  - Message: "§aAll %count% participants earned %virtue% virtue (x%multiplier%)."

- **Prayer Termination:**
  - If leader leaves mosque: prayer ends
  - If leader quits server: prayer ends
  - All participants notified: "§c%player% left the mosque. The congregational prayer has ended!"

- **Mosque Tracking:**
  - `playerInMosque<UUID, String>` tracks current mosque
  - `activePrayers<String, CongregationalPrayer>` tracks active prayers by leader name

### 7. **Friday Prayer (Jumah) System** ✅
**Location:** `HalalCraft.java` (Lines 250-350)

**Features:**
- **Friday Detection:**
  - `isFriday()` uses `Calendar.getInstance().get(Calendar.DAY_OF_WEEK)`
  - Checks for Friday (day 6 in ISO week)

- **Jumah Prayer:**
  - Replaces Dzuhr prayer on Fridays
  - `getCurrentPrayer()` returns "Jumah" instead of "Dzuhr" on Fridays
  - Special announcement with Islamic graphics:
    - "§e§l========== JUMAH PRAYER =========="
    - "§a🕌 It is time for the Friday congregational prayer (Jumah)!"
    - "§fYou must pray in a mosque."
    - "§e=================================="

- **Mosque Enforcement:**
  - Jumah can ONLY be completed in mosque
  - `/pray` in non-mosque location: "§cJumah prayer can only be performed in a mosque!"
  - Error message: "§cYou must be inside a mosque!"

- **Virtue Rewards:**
  - Completing Jumah: +150 virtue (vs +5 for regular prayers)
  - Missing Jumah: -50 virtue (vs -5 for regular prayers)
  - Jumah in congregation: +10 × participants (multiplied by participants)

- **Config:**
  ```yaml
  friday-prayer:
    enabled: true
    virtue-gain: 150
    virtue-penalty-miss: -50
    mosque-only: true
    announcement: "§e§l========== JUMAH PRAYER ..."
    title: "§a§lJumah Prayer Time"
    subtitle: "§fGather at the mosque for..."
  ```

### 8. **Islamic Combat System** ✅
**Location:** `IslamicCombatRule.java` (Lines 1-298)

**Features:**
- **Free Kill Window:**
  - When mob attacks player: free 120 seconds of combat
  - `onMobHitPlayer()` triggers free kill window
  - Message: "§aYou were attacked! Free combat allowed for 120 seconds."
  - `notified` map prevents message spam

- **Projectile Support:**
  - Handles skeleton arrows and other projectiles
  - Checks `Projectile.getShooter() instanceof Monster`
  - Properly attributing damage to mob instead of arrow

- **Illegal Attack Penalty:**
  - Attacking mob outside free kill window: -2 virtue
  - Message: "§cYou attacked a mob without being attacked first. Virtue -2"
  - Prevents griefing/unfair farm killing

- **Farm Animal Recognition:**
  - Detects: Cow, Pig, Sheep, Chicken, Rabbit, Donkey, Horse, Cat, Parrot
  - Separate handling for farm animal slaughter

- **Punishments:**
  - Illegal attacks: -2 virtue
  - Enchanted items: -20 virtue per attack
  - Potion drinking: -10 virtue + nausea effect
  - Pig possession: infinite slowness until milk cure

- **Configurable Durations:**
  - Free kill window: 120 seconds (configurable)
  - Nausea effect: 30 seconds (configurable)

---

## Build Status ✅
- **Last Build:** SUCCESS (Exit Code: 0)
- **Maven Command:** `mvn clean package`
- **Plugin JAR:** `/home/nas/IdeaProjects/HalalCraft/target/HalalCraft-1.21.12.jar`

## Configuration Status ✅
- **Config File:** `src/main/resources/config.yml`
- **Total Settings:** 100+ configurable values
- **All Messages:** 40+ customizable message templates
- **All Virtues:** 8 virtue gain/loss scenarios
- **All Durations:** All effect durations configurable

## Ready for Deployment ✅
The plugin is fully implemented, compiled, and ready to deploy to your Minecraft server.
