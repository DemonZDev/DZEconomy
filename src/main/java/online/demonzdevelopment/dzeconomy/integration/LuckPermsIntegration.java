package online.demonzdevelopment.dzeconomy.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import online.demonzdevelopment.dzeconomy.DZEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Integration with LuckPerms for rank detection
 */
public class LuckPermsIntegration {
    
    private final DZEconomy plugin;
    private final LuckPerms luckPerms;
    
    public LuckPermsIntegration(DZEconomy plugin) {
        this.plugin = plugin;
        this.luckPerms = getLuckPermsAPI();
    }
    
    /**
     * Get LuckPerms API instance
     */
    private LuckPerms getLuckPermsAPI() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            return provider.getProvider();
        }
        return null;
    }
    
    /**
     * Get a player's primary group
     */
    public String getPrimaryGroup(Player player) {
        if (luckPerms == null) {
            return null;
        }
        
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return null;
        }
        
        return user.getPrimaryGroup();
    }
    
    /**
     * Check if LuckPerms is available
     */
    public boolean isAvailable() {
        return luckPerms != null;
    }
}