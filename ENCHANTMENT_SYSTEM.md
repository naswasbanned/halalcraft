# HalalCraft Enchantment System - Implementation Guide

## Overview
This implementation adds a complete **Virtue-Based Enchantment System** to HalalCraft where players purchase pure (halal) enchantments using virtue points instead of using the vanilla enchanting table.

## Key Features Implemented

### 1. **Upgrade Command & GUI** (`/upgrade`)
- Players access enchantments via `/upgrade` command
- Opens a GUI showing all available enchantments with:
  - Display name and description
  - Virtue cost
  - Enchantment level
- Clean sound feedback on open/purchase/deny

### 2. **Enchantment Book System**
- Purchased enchantments come as **Enchanted Books** (unique from vanilla)
- Books are tagged with NBT data containing:
  - `upgrade_enchantment_book`: Identifies as upgrade book
  - `enchant_name`: Name of the enchantment
  - `enchant_level`: Level of the enchantment
  - `upgrade_cost`: Original cost in virtue
- Books are consumed after applying to tools

### 3. **Drag-and-Drop Application** (EnchantmentBookListener)
- Players right-click while holding an enchantment book to apply it
- Target must be a valid tool or armor:
  - Pickaxe, Axe, Shovel, Hoe, Sword
  - Helmet, Chestplate, Leggings, Boots
- Applied tools are marked as "purified" enchantment
- Success message and sound effect on application

### 4. **Impure Enchantment Tracking** (ImpureEnchantmentPenaltyListener)
- Any enchantments from vanilla enchanting table are tagged as "impure/haram"
- When players **use** impure enchanted items:
  - **Combat**: Lose virtue when attacking with impure weapon
  - **Mining**: Lose virtue when breaking blocks with impure tool
  - **Penalty**: Configurable (default: -10 virtue per use)
  - **Cooldown**: 1 second between penalties to prevent spam

### 5. **Virtue Economy**
- `/upgrade` command shows available enchantments with prices
- Players must have sufficient virtue to purchase
- Virtue is deducted immediately upon purchase
- Book consumption counts as use of the enchantment (one-time)

---

## Available Enchantments

Add to your `config.yml` under `upgrade.enchantments`:

```yaml
upgrade:
  enchantments:
    sharpness:
      display-name: "§c⚔ Sharpness V"
      description: "Increase damage dealt by weapons"
      enchantment: "DAMAGE_ALL"
      level: 5
      cost: 50
    
    efficiency:
      display-name: "§e⚒ Efficiency V"
      description: "Increase mining speed"
      enchantment: "BLOCK_EFFICIENCY"
      level: 5
      cost: 45
    
    unbreaking:
      display-name: "§9🛡 Unbreaking III"
      description: "Increase tool durability"
      enchantment: "DURABILITY"
      level: 3
      cost: 40
    
    protection:
      display-name: "§b🪡 Protection IV"
      description: "Reduce damage taken"
      enchantment: "PROTECTION_ENVIRONMENTAL"
      level: 4
      cost: 60
    
    fortune:
      display-name: "§6💎 Fortune III"
      description: "Increase ore drops"
      enchantment: "LOOT_BONUS_BLOCKS"
      level: 3
      cost: 55
    
    knockback:
      display-name: "§f↗ Knockback II"
      description: "Enemies knocked back further"
      enchantment: "KNOCKBACK"
      level: 2
      cost: 30
    
    looting:
      display-name: "§5💀 Looting III"
      description: "Increase mob drops"
      enchantment: "LOOT_BONUS_MOBS"
      level: 3
      cost: 50
```

---

## How Players Use It

### Step 1: Open Upgrade Menu
```
/upgrade
```

### Step 2: Select Enchantment
- Click on an enchantment book in the GUI
- System checks if player has enough virtue
- If sufficient: Book added to inventory, virtue deducted
- If insufficient: Deny sound, error message shown

### Step 3: Apply to Tool
- Hold the enchantment book
- Right-click in the air or while holding a tool
- Book is consumed, enchantment applied to tool
- Tool is marked as "purified"
- Success sound and message displayed

### Step 4: Use Tool with Pure Enchantment
- No virtue loss
- No warnings
- Tool works normally

### Step 5: Using Impure Enchantments
- If player enchants item from vanilla enchanting table:
  - Item is automatically marked as "impure/haram"
  - Yellow lore line added: "⚠ Unpurified Enchantment"
  - When player attacks mobs or breaks blocks:
    - Warning message shown
    - -10 virtue deducted (configurable)
    - Warning sound played
    - Cannot use impure enchantments guilt-free!

---

## Configuration Reference

### Sounds
```yaml
upgrade:
  open-sound:      # Sound when opening /upgrade menu
    type: "block.chest.open"
    volume: 1.0
    pitch: 1.0
  
  buy-sound:       # Sound when purchasing book
    type: "entity.item.pickup"
    volume: 1.0
    pitch: 1.2
  
  deny-sound:      # Sound when purchase fails
    type: "block.anvil.place"
    volume: 1.0
    pitch: 0.5
  
  apply-sound:     # Sound when applying enchantment
    type: "entity.player.levelup"
    volume: 1.0
    pitch: 1.0
```

### Messages
```yaml
upgrade:
  messages:
    insufficient-virtue: "§cYou need %needed% more virtue! Current: %current%"
    inventory-full: "§cYour inventory is full!"
    purchased-book: "§a✓ Received %enchant% for %cost% virtue"
    applied-enchantment: "§a✓ %enchant% applied to your %tool%!"
    no-upgrade-book: "§cThis is not an upgrade enchantment book!"
```

### Virtue Economy
```yaml
economy:
  virtue:
    haram-enchantment-use: -10  # Penalty for using impure enchantments
```

---

## File Structure

### New Files Created

1. **EnchantmentBookListener.java**
   - Handles book purchase from GUI
   - Manages book application to tools
   - Checks validity of target items
   - Marks applied items as "purified"

2. **ImpureEnchantmentPenaltyListener.java**
   - Detects unpurified enchantments
   - Tracks when impure items are used
   - Applies virtue penalties
   - Sends warning messages

### Modified Files

1. **HalalCraft.java**
   - Added `/upgrade` command handler
   - Registered new listeners
   - Added getter methods for NBT keys
   - Made `saveVirtueData()` public

2. **UpgradeListener.java**
   - Already existed, handles GUI display
   - Works with new book application system

3. **config.yml**
   - Added complete upgrade system configuration
   - Added enchantment definitions
   - Added sound settings
   - Added messages

4. **plugin.yml**
   - Added `/upgrade` command definition

---

## Technical Details

### NBT Tags Used

- `HARAM_KEY`: Marks items with unpurified enchantments
- `PURIFIED_KEY`: Marks items with purified enchantments from upgrade system
- `UPGRADE_BOOK_KEY`: Identifies enchantment books from /upgrade
- `ENCHANT_NAME_KEY`: Stores enchantment name for application
- `ENCHANT_LEVEL_KEY`: Stores enchantment level
- `UPGRADE_COST_KEY`: Stores original virtue cost

### Event Listeners

1. **PlayerInteractEvent**: Detects when book is applied to tool
2. **EntityDamageByEntityEvent**: Detects weapon use (impure penalty)
3. **BlockBreakEvent**: Detects tool use (impure penalty)
4. **InventoryClickEvent**: GUI interaction for book purchase

### Cooldowns

- **Book Application**: 500ms cooldown prevents spam
- **Impure Penalty**: 1 second cooldown limits repeated losses

---

## Gameplay Balance

### Suggested Virtue Costs
- Common enchantments (Knockback): 30-40 virtue
- Uncommon (Efficiency, Looting): 45-50 virtue
- Rare (Sharpness, Fortune): 50-55 virtue
- Epic (Protection, Unbreaking): 40-60 virtue

### Penalty System
- Impure enchantments: -10 virtue per use (configurable)
- Encourages use of upgrade system instead of vanilla table
- Creates meaningful consequence for "black magic"

---

## Future Enhancements

Possible additions:
1. **Reforge System**: Convert impure to pure enchantments at an anvil
2. **Enchantment Levels**: Progressive unlocking of higher tier enchantments
3. **Limited Books**: Daily/weekly enchantment book allowance
4. **Rarity System**: Color-coded enchantment tiers
5. **Combined Effects**: Apply multiple books to same item

---

## Troubleshooting

### Enchantment Not Applying
- Check player has right-clicked with book in hand
- Verify target item is in off-hand or main-hand
- Ensure target is a valid tool/armor material

### Virtue Not Deducting
- Check config virtue amounts are set correctly
- Verify `saveVirtueData()` is being called
- Check player has enough virtue in first place

### Impure Penalty Not Triggering
- Ensure item has enchantments
- Check item is NOT marked as purified
- Verify EntityDamageByEntityEvent is firing
- Look for 1-second cooldown between penalties

---

## Installation

1. Replace old HalalCraft jar with new compiled version
2. Update `config.yml` with new upgrade configuration
3. Restart server
4. Players can immediately use `/upgrade` command

No migrations needed - system auto-creates new NBT tags as needed.
