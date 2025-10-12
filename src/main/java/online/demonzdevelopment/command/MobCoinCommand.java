package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.currency.CurrencyType;

public class MobCoinCommand extends AbstractCurrencyCommand {
    public MobCoinCommand(DZEconomy plugin) {
        super(plugin, CurrencyType.MOBCOIN);
    }
}