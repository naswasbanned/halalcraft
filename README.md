# HalalCraft

**Version:** 1.21.12  
**Minecraft Version:** 1.21+  
**Platform:** Spigot/Paper

A comprehensive Islamic-themed Minecraft plugin that brings Islamic practices, virtue economy, and halal gameplay mechanics to your server.

## 📋 Table of Contents

- [Features](#-features)
- [Installation](#-installation)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [Configuration](#-configuration)
- [Gameplay Guide](#-gameplay-guide)
- [Building from Source](#-building-from-source)

---

## ✨ Features

### 🕌 Islamic Practices

- **Prayer System**
  - Five daily prayers with real-time tracking
  - Prayer mats (white carpet) for praying anywhere
  - Congregational prayers in mosques with virtue multipliers
  - Jumah (Friday) prayer with bonus rewards
  - Prayer time warnings and notifications

- **Mosque System**
  - Create mosques with beacon-based areas
  - Protected congregational prayer zones
  - Mosque management commands

- **Duas (Supplications)**
  - Multiple duas with unique effects
  - Healing, protection, blessing, and more
  - Configurable cooldowns and effects

### 💎 Virtue Economy

- **Virtue Currency**
  - Earn virtue through prayers, challenges, and halal activities
  - Lose virtue for missing prayers or haram actions
  - Player-to-player virtue transfers
  - Leaderboard system (shows both online and offline players)

- **Persistent Virtue Accounts**
  - Every player has a server-side virtue account
  - Virtue changes are saved immediately when gained or spent
  - `/virtue <player>` works for offline players too
  - Offline shop owners have their virtue checked and deducted safely

- **Daily Challenges**
  - Rotating daily tasks for virtue rewards
  - Mining, farming, fishing, and combat challenges
  - Track progress and claim rewards

### 🛡️ Halal Gameplay Rules

- **Combat System**
  - No attacking passive animals
  - PvP restrictions with cooldowns
  - Virtue penalties for violations

- **Enchantment Rules**
  - Certain enchantments marked as "haram"
  - Table-enchanted items require purification
  - Purification system using anvils

### 🏪 Shop Systems

- **Virtue Shop**
  - Buy items and enchantment books with virtue
  - Upgrade enchantments on existing items
  - Configurable prices and items

- **Player Shops**
  - **[Sell] Signs** - Players sell items to others
    - Place chest, add items, create sign
    - Customers right-click sign to purchase
    - Seller receives virtue automatically
  
  - **[Buy] Signs** - Players buy items from others
    - Place chest with detector item, create sign
    - Suppliers right-click with items to sell
    - Buyer receives items in chest
    - Offline shop owners have virtue deducted from their account when suppliers sell

  - **Admin Shops** (server-controlled)
    - `[aSell]` signs: infinite-stock shops where players buy from the server
      - Requires a chest with one template item behind the sign
      - Items are cloned; chest stock is not consumed
      - Player pays virtue, no specific player owner is credited
    - `[aBuy]` signs: infinite-funds shops where players sell to the server
      - Server pays virtue to players
      - Items are taken from players and optionally stored in the chest
    - Only server operators (OP) can create `[aSell]` and `[aBuy]` signs

---

## 📥 Installation

1. Download `HalalCraft-1.21.11.jar`
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/HalalCraft/config.yml`
5. (Optional) Customize `upgrade.yml` for enchantment prices

**Requirements:**
- Minecraft 1.21 or higher
- Spigot or Paper server
- Java 17+

---

## 🎮 Commands

### General Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/islam` | View Islamic rules and information | - |
| `/pray` | Perform prayer (on mat or in mosque) | - |
| `/pray together` | Start congregational prayer (mosque only) | - |
| `/pray join <player>` | Join congregational prayer | - |
| `/mat` | Receive prayer mats (OP only) | OP |

### Dua Commands

| Command | Description |
|---------|-------------|
| `/dua list` | List all available duas |
| `/dua <type>` | Say a specific dua (healing, protection, etc.) |

### Mosque Commands

| Command | Description |
|---------|-------------|
| `/mosque create <name>` | Create a mosque at current location |

### Virtue Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/virtue` | Check your virtue balance | - |
| `/virtue list` | View virtue leaderboard | - |
| `/virtue <player>` | Check another player's virtue | - |
| `/virtue give <player> <amount>` | Give virtue to another player | - |
| `/virtue add <player> <amount>` | Admin: Add virtue to player | OP |

### Shop Commands

| Command | Description |
|---------|-------------|
| `/shop` | Open the virtue shop GUI |
| `/upgrade` | Open enchantment upgrade GUI |

### Challenge Commands

| Command | Description |
|---------|-------------|
| `/challenge` | View your daily challenges |
| `/challenge claim <name>` | Claim completed challenge reward |

---

## 🔐 Permissions

```yaml
permissions:
  halal.virtue.add:
    description: Allow admins to add virtue to players
    default: op
  halal.virtue.give:
    description: Allow players to give virtue to other players
    default: true
```

All other commands are available to all players by default.

---

## ⚙️ Configuration

### Main Configuration (`config.yml`)

**Key Settings:**

```yaml
islam:
  rules:
    no-pork: true
    no-alcohol: true
    no-passive-animal-attack: true

economy:
  virtue:
    pray: 5                    # Virtue gained per regular prayer
    daily-challenge: 20        # Virtue per challenge completion
    dua-cost: 2               # Cost to say a dua

mosque:
  beacon-range: 50           # Range of mosque area
  create-cost: 100          # Virtue cost to create mosque

friday-prayer:
  virtue-gain: 150          # Jumah prayer reward
  min-participants: 3       # Minimum for congregational prayer

dua:
  cooldown-seconds: 300     # 5 minute cooldown between duas
  
daily-challenges:
  enabled: true
  reset-time: "00:00"       # Daily reset at midnight
```

### Enchantment Upgrades (`upgrade.yml`)

Configure pricing for each enchantment level upgrade:

```yaml
minecraft:sharpness:
  name: "Sharpness"
  max-level: 5
  levels:
    1:
      virtue: 50
      exp: 5
    2:
      virtue: 75
      exp: 10
```

---

## 📖 Gameplay Guide

### Getting Started

1. **Receive Prayer Mats**
   - Ask an admin for `/mat` command
   - Place white carpet anywhere to create prayer area

2. **Prayer Times**
   - Check scoreboard for current prayer time
   - Stand on prayer mat or enter mosque
   - Type `/pray` to perform prayer
   - Earn 5 virtue per prayer

3. **Earning Virtue**
   - Pray five times daily (Fajr, Zuhr, Asr, Maghrib, Isha)
   - Complete daily challenges (`/challenge`)
   - Participate in Jumah (Friday) prayers in mosques
   - Say duas for blessings

4. **Spending Virtue**
   - `/shop` - Buy items and enchantment books
   - `/upgrade` - Upgrade enchantments on your gear
   - Purify haram-enchanted items at anvil
   - Create mosques (100 virtue)

### Advanced Features

#### Creating a Mosque

1. Find a suitable location
2. Place a beacon (must have 100 virtue)
3. Type `/mosque create <name>`
4. Mosque area extends 50 blocks from beacon
5. Lead congregational prayers for bonuses

#### Congregational Prayer

1. Be inside a mosque during prayer time
2. Leader types `/pray together`
3. Other players type `/pray join <leader>`
4. Leader starts with `/pray start`
5. Everyone earns multiplied virtue (2x+ based on participants)

#### Player Shops

**Selling Items (Sell Sign):**

1. Place a chest
2. Add items you want to sell to the chest
3. Place sign in front of chest with format:
   ```
   [Sell]
   1
   Diamond
   10
   ```
   (Line 1: [Sell], Line 2: quantity, Line 3: item name, Line 4: price)
4. Players right-click sign to purchase

**Buying Items (Buy Sign):**

1. Place a chest
2. **Important:** Add at least 1 detector item (the item you want to buy)
3. Place sign in front of chest with format:
   ```
   [Buy]
   64
   
   50
   ```
   (Line 1: [Buy], Line 2: quantity, Line 3: auto-detected, Line 4: price)
4. Item name is auto-detected from chest
5. Players right-click sign with items to sell to you
6. Items go into your chest, they receive virtue

#### Daily Challenges

- Reset every day at midnight
- View with `/challenge`
- Types: Mining, Farming, Fishing, Combat
- Claim rewards with `/challenge claim <name>`
- Each challenge rewards 20+ virtue

---

## 🔨 Building from Source

**Requirements:**
- Java 17+
- Maven 3.6+

**Build Steps:**

```bash
git clone <repository-url>
cd HalalCraft
mvn clean package
```

Built JAR will be in `target/HalalCraft-1.21.11.jar`

---

## 📝 Version History

### v1.21.11 (Initial Release)

**Features:**
- Complete prayer system with 5 daily prayers
- Mosque creation and management
- Congregational prayer system
- 8 different duas with unique effects
- Virtue economy with leaderboard
- Daily challenge system
- Halal gameplay rules (no pork, no passive animal attacks)
- Enchantment haram detection and purification
- Virtue shop with items and enchantment books
- Enchantment upgrade system
- Player-operated shop signs ([Sell] and [Buy])
- Prayer time warnings and notifications
- Player-to-player virtue transfers

**Shop Systems:**
- [Sell] signs for player-to-player item sales
- [Buy] signs with detector item system
- Auto-detection of item types
- Protected chests with owner-only access

---

## 🤝 Support

For bug reports, feature requests, or questions:
- Create an issue on the repository
- Contact the development team

---

## 📜 License

This plugin is provided as-is for Minecraft server use.

---

## 🌟 Credits

Developed for Islamic-themed Minecraft servers to promote halal gameplay and Islamic values.

**Special Features:**
- Real-time prayer tracking based on Minecraft time
- Cultural authenticity with Arabic terms and practices
- Educational Islamic content
- Family-friendly gameplay mechanics

---

## ⚠️ Important Notes

- Prayer times are based on Minecraft in-game time
- Virtue is saved per-player in config files
- Shop chests are protected - only owners can break them
- Missed prayers result in virtue loss (no status effects)
- Purification required for table-enchanted items before use

---

**Enjoy halal gaming with HalalCraft! 🕌✨**
