# Offline Transaction System

## Overview

The Offline Transaction System allows buyers to purchase items from shops even when the shop owner is offline. Transactions are recorded and automatically applied when the shop owner logs back in.

## How It Works

### For Online Buyers

When a seller right-clicks a [Buy] sign and the buyer (shop owner) is **online**:
1. Transaction is processed immediately
2. Items transferred to chest
3. Virtue transferred instantly
4. Both parties notified

### For Offline Buyers

When a seller right-clicks a [Buy] sign and the buyer (shop owner) is **offline**:
1. Items are transferred to chest immediately
2. Transaction is recorded with:
   - Buyer name
   - Seller name
   - Virtue amount
   - Item details
   - Timestamp
   - Seller is notified: "Buyer is offline - they will receive virtue when they join"

### When Offline Player Joins

When the buyer (shop owner) joins the server:
1. Server detects pending transactions
2. For each valid transaction:
   - Virtue is deducted from buyer
   - Virtue is added to seller
   - Chat notification shows details
   - Transaction is removed from queue

## Security Features

To prevent exploits, the system includes several safeguards:

### 1. **Transaction Age Limit**
- Transactions older than 30 days are automatically rejected
- Prevents old stale transactions from being processed
- Logged as a warning if detected

### 2. **Seller Verification**
- Checks if seller has ever played on server
- Rejects transactions from non-existent players
- Prevents fake transactions from being processed

### 3. **Data Integrity**
- Transactions stored in config.yml with timestamps
- Each transaction includes complete metadata
- Audit trail for server administrators

### 4. **Offline Virtue Storage**
- Seller virtue is stored by UUID
- Works whether seller is online or offline
- Virtue properly saved to config

## Pending Transactions Format

Pending transactions are stored in `config.yml` under `pending-transactions`:

```yaml
pending-transactions:
  - buyer: "PlayerA"
    seller: "PlayerB"
    virtue: 50
    item: "Diamond"
    quantity: 64
    timestamp: 1705830840000
  - buyer: "PlayerC"
    seller: "PlayerD"
    virtue: 25
    item: "Iron Ingot"
    quantity: 32
    timestamp: 1705830900000
```

## Player Experience

### Seller Perspective (Online)

```
[Seller places item on [Buy] sign of offline buyer]
Message: "✓ Sold 64 Diamond for 50 virtue!"
Message: "(Buyer is offline - they will receive virtue when they join)"
```

### Buyer Perspective (Joining Later)

```
[Buyer joins server]
[After 1 tick, transactions are processed]

Message: "✓ Pending transaction processed:"
Message: "Purchased 64 Diamond from PlayerB for 50 virtue"
Message: "Total pending virtue processed: 50"

[Items are in the chest, ready to collect]
```

## Admin Commands

Monitor pending transactions by checking the config file:

```bash
# View pending transactions
vim plugins/HalalCraft/config.yml | grep -A 20 "pending-transactions"
```

## Configuration

Default settings in `config.yml`:

```yaml
# Pending transactions are stored automatically
# Transactions expire after 30 days
# You can manually clear old transactions from config.yml
```

## Best Practices

1. **For Shop Owners:**
   - Place at least one detector item when creating [Buy] signs
   - Ensure chest has adequate space
   - Log in regularly to process pending transactions
   - Keep track of pending sales

2. **For Sellers:**
   - Confirm purchase message shows if buyer is offline
   - Buyer will receive virtue automatically when they join
   - No need for follow-up actions

3. **For Server Admins:**
   - Monitor pending-transactions in config.yml
   - Remove suspicious transactions if needed
   - Clear old transactions (>30 days) periodically
   - Backup config regularly

## Troubleshooting

### Transaction Not Applied

**Check:**
- Is player still unable to access items after rejoin?
- Are there warnings in server logs?
- Is transaction older than 30 days?

**Solution:**
- Manually check config.yml for pending transactions
- Remove if transaction is corrupted or too old
- Player can ask admin to manually add virtue if needed

### Items in Chest But No Virtue

**Possible Cause:**
- Server crashed before transaction was saved

**Solution:**
- Use `/virtue add <player> <amount>` to manually credit virtue
- Ensure config is saved properly

### Transaction Processing Delays

**Cause:**
- Transactions processed on next tick after player joins
- Short delay before chat notification appears

**Solution:**
- This is normal - brief delay is intentional for system stability
- Transactions always process within 1-2 seconds of joining

## Technical Details

### Transaction Processing Flow

```
Player Joins
    ↓
TransactionListener detects PlayerJoinEvent
    ↓
Schedule processPendingTransactions on next tick
    ↓
Load pending-transactions from config.yml
    ↓
For each transaction:
  - Verify transaction age < 30 days
  - Verify seller has ever played
  - Deduct virtue from buyer
  - Add virtue to seller
  - Save config
  - Remove from pending queue
    ↓
Notify player of processed transactions
```

### Anti-Exploit Measures

1. **Timestamp Validation**
   - Transactions must have valid timestamp
   - Must be within 30 days

2. **Seller Verification**
   - Check `Bukkit.getOfflinePlayer(name).hasPlayedBefore()`
   - Only process if seller has played

3. **Data Immutability**
   - Transaction recorded before virtue transfer
   - Cannot create fake transactions

4. **UUID Safety**
   - Uses UUID for virtue storage
   - Prevents name-based exploits
   - Works even if player changes name (Mojang API)

## Future Enhancements

Planned improvements for future versions:

- [ ] Transaction history viewable by players
- [ ] Transaction rollback system for disputes
- [ ] Automatic refund for failed transactions
- [ ] Expiration notifications before 30-day limit
- [ ] Admin commands to view/manage transactions
- [ ] Transaction logging to database (optional)

---

**The Offline Transaction System ensures a smooth shop experience even when players aren't online at the same time! 🛒✨**
