package top.niunaijun.blackboxa.view.apps

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import cbfg.rvadapter.RVHolder
import cbfg.rvadapter.RVHolderFactory
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.databinding.ItemAppBinding
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.RecyclerView

/**
 *
 * @Description: 软件显示界面适配器
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 21:52
 */

class AppsAdapter : RVHolderFactory() {
    
    companion object {
        private const val TAG = "AppsAdapter"
        private const val MAX_ICON_SIZE = 96 // 48dp * 2 for high density
        private val DEFAULT_ICON_COLOR = Color.parseColor("#CCCCCC")
    }

    override fun createViewHolder(parent: ViewGroup?, viewType: Int, item: Any): RVHolder<out Any> {
        return try {
            AppsVH(inflate(R.layout.item_app, parent))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ViewHolder: ${e.message}")
            // Return a fallback ViewHolder to prevent crash
            FallbackAppsVH(inflate(R.layout.item_app, parent))
        }
    }

    class AppsVH(itemView: View) : RVHolder<AppInfo>(itemView) {
        val binding = ItemAppBinding.bind(itemView)
        private var currentIcon: Drawable? = null
        private var isAttached = false

        init {
            try {
                // Optimize icon loading for better scrolling performance
                binding.icon.scaleType = ImageView.ScaleType.CENTER_CROP
                
                // Add view tree observer to optimize icon loading
                itemView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        if (isAttached) {
                            itemView.viewTreeObserver.removeOnPreDrawListener(this)
                        }
                        return true
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ViewHolder: ${e.message}")
            }
        }

        override fun setContent(item: AppInfo, isSelected: Boolean, payload: Any?) {
            try {
                // Safely set the icon with optimized loading
                setIconSafely(item.icon, item.packageName)
                
                // Safely set the name
                binding.name.text = item.name ?: "Unknown App"
                
                // Handle XP module label
                if (item.isXpModule) {
                    binding.cornerLabel.visibility = View.VISIBLE
                } else {
                    binding.cornerLabel.visibility = View.INVISIBLE
                }
                
                // Mark as attached for optimization
                isAttached = true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error setting content for ${item.packageName}: ${e.message}")
                setSafeDefaults()
            }
        }

        private fun setIconSafely(icon: Drawable?, packageName: String) {
            try {
                if (icon != null) {
                    // Optimize icon for memory efficiency
                    val optimizedIcon = optimizeIcon(icon)
                    binding.icon.setImageDrawable(optimizedIcon)
                    currentIcon = optimizedIcon
                } else {
                    // Set a default placeholder icon
                    binding.icon.setImageDrawable(createDefaultIcon())
                    currentIcon = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set icon for $packageName: ${e.message}")
                binding.icon.setImageDrawable(createDefaultIcon())
                currentIcon = null
            }
        }

        private fun optimizeIcon(icon: Drawable): Drawable {
            return try {
                // If it's a BitmapDrawable, ensure it's not too large
                if (icon is BitmapDrawable) {
                    val bitmap = icon.bitmap
                    if (bitmap.width > MAX_ICON_SIZE || bitmap.height > MAX_ICON_SIZE) {
                        // Create a scaled down version
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            bitmap, MAX_ICON_SIZE, MAX_ICON_SIZE, true
                        )
                        BitmapDrawable(itemView.resources, scaledBitmap)
                    } else {
                        icon
                    }
                } else {
                    icon
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error optimizing icon: ${e.message}")
                icon
            }
        }

        private fun createDefaultIcon(): Drawable {
            return try {
                ColorDrawable(DEFAULT_ICON_COLOR)
            } catch (e: Exception) {
                Log.w(TAG, "Error creating default icon: ${e.message}")
                ColorDrawable(Color.GRAY)
            }
        }

        private fun setSafeDefaults() {
            try {
                binding.icon.setImageDrawable(createDefaultIcon())
                binding.name.text = "Unknown App"
                binding.cornerLabel.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.e(TAG, "Error setting safe defaults: ${e.message}")
            }
        }
    }

    // Fallback ViewHolder for emergency cases
    class FallbackAppsVH(itemView: View) : RVHolder<AppInfo>(itemView) {
        val binding = ItemAppBinding.bind(itemView)

        override fun setContent(item: AppInfo, isSelected: Boolean, payload: Any?) {
            try {
                // Set minimal content to prevent crash
                binding.icon.setImageDrawable(ColorDrawable(DEFAULT_ICON_COLOR))
                binding.name.text = item.name ?: "Unknown App"
                binding.cornerLabel.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.e(TAG, "Error in fallback ViewHolder: ${e.message}")
            }
        }
    }
}