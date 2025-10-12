package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;
import org.bukkit.command.PluginCommand;

public class CommandRegistrar {
    private final DZEconomy plugin;

    public CommandRegistrar(DZEconomy plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        registerCurrencyCommand("money", CurrencyType.MONEY);
        registerCurrencyCommand("mobcoin", CurrencyType.MOBCOIN);
        registerCurrencyCommand("gem", CurrencyType.GEM);
        
        PluginCommand economyCmd = plugin.getCommand("economy");
        if (economyCmd != null) {
            EconomyCommand economyCommand = new EconomyCommand(plugin);
            economyCmd.setExecutor(economyCommand);
            economyCmd.setTabCompleter(economyCommand);
        }
        
        PluginCommand adminCmd = plugin.getCommand("dzeconomy");
        if (adminCmd != null) {
            AdminCommand adminCommand = new AdminCommand(plugin);
            adminCmd.setExecutor(adminCommand);
            adminCmd.setTabCompleter(adminCommand);
        }
    }

    private void registerCurrencyCommand(String name, CurrencyType type) {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            AbstractCurrencyCommand currencyCommand = switch (type) {
                case MONEY -> new MoneyCommand(plugin);
                case MOBCOIN -> new MobCoinCommand(plugin);
                case GEM -> new GemCommand(plugin);
            };
            command.setExecutor(currencyCommand);
            command.setTabCompleter(currencyCommand);
        }
    }
}