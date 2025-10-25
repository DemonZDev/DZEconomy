package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;

/**
 * MobCoin currency command handler
 */
public class MobCoinCommand extends BaseCurrencyCommand {
    
    public MobCoinCommand(DZEconomy plugin) {
        super(plugin, CurrencyType.MOBCOIN);
    }
}