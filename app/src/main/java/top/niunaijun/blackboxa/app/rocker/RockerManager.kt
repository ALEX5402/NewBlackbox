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
import kotlin.math.cos
import kotlin.math.sin
import top.niunaijun.blackbox.entity.location.BLocation
import top.niunaijun.blackbox.fake.frameworks.BLocationManager
import top.niunaijun.blackboxa.app.App
import top.niunaijun.blackboxa.widget.EnFloatView



object RockerManager {

    private const val TAG = "RockerManager"
    private var isInitialized = false

    
    private const val Ea = 6378137.0 
    private const val Eb = 6356725.0 

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

            
            application.registerActivityLifecycleCallbacks(
                    object : BaseActivityLifecycleCallback {
                        override fun onActivityStarted(activity: Activity) {
                            super.onActivityStarted(activity)
                            try {
                                FloatingView.get().attach(activity)
                                Log.d(
                                        TAG,
                                        "Floating view attached to activity: ${activity.javaClass.simpleName}"
                                )
                            } catch (e: Exception) {
                                Log.e(
                                        TAG,
                                        "Error attaching floating view to activity: ${e.message}"
                                )
                            }
                        }

                        override fun onActivityStopped(activity: Activity) {
                            super.onActivityStopped(activity)
                            try {
                                FloatingView.get().detach(activity)
                                Log.d(
                                        TAG,
                                        "Floating view detached from activity: ${activity.javaClass.simpleName}"
                                )
                            } catch (e: Exception) {
                                Log.e(
                                        TAG,
                                        "Error detaching floating view from activity: ${e.message}"
                                )
                            }
                        }
                    }
            )

            isInitialized = true
            Log.d(
                    TAG,
                    "RockerManager initialized successfully - Floating GPS joystick is now active!"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing RockerManager: ${e.message}")
            Log.e(TAG, "Stack trace: ", e)
        }
    }

    private fun initFloatView(): FloatingMagnetView? {
        return try {
            val params =
                    FrameLayout.LayoutParams(
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

            Log.d(
                    TAG,
                    "Changing location - Distance: ${distance}m, Angle: ${angle}°, Current: ${location.latitude}, ${location.longitude}"
            )

            
            val dx = distance * sin(angle * Math.PI / 180.0)
            val dy = distance * cos(angle * Math.PI / 180.0)

            
            val ec = Eb + (Ea - Eb) * (90.0 - location.latitude) / 90.0
            val ed = ec * cos(location.latitude * Math.PI / 180)

            val newLng = (dx / ed + location.longitude * Math.PI / 180.0) * 180.0 / Math.PI
            val newLat = (dy / ec + location.latitude * Math.PI / 180.0) * 180.0 / Math.PI

            val newLocation = BLocation(newLat, newLng)

            
            BLocationManager.get().setLocation(userId, packageName, newLocation)

            Log.d(TAG, "Location updated - New: ${newLat}, ${newLng}")
        } catch (e: Exception) {
            Log.e(TAG, "Error changing location: ${e.message}")
            Log.e(TAG, "Stack trace: ", e)
        }
    }

    
    fun isActive(): Boolean {
        return isInitialized
    }

    
    fun checkPermissions(context: Context): Boolean {
        return try {
            
            val hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
            if (!hasOverlayPermission) {
                Log.w(
                        TAG,
                        "Overlay permission not granted - RockerManager cannot show floating view"
                )
                return false
            }

            
            val hasLocationPermission =
                    context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
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

    
    fun getRequiredPermissions(): List<String> {
        return listOf(
                android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    
    fun cleanup() {
        try {
            isInitialized = false
            Log.d(TAG, "RockerManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}
