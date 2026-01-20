# Changelog

All notable changes to HalalCraft will be documented in this file.

## [1.21.11] - 2026-01-20

### 🎉 Initial Release

#### Added

**Prayer System**
- Five daily prayers (Fajr, Zuhr, Asr, Maghrib, Isha) based on Minecraft time
- Prayer mat system using white carpets
- Mosque creation with beacon-based areas
- Congregational prayer with virtue multipliers
- Jumah (Friday) prayer with special rewards (150 virtue)
- Prayer time warnings and notifications
- Automatic prayer tracking and missed prayer detection

**Dua System**
- 8 different duas with unique effects:
  - Healing (regeneration + instant health)
  - Protection (resistance + damage reduction)
  - Blessing (luck + absorption)
  - Strength (increased damage)
  - Speed (movement boost)
  - Food (saturation + health)
  - Peace (removes negative effects)
  - Travel (safe teleportation)
- Configurable cooldowns (5 minutes default)
- Virtue cost per dua (2 virtue)

**Virtue Economy**
- Player virtue tracking system
- Virtue leaderboard (`/virtue list`)
- Earn virtue through:
  - Regular prayers (5 virtue)
  - Jumah prayers (150 virtue)
  - Daily challenges (20+ virtue)
  - Congregational prayers (multiplier bonuses)
- Spend virtue on:
  - Shop items
  - Enchantment upgrades
  - Mosque creation
  - Duas
- Player-to-player virtue transfers (`/virtue give`)
- Admin virtue management (`/virtue add`)

**Daily Challenges**
- 4 challenge categories: Mining, Farming, Fishing, Combat
- Daily reset at midnight
- Random task generation
- Claim system with virtue rewards
- Progress tracking

**Halal Gameplay Rules**
- No pork consumption
- No attacking passive animals (virtue penalty)
- PvP cooldown system
- Haram enchantment detection
- Table-enchanted items require purification

**Shop Systems**
- Virtue shop GUI (`/shop`)
  - Purchase items with virtue
  - Buy enchantment books
- Enchantment upgrade system (`/upgrade`)
  - Upgrade existing enchantments
  - Level-by-level progression
  - Configurable pricing
- Player shop signs:
  - [Sell] signs - Sell items to other players
  - [Buy] signs - Buy items from other players
  - Auto-detection of item types
  - Protected chest system
  - Automatic virtue transactions

**Purification System**
- Anvil-based purification for haram items
- Purification book crafting
- Virtue cost for purification
- Clear indicators for haram items

**Commands**
- `/islam` - View Islamic rules
- `/pray` - Perform prayer
- `/pray together` - Start congregational prayer
- `/pray join <player>` - Join congregational prayer
- `/pray start` - Begin congregational prayer
- `/mat` - Give prayer mats (OP)
- `/dua list` - List available duas
- `/dua <type>` - Say a dua
- `/mosque create <name>` - Create mosque
- `/virtue` - Check virtue balance
- `/virtue list` - View leaderboard
- `/virtue <player>` - Check player virtue
- `/virtue give <player> <amount>` - Give virtue
- `/virtue add <player> <amount>` - Admin add virtue
- `/shop` - Open virtue shop
- `/upgrade` - Open upgrade GUI
- `/challenge` - View challenges
- `/challenge claim <name>` - Claim reward

**Configuration**
- Comprehensive config.yml with all settings
- upgrade.yml for enchantment pricing
- Per-player data persistence
- Customizable messages
- Adjustable virtue gains/costs

#### Fixed
- Prayer mat detection now works correctly
- Buy sign chest inventory persistence
- Enchantment name recognition with minecraft: format
- Config cleanup (removed duplicates)
- Proper virtue transactions for player shops

#### Technical
- Minecraft 1.21.11 support
- Paper API 1.21.1-R0.1-SNAPSHOT
- Java 17 compatibility
- Maven build system
- No external dependencies (removed Vault)
- Custom economy implementation

---

## Upcoming Features

### Planned for Future Releases

- [ ] Offline player virtue crediting for shop sales
- [ ] Shop transaction history
- [ ] Multiple mosque support per player
- [ ] Ramadan special events
- [ ] Zakat (charity) system
- [ ] Islamic calendar integration
- [ ] Athan (call to prayer) sound effects
- [ ] Qibla direction compass
- [ ] Islamic education quizzes
- [ ] Eid celebration events

---

## Bug Reports

If you encounter any issues, please report them with:
- Plugin version
- Minecraft version
- Server type (Spigot/Paper)
- Steps to reproduce
- Error messages (if any)

---

**May Allah bless this project and its users! 🤲**
