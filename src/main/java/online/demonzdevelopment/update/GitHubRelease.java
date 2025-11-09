package online.demonzdevelopment.update;

import java.util.List;

/**
 * Represents a GitHub release
 */
public class GitHubRelease {
    private final String tagName;
    private final String name;
    private final boolean draft;
    private final boolean prerelease;
    private final List<Asset> assets;
    
    public GitHubRelease(String tagName, String name, boolean draft, boolean prerelease, List<Asset> assets) {
        this.tagName = tagName;
        this.name = name;
        this.draft = draft;
        this.prerelease = prerelease;
        this.assets = assets;
    }
    
    public String getTagName() {
        return tagName;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isDraft() {
        return draft;
    }
    
    public boolean isPrerelease() {
        return prerelease;
    }
    
    public List<Asset> getAssets() {
        return assets;
    }
    
    /**
     * Get version without 'v' prefix
     */
    public String getVersion() {
        return tagName.startsWith("v") ? tagName.substring(1) : tagName;
    }
    
    /**
     * Find JAR asset
     */
    public Asset getJarAsset() {
        return assets.stream()
                .filter(asset -> asset.getName().endsWith(".jar"))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Represents a release asset
     */
    public static class Asset {
        private final String name;
        private final String browserDownloadUrl;
        private final long size;
        
        public Asset(String name, String browserDownloadUrl, long size) {
            this.name = name;
            this.browserDownloadUrl = browserDownloadUrl;
            this.size = size;
        }
        
        public String getName() {
            return name;
        }
        
        public String getBrowserDownloadUrl() {
            return browserDownloadUrl;
        }
        
        public long getSize() {
            return size;
        }
    }
}
