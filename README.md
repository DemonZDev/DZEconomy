# DZEconomy
![IMG-20250419-WA0007](https://github.com/user-attachments/assets/e830d283-8c95-41a4-bd20-939ada75d50b)

---

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
