package online.demonzdevelopment.dzeconomy.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Handles downloading plugin updates
 */
public class UpdateDownloader {
    
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    private final Logger logger;
    
    public UpdateDownloader(Logger logger) {
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    
    /**
     * Download a file from URL to destination
     */
    public CompletableFuture<Path> downloadFile(String url, Path destination) {
        logger.info("Downloading from: " + url);
        logger.info("Saving to: " + destination);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", "DZEconomy-Updater")
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Failed to download file: HTTP " + response.statusCode());
                    }
                    
                    try {
                        // Create parent directories if needed
                        Files.createDirectories(destination.getParent());
                        
                        // Download to temporary file first
                        Path tempFile = destination.getParent().resolve(destination.getFileName() + ".tmp");
                        
                        try (InputStream in = response.body()) {
                            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                        
                        // Verify size
                        long fileSize = Files.size(tempFile);
                        logger.info(String.format("Downloaded %d bytes", fileSize));
                        
                        if (fileSize < 1024) {
                            Files.deleteIfExists(tempFile);
                            throw new IOException("Downloaded file is too small (< 1KB), likely invalid");
                        }
                        
                        // Move to final destination
                        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
                        
                        logger.info("Download completed successfully!");
                        return destination;
                        
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to save downloaded file", e);
                    }
                });
    }
    
    /**
     * Calculate SHA-256 hash of a file
     */
    public String calculateSHA256(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
}
