package com.example.facegrid.data.gallery

import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.facegrid.domain.model.SavedCollage
import com.example.facegrid.domain.repository.GalleryRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreGalleryRepository(context: Context) : GalleryRepository {
    private val appContext = context.applicationContext

    override suspend fun listSaved(): List<SavedCollage> = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val (selection, selectionArgs) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?" to
                    arrayOf("${Environment.DIRECTORY_PICTURES}/FaceGrid/", "facegrid_%")
        } else {
            "${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?" to
                    arrayOf(
                        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/FaceGrid/%",
                        "facegrid_%"
                    )
        }
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SavedCollage(
                            uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                cursor.getLong(idColumn)
                            ),
                            displayName = cursor.getString(nameColumn),
                            dateAddedSeconds = cursor.getLong(dateColumn)
                        )
                    )
                }
            }
        } ?: emptyList()
    }

    override suspend fun save(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val filename = "facegrid_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/FaceGrid"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "FaceGrid"
                )
                directory.mkdirs()
                put(MediaStore.Images.Media.DATA, File(directory, filename).absolutePath)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Gallery refused the new image")
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        95,
                        stream
                    )
                ) error("Could not encode collage")
            } ?: error("Could not open gallery output")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }
            uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
