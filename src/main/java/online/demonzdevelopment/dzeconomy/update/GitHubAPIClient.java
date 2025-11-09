package online.demonzdevelopment.dzeconomy.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * GitHub API client for fetching releases
 */
public class GitHubAPIClient {
    
    private static final String API_BASE = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_RETRIES = 3;
    
    private final String owner;
    private final String repo;
    private final HttpClient httpClient;
    private final Logger logger;
    
    public GitHubAPIClient(String owner, String repo, Logger logger) {
        this.owner = owner;
        this.repo = repo;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }
    
    /**
     * Get the latest release
     */
    public CompletableFuture<GitHubRelease> getLatestRelease() {
        String url = String.format("%s/repos/%s/%s/releases/latest", API_BASE, owner, repo);
        return fetchRelease(url);
    }
    
    /**
     * Get a release by tag name
     */
    public CompletableFuture<GitHubRelease> getReleaseByTag(String tag) {
        String tagWithPrefix = tag.startsWith("v") ? tag : "v" + tag;
        String url = String.format("%s/repos/%s/%s/releases/tags/%s", API_BASE, owner, repo, tagWithPrefix);
        return fetchRelease(url);
    }
    
    /**
     * Get all releases
     */
    public CompletableFuture<List<GitHubRelease>> getAllReleases() {
        String url = String.format("%s/repos/%s/%s/releases", API_BASE, owner, repo);
        return fetchWithRetry(url, 0)
                .thenApply(this::parseReleaseList);
    }
    
    /**
     * Fetch a single release
     */
    private CompletableFuture<GitHubRelease> fetchRelease(String url) {
        return fetchWithRetry(url, 0)
                .thenApply(this::parseRelease);
    }
    
    /**
     * Fetch with exponential backoff retry
     */
    private CompletableFuture<String> fetchWithRetry(String url, int attempt) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DZEconomy-Updater")
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    int statusCode = response.statusCode();
                    
                    if (statusCode == 200) {
                        return CompletableFuture.completedFuture(response.body());
                    }
                    
                    // Handle rate limiting
                    if (statusCode == 403 || statusCode == 429) {
                        if (attempt < MAX_RETRIES) {
                            long delayMs = (long) Math.pow(2, attempt) * 1000;
                            logger.warning(String.format("GitHub API rate limited (attempt %d/%d). Retrying in %dms...", 
                                    attempt + 1, MAX_RETRIES, delayMs));
                            
                            return CompletableFuture.supplyAsync(() -> {
                                try {
                                    Thread.sleep(delayMs);
                                    return null;
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }).thenCompose(ignored -> fetchWithRetry(url, attempt + 1));
                        }
                        return CompletableFuture.failedFuture(
                                new IOException("GitHub API rate limit exceeded"));
                    }
                    
                    if (statusCode == 404) {
                        return CompletableFuture.failedFuture(
                                new IOException("Release not found"));
                    }
                    
                    return CompletableFuture.failedFuture(
                            new IOException("GitHub API returned status: " + statusCode));
                });
    }
    
    /**
     * Parse single release from JSON
     */
    private GitHubRelease parseRelease(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return parseReleaseObject(obj);
    }
    
    /**
     * Parse list of releases from JSON
     */
    private List<GitHubRelease> parseReleaseList(String json) {
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        List<GitHubRelease> releases = new ArrayList<>();
        
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            releases.add(parseReleaseObject(obj));
        }
        
        return releases;
    }
    
    /**
     * Parse release object from JSON
     */
    private GitHubRelease parseReleaseObject(JsonObject obj) {
        String tagName = obj.get("tag_name").getAsString();
        String name = obj.has("name") && !obj.get("name").isJsonNull() 
                ? obj.get("name").getAsString() 
                : tagName;
        boolean draft = obj.get("draft").getAsBoolean();
        boolean prerelease = obj.get("prerelease").getAsBoolean();
        
        List<GitHubRelease.Asset> assets = new ArrayList<>();
        JsonArray assetsArray = obj.getAsJsonArray("assets");
        for (JsonElement assetElement : assetsArray) {
            JsonObject assetObj = assetElement.getAsJsonObject();
            String assetName = assetObj.get("name").getAsString();
            String downloadUrl = assetObj.get("browser_download_url").getAsString();
            long size = assetObj.get("size").getAsLong();
            
            assets.add(new GitHubRelease.Asset(assetName, downloadUrl, size));
        }
        
        return new GitHubRelease(tagName, name, draft, prerelease, assets);
    }
}
