package top.niunaijun.blackbox.core.system;

import java.io.File;

/**
 * Configuration class for JAR file management
 * Centralizes JAR-related settings and requirements
 */
public class JarConfig {
    
    /**
     * JAR file definitions
     */
    public static class JarDefinition {
        private final String assetName;
        private final String fileName;
        private final long minSize;
        private final String description;
        private final boolean required;
        
        public JarDefinition(String assetName, String fileName, long minSize, String description, boolean required) {
            this.assetName = assetName;
            this.fileName = fileName;
            this.minSize = minSize;
            this.description = description;
            this.required = required;
        }
        
        public String getAssetName() { return assetName; }
        public String getFileName() { return fileName; }
        public long getMinSize() { return minSize; }
        public String getDescription() { return description; }
        public boolean isRequired() { return required; }
    }
    
    // JAR file definitions
    public static final JarDefinition EMPTY_JAR = new JarDefinition(
        "empty.jar", 
        "empty.apk", 
        100L, 
        "Empty JAR for fallback DEX loading", 
        true
    );
    
    public static final JarDefinition JUNIT_JAR = new JarDefinition(
        "junit.jar", 
        "junit.apk", 
        1000L, 
        "JUnit testing framework support", 
        false
    );
    
    // Performance settings
    public static final int DEFAULT_BUFFER_SIZE = 8192; // 8KB
    public static final int MAX_BUFFER_SIZE = 32768; // 32KB
    public static final int MIN_BUFFER_SIZE = 1024; // 1KB
    
    // Retry settings
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_DELAY_MS = 1000; // 1 second
    public static final long MAX_RETRY_DELAY_MS = 5000; // 5 seconds
    
    // Validation settings
    public static final boolean ENABLE_FILE_HASHING = true;
    public static final boolean ENABLE_SIZE_VALIDATION = true;
    public static final boolean ENABLE_ASYNC_LOADING = true;
    
    // Cache settings
    public static final int MAX_CACHE_SIZE = 10;
    public static final long CACHE_CLEANUP_INTERVAL_MS = 300000; // 5 minutes
    
    /**
     * Get all required JAR definitions
     */
    public static JarDefinition[] getRequiredJars() {
        return new JarDefinition[]{EMPTY_JAR, JUNIT_JAR};
    }
    
    /**
     * Get JAR definition by asset name
     */
    public static JarDefinition getJarDefinition(String assetName) {
        for (JarDefinition jar : getRequiredJars()) {
            if (jar.getAssetName().equals(assetName)) {
                return jar;
            }
        }
        return null;
    }
    
    /**
     * Validate file against JAR definition
     */
    public static boolean validateFile(File file, JarDefinition definition) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        if (ENABLE_SIZE_VALIDATION && file.length() < definition.getMinSize()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get optimal buffer size based on available memory
     */
    public static int getOptimalBufferSize() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        
        // Use larger buffer for devices with more memory
        if (maxMemory > 512 * 1024 * 1024) { // > 512MB
            return MAX_BUFFER_SIZE;
        } else if (maxMemory > 256 * 1024 * 1024) { // > 256MB
            return DEFAULT_BUFFER_SIZE;
        } else {
            return MIN_BUFFER_SIZE;
        }
    }
}
