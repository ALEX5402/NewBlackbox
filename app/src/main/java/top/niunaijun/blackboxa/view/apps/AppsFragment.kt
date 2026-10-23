package top.niunaijun.blackboxa.view.apps

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cbfg.rvadapter.RVAdapter
import com.afollestad.materialdialogs.MaterialDialog
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.env.BEnvironment
import top.niunaijun.blackbox.utils.FileUtils
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs

class AppsFragment : Fragment() {

    var userID: Int = 0
    private lateinit var viewModel: AppsViewModel
    private lateinit var mAdapter: RVAdapter<AppInfo>
    private val viewBinding: FragmentAppsBinding by inflate()

    companion object {
        private const val TAG = "AppsFragment"
        fun newInstance(userID: Int): AppsFragment {
            val fragment = AppsFragment()
            val bundle = bundleOf("userID" to userID)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, InjectionUtil.getAppsFactory()).get(AppsViewModel::class.java)
        userID = requireArguments().getInt("userID", 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding.stateView.showEmpty()

        mAdapter = RVAdapter<AppInfo>(requireContext(), AppsAdapter()).bind(viewBinding.recyclerView)
        viewBinding.recyclerView.adapter = mAdapter

        val layoutManager = GridLayoutManager(requireContext(), 4)
        viewBinding.recyclerView.layoutManager = layoutManager
        viewBinding.recyclerView.setItemViewCacheSize(20)
        viewBinding.recyclerView.setHasFixedSize(true)

        val touchCallBack = AppsTouchCallBack { from, to ->
            onItemMove(from, to)
            viewModel.updateSortLiveData.postValue(true)
        }
        val itemTouchHelper = ItemTouchHelper(touchCallBack)
        itemTouchHelper.attachToRecyclerView(viewBinding.recyclerView)

        mAdapter.setItemClickListener { _, data, _ ->
            showLoading()
            viewModel.launchApk(data.packageName, userID)
        }

        mAdapter.setItemLongClickListener { _, data, _ ->
            showAppOptionsMenu(data)
        }

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun onStart() {
        super.onStart()
        try {
            BlackBoxCore.get().addServiceAvailableCallback {
                viewModel.getInstalledAppsWithRetry(userID)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering callback: ${e.message}")
        }
        viewModel.getInstalledAppsWithRetry(userID)
    }

    private fun onItemMove(fromPosition: Int, toPosition: Int) {
        val items = mAdapter.getItems()
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) Collections.swap(items, i, i + 1)
        } else {
            for (i in fromPosition downTo toPosition + 1) Collections.swap(items, i, i - 1)
        }
        mAdapter.notifyItemMoved(fromPosition, toPosition)
    }

    /**
     * Menu opsi lengkap (10 Opsi) saat icon ditekan lama (Long Click)
     */
    private fun showAppOptionsMenu(data: AppInfo) {
        val options = arrayOf(
            "🚀 Buka Aplikasi",
            "⏹️ Paksa Berhenti (Force Stop)",
            "🧹 Bersihkan Cache Saja",
            "🗑️ Hapus Semua Data (Clear Data)",
            "💾 Cadangkan Data (Backup ke Storage)",
            "📥 Pulihkan Data (Restore dari Backup)",
            "📱 Buat Pintasan Desktop",
            "📋 Salin Nama Package",
            "ℹ️ Detail & Info Aplikasi",
            "❌ Copot Pemasangan (Uninstall)"
        )

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle(data.name ?: data.packageName)
        builder.setItems(options) { dialog, which ->
            dialog.dismiss()
            handleMenuChoice(which, data)
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun handleMenuChoice(index: Int, data: AppInfo) {
        when (index) {
            0 -> {
                showLoading()
                viewModel.launchApk(data.packageName, userID)
            }
            1 -> stopApk(data)
            2 -> clearCacheOnly(data)
            3 -> clearApk(data)
            4 -> backupAppData(data)
            5 -> restoreAppData(data)
            6 -> ShortcutUtil.createShortcut(requireContext(), userID, data)
            7 -> copyPackageName(data)
            8 -> showAppDetails(data)
            9 -> {
                if (data.isXpModule) {
                    toast(R.string.uninstall_module_toast)
                } else {
                    unInstallApk(data)
                }
            }
        }
    }

    private fun copyPackageName(data: AppInfo) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Package Name", data.packageName)
        clipboard.setPrimaryClip(clip)
        toast("Tersalin: ${data.packageName}")
    }

    private fun showAppDetails(data: AppInfo) {
        val dataDir = BEnvironment.getDataDir(data.packageName, userID)
        val dataSize = formatSize(getFolderSize(dataDir))
        val message = StringBuilder()
            .append("Nama: ").append(data.name).append("\n")
            .append("Package: ").append(data.packageName).append("\n")
            .append("Ruang (User): ").append(userID).append("\n")
            .append("Ukuran Data: ").append(dataSize).append("\n")
            .append("Path: ").append(dataDir.absolutePath)
            .toString()

        MaterialDialog(requireContext()).show {
            title(text = "Info Aplikasi")
            message(text = message)
            positiveButton(text = "Tutup")
        }
    }

    private fun clearCacheOnly(data: AppInfo) {
        MaterialDialog(requireContext()).show {
            title(text = "Bersihkan Cache")
            message(text = "Hapus file cache untuk ${data.name}?")
            positiveButton(R.string.done) {
                try {
                    val cacheDir = BEnvironment.getDataCacheDir(data.packageName, userID)
                    val extCacheDir = BEnvironment.getExternalDataCacheDir(data.packageName, userID)
                    FileUtils.deleteDir(cacheDir)
                    FileUtils.deleteDir(extCacheDir)
                    toast("Cache untuk ${data.name} berhasil dibersihkan")
                } catch (e: Exception) {
                    toast("Gagal menghapus cache: ${e.message}")
                }
            }
            negativeButton(R.string.cancel)
        }
    }

    private fun backupAppData(data: AppInfo) {
        val backupDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BlackBox_Backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val backupFile = File(backupDir, "${data.packageName}_user${userID}_backup.zip")

        MaterialDialog(requireContext()).show {
            title(text = "Cadangkan Data")
            message(text = "Data aplikasi akan dicadangkan ke:\n${backupFile.absolutePath}")
            positiveButton(text = "Mulai Backup") {
                showLoading()
                Thread {
                    try {
                        BlackBoxCore.get().stopPackage(data.packageName, userID)
                        val sourceDir = BEnvironment.getDataDir(data.packageName, userID)
                        zipFolder(sourceDir, backupFile)
                        activity?.runOnUiThread {
                            hideLoading()
                            toast("Cadangan berhasil dibuat di:\nDownloads/BlackBox_Backups")
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            hideLoading()
                            toast("Backup gagal: ${e.message}")
                        }
                    }
                }.start()
            }
            negativeButton(R.string.cancel)
        }
    }

    private fun restoreAppData(data: AppInfo) {
        val backupDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BlackBox_Backups")
        val backupFile = File(backupDir, "${data.packageName}_user${userID}_backup.zip")

        if (!backupFile.exists()) {
            toast("File backup tidak ditemukan:\n${backupFile.name}")
            return
        }

        MaterialDialog(requireContext()).show {
            title(text = "Pulihkan Data")
            message(text = "Pulihkan data ${data.name} dari file cadangan:\n${backupFile.name}?\nData saat ini akan ditimpa.")
            positiveButton(text = "Pulihkan") {
                showLoading()
                Thread {
                    try {
                        BlackBoxCore.get().stopPackage(data.packageName, userID)
                        val targetDir = BEnvironment.getDataDir(data.packageName, userID)
                        unzipFile(backupFile, targetDir)
                        activity?.runOnUiThread {
                            hideLoading()
                            toast("Pemulihan data berhasil!")
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            hideLoading()
                            toast("Restore gagal: ${e.message}")
                        }
                    }
                }.start()
            }
            negativeButton(R.string.cancel)
        }
    }

    private fun zipFolder(srcFolder: File, destZipFile: File) {
        val zip = ZipOutputStream(FileOutputStream(destZipFile))
        addFolderToZip("", srcFolder, zip)
        zip.flush()
        zip.close()
    }

    private fun addFolderToZip(path: String, srcFolder: File, zip: ZipOutputStream) {
        val files = srcFolder.listFiles() ?: return
        for (file in files) {
            val currentPath = if (path.isEmpty()) file.name else "$path/${file.name}"
            if (file.isDirectory) {
                addFolderToZip(currentPath, file, zip)
            } else {
                val buffer = ByteArray(4096)
                val inStream = FileInputStream(file)
                zip.putNextEntry(ZipEntry(currentPath))
                var len: Int
                while (inStream.read(buffer).also { len = it } > 0) {
                    zip.write(buffer, 0, len)
                }
                inStream.close()
            }
        }
    }

    private fun unzipFile(zipFile: File, targetDir: File) {
        val zis = ZipInputStream(FileInputStream(zipFile))
        var entry: ZipEntry? = zis.nextEntry
        val buffer = ByteArray(4096)
        while (entry != null) {
            val file = File(targetDir, entry.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                val fos = FileOutputStream(file)
                var len: Int
                while (zis.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
    }

    private fun getFolderSize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return size
    }

    private fun formatSize(size: Long): String {
        val df = java.text.DecimalFormat("#.##")
        val sizeKb = 1024.0
        val sizeMb = sizeKb * 1024.0
        val sizeGb = sizeMb * 1024.0
        return when {
            size >= sizeGb -> "${df.format(size / sizeGb)} GB"
            size >= sizeMb -> "${df.format(size / sizeMb)} MB"
            size >= sizeKb -> "${df.format(size / sizeKb)} KB"
            else -> "$size B"
        }
    }

    private fun initData() {
        viewBinding.stateView.showLoading()
        viewModel.getInstalledApps(userID)
        viewModel.appsLiveData.observe(viewLifecycleOwner) {
            if (it != null) {
                mAdapter.setItems(it)
                if (it.isEmpty()) viewBinding.stateView.showEmpty() else viewBinding.stateView.showContent()
            }
        }

        viewModel.resultLiveData.observe(viewLifecycleOwner) {
            if (!TextUtils.isEmpty(it)) {
                hideLoading()
                requireContext().toast(it)
                viewModel.getInstalledApps(userID)
                scanUser()
            }
        }

        viewModel.launchLiveData.observe(viewLifecycleOwner) {
            it?.run {
                hideLoading()
                if (!it) toast(R.string.start_fail)
            }
        }

        viewModel.updateSortLiveData.observe(viewLifecycleOwner) {
            if (this::mAdapter.isInitialized) {
                viewModel.updateApkOrder(userID, mAdapter.getItems())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.resultLiveData.value = null
        viewModel.launchLiveData.value = null
    }

    private fun unInstallApk(info: AppInfo) {
        MaterialDialog(requireContext()).show {
            title(R.string.uninstall_app)
            message(text = getString(R.string.uninstall_app_hint, info.name))
            positiveButton(R.string.done) {
                showLoading()
                viewModel.unInstall(info.packageName, userID)
            }
            negativeButton(R.string.cancel)
        }
    }

    private fun stopApk(info: AppInfo) {
        MaterialDialog(requireContext()).show {
            title(R.string.app_stop)
            message(text = getString(R.string.app_stop_hint, info.name))
            positiveButton(R.string.done) {
                BlackBoxCore.get().stopPackage(info.packageName, userID)
                toast(getString(R.string.is_stop, info.name))
            }
            negativeButton(R.string.cancel)
        }
    }

    private fun clearApk(info: AppInfo) {
        MaterialDialog(requireContext()).show {
            title(R.string.app_clear)
            message(text = getString(R.string.app_clear_hint, info.name))
            positiveButton(R.string.done) {
                showLoading()
                viewModel.clearApkData(info.packageName, userID)
            }
            negativeButton(R.string.cancel)
        }
    }

    fun installApk(source: String) {
        showLoading()
        viewModel.install(source, userID)
    }

    private fun scanUser() {
        (requireActivity() as? MainActivity)?.scanUser()
    }

    private fun showLoading() {
        if (requireActivity() is LoadingActivity) {
            (requireActivity() as LoadingActivity).showLoading()
        }
    }

    private fun hideLoading() {
        if (requireActivity() is LoadingActivity) {
            (requireActivity() as LoadingActivity).hideLoading()
        }
    }
}
