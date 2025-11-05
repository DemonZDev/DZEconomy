# DZEconomy - Professional Multi-Currency Economy Plugin

![Version](https://img.shields.io/badge/version-1.2.0-brightgreen)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-blue)
![Java](https://img.shields.io/badge/java-21-orange)

A professional, production-ready multi-currency economy plugin for PaperMC servers featuring **Money**, **MobCoin**, and **Gems** with advanced rank-based systems, interactive request GUIs, PVP transfers, conversion mechanics, and comprehensive API integration.

## 🌟 Features

### **Multi-Currency System**
- **Money ($)** - Primary economy currency
- **MobCoin (MC)** - Earned by killing mobs
- **Gems (◆)** - Premium rare currency

### **Advanced Number Formatting**
- Automatic decimal truncation to 2 places
- Short form notation (1K, 1M, 1B, 1T, 1Q, etc.)
- Supports up to 10^100 (1 Googol)

### **Rank-Based Economy**
- Full LuckPerms integration
- Per-currency transfer tax rates
- Configurable cooldowns and daily limits
- Boss kill bonuses (MobCoin)
- Conversion tax rates

### **Currency Transfers**
- Player-to-player sending
- Request system with interactive GUI
- 120-second request timeout
- Tax system based on sender's rank
- Daily send/request limits

### **Interactive Request GUI**
- Real-time countdown display
- Accept/Deny buttons
- Respects existing open inventories
- Configurable sounds and intervals

### **Mob Kill Rewards**
- Neutral mobs: 1 MC
- Easy hostile: 2 MC
- Hard hostile: 4 MC
- Boss mobs: 50 MC + rank bonus

### **PVP Economy**
- Transfer all currencies on player kill
- Optional broadcast for large transfers
- Configurable per-currency

### **Currency Conversion**
- Bidirectional conversion between all currencies
- Configurable exchange rates
- Rank-based conversion tax

### **Storage Options**
- **FlatFile** (YAML) - Default
- **SQLite** - Local database
- **MySQL** - Remote database with HikariCP pooling

### **PlaceholderAPI Integration**
- `%dz_money%` - Short form balance
- `%dz_money_full%` - Full balance
- `%dz_money_formatted%` - With symbol
- `%dz_mobcoin%`, `%dz_gem%` - Same format
- `%dz_rank%` - Player's rank display name
- `%dz_rank_priority%` - Rank priority

### **Public API**
Complete API for third-party plugin integration via Bukkit ServicesManager

---

## 📦 Installation

1. **Download** DZEconomy JAR file
2. **Place** in your server's `plugins` folder
3. **Install dependencies**:
   - [LuckPerms](https://luckperms.net/) (Required)
   - [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) (Optional)
4. **Restart** your server
5. **Configure** files in `plugins/DZEconomy/`

---

## 🎮 Commands

### **Money Commands**
```
/money balance [player]          - Check balance
/money send <player> <amount>    - Send money
/money request <player> <amount> - Request money
/money accept                    - Accept pending request
/money deny                      - Deny pending request
/money add <player> <amount>     - Add money (Admin)
/money help                      - Show help menu
```
**Aliases:** `/bal`, `/balance`

### **MobCoin Commands**
```
/mobcoin balance [player]          - Check balance
/mobcoin send <player> <amount>    - Send mobcoins
/mobcoin request <player> <amount> - Request mobcoins
/mobcoin accept                    - Accept pending request
/mobcoin deny                      - Deny pending request
/mobcoin add <player> <amount>     - Add mobcoins (Admin)
```
**Aliases:** `/mc`, `/mobcoins`

### **Gem Commands**
```
/gem balance [player]          - Check balance
/gem send <player> <amount>    - Send gems
/gem request <player> <amount> - Request gems
/gem accept                    - Accept pending request
/gem deny                      - Deny pending request
/gem add <player> <amount>     - Add gems (Admin)
```
**Aliases:** `/gems`

### **Economy Commands**
```
/economy convert <from> <to> <amount> - Convert currencies
/economy version                      - Check plugin version and updates
/economy reload                       - Reload configuration (Admin)
```
**Example:** `/economy convert money gem 10000`
**Aliases:** `/eco`, `/dzeco`

---

## 🔑 Permissions

### **Money Permissions**
- `dzeconomy.money.balance` - Check own balance
- `dzeconomy.money.balance.others` - Check others' balance
- `dzeconomy.money.send` - Send money
- `dzeconomy.money.request` - Request money
- `dzeconomy.money.accept` - Accept requests
- `dzeconomy.money.deny` - Deny requests

### **MobCoin Permissions**
- `dzeconomy.mobcoin.*` - Same structure as money

### **Gem Permissions**
- `dzeconomy.gem.*` - Same structure as money

### **Economy Permissions**
- `dzeconomy.economy.convert` - Convert currencies
- `dzeconomy.economy.version` - Check version
- `dzeconomy.admin` - Admin notifications (updates)
- `dzeconomy.admin.reload` - Reload plugin
- `dzeconomy.admin.update` - Use /dzeconomy update (Admin)
- `dzeconomy.admin.money.add` - Add money
- `dzeconomy.admin.mobcoin.add` - Add mobcoins
- `dzeconomy.admin.gem.add` - Add gems

### **Rank Permissions**
- `dzeconomy.default` - Default rank (granted by default)

---

## ⚙️ Configuration

### **config.yml**
Main plugin configuration for storage, currencies, display, PVP, GUI, integrations, and limits.

**Key Settings:**
```yaml
# Update checker
update-checker:
  enabled: true
  notify-on-join: true

storage:
  type: FLATFILE  # FLATFILE, SQLITE, MYSQL
  auto-save-interval: 5  # Minutes

currencies:
  money:
    symbol: "$"
    starting-balance: 50000.00
  mobcoin:
    symbol: "MC"
    starting-balance: 500.00
  gem:
    symbol: "◆"
    starting-balance: 5.00

conversion:
  enabled: true
  rates:
    gem-to-mobcoin: 100.0
    gem-to-money: 10000.0
    mobcoin-to-money: 100.0

pvp-economy:
  enabled: true
  transfer-money: true
  transfer-mobcoins: true
  transfer-gems: true
```

### **ranks.yml**
Define unlimited custom ranks with per-currency settings.

**Example Rank:**
```yaml
ranks:
  vip:
    display-name: "&a&lVIP"
    priority: 20
    money:
      transfer-tax: 1.0
      transfer-cooldown: 60
      daily-transfer-limit: 20
      daily-request-limit: 20
    mobcoin:
      transfer-tax: 1.0
      transfer-cooldown: 60
      daily-transfer-limit: 20
      daily-request-limit: 20
      boss-kill-bonus: 10.0
    gem:
      transfer-tax: 1.0
      transfer-cooldown: 60
      daily-transfer-limit: 20
      daily-request-limit: 20
    conversion:
      enabled: true
      tax: 0.5
```

### **mob-rewards.yml**
Configure mob kill rewards by category.

**Categories:**
- **Neutral** (1 MC): Passive animals
- **Easy** (2 MC): Common hostile mobs
- **Hard** (4 MC): Challenging hostile mobs
- **Boss** (50 MC): Ender Dragon, Wither, Warden

### **messages.yml**
Fully customizable messages with color codes and placeholders.

---

## 🔌 API Usage

### **Maven Dependency**
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.DemonZDev</groupId>
    <artifactId>DZEconomy</artifactId>
    <version>1.2.0</version>
    <scope>provided</scope>
</dependency>
```

### **Accessing the API**
```java
import online.demonzdevelopment.dzeconomy.api.DZEconomyAPI;
import online.demonzdevelopment.dzeconomy.currency.CurrencyType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

// Get API instance via ServicesManager
RegisteredServiceProvider<DZEconomyAPI> provider = Bukkit.getServicesManager()
    .getRegistration(DZEconomyAPI.class);

if (provider != null) {
    DZEconomyAPI api = provider.getProvider();
    // API is available
} else {
    // DZEconomy not found or not loaded
}
```

### **Example Operations**
```java
UUID player = playerObj.getUniqueId();

// Get balance
double balance = api.getBalance(player, CurrencyType.MONEY);

// Check balance
if (api.hasBalance(player, CurrencyType.MONEY, 1000.0)) {
    // Player has at least 1000 money
}

// Add currency
api.addCurrency(player, CurrencyType.MOBCOIN, 100.0);

// Remove currency
api.removeCurrency(player, CurrencyType.GEM, 5.0);

// Transfer between players
api.transferCurrency(sender, receiver, CurrencyType.MONEY, 500.0);

// Convert currencies
api.convertCurrency(player, CurrencyType.MONEY, CurrencyType.GEM, 10000.0);

// Get player rank
Rank rank = api.getPlayerRank(player);

// Format currency
String formatted = api.formatCurrency(1500.0, CurrencyType.MONEY);
// Returns: "$1.5K"
```

---

## 📊 Exchange Rates

**Default Rates:**
- 1 Gem = 100 MobCoins
- 1 Gem = 10,000 Money
- 1 MobCoin = 100 Money

All rates are fully configurable in `config.yml`.

---

## 🎯 Validation System

All transactions follow strict validation chains:

1. **UUID Validation** - Verify players exist
2. **Online Check** - Ensure required players are online
3. **Self-Prevention** - Block self-transactions
4. **Amount Validation** - Check amount > 0
5. **Balance Check** - Verify sufficient funds
6. **Tax Calculation** - Compute rank-based tax
7. **Total Check** - Verify amount + tax available
8. **Limit Check** - Ensure daily limits not exceeded
9. **Cooldown Check** - Verify cooldown expired

---

## 🗄️ Storage

### **FlatFile (Default)**
- Location: `plugins/DZEconomy/data/players/[UUID].yml`
- One file per player
- No external dependencies

### **SQLite**
- Location: `plugins/DZEconomy/data/economy.db`
- Local database file
- Better performance for large servers

### **MySQL**
- Remote database support
- HikariCP connection pooling
- Recommended for networks

Configure in `config.yml`:
```yaml
storage:
  type: MYSQL
  mysql:
    host: "localhost"
    port: 3306
    database: "dzeconomy"
    username: "root"
    password: "password"
```

---

## 🔄 Updater

Admin-only self-updater integrating GitHub Releases for DemonZDev/DZEconomy.

- Command: `/dzeconomy update <version|previous|next|latest|auto>`
- Permission: `dzeconomy.admin.update` (default: op)
- Target path: `plugins/update/DZEconomy-v{resolvedVersion}.jar`

### Modes
- latest: Fetch latest non-draft, non-prerelease, download the JAR asset
- version: Fetch exact tag v{version}; if already on that version, abort gracefully
- previous/next: List all releases, semver-sort by tag_name, pick neighbor relative to current version
- auto: Check latest vs current; download only if newer. Runs on command and optionally on server start

### Safety & Performance
- Fully async using Java 21 HttpClient with 5s connect/read timeouts
- Handles 403/429 with exponential backoff
- Verifies asset size and SHA-256 digest when provided by GitHub assets
- Never downgrades unless explicitly using previous
- Strips leading 'v' from tag_name and uses semver compare (major.minor.patch)

### Apply Strategy
- Primary: Save to update folder; restart required for apply (Paper/Spigot standard)
- Hot-reload (best-effort): Attempts safe disable, load JAR via PluginManager, then enable. If any step fails, falls back to restart-required flow

### Configuration (config.yml)
```yaml
updater:
  enabled: true           # Enable updater features and commands
  autoOnStart: false      # Auto-check and download newer release on server start
  attempt-hot-reload: false  # Try hot-reload; falls back to update folder on failure
```

### Acceptance
1. latest downloads newest non-draft, non-prerelease JAR
2. version fetches exact tag
3. previous/next select neighbors by semver
4. auto updates only when newer exists
5. Restart flow works via plugins/update
6. Hot-reload attempts gracefully degrade to restart-required if unsafe

---

## 🔧 Technical Details

- **Platform:** PaperMC/Bukkit 1.21.1
- **Java Version:** 21
- **Build System:** Maven
- **Dependencies:**
  - LuckPerms API 5.4
  - PlaceholderAPI 2.11.6
  - HikariCP 5.1.0 (bundled)
  - SQLite JDBC 3.45.0 (bundled)

---

## 📝 Notes

- All commands are **case-insensitive**
- Numbers support **K/M/B/T** suffixes (e.g., `1.5k = 1500`)
- Request GUI **respects existing open inventories**
- Daily limits reset at configured time (default: 00:00)
- Auto-save runs every 5 minutes by default
- PVP transfers are **instant** on kill

---

## 🐛 Support

For issues, questions, or feature requests:
- **Author:** DemonZ Development
- **Version:** 1.2.0
- **Website:** https://demonzdevelopment.online

### **API Integration Troubleshooting**
If other plugins can't find DZEconomy API:
1. Ensure DZEconomy is loaded **before** your plugin (add to `depend:` or `softdepend:` in plugin.yml)
2. Use the correct import: `import online.demonzdevelopment.dzeconomy.api.DZEconomyAPI;`
3. Check API registration: `Bukkit.getServicesManager().getRegistration(DZEconomyAPI.class)`
4. Verify DZEconomy v1.2.0+ is installed (older versions have different package structure)

---

## 📜 License

This plugin is proprietary software. All rights reserved.

---

## 🎉 Features Showcase

```
[DZEconomy] Welcome to the server, Steve!
[DZEconomy] You've received:
[DZEconomy]   • $50,000 Money
[DZEconomy]   • 500 MobCoins
[DZEconomy]   • 5 Gems

> /money send Alex 1000
[DZEconomy] Successfully sent $1,000 to Alex! Tax: $50

> /mobcoin request Steve 500
[DZEconomy] Request sent to Steve for 500 MC!

> /economy convert money gem 10000
[DZEconomy] Converted $10,000 to 1 Gem! Tax: $300

> Killed Zombie
[DZEconomy] Killed Zombie: +2 MobCoin

> Killed Ender Dragon
[DZEconomy] BOSS KILL! Killed Ender Dragon: +55 MobCoin (+10% rank bonus)
```

---

**Enjoy DZEconomy! 🎮💰**
