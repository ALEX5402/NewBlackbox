package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.DexFileRecovery;


public class ClassLoaderProxy extends ClassInvocationStub {
    private static final String TAG = "ClassLoaderProxy";
    
    
    private static final Map<String, Class<?>> sClassCache = new HashMap<>();
    
    
    private static final Map<String, Boolean> sDexFileCache = new HashMap<>();
    
    
    private static final List<ClassLoader> sFallbackClassLoaders = new ArrayList<>();

    public ClassLoaderProxy() {
        super();
        initializeFallbackClassLoaders();
    }

    @Override
    protected Object getWho() {
        return null; 
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        
        Slog.d(TAG, "ClassLoader proxy injected for DEX file corruption prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    private void initializeFallbackClassLoaders() {
        try {
            
            Context hostContext = BlackBoxCore.getContext();
            if (hostContext != null) {
                ClassLoader hostClassLoader = hostContext.getClassLoader();
                if (hostClassLoader != null) {
                    sFallbackClassLoaders.add(hostClassLoader);
                    Slog.d(TAG, "Added host class loader as fallback");
                }
            }
            
            
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if (systemClassLoader != null) {
                sFallbackClassLoaders.add(systemClassLoader);
                Slog.d(TAG, "Added system class loader as fallback");
            }
            
            
            ClassLoader bootClassLoader = Object.class.getClassLoader();
            if (bootClassLoader != null) {
                sFallbackClassLoaders.add(bootClassLoader);
                Slog.d(TAG, "Added boot class loader as fallback");
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to initialize fallback class loaders: " + e.getMessage());
        }
    }

    
    @ProxyMethod("loadClass")
    public static class LoadClass extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String className = (String) args[0];
            boolean resolve = args.length > 1 ? (Boolean) args[1] : false;
            
            
            if (sClassCache.containsKey(className)) {
                Slog.d(TAG, "ClassLoader: loadClass returning cached class: " + className);
                return sClassCache.get(className);
            }
            
            
            if (isProblematicClass(className)) {
                Slog.d(TAG, "ClassLoader: loadClass called for problematic class: " + className + ", returning null");
                return null;
            }
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    sClassCache.put(className, (Class<?>) result);
                    Slog.d(TAG, "ClassLoader: loadClass successful for: " + className);
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "ClassLoader: loadClass failed for " + className + ", trying fallbacks: " + e.getMessage());
            }
            
            
            Class<?> fallbackResult = tryFallbackClassLoaders(className);
            if (fallbackResult != null) {
                sClassCache.put(className, fallbackResult);
                Slog.d(TAG, "ClassLoader: loadClass successful via fallback for: " + className);
                return fallbackResult;
            }
            
            
            Slog.w(TAG, "ClassLoader: loadClass failed for " + className + ", returning null");
            return null;
        }
    }

    
    @ProxyMethod("findClass")
    public static class FindClass extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String className = (String) args[0];
            
            
            if (sClassCache.containsKey(className)) {
                Slog.d(TAG, "ClassLoader: findClass returning cached class: " + className);
                return sClassCache.get(className);
            }
            
            
            if (isProblematicClass(className)) {
                Slog.d(TAG, "ClassLoader: findClass called for problematic class: " + className + ", returning null");
                return null;
            }
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    sClassCache.put(className, (Class<?>) result);
                    Slog.d(TAG, "ClassLoader: findClass successful for: " + className);
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "ClassLoader: findClass failed for " + className + ", trying fallbacks: " + e.getMessage());
            }
            
            
            Class<?> fallbackResult = tryFallbackClassLoaders(className);
            if (fallbackResult != null) {
                sClassCache.put(className, fallbackResult);
                Slog.d(TAG, "ClassLoader: findClass successful via fallback for: " + className);
                return fallbackResult;
            }
            
            
            Slog.w(TAG, "ClassLoader: findClass failed for " + className + ", returning null");
            return null;
        }
    }

    
    @ProxyMethod("forName")
    public static class ForName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String className = (String) args[0];
            
            
            if (sClassCache.containsKey(className)) {
                Slog.d(TAG, "ClassLoader: forName returning cached class: " + className);
                return sClassCache.get(className);
            }
            
            
            if (isProblematicClass(className)) {
                Slog.d(TAG, "ClassLoader: forName called for problematic class: " + className + ", returning null");
                return null;
            }
            
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    sClassCache.put(className, (Class<?>) result);
                    Slog.d(TAG, "ClassLoader: forName successful for: " + className);
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "ClassLoader: forName failed for " + className + ", trying fallbacks: " + e.getMessage());
            }
            
            
            Class<?> fallbackResult = tryFallbackClassLoaders(className);
            if (fallbackResult != null) {
                sClassCache.put(className, fallbackResult);
                Slog.d(TAG, "ClassLoader: forName successful via fallback for: " + className);
                return fallbackResult;
            }
            
            
            Slog.w(TAG, "ClassLoader: forName failed for " + className + ", returning null");
            return null;
        }
    }

    
    @ProxyMethod("openDexFile")
    public static class OpenDexFile extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String sourceFile = (String) args[0];
            
            
            if (!isValidDexFile(sourceFile)) {
                Slog.w(TAG, "DexFile: Invalid DEX file detected: " + sourceFile + ", attempting recovery");
                
                
                String recoveredFile = recoverDexFile(sourceFile);
                if (recoveredFile != null) {
                    args[0] = recoveredFile;
                    Slog.d(TAG, "DexFile: Using recovered DEX file: " + recoveredFile);
                } else {
                    Slog.e(TAG, "DexFile: Could not recover DEX file: " + sourceFile);
                    return null; 
                }
            }
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "DexFile: openDexFile failed for " + sourceFile + ", returning null: " + e.getMessage());
                return null; 
            }
        }
    }

    
    @ProxyMethod("loadDexFile")
    public static class LoadDexFile extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("classes.dex") && errorMessage.contains("Entry not found")) {
                    Slog.w(TAG, "DexPathList: classes.dex entry not found, attempting recovery");
                    
                    
                    String corruptedFilePath = extractCorruptedFilePathFromError(e, who);
                    if (corruptedFilePath != null) {
                        DexFileRecovery.RecoveryResult recoveryResult = DexFileRecovery.recoverDexFile(corruptedFilePath);
                        if (recoveryResult.success) {
                            Slog.d(TAG, "DexPathList: Successfully recovered DEX file via " + recoveryResult.recoveryMethod);
                            
                            Object recoveredResult = createFallbackDexPathList(who, recoveryResult.recoveredFilePath);
                            if (recoveredResult != null) {
                                return recoveredResult;
                            }
                        } else {
                            Slog.w(TAG, "DexPathList: DEX file recovery failed: " + recoveryResult.errorMessage);
                        }
                    }
                }
                
                Slog.w(TAG, "DexPathList: loadDexFile failed, returning null: " + e.getMessage());
                return null; 
            }
        }
        
        
        private String extractCorruptedFilePathFromError(Exception e, Object who) {
            try {
                
                String errorMessage = e.getMessage();
                if (errorMessage != null) {
                    
                    if (errorMessage.contains("/data/app/")) {
                        
                        int startIndex = errorMessage.indexOf("/data/app/");
                        if (startIndex >= 0) {
                            int endIndex = errorMessage.indexOf(" ", startIndex);
                            if (endIndex < 0) {
                                endIndex = errorMessage.length();
                            }
                            return errorMessage.substring(startIndex, endIndex);
                        }
                    }
                }
                
                
                try {
                    Field pathListField = who.getClass().getDeclaredField("pathList");
                    pathListField.setAccessible(true);
                    Object pathList = pathListField.get(who);
                    if (pathList != null) {
                        Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
                        dexElementsField.setAccessible(true);
                        Object[] dexElements = (Object[]) dexElementsField.get(pathList);
                        if (dexElements != null && dexElements.length > 0) {
                            
                            Field pathField = dexElements[0].getClass().getDeclaredField("path");
                            pathField.setAccessible(true);
                            return (String) pathField.get(dexElements[0]);
                        }
                    }
                } catch (Exception reflectionException) {
                    Slog.w(TAG, "Could not extract path via reflection: " + reflectionException.getMessage());
                }
                
            } catch (Exception ex) {
                Slog.w(TAG, "Error extracting corrupted file path: " + ex.getMessage());
            }
            
            return null;
        }
    }

    
    private static Class<?> tryFallbackClassLoaders(String className) {
        for (ClassLoader fallbackLoader : sFallbackClassLoaders) {
            try {
                Class<?> result = fallbackLoader.loadClass(className);
                if (result != null) {
                    Slog.d(TAG, "Successfully loaded class " + className + " via fallback loader: " + fallbackLoader.getClass().getSimpleName());
                    return result;
                }
            } catch (Exception e) {
                
            }
        }
        return null;
    }

    
    private static boolean isValidDexFile(String sourceFile) {
        if (sourceFile == null) return false;
        
        
        if (sDexFileCache.containsKey(sourceFile)) {
            return sDexFileCache.get(sourceFile);
        }
        
        try {
            File file = new File(sourceFile);
            if (!file.exists()) {
                sDexFileCache.put(sourceFile, false);
                return false;
            }
            
            
            if (sourceFile.endsWith(".apk") || sourceFile.endsWith(".zip")) {
                
                boolean isValid = validateApkFile(file);
                sDexFileCache.put(sourceFile, isValid);
                return isValid;
            }
            
            sDexFileCache.put(sourceFile, true);
            return true;
            
        } catch (Exception e) {
            Slog.w(TAG, "Error validating DEX file " + sourceFile + ": " + e.getMessage());
            sDexFileCache.put(sourceFile, false);
            return false;
        }
    }

    
    private static boolean validateApkFile(File apkFile) {
        try {
            
            
            return apkFile.length() > 1000; 
        } catch (Exception e) {
            return false;
        }
    }

    
    private static String recoverDexFile(String sourceFile) {
        try {
            
            String alternativeFile = findAlternativeApkFile(sourceFile);
            if (alternativeFile != null) {
                return alternativeFile;
            }
            
            
            Context hostContext = BlackBoxCore.getContext();
            if (hostContext != null) {
                ApplicationInfo appInfo = hostContext.getApplicationInfo();
                if (appInfo != null && appInfo.sourceDir != null) {
                    Slog.d(TAG, "Using host app APK as DEX fallback: " + appInfo.sourceDir);
                    return appInfo.sourceDir;
                }
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Error recovering DEX file: " + e.getMessage());
        }
        
        return null;
    }

    
    private static String findAlternativeApkFile(String originalFile) {
        try {
            File original = new File(originalFile);
            File parent = original.getParentFile();
            if (parent != null && parent.exists()) {
                File[] files = parent.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".apk") && file.length() > 1000000) { 
                            Slog.d(TAG, "Found alternative APK file: " + file.getAbsolutePath());
                            return file.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error finding alternative APK file: " + e.getMessage());
        }
        return null;
    }

    
    private static Object recoverDexPathList(Object who, Object[] args) {
        try {
            
            Context hostContext = BlackBoxCore.getContext();
            if (hostContext != null) {
                ApplicationInfo appInfo = hostContext.getApplicationInfo();
                if (appInfo != null && appInfo.sourceDir != null) {
                    
                    Slog.d(TAG, "Recovering DexPathList using host APK: " + appInfo.sourceDir);
                    return createFallbackDexPathList(who, appInfo.sourceDir);
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error recovering DexPathList: " + e.getMessage());
        }
        return null;
    }

    
    private static Object createFallbackDexPathList(Object who, String fallbackApk) {
        try {
            
            
            Slog.d(TAG, "Creating fallback DexPathList with: " + fallbackApk);
            return null; 
        } catch (Exception e) {
            Slog.w(TAG, "Error creating fallback DexPathList: " + e.getMessage());
            return null;
        }
    }

    
    private static boolean isProblematicClass(String className) {
        if (className == null) return false;
        
        
        if (className.contains("kotlinx.coroutines.test")) {
            return true;
        }
        
        
        if (className.contains("com.mediatek.perfservice") || 
            className.contains("com.mediatek.powerhalmgr")) {
            return true;
        }
        
        
        if (className.contains("androidx.datastore.preferences.protobuf") ||
            className.contains("NewInstanceSchemaFull") ||
            className.contains("ExtensionSchemaFull")) {
            return true;
        }
        
        
        if (className.contains("org.robolectric") ||
            className.contains("Robolectric")) {
            return true;
        }
        
        
        if (className.contains("TestMainDispatcherFactory") ||
            className.contains("PerfServiceWrapper") ||
            className.contains("PowerHalMgrFactory")) {
            return true;
        }
        
        return false;
    }

    
    public static void clearClassCache() {
        sClassCache.clear();
        sDexFileCache.clear();
        Slog.d(TAG, "Class and DEX file caches cleared");
    }

    
    public static String getCacheStats() {
        return "Class Cache: " + sClassCache.size() + " entries, DEX Cache: " + sDexFileCache.size() + " entries";
    }
}
