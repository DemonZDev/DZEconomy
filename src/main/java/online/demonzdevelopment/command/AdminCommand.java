package online.demonzdevelopment.command;

import online.demonzdevelopment.DZEconomy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final DZEconomy plugin;

    public AdminCommand(DZEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dzeconomy.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("general.no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "debug" -> handleDebug(sender);
            default -> showHelp(sender);
        }
        
        return true;
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessage("admin.reload"));
        plugin.reload();
        sender.sendMessage(plugin.getMessageManager().getMessage("general.reload-success"));
    }

    private void handleDebug(CommandSender sender) {
        boolean currentDebug = plugin.getConfigManager().isDebugMode();
        plugin.getConfig().set("debug", !currentDebug);
        plugin.saveConfig();
        plugin.getConfigManager().load();
        
        if (!currentDebug) {
            sender.sendMessage(plugin.getMessageManager().getMessage("admin.debug-enabled"));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("admin.debug-disabled"));
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessage("admin.help.header"));
        sender.sendMessage(plugin.getMessageManager().getMessage("admin.help.reload"));
        sender.sendMessage(plugin.getMessageManager().getMessage("admin.help.debug"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "debug");
        }
        return Collections.emptyList();
    }
}