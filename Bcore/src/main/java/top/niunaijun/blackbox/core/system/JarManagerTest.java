package top.niunaijun.blackbox.core.system;

import android.util.Log;


public class JarManagerTest {
    private static final String TAG = "JarManagerTest";
    
    
    public static void testJarManager() {
        Log.i(TAG, "Starting JAR Manager test");
        
        try {
            JarManager jarManager = JarManager.getInstance();
            
            
            Log.d(TAG, "Testing async initialization");
            jarManager.initializeAsync();
            
            
            Thread.sleep(2000);
            
            
            if (!jarManager.isReady()) {
                Log.d(TAG, "Async initialization not complete, trying sync");
                jarManager.initializeSync();
            }
            
            
            Log.d(TAG, "Testing JAR file retrieval");
            testJarFileRetrieval(jarManager);
            
            
            Log.d(TAG, "Testing cache statistics");
            String stats = jarManager.getCacheStats();
            Log.i(TAG, "Cache stats: " + stats);
            
            
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
    
    
    private static void testJarFileRetrieval(JarManager jarManager) {
        
        if (jarManager.getEmptyJar() != null) {
            Log.d(TAG, "Empty JAR retrieved successfully");
        } else {
            Log.w(TAG, "Empty JAR retrieval failed");
        }
        
        
        if (jarManager.getJunitJar() != null) {
            Log.d(TAG, "JUnit JAR retrieved successfully");
        } else {
            Log.w(TAG, "JUnit JAR retrieval failed");
        }
        
        
        if (jarManager.getJarFile("empty.jar") != null) {
            Log.d(TAG, "Generic JAR retrieval for empty.jar successful");
        } else {
            Log.w(TAG, "Generic JAR retrieval for empty.jar failed");
        }
    }
    
    
    public static void testConfiguration() {
        Log.i(TAG, "Testing JAR configuration");
        
        
        JarConfig.JarDefinition[] jars = JarConfig.getRequiredJars();
        Log.d(TAG, "Found " + jars.length + " JAR definitions");
        
        for (JarConfig.JarDefinition jar : jars) {
            Log.d(TAG, "JAR: " + jar.getAssetName() + 
                      ", File: " + jar.getFileName() + 
                      ", MinSize: " + jar.getMinSize() + 
                      ", Required: " + jar.isRequired() + 
                      ", Description: " + jar.getDescription());
        }
        
        
        int bufferSize = JarConfig.getOptimalBufferSize();
        Log.d(TAG, "Optimal buffer size: " + bufferSize + " bytes");
        
        
        boolean enableValidation = JarConfig.ENABLE_SIZE_VALIDATION;
        boolean enableHashing = JarConfig.ENABLE_FILE_HASHING;
        boolean enableAsync = JarConfig.ENABLE_ASYNC_LOADING;
        
        Log.d(TAG, "Validation enabled: " + enableValidation);
        Log.d(TAG, "Hashing enabled: " + enableHashing);
        Log.d(TAG, "Async loading enabled: " + enableAsync);
    }
}
