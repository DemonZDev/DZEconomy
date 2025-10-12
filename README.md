# DZEconomy

A comprehensive multi-currency economy plugin for PaperMC 1.21.1 servers.

## Features

- **Three Independent Currencies**: Money, MobCoin, and Gem
- **Rank-Based Permissions**: Integration with LuckPerms for tiered benefits
- **Transaction System**: Send, request, accept/deny with cooldowns and limits
- **Currency Conversion**: Exchange between currencies with configurable rates
- **Mob Rewards**: Earn MobCoins from killing mobs with boss bonuses
- **PvP Economy**: Transfer all currencies on player death
- **PlaceholderAPI Support**: Display balances in other plugins
- **Database Support**: MySQL or flat-file storage
- **Developer API**: Full async API for third-party integration

## Installation

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

## License

All rights reserved © DemonzDevelopment 2024