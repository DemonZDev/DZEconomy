package online.demonzdevelopment.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtil {
    public static String colorize(String message) {
        if (message == null) return "";
        return message.replace("&", "§");
    }

    public static Component toComponent(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    public static String stripColor(String message) {
        if (message == null) return "";
        return message.replaceAll("§[0-9a-fk-or]", "");
    }
}