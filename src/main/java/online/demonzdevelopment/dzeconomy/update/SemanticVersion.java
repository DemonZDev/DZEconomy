package online.demonzdevelopment.dzeconomy.update;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic version parser and comparator
 */
public class SemanticVersion implements Comparable<SemanticVersion> {
    
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z0-9.]+))?");
    
    private final int major;
    private final int minor;
    private final int patch;
    private final String suffix;
    
    public SemanticVersion(int major, int minor, int patch, String suffix) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.suffix = suffix;
    }
    
    /**
     * Parse version string (e.g., "1.2.3" or "v1.2.3-SNAPSHOT")
     */
    public static SemanticVersion parse(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        
        // Strip 'v' prefix if present
        String cleanVersion = version.startsWith("v") ? version.substring(1) : version;
        
        Matcher matcher = VERSION_PATTERN.matcher(cleanVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }
        
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String suffix = matcher.group(4);
        
        return new SemanticVersion(major, minor, patch, suffix);
    }
    
    public int getMajor() {
        return major;
    }
    
    public int getMinor() {
        return minor;
    }
    
    public int getPatch() {
        return patch;
    }
    
    public String getSuffix() {
        return suffix;
    }
    
    /**
     * Check if this version is newer than another
     */
    public boolean isNewerThan(SemanticVersion other) {
        return compareTo(other) > 0;
    }
    
    /**
     * Check if this version is older than another
     */
    public boolean isOlderThan(SemanticVersion other) {
        return compareTo(other) < 0;
    }
    
    @Override
    public int compareTo(SemanticVersion other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        if (this.patch != other.patch) {
            return Integer.compare(this.patch, other.patch);
        }
        
        // Versions without suffix are considered newer than with suffix
        if (this.suffix == null && other.suffix != null) {
            return 1;
        }
        if (this.suffix != null && other.suffix == null) {
            return -1;
        }
        if (this.suffix != null && other.suffix != null) {
            return this.suffix.compareTo(other.suffix);
        }
        
        return 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemanticVersion that = (SemanticVersion) o;
        return major == that.major &&
               minor == that.minor &&
               patch == that.patch &&
               Objects.equals(suffix, that.suffix);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, suffix);
    }
    
    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (suffix != null ? "-" + suffix : "");
    }
}
