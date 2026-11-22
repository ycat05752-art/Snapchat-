package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PrivateMediaStorage {

    private fun getSnapsDir(context: Context): File {
        val dir = File(context.filesDir, "private_snaps")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getVoiceDir(context: Context): File {
        val dir = File(context.filesDir, "private_voice")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun savePrivateBitmap(
        context: Context,
        bitmap: Bitmap,
        quality: Int = 92
    ): File = withContext(Dispatchers.IO) {
        val file = File(getSnapsDir(context), "snap_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        file
    }

    suspend fun copyUriToPrivateFile(
        context: Context,
        sourceUri: Uri,
        extension: String = "jpg"
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dest = File(getSnapsDir(context), "import_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            if (dest.exists() && dest.length() > 0) dest else null
        } catch (_: Exception) {
            null
        }
    }

    fun createPrivateVideoFile(context: Context): File {
        return File(getSnapsDir(context), "vid_${System.currentTimeMillis()}.mp4")
    }

    fun createPrivateVoiceFile(context: Context): File {
        return File(getVoiceDir(context), "voice_${System.currentTimeMillis()}.m4a")
    }

    @Suppress("DEPRECATION")
    fun createAudioRecorder(context: Context, outputFile: File): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile(outputFile.absolutePath)
        recorder.prepare()
        return recorder
    }

    suspend fun exportToDeviceGallery(
        context: Context,
        file: File,
        isVideo: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext false
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) "${Environment.DIRECTORY_MOVIES}/NILESH_SNAPCHAT"
                        else "${Environment.DIRECTORY_PICTURES}/NILESH_SNAPCHAT"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val collection = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val itemUri = resolver.insert(collection, values) ?: return@withContext false
            resolver.openOutputStream(itemUri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun sharePrivateFile(context: Context, file: File, isVideo: Boolean) {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (isVideo) "video/mp4" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via NILESH SNAPCHAT"))
    }

    fun calculateCacheSizeBytes(context: Context): Long {
        fun dirSize(f: File): Long =
            if (f.isDirectory) f.listFiles()?.sumOf { dirSize(it) } ?: 0L else f.length()
        return dirSize(context.cacheDir)
    }

    fun clearAppCache(context: Context): Long {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        return calculateCacheSizeBytes(context)
    }

    fun deleteFileQuietly(path: String) {
        try {
            File(path).delete()
        } catch (_: Exception) {
        }
    }
}
