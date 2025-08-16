package top.niunaijun.blackboxa.app.rocker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.imuxuan.floatingview.FloatingMagnetView
import com.imuxuan.floatingview.FloatingView
import top.niunaijun.blackbox.entity.location.BLocation
import top.niunaijun.blackbox.fake.frameworks.BLocationManager
import top.niunaijun.blackboxa.app.App
import top.niunaijun.blackboxa.widget.EnFloatView
import kotlin.math.cos
import kotlin.math.sin

/**
 * RockerManager - Advanced GPS Location Spoofing with Floating Joystick Control
 * 
 * @Description: Provides a floating joystick/rocker control for real-time GPS location manipulation
 *              in virtual apps. Allows users to move their fake GPS location by dragging a joystick.
 * 
 * @Features:
 * - Floating joystick that appears on screen
 * - Real-time GPS coordinate updates
 * - Precise distance and angle control
 * - App-specific location spoofing
 * - Automatic activity lifecycle management
 * 
 * @Usage:
 * 1. RockerManager automatically initializes when virtual apps start
 * 2. A floating joystick appears on the left side of the screen
 * 3. Drag the joystick to change GPS location:
 *    - Distance: How far to move (in meters)
 *    - Angle: Direction to move (0-360 degrees)
 * 4. Location updates happen in real-time as you move the joystick
 * 
 * @Permissions Required:
 * - SYSTEM_ALERT_WINDOW: To show floating view over other apps
 * - ACCESS_FINE_LOCATION: To access GPS location services
 * - ACCESS_COARSE_LOCATION: For approximate location access
 * 
 * @Requirements:
 * - Fake location must be enabled in BlackBox
 * - User must grant overlay and location permissions
 * - Virtual app must be running
 * 
 * @Author: kotlinMiku
 * @CreateDate: 2022/3/19 19:37
 * @LastModified: 2024 - Enhanced with better error handling and permissions
 */
object RockerManager {

    private const val TAG = "RockerManager"
    private var isInitialized = false

    // Earth radius constants for coordinate calculations
    private const val Ea = 6378137.0     // Equator radius (meters)
    private const val Eb = 6356725.0     // Polar radius (meters)

    fun init(application: Application?, userId: Int) {
        try {
            if (isInitialized) {
                Log.d(TAG, "RockerManager already initialized, skipping...")
                return
            }

            if (application == null) {
                Log.w(TAG, "Application is null, cannot initialize RockerManager")
                return
            }

            // Check if required permissions are granted
            if (!checkPermissions(application)) {
                Log.w(TAG, "Required permissions not granted, RockerManager cannot initialize")
                Log.w(TAG, "Please grant: ${getRequiredPermissions().joinToString(", ")}")
                return
            }

            if (!BLocationManager.isFakeLocationEnable()) {
                Log.d(TAG, "Fake location is not enabled, RockerManager will not initialize")
                return
            }

            Log.d(TAG, "Initializing RockerManager for userId: $userId")
            
            val enFloatView = initFloatView()
            if (enFloatView is EnFloatView) {
                enFloatView.setListener { angle: Float, distance: Float ->
                    changeLocation(distance, angle, application.packageName, userId)
                }
                Log.d(TAG, "Floating view initialized successfully")
            } else {
                Log.w(TAG, "Failed to initialize floating view")
                return
            }

            // Register activity lifecycle callbacks for floating view management
            application.registerActivityLifecycleCallbacks(object : BaseActivityLifecycleCallback {
                override fun onActivityStarted(activity: Activity) {
                    super.onActivityStarted(activity)
                    try {
                        FloatingView.get().attach(activity)
                        Log.d(TAG, "Floating view attached to activity: ${activity.javaClass.simpleName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error attaching floating view to activity: ${e.message}")
                    }
                }

                override fun onActivityStopped(activity: Activity) {
                    super.onActivityStopped(activity)
                    try {
                        FloatingView.get().detach(activity)
                        Log.d(TAG, "Floating view detached from activity: ${activity.javaClass.simpleName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error detaching floating view from activity: ${e.message}")
                    }
                }
            })

            isInitialized = true
            Log.d(TAG, "RockerManager initialized successfully - Floating GPS joystick is now active!")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing RockerManager: ${e.message}")
            Log.e(TAG, "Stack trace: ", e)
        }
    }

    private fun initFloatView(): FloatingMagnetView? {
        return try {
            val params = FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )

            params.gravity = Gravity.START or Gravity.CENTER
            val view = EnFloatView(App.getContext())
            view.layoutParams = params

            FloatingView.get().customView(view)
            Log.d(TAG, "Floating view created successfully")

            FloatingView.get().view
        } catch (e: Exception) {
            Log.e(TAG, "Error creating floating view: ${e.message}")
            null
        }
    }

    private fun changeLocation(distance: Float, angle: Float, packageName: String, userId: Int) {
        try {
            val location = BLocationManager.get().getLocation(userId, packageName)
            if (location == null) {
                Log.w(TAG, "No current location found for package: $packageName, userId: $userId")
                return
            }

            Log.d(TAG, "Changing location - Distance: ${distance}m, Angle: ${angle}°, Current: ${location.latitude}, ${location.longitude}")

            // Calculate new coordinates based on joystick input
            val dx = distance * sin(angle * Math.PI / 180.0)
            val dy = distance * cos(angle * Math.PI / 180.0)

            // Use ellipsoid model for more accurate coordinate calculations
            val ec = Eb + (Ea - Eb) * (90.0 - location.latitude) / 90.0
            val ed = ec * cos(location.latitude * Math.PI / 180)

            val newLng = (dx / ed + location.longitude * Math.PI / 180.0) * 180.0 / Math.PI
            val newLat = (dy / ec + location.latitude * Math.PI / 180.0) * 180.0 / Math.PI
            
            val newLocation = BLocation(newLat, newLng)

            // Update the location
            BLocationManager.get().setLocation(userId, packageName, newLocation)
            
            Log.d(TAG, "Location updated - New: ${newLat}, ${newLng}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error changing location: ${e.message}")
            Log.e(TAG, "Stack trace: ", e)
        }
    }

    /**
     * Check if RockerManager is currently initialized and active
     */
    fun isActive(): Boolean {
        return isInitialized
    }

    /**
     * Check if required permissions are granted for RockerManager to work
     */
    fun checkPermissions(context: Context): Boolean {
        return try {
            // Check if overlay permission is granted (required for floating view)
            val hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
            if (!hasOverlayPermission) {
                Log.w(TAG, "Overlay permission not granted - RockerManager cannot show floating view")
                return false
            }

            // Check if location permissions are granted
            val hasLocationPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasLocationPermission) {
                Log.w(TAG, "Location permission not granted - RockerManager cannot access location")
                return false
            }

            Log.d(TAG, "All required permissions are granted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions: ${e.message}")
            false
        }
    }

    /**
     * Get a list of required permissions for RockerManager
     */
    fun getRequiredPermissions(): List<String> {
        return listOf(
            android.Manifest.permission.SYSTEM_ALERT_WINDOW,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Clean up resources (useful for testing or when disabling the feature)
     */
    fun cleanup() {
        try {
            isInitialized = false
            Log.d(TAG, "RockerManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}