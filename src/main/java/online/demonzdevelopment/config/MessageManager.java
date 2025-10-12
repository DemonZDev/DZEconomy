package online.demonzdevelopment.config;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final DZEconomy plugin;
    private FileConfiguration messages;
    private String prefix;

    public MessageManager(DZEconomy plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = messages.getString("prefix", "&8[&aDZEconomy&8] ");
    }

    public String getMessage(String path) {
        String message = messages.getString(path, "");
        return MessageUtil.colorize(message.replace("{prefix}", prefix));
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String getPrefix() {
        return MessageUtil.colorize(prefix);
    }
}