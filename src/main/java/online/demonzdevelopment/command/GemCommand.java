package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;

public class GemCommand extends AbstractCurrencyCommand {
    public GemCommand(DZEconomy plugin) {
        super(plugin, CurrencyType.GEM);
    }
}