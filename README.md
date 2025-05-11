# DZEconomy
![IMG-20250419-WA0007](https://github.com/user-attachments/assets/e830d283-8c95-41a4-bd20-939ada75d50b)
|Support|Version|
|---|---|
|PaperMC API|1.21.1|
|LuckPerms API|Latest|
|PlaceholderAPI|Latest|

|No Support|Version|
|---|---|
|Vault|Any|
|SpigotMC|Any|

|Project Dependencies|Version|
|---|---|
|Kotlin|1.8.21|
|Gradle|8.7|
|DZEconomy|1.0.0|

|Server Dependencies|Version|
|---|---|
|PaperMC|1.21.1|
|PlaceholderAPI|Latest|
|LuckPermsHook|Latest|

---
## Table of Contents
- [Overview](#overview)
- [Currencies System](#currencies-system)
- [Command System](#command-system)
- 
---
## Overview
DZEconomy is a comprehensive multi-currency economy plugin for PaperMC servers providing advanced economic infrastructure. The plugin offers three different currencies, rank-based permissions and limitations, mob rewards, and an extensive configuration system.

---
## Currencies System
DZEconomy provides three different currencies, each with its own value and purpose:
|Currency|Description|Initial Amount|Acquisition|
|---|---|---|---|
|Money|Standard currency similar to Vault|50,000|Player transactions, admin commands|
|MobCoin|Special currency earned by killing mobs|500|Mob kills, player transactions, admin commands|
|Gems|Premium high-value currency|5|Player transactions, admin commands|
### Currency Conversion
Players can convert between currencies using the economy conversion command. The default exchange rates are:
- 1 Gem = 100 MobCoin = 10,000 Money
These rates are fully configurable in the plugin configuration.

---
## Command System
### Money Commands
|Command|Description|Implementation Details|
|---|---|---|
|/money balance|Checks the balance of the player|Displays the player's current Money balance in a formatted message|
|/money send <player_name> <amount>|Sends Money to another player|Verifies both player UUIDs Exist, Ensures sender is online, Verifies sender has sufficient funds (amount + tax), Applies rank-based tax (default: 5%, example: 2%), Enforces daily transaction limits (default: 5 times/day, example: 10 times/day), Applies cooldown period (default: 300s, example: 150s), Sends notification to both parties, Automatically shows updated balance to both players|
|/money request <player_name> <amount>|Requests Money from another player|Verifies both players exist and are online, Notifies requester confirmation message, Notifies requested player with accept/deny options, Sets 120-second timeout for request, If accepted, performs the same checks as /money send, If denied or timed out, cancels the request, Applies send limits, cooldowns, and taxes to the requested player|
|/money add <player_name> <amount>|Admin command to add Money to a player|Requires operator permission, Validates player UUID exists, Adds money to player's account from the economy, Notifies player of added amount, Shows updated balance automatically|
|/money accept or /money deny|Accept or Deny every Money pending request|Cheak if the player have any pending request, Cheak if the request is time out, Notify player about no pending request or time outed pending request|
|/money or /money help|Shows help menu for Money commands|Lists all money commands with descriptions, Shows admin commands only if player has operator permissions|
### MobCoin Commands
|Command|Description|
|---|---|
|/mobcoin balance|Checks the MobCoin balance of the player|
|/mobcoin send <player_name> <amount>|Sends MobCoins to another player (with same conditions as money send)|
|/mobcoin request <player_name> <amount>|Requests MobCoins from another player (with same conditions as money request)|
|/mobcoin add <player_name> |Admin command to add MobCoins to a player|
|/mobcoin accept or /mobcoin deny|Accept or Deny every MobCoin pending request|
|/mobcoin or /mobcoin help|Shows help menu for MobCoin commands|
### Gem Commands
|Command|Description|
|---|---|
|/gem balance|Checks the Gem balance of the player|
|/gem send <player_name> <amount>|Sends Gems to another player (with same conditions as money send)|
|/gem request <player_name> <amount>|Requests Gems from another player (with same conditions as money request)|
|/gem add <player_name> <amount>|Admin command to add Gems to a player|
|/gem accept or /gem deny|Accept or Deny every Gem pending request|
|/gem or /gem help|Shows help menu for Gem commands|
### Economy Commands
|Command|Description|Implementation Details|
|--|---|---|
|/economy <change_currency> <get_currency> <amount>|Converts one currency type to another|Valid currency types: money, mobcoin, gem, Verifies player has sufficient funds of source currency, Applies rank-based conversion tax, Uses configured exchange rates, Notifies player of conversion results|

---
## Rank System
The rank system provides different economic benefits based on player permissions.

---
## Mob Reward System
Players receive MobCoins when killing mobs, with different rewards based on mob difficulty
### Rank-Based Boss Kill Bonuses
Players receive additional rewards when killing boss mobs based on their rank:
- Default rank: 0% bonus (50 MobCoins total)
- Example rank: 5% bonus (52.5 MobCoins total)

---
## PVP Economy
When a player kills another player, the killer will receive all of the killed player's money, MobCoins, and Gems. This creates a high-risk, high-reward PVP system.

---
## Modern Features
### Case-Insensitive Commands
Commands work regardless of capitalization. For example, both /money send and /moNEy SenD will work the same.
### Decimal Limitations
Currency values are limited to two decimal places for cleaner display and easier management. For example, if a player has 3.46473873833833 Gems, only 3.46 Gems will be processed and displayed.
### Short Form Notation
Large numbers are automatically displayed in short form notation for cleaner UI in leaderboards and messages:
| Display | Value | Full Number | Name |
|---------|-------|-------------|------|
| 1K      | 10^3  | 1,000 | One thousand |
| 10K     | 10^4  | 10,000 | Ten thousand |
| 100K    | 10^5  | 100,000 | One hundred thousand |
| 1M      | 10^6  | 1,000,000 | One million |
| 10M     | 10^7  | 10,000,000 | Ten million |
| 100M    | 10^8  | 100,000,000 | One hundred million |
| 1B      | 10^9  | 1,000,000,000 | One billion |
| 10B     | 10^10 | 10,000,000,000 | Ten billion |
| 100B    | 10^11 | 100,000,000,000 | One hundred billion |
| 1T      | 10^12 | 1,000,000,000,000 | One trillion |
| 10T     | 10^13 | 10,000,000,000,000 | Ten trillion |
| 100T    | 10^14 | 100,000,000,000,000 | One hundred trillion |
| 1Q      | 10^15 | 1,000,000,000,000,000 | One quadrillion |
| 1Qi     | 10^18 | 1,000,000,000,000,000,000 | One quintillion |
| 1S      | 10^21 | 1,000,000,000,000,000,000,000 | One sextillion |
| 1Sp     | 10^24 | 1,000,000,000,000,000,000,000,000 | One septillion |
| 1O      | 10^27 | 1,000,000,000,000,000,000,000,000,000 | One octillion |
| 1N      | 10^30 | 1,000,000,000,000,000,000,000,000,000,000 | One nonillion |
| 1D      | 10^33 | 1,000,000,000,000,000,000,000,000,000,000,000 | One decillion |
| 1Ud     | 10^36 | 1,000,000,000,000,000,000,000,000,000,000,000,000 | One undecillion |
| 1Dd     | 10^39 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000 | One duodecillion |
| 1Td     | 10^42 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One tredecillion |
| 1Qd     | 10^45 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One quattuordecillion |
| 1Qnd    | 10^48 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One quindecillion |
| 1Sd     | 10^51 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One sexdecillion |
| 1Spd    | 10^54 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One septendecillion |
| 1Od     | 10^57 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One octodecillion |
| 1Nd     | 10^60 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One novemdecillion |
| 1V      | 10^63 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One vigintillion |
| 1Uv     | 10^66 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One unvigintillion |
| 1Dv     | 10^69 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One duovigintillion |
| 1Tv     | 10^72 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One tresvigintillion |
| 1Qv     | 10^75 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One quattuorvigintillion |
| 1Qnv    | 10^78 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One quinvigintillion |
| 1Sv     | 10^81 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One sexvigintillion |
| 1Spv    | 10^84 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One septenvigintillion |
| 1Ov     | 10^87 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One octovigintillion |
| 1Nv     | 10^90 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One novemvigintillion |
| 1Tr      | 10^93 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One trigintillion |
| 1Utr     | 10^96 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One untrigintillion |
| 1Dtr     | 10^99 | 1,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One duotrigintillion |
| 1G      | 10^100| 10,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 | One Google (Googol) |
### Short Form Notation Examples:
- Player with 6,000 money: Displays as 6K
- Player with 6,100 money: Displays as 6.1K
- Player with 6,001 money: Displays as 6K (decimal limitation)
- Player with 6,703.35732 money: Displays as 6.70K (decimal limitation)

---
## Integration
### PlaceholderAPI Support
The plugin provides the following placeholders for use with PlaceholderAPI:
- %dz_money% - Player's money balance
- %dz_mobcoin% - Player's MobCoin balance
- %dz_gem% - Player's Gem balance
### LuckPerms Integration
The plugin integrates with LuckPerms for permission-based rank assignment. Players will receive economic benefits based on their highest applicable rank permission.

---
## Storage System
The plugin uses FlatFileStorageProvider for data storage. No external database is required. Player data is stored efficiently with PlayerDataManager optimization.

---
## Configuration
When the plugin is loaded for the first time, it creates a configuration directory with several YAML files:
### config.yml
```yaml
debug: false
auto-save-interval: 5
currencies:
  money: { enabled: true, new-player-bonus: 50000, symbol: "$", decimal-places: 2 }
  mobcoin: { enabled: true, new-player-bonus: 500, symbol: "⛂", decimal-places: 2 }
  gem: { enabled: true, new-player-bonus: 5, symbol: "♦", decimal-places: 2 }
conversion:
  enabled: true
  rates:
    money-to-mobcoin: 100
    mobcoin-to-money: 100
    mobcoin-to-gem: 100
    gem-to-mobcoin: 100
    money-to-gem: 10000
    gem-to-money: 10000
format:
  use-short-form: true
  decimal-limit: 2
hooks:
  placeholderapi: true
  luckperms: true
```
