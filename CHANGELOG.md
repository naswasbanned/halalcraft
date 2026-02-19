# Changelog

All notable changes to HalalCraft will be documented in this file.

## [1.21.12] - 2026-02-20

### 🔄 Improved Virtue & Shop Systems

#### Added

- **Persistent Virtue Accounts**
  - Every player now has a server-side virtue account stored by UUID
  - Virtue changes are saved immediately whenever players gain or spend virtue
  - `/virtue <player>` now works reliably for offline players
  - Virtue leaderboard (`/virtue list`) reads from the persistent account data

- **Offline-Safe Player Shops**
  - [Buy] shop owners can be offline while others sell to their shops
  - Offline owners' virtue is checked before each transaction using the stored account
  - Virtue is deducted from the owner's account even if they are offline
  - Owners can no longer go into negative virtue because of offline sales
  - Fixed exploit where offline owners could be drained without their virtue changing

- **Admin Shops**
  - New admin-only shop signs:
    - `[aSell]` – Admin sell shop (infinite items)
    - `[aBuy]` – Admin buy shop (infinite virtue)
  - Only OPs can create `[aSell]` and `[aBuy]` signs (normal players are blocked)
  - Admin sell shops:
    - Use chest item as a template and give infinite copies to players
    - Do not consume chest stock
    - Players pay virtue; no specific player owner is credited
  - Admin buy shops:
    - Server pays virtue to players for their items
    - Items are removed from players and can be stored in the chest
    - No owner virtue checks or deductions (server has infinite virtue)

- **Chest Shop Browser (`/shop`)**
  - `/shop` now opens a GUI browser listing all active `[Sell]`, `[Buy]`, `[aSell]`, and `[aBuy]` chest shops.
  - Players can choose Buy or Sell mode and see owner, item, price, and location.
  - Chest shop metadata is stored in `config.yml` under `chestshops.*` to avoid scanning worlds/chunks and reduce lag.

- **Open Shop Market (`/openshop`)**
  - New one-item listing system backed by `openshop.listings.*` in `config.yml`.
  - `/openshop` opens a paginated GUI of all listings; `/openshop sell <price>` lists the item in your hand for virtue.
  - Players can have multiple listings; the per-player cap is configurable via `openshop.max-listings-per-player`.
  - Listings can be cancelled by right-clicking your own item in the GUI; virtue is transferred even if the seller is offline.
  - New listings broadcast a server-wide announcement.

- **Command Snippets / Tab Completion**
  - Added tab-completion for: `/dua`, `/pray`, `/mosque`, `/virtue`, `/challenge`, `/shop`, `/openshop`, `/chestshop`, `/upgrade`, `/halalcraft`.
  - Suggests available subcommands (e.g. `list`, `claim`, `sell`) and player names where appropriate.

- **Manual Virtue Save**
  - New OP-only command `/savevirtue` to manually flush all virtue data to disk.

#### Fixed

- Multiple issues with offline [Buy] shop virtue handling:
  - Owner virtue not updating in `/virtue list` after offline sales
  - Potential negative virtue values from queued offline transactions
  - Inconsistent virtue between in-memory cache and config storage
- Ensured virtue leaderboard and `/virtue` commands always reflect the latest stored values.

---

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
