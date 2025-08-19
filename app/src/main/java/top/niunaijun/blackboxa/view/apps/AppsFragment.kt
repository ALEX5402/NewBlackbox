package top.niunaijun.blackboxa.view.apps

import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cbfg.rvadapter.RVAdapter
import com.afollestad.materialdialogs.MaterialDialog
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.databinding.FragmentAppsBinding
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.ShortcutUtil
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.util.MemoryManager
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.base.LoadingActivity
import top.niunaijun.blackboxa.view.main.MainActivity
import java.util.*
import kotlin.math.abs


/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 22:21
 */
class AppsFragment : Fragment() {

    var userID: Int = 0

    private lateinit var viewModel: AppsViewModel

    private lateinit var mAdapter: RVAdapter<AppInfo>

    private val viewBinding: FragmentAppsBinding by inflate()

    private var popupMenu: PopupMenu? = null

    companion object {
        private const val TAG = "AppsFragment"
        
        fun newInstance(userID:Int): AppsFragment {
            val fragment = AppsFragment()
            val bundle = bundleOf("userID" to userID)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            viewModel =
                ViewModelProvider(this, InjectionUtil.getAppsFactory()).get(AppsViewModel::class.java)
            userID = requireArguments().getInt("userID", 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            viewBinding.stateView.showEmpty()

            mAdapter =
                RVAdapter<AppInfo>(requireContext(), AppsAdapter()).bind(viewBinding.recyclerView)

            viewBinding.recyclerView.adapter = mAdapter
            
            // Enhanced RecyclerView configuration for better performance and crash prevention
            val layoutManager = GridLayoutManager(requireContext(), 4)
            layoutManager.isItemPrefetchEnabled = true
            layoutManager.initialPrefetchItemCount = 8
            viewBinding.recyclerView.layoutManager = layoutManager
            
            // Enable view cache for better scrolling performance
            viewBinding.recyclerView.setItemViewCacheSize(20)
            viewBinding.recyclerView.setHasFixedSize(true)
            
            // Add scroll listener for crash detection and prevention
            viewBinding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    try {
                        super.onScrollStateChanged(recyclerView, newState)
                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                // Scrolling stopped, optimize memory
                                MemoryManager.optimizeMemoryForRecyclerView()
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING -> {
                                // User is scrolling, ensure smooth performance
                                // Note: Drawing cache is deprecated, using modern alternatives
                            }
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                // Scrolling is settling, prepare for idle state
                                // Note: Drawing cache is deprecated, using modern alternatives
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in scroll state change: ${e.message}")
                    }
                }
                
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    try {
                        super.onScrolled(recyclerView, dx, dy)
                        // Monitor scroll performance
                        if (Math.abs(dy) > 100) {
                            // Fast scrolling detected, optimize memory
                            
                            // Check memory usage during fast scrolling
                            if (MemoryManager.isMemoryCritical()) {
                                Log.w(TAG, "Memory critical during fast scrolling, forcing GC")
                                MemoryManager.forceGarbageCollectionIfNeeded()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in scroll: ${e.message}")
                    }
                }
            })

            val touchCallBack = AppsTouchCallBack { from, to ->
                try {
                    onItemMove(from, to)
                    viewModel.updateSortLiveData.postValue(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in touch callback: ${e.message}")
                }
            }

            val itemTouchHelper = ItemTouchHelper(touchCallBack)
            itemTouchHelper.attachToRecyclerView(viewBinding.recyclerView)

            mAdapter.setItemClickListener { _, data, _ ->
                try {
                    showLoading()
                    viewModel.launchApk(data.packageName, userID)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching app: ${e.message}")
                    hideLoading()
                }
            }

            interceptTouch()
            setOnLongClick()
            return viewBinding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}")
            // Return a simple view to prevent crash
            return View(requireContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            super.onViewCreated(view, savedInstanceState)
            initData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated: ${e.message}")
        }
    }

    override fun onStart() {
        try {
            super.onStart()
            
            // Register callback to refresh app list when services become available
            try {
                BlackBoxCore.get().addServiceAvailableCallback {
                    Log.d(TAG, "Services became available, refreshing app list")
                    // Refresh the app list when services are ready
                    viewModel.getInstalledAppsWithRetry(userID)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering service available callback: ${e.message}")
            }
            
            viewModel.getInstalledAppsWithRetry(userID)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStart: ${e.message}")
        }
    }

    /**
     * 拖拽优化
     */
    private fun interceptTouch() {
        try {
            val point = Point()
            var isScrolling = false
            var scrollStartTime = 0L
            
            viewBinding.recyclerView.setOnTouchListener { _, e ->
                try {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Reset scroll state
                            isScrolling = false
                            scrollStartTime = System.currentTimeMillis()
                            point.set(0, 0)
                        }
                        
                        MotionEvent.ACTION_UP -> {
                            val scrollDuration = System.currentTimeMillis() - scrollStartTime
                            
                            // Only show popup if it wasn't a scroll gesture
                            if (!isScrolling && !isMove(point, e) && scrollDuration < 500) {
                                try {
                                    popupMenu?.show()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error showing popup menu: ${e.message}")
                                }
                            }
                            
                            popupMenu = null
                            point.set(0, 0)
                            isScrolling = false
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (point.x == 0 && point.y == 0) {
                                point.x = e.rawX.toInt()
                                point.y = e.rawY.toInt()
                            }
                            
                            // Check if this is a scroll gesture
                            if (isMove(point, e)) {
                                isScrolling = true
                                popupMenu?.dismiss()
                            }
                            
                            // Handle float button visibility
                            isDownAndUp(point, e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in touch listener: ${e.message}")
                }
                return@setOnTouchListener false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in interceptTouch: ${e.message}")
        }
    }

    private fun isMove(point: Point, e: MotionEvent): Boolean {
        return try {
            val max = 40

            val x = point.x
            val y = point.y

            val xU = abs(x - e.rawX)
            val yU = abs(y - e.rawY)
            xU > max || yU > max
        } catch (e: Exception) {
            Log.e(TAG, "Error in isMove: ${e.message}")
            false
        }
    }

    private fun isDownAndUp(point: Point, e: MotionEvent) {
        try {
            val min = 10
            val y = point.y
            val yU = y - e.rawY

            if (abs(yU) > min) {
                try {
                    (requireActivity() as? MainActivity)?.showFloatButton(yU < 0)
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing/hiding float button: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in isDownAndUp: ${e.message}")
        }
    }

    private fun onItemMove(fromPosition: Int, toPosition: Int) {
        try {
            // Validate positions to prevent crashes
            val items = mAdapter.getItems()
            if (fromPosition < 0 || toPosition < 0 || 
                fromPosition >= items.size || toPosition >= items.size) {
                Log.w(TAG, "Invalid positions for move: from=$fromPosition, to=$toPosition, size=${items.size}")
                return
            }
            
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    try {
                        Collections.swap(items, i, i + 1)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error swapping items at position $i: ${e.message}")
                        return
                    }
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    try {
                        Collections.swap(items, i, i - 1)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error swapping items at position $i: ${e.message}")
                        return
                    }
                }
            }
            
            try {
                mAdapter.notifyItemMoved(fromPosition, toPosition)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying item moved: ${e.message}")
                // Fallback to full refresh if move notification fails
                mAdapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onItemMove: ${e.message}")
        }
    }

    private fun setOnLongClick() {
        try {
            mAdapter.setItemLongClickListener { view, data, _ ->
                try {
                    popupMenu = PopupMenu(requireContext(),view).also {
                        it.inflate(R.menu.app_menu)
                        it.setOnMenuItemClickListener { item ->
                            try {
                                when (item.itemId) {
                                    R.id.app_remove -> {
                                        if (data.isXpModule) {
                                            toast(R.string.uninstall_module_toast)
                                        } else {
                                            unInstallApk(data)
                                        }
                                    }

                                    R.id.app_clear -> {
                                        clearApk(data)
                                    }

                                    R.id.app_stop -> {
                                        stopApk(data)
                                    }

                                    R.id.app_shortcut -> {
                                        ShortcutUtil.createShortcut(requireContext(), userID, data)
                                    }
                                }
                                return@setOnMenuItemClickListener true
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in menu item click: ${e.message}")
                                return@setOnMenuItemClickListener false
                            }
                        }
                        it.show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in long click: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setOnLongClick: ${e.message}")
        }
    }
    
    private fun initData() {
        try {
            viewBinding.stateView.showLoading()
            viewModel.getInstalledApps(userID)
            viewModel.appsLiveData.observe(viewLifecycleOwner) {
                try {
                    if (it != null) {
                        mAdapter.setItems(it)
                        if (it.isEmpty()) {
                            viewBinding.stateView.showEmpty()
                        } else {
                            viewBinding.stateView.showContent()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing apps data: ${e.message}")
                }
            }

            viewModel.resultLiveData.observe(viewLifecycleOwner) {
                try {
                    if (!TextUtils.isEmpty(it)) {
                        hideLoading()
                        requireContext().toast(it)
                        viewModel.getInstalledApps(userID)
                        scanUser()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing result data: ${e.message}")
                }
            }

            viewModel.launchLiveData.observe(viewLifecycleOwner) {
                try {
                    it?.run {
                        hideLoading()
                        if (!it) {
                            toast(R.string.start_fail)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing launch data: ${e.message}")
                }
            }

            viewModel.updateSortLiveData.observe(viewLifecycleOwner) {
                try {
                    if (this::mAdapter.isInitialized) {
                        viewModel.updateApkOrder(userID, mAdapter.getItems())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing sort data: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initData: ${e.message}")
        }
    }

    override fun onStop() {
        try {
            super.onStop()
            viewModel.resultLiveData.value = null
            viewModel.launchLiveData.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStop: ${e.message}")
        }
    }

    private fun unInstallApk(info: AppInfo) {
        try {
            MaterialDialog(requireContext()).show {
                title(R.string.uninstall_app)
                message(text = getString(R.string.uninstall_app_hint, info.name))
                positiveButton(R.string.done) {
                    try {
                        showLoading()
                        viewModel.unInstall(info.packageName, userID)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uninstalling app: ${e.message}")
                        hideLoading()
                    }
                }
                negativeButton(R.string.cancel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing uninstall dialog: ${e.message}")
        }
    }

    /**
     * 强行停止软件
     * @param info AppInfo
     */
    private fun stopApk(info: AppInfo) {
        try {
            MaterialDialog(requireContext()).show {
                title(R.string.app_stop)
                message(text = getString(R.string.app_stop_hint,info.name))
                positiveButton(R.string.done) {
                    try {
                        BlackBoxCore.get().stopPackage(info.packageName, userID)
                        toast(getString(R.string.is_stop,info.name))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping app: ${e.message}")
                    }
                }
                negativeButton(R.string.cancel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing stop dialog: ${e.message}")
        }
    }

    /**
     * 清除软件数据
     * @param info AppInfo
     */
    private fun clearApk(info: AppInfo) {
        try {
            MaterialDialog(requireContext()).show {
                title(R.string.app_clear)
                message(text = getString(R.string.app_clear_hint,info.name))
                positiveButton(R.string.done) {
                    try {
                        showLoading()
                        viewModel.clearApkData(info.packageName, userID)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error clearing app data: ${e.message}")
                        hideLoading()
                    }
                }
                negativeButton(R.string.cancel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing clear dialog: ${e.message}")
        }
    }

    fun installApk(source: String) {
        try {
            showLoading()
            viewModel.install(source, userID)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK: ${e.message}")
            hideLoading()
        }
    }

    private fun scanUser() {
        try {
            (requireActivity() as? MainActivity)?.scanUser()
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning user: ${e.message}")
        }
    }

    private fun showLoading() {
        try {
            if(requireActivity() is LoadingActivity){
                (requireActivity() as LoadingActivity).showLoading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading: ${e.message}")
        }
    }

    private fun hideLoading() {
        try {
            if(requireActivity() is LoadingActivity){
                (requireActivity() as LoadingActivity).hideLoading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding loading: ${e.message}")
        }
    }
}
