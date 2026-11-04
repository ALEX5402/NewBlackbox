package top.niunaijun.blackboxa.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.env.BEnvironment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SnapshotManager {
    private const val TAG = "SnapshotManager"

    suspend fun createSnapshot(context: Context, destinationUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val outputStream: OutputStream =
                    context.contentResolver.openOutputStream(destinationUri)
                        ?: return@withContext Result.failure(Exception("Could not open destination stream"))

                ZipOutputStream(outputStream.buffered()).use { zipOut ->
                    val rootDir = BEnvironment.getVirtualRoot()
                    if (rootDir.exists()) {
                        zipDirectory(rootDir, rootDir.absolutePath, zipOut)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating snapshot", e)
                Result.failure(e)
            }
        }

    suspend fun restoreSnapshot(context: Context, sourceUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream =
                    context.contentResolver.openInputStream(sourceUri)
                        ?: return@withContext Result.failure(Exception("Could not open source stream"))

                val rootDir = BEnvironment.getVirtualRoot()
                if (!rootDir.exists()) {
                    rootDir.mkdirs()
                }

                ZipInputStream(inputStream.buffered()).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        val newFile = File(rootDir, entry.name)

                        // Protect against Zip Slip vulnerability
                        val canonicalDest = rootDir.canonicalPath
                        val canonicalEntry = newFile.canonicalPath
                        if (!canonicalEntry.startsWith(canonicalDest)) {
                            throw SecurityException("Zip entry is outside target dir: " + entry.name)
                        }

                        if (entry.isDirectory) {
                            if (!newFile.isDirectory && !newFile.mkdirs()) {
                                Log.w(TAG, "Failed to create directory: " + newFile.absolutePath)
                            }
                        } else {
                            val parent = newFile.parentFile
                            if (parent != null && !parent.exists()) {
                                parent.mkdirs()
                            }
                            FileOutputStream(newFile).use { fileOut ->
                                zipIn.copyTo(fileOut)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring snapshot", e)
                Result.failure(e)
            }
        }

    private fun zipDirectory(fileToZip: File, rootPath: String, zipOut: ZipOutputStream) {
        if (fileToZip.isHidden) {
            return
        }

        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles() ?: return
            for (child in children) {
                zipDirectory(child, rootPath, zipOut)
            }
        } else {
            val relativePath = fileToZip.absolutePath.substring(rootPath.length).trimStart(File.separatorChar)
            val zipEntry = ZipEntry(relativePath)
            zipOut.putNextEntry(zipEntry)
            FileInputStream(fileToZip).use { fis ->
                fis.copyTo(zipOut)
            }
            zipOut.closeEntry()
        }
    }
}
