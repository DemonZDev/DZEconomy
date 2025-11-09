package online.demonzdevelopment.dzeconomy.command;

import online.demonzdevelopment.dzeconomy.DZEconomy;
import online.demonzdevelopment.dzeconomy.currency.CurrencyType;

/**
 * MobCoin currency command handler
 */
public class MobCoinCommand extends BaseCurrencyCommand {
    
    public MobCoinCommand(DZEconomy plugin) {
        super(plugin, CurrencyType.MOBCOIN);
    }
}