package online.demonzdevelopment.dzeconomy.util;

import org.bukkit.ChatColor;

/**
 * Utility class for translating color codes
 */
public class ColorUtil {
    
    /**
     * Translate color codes using & as the color code character
     */
    public static String translate(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Translate multiple lines
     */
    public static String[] translate(String... lines) {
        String[] translated = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            translated[i] = translate(lines[i]);
        }
        return translated;
    }
    
    /**
     * Strip all color codes from text
     */
    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(translate(text));
    }
}