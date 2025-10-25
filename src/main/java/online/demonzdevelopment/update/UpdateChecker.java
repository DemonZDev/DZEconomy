package online.demonzdevelopment.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import online.demonzdevelopment.DZEconomy;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Professional GitHub Release API-based update checker
 * 
 * Checks for updates asynchronously without blocking the main thread
 * 
 * @author DemonZ Development
 * @version 1.1.1
 */
public class UpdateChecker {
    
    private static final String GITHUB_API_URL = "https://api.github.com/repos/DemonZDev/DZEconomy/releases/latest";
    private static final int TIMEOUT_SECONDS = 5;
    
    private final DZEconomy plugin;
    private String latestVersion;
    private String downloadUrl;
    private boolean updateAvailable;
    private boolean checkComplete;
    
    public UpdateChecker(DZEconomy plugin) {
        this.plugin = plugin;
        this.updateAvailable = false;
        this.checkComplete = false;
    }
    
    /**
     * Check for updates asynchronously
     */
    public CompletableFuture<Boolean> checkForUpdates() {
        if (!plugin.getConfigManager().getConfig().getBoolean("update-checker.enabled", true)) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_SECONDS * 1000);
                connection.setReadTimeout(TIMEOUT_SECONDS * 1000);
                connection.setRequestProperty("User-Agent", "DZEconomy-UpdateChecker");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                    
                    // Extract version from tag_name (format: v1.0.0 or 1.0.0)
                    String tagName = jsonResponse.get("tag_name").getAsString();
                    latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                    
                    // Get download URL
                    if (jsonResponse.has("html_url")) {
                        downloadUrl = jsonResponse.get("html_url").getAsString();
                    } else {
                        downloadUrl = "https://github.com/DemonZDev/DZEconomy/releases/latest";
                    }
                    
                    // Compare versions
                    String currentVersion = plugin.getDescription().getVersion();
                    updateAvailable = isNewerVersion(currentVersion, latestVersion);
                    checkComplete = true;
                    
                    return updateAvailable;
                } else {
                    plugin.getLogger().warning("Failed to check for updates: HTTP " + responseCode);
                    checkComplete = true;
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Update checker failed: " + e.getMessage());
                checkComplete = true;
                return false;
            }
        });
    }
    
    /**
     * Log update status to console with color-coded messages
     */
    public void logUpdateStatus() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!checkComplete) {
                    return;
                }
                
                String currentVersion = plugin.getDescription().getVersion();
                
                if (updateAvailable) {
                    plugin.getLogger().info("=====================================");
                    plugin.getLogger().warning("UPDATE AVAILABLE!");
                    plugin.getLogger().info("Current version: " + currentVersion);
                    plugin.getLogger().info("Latest version: " + latestVersion);
                    plugin.getLogger().info("Download: " + downloadUrl);
                    plugin.getLogger().info("=====================================");
                } else {
                    plugin.getLogger().info("DZEconomy is up to date! (v" + currentVersion + ")");
                }
                
                cancel();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    /**
     * Get formatted update message for admins
     */
    public String getUpdateMessage() {
        if (!checkComplete || !updateAvailable) {
            return null;
        }
        
        String currentVersion = plugin.getDescription().getVersion();
        return "§8[§6DZ§eEconomy§8] §e§lUPDATE AVAILABLE! §7Current: §cv" + currentVersion + 
               " §7→ §aLatest: §av" + latestVersion + " §7- §b" + downloadUrl;
    }
    
    /**
     * Compare semantic versions (1.0.0 format)
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");
            
            int maxLength = Math.max(currentParts.length, latestParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse version numbers: " + e.getMessage());
            return false;
        }
    }
    
    // Getters
    
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public boolean isCheckComplete() {
        return checkComplete;
    }
}