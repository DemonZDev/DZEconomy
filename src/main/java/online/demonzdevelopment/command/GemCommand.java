package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;

/**
 * Gem currency command handler
 */
public class GemCommand extends BaseCurrencyCommand {
    
    public GemCommand(DZEconomy plugin) {
        super(plugin, CurrencyType.GEM);
    }
}