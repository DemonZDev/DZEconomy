package online.demonzdevelopment.util;

import online.demonzdevelopment.DZEconomy;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for message handling with placeholder replacement
 */
public class MessagesUtil {
    
    private final DZEconomy plugin;
    
    public MessagesUtil(DZEconomy plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get a message from messages.yml
     */
    public String getMessage(String path) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();
        String message = messages.getString(path, "&cMessage not found: " + path);
        return ColorUtil.translate(message);
    }
    
    /**
     * Get a message with placeholder replacement
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        // Replace prefix
        String prefix = getMessage("prefix");
        message = message.replace("{prefix}", prefix);
        
        // Replace placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
    
    /**
     * Quick method to create placeholder map
     */
    public static Map<String, String> placeholders(Object... keyValuePairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
            map.put(keyValuePairs[i].toString(), keyValuePairs[i + 1].toString());
        }
        return map;
    }
}