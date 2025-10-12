package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;

public class MoneyCommand extends AbstractCurrencyCommand {
    public MoneyCommand(DZEconomy plugin) {
        super(plugin, CurrencyType.MONEY);
    }
}