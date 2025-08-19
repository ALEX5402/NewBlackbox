package top.niunaijun.blackbox.core.system;

import android.util.Log;

/**
 * Simple test class for JarManager improvements
 * This can be used to verify the JAR management system works correctly
 */
public class JarManagerTest {
    private static final String TAG = "JarManagerTest";
    
    /**
     * Test JAR manager functionality
     */
    public static void testJarManager() {
        Log.i(TAG, "Starting JAR Manager test");
        
        try {
            JarManager jarManager = JarManager.getInstance();
            
            // Test async initialization
            Log.d(TAG, "Testing async initialization");
            jarManager.initializeAsync();
            
            // Wait a bit for async initialization
            Thread.sleep(2000);
            
            // Test sync initialization if needed
            if (!jarManager.isReady()) {
                Log.d(TAG, "Async initialization not complete, trying sync");
                jarManager.initializeSync();
            }
            
            // Test JAR file retrieval
            Log.d(TAG, "Testing JAR file retrieval");
            testJarFileRetrieval(jarManager);
            
            // Test cache statistics
            Log.d(TAG, "Testing cache statistics");
            String stats = jarManager.getCacheStats();
            Log.i(TAG, "Cache stats: " + stats);
            
            // Test individual JAR info
            Log.d(TAG, "Testing individual JAR info");
            String emptyJarInfo = jarManager.getJarInfo("empty.jar");
            String junitJarInfo = jarManager.getJarInfo("junit.jar");
            
            Log.i(TAG, "Empty JAR info: " + emptyJarInfo);
            Log.i(TAG, "JUnit JAR info: " + junitJarInfo);
            
            Log.i(TAG, "JAR Manager test completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "JAR Manager test failed", e);
        }
    }
    
    /**
     * Test JAR file retrieval
     */
    private static void testJarFileRetrieval(JarManager jarManager) {
        // Test empty.jar
        if (jarManager.getEmptyJar() != null) {
            Log.d(TAG, "Empty JAR retrieved successfully");
        } else {
            Log.w(TAG, "Empty JAR retrieval failed");
        }
        
        // Test junit.jar
        if (jarManager.getJunitJar() != null) {
            Log.d(TAG, "JUnit JAR retrieved successfully");
        } else {
            Log.w(TAG, "JUnit JAR retrieval failed");
        }
        
        // Test generic retrieval
        if (jarManager.getJarFile("empty.jar") != null) {
            Log.d(TAG, "Generic JAR retrieval for empty.jar successful");
        } else {
            Log.w(TAG, "Generic JAR retrieval for empty.jar failed");
        }
    }
    
    /**
     * Test configuration
     */
    public static void testConfiguration() {
        Log.i(TAG, "Testing JAR configuration");
        
        // Test JAR definitions
        JarConfig.JarDefinition[] jars = JarConfig.getRequiredJars();
        Log.d(TAG, "Found " + jars.length + " JAR definitions");
        
        for (JarConfig.JarDefinition jar : jars) {
            Log.d(TAG, "JAR: " + jar.getAssetName() + 
                      ", File: " + jar.getFileName() + 
                      ", MinSize: " + jar.getMinSize() + 
                      ", Required: " + jar.isRequired() + 
                      ", Description: " + jar.getDescription());
        }
        
        // Test buffer size calculation
        int bufferSize = JarConfig.getOptimalBufferSize();
        Log.d(TAG, "Optimal buffer size: " + bufferSize + " bytes");
        
        // Test file validation
        boolean enableValidation = JarConfig.ENABLE_SIZE_VALIDATION;
        boolean enableHashing = JarConfig.ENABLE_FILE_HASHING;
        boolean enableAsync = JarConfig.ENABLE_ASYNC_LOADING;
        
        Log.d(TAG, "Validation enabled: " + enableValidation);
        Log.d(TAG, "Hashing enabled: " + enableHashing);
        Log.d(TAG, "Async loading enabled: " + enableAsync);
    }
}
