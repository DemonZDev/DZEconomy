package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;

/**
 * Money currency command handler
 */
public class MoneyCommand extends BaseCurrencyCommand {
    
    public MoneyCommand(DZEconomy plugin) {
        super(plugin, CurrencyType.MONEY);
    }
}