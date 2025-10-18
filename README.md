DZEconomy - Advanced Multi-Currency Economy for PaperMC

Revolutionize Your Server's Economy

Tired of basic, single-currency economy plugins that limit your server's potential? DZEconomy is here to transform your economic landscape with a sophisticated, multi-currency system designed specifically for modern PaperMC servers (1.21.1). Whether you're running a complex RPG server, a competitive PvP network, or a vibrant trading hub, DZEconomy provides the robust foundation your server deserves.

🏦 Three Distinct Economies, One Powerful Plugin

DZEconomy introduces three fully independent currency systems that can operate separately or interact seamlessly:

· 💰 Money - Your primary currency for everyday transactions, trading, and server economy
· ⚔️ MobCoin - Combat-focused currency earned through PvE activities and mob hunting
· 💎 Gems - Premium currency for high-value transactions and exclusive items

Each currency maintains its own balance system, commands, and economic rules while allowing players to exchange between them through a sophisticated conversion system.

🎯 Advanced Features for Modern Servers

Rank-Based Economic Progression

Integrated deeply with LuckPerms, DZEconomy allows you to create tiered economic systems where higher-ranked players enjoy:

· Increased transaction limits
· Reduced taxes on transfers
· Shorter cooldowns between transactions
· Special currency bonuses and perks

Dynamic Player Interactions

Go beyond simple payments with our comprehensive transaction system:

· Send funds directly to other players
· Request payments with accept/deny functionality
· Secure transactions with configurable cooldowns and limits
· Transaction logging for administrative oversight

Thrilling PvP Economy

Intensify player combat with our full PvP economy implementation:

· Players drop all currencies on death
· Victors receive the spoils directly
· Creates high-stakes PvP encounters
· Configurable for different game modes

Rewarding PvE Gameplay

Transform mob hunting into an economic activity:

· Custom MobCoin rewards for every mob type
· Special bonuses for boss mobs
· Configurable reward rates in mob-rewards.yml
· Encourages diverse gameplay activities

🔌 Seamless Integrations

PlaceholderAPI Support

Display economic information anywhere with our comprehensive placeholders:

· %dz_money%, %dz_mobcoin%, %dz_gem% for currency balances
· %dz_rank% for player's economic rank
· Perfect for scoreboards, chat, and GUI displays

Developer-Friendly API

Build upon DZEconomy with our full asynchronous API:

```java
// Easy integration for other plugins
economy.depositAsync(player, CurrencyType.MONEY, amount, "Quest Reward");
economy.getBalanceAsync(player, CurrencyType.MOBCOIN);
```

⚙️ Complete Customization & Control

Extensive Configuration Options

· config.yml - Currency settings, conversion rates, database configuration
· ranks.yml - Rank-based limits, taxes, cooldowns, and bonuses
· mob-rewards.yml - Mob-specific rewards and bonus configurations
· messages.yml - Fully customizable messages with color codes

Flexible Storage Solutions

Choose the database backend that fits your needs:

· YAML flat-file storage for smaller servers
· MySQL support for large networks and cross-server synchronization
· Async operations for optimal performance

🎮 Player-Friendly Commands

Intuitive Command Structure

· /money balance|send|request|accept|deny - Manage Money currency
· /mobcoin balance|send|request|accept|deny - Handle MobCoin transactions
· /gem balance|send|request|accept|deny - Control Gem currency
· /economy <from> <to> <amount> - Convert between currencies
· /dzeconomy reload|debug - Administrative controls

🚀 Quick Start Guide

Installation

1. Download DZEconomy.jar from Modrinth
2. Place in your server's plugins/ folder
3. Restart your server
4. Configure the YAML files to match your server's needs
5. (Optional) Install PlaceholderAPI and LuckPerms for full functionality

Basic Configuration

After first run, customize:

· Currency names and symbols in config.yml
· Rank benefits and limits in ranks.yml
· Mob rewards in mob-rewards.yml
· Server messages in messages.yml

💻 For Developers

DZEconomy offers a comprehensive, fully-asynchronous API that allows other plugins to seamlessly integrate with our economic system:

```java
// Get the API instance
EconomyService economy = Bukkit.getServicesManager().getRegistration(EconomyService.class).getProvider();

// Perform async transactions
economy.depositAsync(playerUUID, CurrencyType.MONEY, amount, "Quest Reward")
    .thenAccept(result -> {
        if (result.isSuccess()) {
            // Handle success
        }
    });
```

🛡️ Reliable & Production-Ready

Built with stability and performance in mind:

· Full async operations to prevent server lag
· Comprehensive error handling and logging
· SQL injection protection and secure coding practices
· Regular updates and active maintenance
· Thorough testing on PaperMC 1.21.1

📊 Perfect For Various Server Types

· RPG Servers - Use different currencies for different aspects of your game (combat, trading, premium content)
· PvP Networks - High-stakes economy with full currency drops on death
· MINIGAME NETWORKS - Reward players across different game modes with appropriate currencies
· SURVIVAL ECONOMIES - Create complex trading systems with currency conversion
· HYBRID SERVERS - Support multiple gameplay styles within one economic framework

🔒 Security & Permissions

Comprehensive permission system ensures proper access control:

· dzeconomy.default - Basic player permissions
· dzeconomy.add - Add currency to players (admin)
· dzeconomy.admin - Full administrative access
· Rank-specific permissions via LuckPerms integration

🌟 Why Choose DZEconomy?

Unlike other economy plugins that offer basic functionality, DZEconomy provides:

· True multi-currency support with independent economies
· Deep LuckPerms integration for progressive economic systems
· Comprehensive PvP and PvE economic integration
· Full async API for developers
· Extensive customization options
· Active development and community support

📞 Support & Community

Need help? Have suggestions? Join our community!

· GitHub Repository for bug reports and feature requests
· Regular updates with new features and improvements
· Active development with responsive support

---

Transform your server's economy today with DZEconomy - the professional-grade economic solution for modern Minecraft servers.

All rights reserved © DemonzDevelopment 2024
1. Download DZEconomy.jar
2. Place in your server's `plugins/` folder
3. Restart the server
4. Configure `config.yml`, `ranks.yml`, `mob-rewards.yml`, and `messages.yml`
5. (Optional) Install PlaceholderAPI and LuckPerms for full functionality

## Commands

### Money Commands
- `/money balance` - Check your money balance
- `/money send <player> <amount>` - Send money to another player
- `/money request <player> <amount>` - Request money from another player
- `/money accept` - Accept a pending request
- `/money deny` - Deny a pending request
- `/money add <player> <amount>` - Add money (admin only)
- `/money help` - Show help menu

### MobCoin Commands
Same structure as money commands, use `/mobcoin` instead.

### Gem Commands
Same structure as money commands, use `/gem` instead.

### Economy Commands
- `/economy <from> <to> <amount>` - Convert between currencies
  - Example: `/economy money mobcoin 1000`

### Admin Commands
- `/dzeconomy reload` - Reload configuration
- `/dzeconomy debug` - Toggle debug mode

## Placeholders

- `%dz_money%` - Player's money balance
- `%dz_mobcoin%` - Player's MobCoin balance
- `%dz_gem%` - Player's Gem balance
- `%dz_rank%` - Player's economy rank

## Configuration

### config.yml
Configure currencies, conversion rates, database settings, and display format.

### ranks.yml
Define rank-based limits, taxes, cooldowns, and bonuses per currency.

### mob-rewards.yml
Set MobCoin rewards for different mob categories.

### messages.yml
Customize all plugin messages with color codes.

## Developer API

### Getting the API

```java
import online.demonzdevelopment.api.EconomyService;
import org.bukkit.Bukkit;

EconomyService economyAPI = Bukkit.getServicesManager().getRegistration(EconomyService.class).getProvider();
```

### Example Usage

```java
import online.demonzdevelopment.api.EconomyService;
import online.demonzdevelopment.api.TransactionResult;
import online.demonzdevelopment.currency.CurrencyType;
import java.math.BigDecimal;
import java.util.UUID;

public class MyPlugin extends JavaPlugin {
    private EconomyService economy;
    
    @Override
    public void onEnable() {
        RegisteredServiceProvider<EconomyService> rsp = 
            Bukkit.getServicesManager().getRegistration(EconomyService.class);
        
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }
    
    public void rewardPlayer(UUID player, BigDecimal amount) {
        economy.depositAsync(player, CurrencyType.MONEY, amount, "Quest Reward")
            .thenAccept(result -> {
                if (result.isSuccess()) {
                    getLogger().info("Rewarded player with " + amount);
                }
            });
    }
    
    public void checkBalance(UUID player) {
        economy.getBalanceAsync(player, CurrencyType.MONEY)
            .thenAccept(balance -> {
                getLogger().info("Player balance: " + balance);
            });
    }
}
```

## Permissions

- `dzeconomy.default` - Default rank permissions (default: true)
- `dzeconomy.add` - Add currency to players (default: op)
- `dzeconomy.admin` - Access admin commands (default: op)

## Support

For support, feature requests, or bug reports, please visit our GitHub repository.
