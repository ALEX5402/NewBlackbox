# BlackBox Virtual Environment - Complete User Guide

## Table of Contents
1. [Overview](#overview)
2. [Installation & Setup](#installation--setup)
3. [App Management](#app-management)
4. [WebView & Browser Support](#webview--browser-support)
5. [Google Services Integration](#google-services-integration)
6. [Background Job Management](#background-job-management)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Features](#advanced-features)
9. [API Reference](#api-reference)
10. [Frequently Asked Questions](#frequently-asked-questions)

---

## Overview

BlackBox is a comprehensive Android virtualization solution that creates isolated environments for running apps. The latest version includes significant improvements for:

- **App Installation & Management**: Robust app installation with cloning prevention
- **WebView Support**: Complete WebView compatibility for browsers and web apps
- **Google Services**: Enhanced Google account and GMS integration
- **Background Jobs**: WorkManager and JobScheduler compatibility
- **UID Management**: Smart UID spoofing for system compatibility
- **Crash Prevention**: Comprehensive error handling and recovery

---

## Installation & Setup

### Prerequisites
- Android 8.0+ (API 26+)
- Root access (recommended for full functionality)
- At least 2GB free storage space
- Internet connection for initial setup

### Basic Installation
1. **Download BlackBox APK** from the official source
2. **Install the APK** using your preferred method
3. **Grant Permissions** when prompted:
   - Storage access
   - System overlay (for floating features)
   - Location (for GPS spoofing)
   - Notification access (Android 12+)

### Initial Configuration
```bash
# First launch will create virtual environment
# Wait for initialization to complete
# Check logs for any setup issues
```

---

## App Management

### Installing Apps

#### Method 1: APK File Installation
```java
// Using BlackBoxCore API
BlackBoxCore.get().installPackageAsUser(apkFile, userId);

// Example with error handling
try {
    InstallResult result = BlackBoxCore.get().installPackageAsUser(apkFile, 0);
    if (result.isSuccess()) {
        Log.d("BlackBox", "App installed successfully: " + result.getPackageName());
    } else {
        Log.e("BlackBox", "Installation failed: " + result.getErrorMessage());
    }
} catch (Exception e) {
    Log.e("BlackBox", "Installation error", e);
}
```

#### Method 2: Package Name Installation
```java
// Install from existing package
BlackBoxCore.get().installPackageAsUser("com.example.app", userId);

// Check if package exists first
if (BlackBoxCore.getPackageManager().getPackageInfo("com.example.app", 0) != null) {
    BlackBoxCore.get().installPackageAsUser("com.example.app", userId);
}
```

#### Method 3: URI Installation
```java
// Install from content URI
Uri apkUri = Uri.parse("content://com.example.provider/app.apk");
BlackBoxCore.get().installPackageAsUser(apkUri, userId);
```

### App Removal

#### Uninstall Virtual App
```java
// Remove app from virtual environment
BlackBoxCore.get().uninstallPackage(packageName, userId);

// Force uninstall if needed
BlackBoxCore.get().uninstallPackage(packageName, userId, true);
```

#### Clean App Data
```java
// Clear app data without uninstalling
BlackBoxCore.get().clearAppData(packageName, userId);

// Clear specific data types
BlackBoxCore.get().clearAppData(packageName, userId, "cache");
BlackBoxCore.get().clearAppData(packageName, userId, "data");
```

### App Management Utilities

#### List Installed Apps
```java
// Get all virtual apps
List<AppInfo> virtualApps = BlackBoxCore.get().getInstalledApps(userId);

// Get specific app info
AppInfo appInfo = BlackBoxCore.get().getAppInfo(packageName, userId);

// Check if app is installed
boolean isInstalled = BlackBoxCore.get().isAppInstalled(packageName, userId);
```

#### App Configuration
```java
// Enable/disable app
BlackBoxCore.get().setAppEnabled(packageName, userId, true);

// Set app permissions
BlackBoxCore.get().setAppPermission(packageName, permission, userId, true);

// Configure app settings
BlackBoxCore.get().setAppSetting(packageName, setting, value, userId);
```

---

## WebView & Browser Support

### WebView Configuration

#### Automatic WebView Setup
The new WebView system automatically handles:
- **Unique Data Directories**: Each virtual app gets isolated WebView storage
- **Process Isolation**: WebView conflicts between apps are prevented
- **Data Persistence**: WebView data is preserved per app

#### Manual WebView Configuration
```java
// Set custom WebView data directory
WebView.setDataDirectorySuffix("custom_suffix");

// Configure WebView settings
WebView webView = new WebView(context);
WebSettings settings = webView.getSettings();
settings.setJavaScriptEnabled(true);
settings.setDomStorageEnabled(true);
settings.setDatabaseEnabled(true);
```

### Browser App Support

#### Chrome/Firefox Compatibility
```java
// Browser apps automatically get:
// - Isolated WebView instances
// - Separate cookie storage
// - Independent cache directories
// - Process isolation
```

#### Web App Support
```java
// Progressive Web Apps (PWAs) work with:
// - Service worker isolation
// - Cache storage separation
// - Background sync support
```

---

## Google Services Integration

### Google Account Management

#### Automatic Account Handling
```java
// Google accounts are automatically managed:
// - Mock Google accounts for virtual environment
// - Authentication token handling
// - Account synchronization
```

#### Custom Account Configuration
```java
// Add custom Google accounts
AccountManager accountManager = AccountManager.get(context);
Account account = new Account("user@gmail.com", "com.google");
accountManager.addAccountExplicitly(account, "password", null);

// Configure account sync
ContentResolver.setSyncAutomatically(account, "com.google", true);
```

### Google Play Services

#### GMS Compatibility
```java
// Google Play Services automatically:
// - Returns mock package info
// - Handles authentication requests
// - Provides fallback implementations
```

#### Custom GMS Configuration
```java
// Override GMS behavior if needed
GmsProxy.setCustomGmsInfo("com.example.gms", customInfo);

// Configure GMS permissions
GmsProxy.setGmsPermission("com.example.gms", permission, true);
```

---

## Background Job Management

### WorkManager Integration

#### Automatic WorkManager Handling
```java
// WorkManager automatically:
// - Handles UID validation issues
// - Provides fallback implementations
// - Prevents crashes on job scheduling
```

#### Custom Work Configuration
```java
// Configure custom work
WorkManager workManager = WorkManager.getInstance(context);

// Create work request
OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class)
    .setInputData(inputData)
    .build();

// Enqueue work
workManager.enqueue(workRequest);
```

### JobScheduler Compatibility

#### Job Scheduling
```java
// Jobs are automatically handled with:
// - UID validation bypass
// - Fallback mechanisms
// - Error recovery
```

#### Custom Job Configuration
```java
// Create custom job
JobInfo.Builder builder = new JobInfo.Builder(jobId, componentName);
builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
builder.setRequiresCharging(true);

// Schedule job
JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
scheduler.schedule(builder.build());
```

---

## Advanced Features

### UID Spoofing

#### Automatic UID Management
```java
// UID spoofing automatically:
// - Detects UID validation issues
// - Selects appropriate UIDs for operations
// - Provides fallback UIDs when needed
```

#### Custom UID Configuration
```java
// Configure custom UID for specific operations
UIDSpoofingHelper.setCustomUID("operation", "package", customUID);

// Override UID selection logic
UIDSpoofingHelper.setUIDStrategy("operation", customStrategy);
```

### Process Management

#### Virtual Process Control
```java
// Control virtual processes
BlackBoxCore.get().startVirtualProcess(packageName, userId);
BlackBoxCore.get().stopVirtualProcess(packageName, userId);

// Monitor process status
ProcessInfo processInfo = BlackBoxCore.get().getProcessInfo(packageName, userId);
```

#### Memory Management
```java
// Optimize memory usage
BlackBoxCore.get().optimizeMemory(userId);

// Clear unused resources
BlackBoxCore.get().clearUnusedResources(userId);
```

---

## Troubleshooting

### Common Issues

#### App Installation Failures
```bash
# Check logs for installation errors
adb logcat | grep "BlackBox"

# Common solutions:
# 1. Ensure sufficient storage space
# 2. Check APK file integrity
# 3. Verify package compatibility
# 4. Clear BlackBox cache
```

#### WebView Issues
```bash
# WebView troubleshooting:
# 1. Check WebView data directories
# 2. Verify WebView provider status
# 3. Clear WebView cache
# 4. Restart virtual environment
```

#### Google Services Problems
```bash
# GMS troubleshooting:
# 1. Check GMS proxy status
# 2. Verify account configuration
# 3. Clear GMS cache
# 4. Reinstall GMS components
```

### Debug Mode

#### Enable Debug Logging
```java
// Enable comprehensive logging
BlackBoxCore.setDebugMode(true);

// Set log level
Slog.setLogLevel(Slog.LEVEL_DEBUG);

// Enable specific debug features
BlackBoxCore.enableDebugFeature("webview", true);
BlackBoxCore.enableDebugFeature("gms", true);
```

#### Log Analysis
```bash
# Filter BlackBox logs
adb logcat | grep "BlackBox\|WebView\|GmsProxy\|WorkManager"

# Save logs to file
adb logcat > blackbox_logs.txt

# Analyze specific components
adb logcat | grep "JobServiceStub\|WebViewProxy\|GoogleAccountManagerProxy"
```

---

## API Reference

### Core Classes

#### BlackBoxCore
```java
// Main entry point
BlackBoxCore core = BlackBoxCore.get();

// Core methods
core.installPackageAsUser(apkFile, userId);
core.uninstallPackage(packageName, userId);
core.getInstalledApps(userId);
core.isAppInstalled(packageName, userId);
```

#### BActivityThread
```java
// Activity thread management
int userId = BActivityThread.getUserId();
String packageName = BActivityThread.getAppPackageName();
String processName = BActivityThread.getAppProcessName();
```

#### UIDSpoofingHelper
```java
// UID management utilities
int systemUID = UIDSpoofingHelper.getSystemUID();
int packageUID = UIDSpoofingHelper.getPackageUID(packageName);
boolean needsSpoofing = UIDSpoofingHelper.needsUIDSpoofing(operation, packageName);
```

### Service Proxies

#### WebViewProxy
```java
// WebView management
WebViewProxy.configureWebView(webView, context);
WebViewProxy.setDataDirectorySuffix(suffix);
String dataDir = WebViewProxy.getDataDirectory();
```

#### WorkManagerProxy
```java
// WorkManager compatibility
WorkManagerProxy.enqueueWork(workRequest);
WorkManagerProxy.cancelWork(workId);
List<WorkInfo> workInfos = WorkManagerProxy.getWorkInfos();
```

#### GoogleAccountManagerProxy
```java
// Google account management
Account[] accounts = GoogleAccountManagerProxy.getAccounts();
String token = GoogleAccountManagerProxy.getAuthToken(account, authTokenType);
boolean success = GoogleAccountManagerProxy.addAccount(account, password, extras);
```

---

## Frequently Asked Questions

### Q: Why do some apps show black screens?
**A**: This is usually caused by context or resource loading issues. The new BlackBox version includes comprehensive fixes for:
- Context management
- Resource loading
- Activity lifecycle
- Service initialization

### Q: How do I fix WebView issues in browsers?
**A**: The new WebView system automatically handles:
- Data directory conflicts
- Process isolation
- Provider issues
- Cache management

### Q: Why do background jobs fail?
**A**: Background job failures are now handled by:
- WorkManager compatibility layer
- JobScheduler UID validation bypass
- Smart UID spoofing
- Graceful fallback mechanisms

### Q: How do I prevent app cloning issues?
**A**: BlackBox now includes:
- Automatic cloning prevention
- Package validation
- Security checks
- Error messages for blocked installations

### Q: What if Google services don't work?
**A**: The new GMS system provides:
- Mock Google Play Services
- Account authentication fallbacks
- Token management
- Service compatibility layers

---

## Support & Updates

### Getting Help
- **Documentation**: Check this Docs.md file
- **Logs**: Enable debug mode and analyze logs
- **Community**: Join BlackBox user forums
- **Issues**: Report bugs with detailed logs

### Version History
- **v2.0**: Complete rewrite with new architecture
- **v2.1**: WebView and browser compatibility
- **v2.2**: Google services integration
- **v2.3**: Background job management
- **Current**: UID spoofing and crash prevention

### Future Features
- **Enhanced Security**: Additional anti-detection features
- **Performance**: Memory and CPU optimization
- **Compatibility**: Support for more Android versions
- **Integration**: Additional service proxies

---

## Conclusion

The new BlackBox virtual environment provides a robust, feature-rich solution for Android app virtualization. With comprehensive WebView support, Google services integration, and background job management, it offers enterprise-grade functionality for both developers and end users.

For the best experience:
1. **Keep BlackBox updated** to the latest version
2. **Enable debug logging** when troubleshooting
3. **Monitor system resources** for optimal performance
4. **Report issues** with detailed logs for faster resolution

Happy virtualizing! 🚀✨
