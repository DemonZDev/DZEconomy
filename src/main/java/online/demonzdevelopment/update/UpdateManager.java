package online.demonzdevelopment.update;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages plugin updates from GitHub Releases
 */
public class UpdateManager {
    
    private static final String GITHUB_OWNER = "DemonZDev";
    private static final String GITHUB_REPO = "DZEconomy";
    
    private final DZEconomy plugin;
    private final Logger logger;
    private final GitHubAPIClient apiClient;
    private final UpdateDownloader downloader;
    private final Path updateFolder;
    
    public UpdateManager(DZEconomy plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.apiClient = new GitHubAPIClient(GITHUB_OWNER, GITHUB_REPO, logger);
        this.downloader = new UpdateDownloader(logger);
        this.updateFolder = new File(plugin.getDataFolder().getParentFile(), "update").toPath();
    }
    
    /**
     * Get current plugin version
     */
    public SemanticVersion getCurrentVersion() {
        return SemanticVersion.parse(plugin.getDescription().getVersion());
    }
    
    /**
     * Update to latest version
     */
    public CompletableFuture<UpdateResult> updateToLatest(CommandSender sender) {
        sendMessage(sender, "&eChecking for latest version...");
        
        return apiClient.getLatestRelease()
                .thenCompose(release -> {
                    if (release.isDraft() || release.isPrerelease()) {
                        sendMessage(sender, "&cLatest release is a draft or prerelease!");
                        return CompletableFuture.completedFuture(
                                new UpdateResult(false, "Latest release is not stable", null));
                    }
                    
                    SemanticVersion latestVersion = SemanticVersion.parse(release.getVersion());
                    SemanticVersion currentVersion = getCurrentVersion();
                    
                    if (!latestVersion.isNewerThan(currentVersion)) {
                        sendMessage(sender, "&aYou are already on the latest version!");
                        return CompletableFuture.completedFuture(
                                new UpdateResult(false, "Already on latest version", latestVersion));
                    }
                    
                    sendMessage(sender, "&eFound update: &av" + latestVersion + " &7(current: v" + currentVersion + ")");
                    return downloadAndApply(sender, release, latestVersion);
                });
    }
    
    /**
     * Update to specific version
     */
    public CompletableFuture<UpdateResult> updateToVersion(CommandSender sender, String version) {
        sendMessage(sender, "&eSearching for version &av" + version + "&e...");
        
        SemanticVersion targetVersion;
        try {
            targetVersion = SemanticVersion.parse(version);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "&cInvalid version format! Use format: 1.2.3");
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "Invalid version format", null));
        }
        
        SemanticVersion currentVersion = getCurrentVersion();
        if (targetVersion.equals(currentVersion)) {
            sendMessage(sender, "&cYou are already on version v" + version + "!");
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "Already on this version", targetVersion));
        }
        
        return apiClient.getReleaseByTag(version)
                .thenCompose(release -> {
                    sendMessage(sender, "&eFound release: &a" + release.getName());
                    return downloadAndApply(sender, release, targetVersion);
                })
                .exceptionally(ex -> {
                    sendMessage(sender, "&cVersion not found: v" + version);
                    return new UpdateResult(false, "Version not found", null);
                });
    }
    
    /**
     * Update to previous version
     */
    public CompletableFuture<UpdateResult> updateToPrevious(CommandSender sender) {
        sendMessage(sender, "&eFinding previous version...");
        
        return findRelativeVersion(sender, -1);
    }
    
    /**
     * Update to next version
     */
    public CompletableFuture<UpdateResult> updateToNext(CommandSender sender) {
        sendMessage(sender, "&eFinding next version...");
        
        return findRelativeVersion(sender, 1);
    }
    
    /**
     * Auto-update if newer version available
     */
    public CompletableFuture<UpdateResult> autoUpdate() {
        if (!plugin.getConfigManager().getConfig().getBoolean("updater.enabled", true)) {
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "Auto-update disabled", null));
        }
        
        return apiClient.getLatestRelease()
                .thenCompose(release -> {
                    if (release.isDraft() || release.isPrerelease()) {
                        return CompletableFuture.completedFuture(
                                new UpdateResult(false, "Latest release is not stable", null));
                    }
                    
                    SemanticVersion latestVersion = SemanticVersion.parse(release.getVersion());
                    SemanticVersion currentVersion = getCurrentVersion();
                    
                    if (!latestVersion.isNewerThan(currentVersion)) {
                        logger.info("Already on latest version (v" + currentVersion + ")");
                        return CompletableFuture.completedFuture(
                                new UpdateResult(false, "Already on latest version", latestVersion));
                    }
                    
                    logger.info("New version available: v" + latestVersion + " (current: v" + currentVersion + ")");
                    logger.info("Auto-updating to v" + latestVersion + "...");
                    
                    return downloadAndApply(null, release, latestVersion);
                })
                .exceptionally(ex -> {
                    logger.warning("Auto-update check failed: " + ex.getMessage());
                    return new UpdateResult(false, "Update check failed", null);
                });
    }
    
    /**
     * Find relative version (previous or next)
     */
    private CompletableFuture<UpdateResult> findRelativeVersion(CommandSender sender, int offset) {
        return apiClient.getAllReleases()
                .thenCompose(releases -> {
                    // Filter out drafts and prereleases, then sort by version
                    List<GitHubRelease> stableReleases = releases.stream()
                            .filter(r -> !r.isDraft() && !r.isPrerelease())
                            .sorted(Comparator.comparing(
                                    r -> SemanticVersion.parse(r.getVersion())))
                            .collect(Collectors.toList());
                    
                    if (stableReleases.isEmpty()) {
                        sendMessage(sender, "&cNo stable releases found!");
                        return CompletableFuture.completedFuture(
                                new UpdateResult(false, "No stable releases", null));
                    }
                    
                    SemanticVersion currentVersion = getCurrentVersion();
                    
                    // Find current version index
                    int currentIndex = -1;
                    for (int i = 0; i < stableReleases.size(); i++) {
                        SemanticVersion v = SemanticVersion.parse(stableReleases.get(i).getVersion());
                        if (v.equals(currentVersion)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    
                    int targetIndex = currentIndex + offset;
                    
                    if (targetIndex < 0 || targetIndex >= stableReleases.size()) {
                        String direction = offset < 0 ? "previous" : "next";
                        sendMessage(sender, "&cNo " + direction + " version available!");
                        return CompletableFuture.completedFuture(
                                new UpdateResult(false, "No " + direction + " version", null));
                    }
                    
                    GitHubRelease targetRelease = stableReleases.get(targetIndex);
                    SemanticVersion targetVersion = SemanticVersion.parse(targetRelease.getVersion());
                    
                    String direction = offset < 0 ? "previous" : "next";
                    sendMessage(sender, "&eFound " + direction + " version: &av" + targetVersion);
                    
                    return downloadAndApply(sender, targetRelease, targetVersion);
                });
    }
    
    /**
     * Download and apply update
     */
    private CompletableFuture<UpdateResult> downloadAndApply(CommandSender sender, 
                                                               GitHubRelease release, 
                                                               SemanticVersion version) {
        GitHubRelease.Asset jarAsset = release.getJarAsset();
        if (jarAsset == null) {
            sendMessage(sender, "&cNo JAR file found in release!");
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "No JAR asset found", version));
        }
        
        sendMessage(sender, "&eDownloading &av" + version + "&e...");
        sendMessage(sender, "&7Size: &e" + formatBytes(jarAsset.getSize()));
        
        try {
            Files.createDirectories(updateFolder);
        } catch (IOException e) {
            sendMessage(sender, "&cFailed to create update folder!");
            return CompletableFuture.completedFuture(
                    new UpdateResult(false, "Failed to create update folder", version));
        }
        
        String fileName = "DZEconomy-v" + version + ".jar";
        Path destination = updateFolder.resolve(fileName);
        
        return downloader.downloadFile(jarAsset.getBrowserDownloadUrl(), destination)
                .thenCompose(downloadedFile -> {
                    sendMessage(sender, "&aDownload completed!");
                    sendMessage(sender, "&eSaved to: &7" + downloadedFile.getFileName());
                    
                    // Attempt hot-reload if possible
                    if (plugin.getConfigManager().getConfig().getBoolean("updater.attempt-hot-reload", false)) {
                        sendMessage(sender, "&eAttempting hot-reload...");
                        boolean reloaded = attemptHotReload(downloadedFile);
                        
                        if (reloaded) {
                            sendMessage(sender, "&a&l✓ Plugin hot-reloaded successfully!");
                            return CompletableFuture.completedFuture(
                                    new UpdateResult(true, "Hot-reload successful", version));
                        } else {
                            sendMessage(sender, "&cHot-reload failed. Restart required.");
                        }
                    }
                    
                    sendMessage(sender, "&a&l✓ Update ready!");
                    sendMessage(sender, "&eRestart the server to apply v" + version);
                    
                    return CompletableFuture.completedFuture(
                            new UpdateResult(true, "Update downloaded, restart required", version));
                })
                .exceptionally(ex -> {
                    sendMessage(sender, "&cDownload failed: " + ex.getMessage());
                    logger.warning("Download failed: " + ex.getMessage());
                    return new UpdateResult(false, "Download failed", version);
                });
    }
    
    /**
     * Attempt hot-reload (best-effort)
     */
    private boolean attemptHotReload(Path newJarPath) {
        try {
            PluginManager pm = Bukkit.getPluginManager();
            
            // Disable current plugin
            pm.disablePlugin(plugin);
            
            // Try to load new plugin
            Plugin newPlugin = pm.loadPlugin(newJarPath.toFile());
            if (newPlugin == null) {
                logger.warning("Failed to load new plugin JAR");
                pm.enablePlugin(plugin); // Re-enable old plugin
                return false;
            }
            
            // Enable new plugin
            pm.enablePlugin(newPlugin);
            
            return true;
        } catch (Exception e) {
            logger.warning("Hot-reload failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send colored message to sender
     */
    private void sendMessage(CommandSender sender, String message) {
        if (sender != null) {
            String prefix = ColorUtil.translate("&8[&6DZ&eEconomy&8]&r ");
            sender.sendMessage(ColorUtil.translate(prefix + message));
        }
    }
    
    /**
     * Format bytes to human-readable
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
    
    /**
     * Update result
     */
    public static class UpdateResult {
        private final boolean success;
        private final String message;
        private final SemanticVersion version;
        
        public UpdateResult(boolean success, String message, SemanticVersion version) {
            this.success = success;
            this.message = message;
            this.version = version;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public SemanticVersion getVersion() {
            return version;
        }
    }
}
