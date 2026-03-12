package com.carcassonne.companion.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Copies an image from a URI (gallery or camera) into the app's private storage.
     * Returns the absolute path to the saved file, or null on failure.
     */
    fun saveImageFromUri(context: Context, uri: Uri, playerId: Int): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Resize to max 512px to save storage
            val scaled = scaleBitmap(bitmap, 512)

            val dir = File(context.filesDir, "avatars")
            dir.mkdirs()
            val file = File(dir, "player_${playerId}_${System.currentTimeMillis()}.jpg")

            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a temp file for camera capture and returns its URI.
     */
    fun createTempImageFile(context: Context): File {
        val dir = File(context.filesDir, "avatars")
        dir.mkdirs()
        return File(dir, "temp_capture_${System.currentTimeMillis()}.jpg")
    }

    fun deleteAvatarFile(path: String?) {
        if (path != null) {
            try { File(path).delete() } catch (_: Exception) {}
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxSize && h <= maxSize) return bitmap
        val ratio = w.toFloat() / h.toFloat()
        val (nw, nh) = if (w > h) Pair(maxSize, (maxSize / ratio).toInt())
                       else Pair((maxSize * ratio).toInt(), maxSize)
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }
}
