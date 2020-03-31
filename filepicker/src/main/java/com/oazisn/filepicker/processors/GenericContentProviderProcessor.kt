package com.oazisn.filepicker.processors

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.oazisn.filepicker.FilePickerConstants.Companion.URI
import com.oazisn.filepicker.uriHelper.FilePickerUriHelper
import com.oazisn.filepicker.utils.isDebug
import com.oazisn.filepicker.utils.logd
import com.oazisn.filepicker.utils.loge
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by bison on 31/10/17.
 */
class GenericContentProviderProcessor : IUriProcessor {
    var uriProcessListener: UriProcessListener? = null

    @SuppressLint("NewApi")
    override fun process(
        context: Context?,
        uri: Uri?,
        uriProcessListener: UriProcessListener?
    ) {
        this.uriProcessListener = uriProcessListener
        if (!isValidUri(uri)) {
            if (uriProcessListener != null) {
                loge("URI not recognized, bailing out")
                uriProcessListener.onProcessingFailure()
                return
            }
        }
        val mimeType = context!!.contentResolver.getType(uri!!)
        try {
            var filename: String? = null
            if (isDropboxFilecache(uri)) {
                logd("Uri is dropbox filecache")
                filename = getSAFDisplayName(context, uri)
            }
            if (isExternalStorageDocument(uri) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && filename == null) {
                logd("isExternalStorageDocument")
                val docId: String
                docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    filename = getLastSegmentString(
                        Environment.getExternalStorageDirectory()
                            .toString() + "/" + split[1]
                    )
                }
            }
            if (filename == null) filename = getFilenameFromMediaProvider(context, uri)
            // try alternative method
            if (filename == null) {
                val id = uri.lastPathSegment
                logd("Trying mediastore id $id")
                try {
                    filename = getFilenameFromMediaStore(context, id)
                    try {
                        val ts = stripExtension(filename)!!.toLong()
                        filename = generateDCIMFilename("IMG", ts)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (filename == null && isDownloadsDocument(uri)) {
                val id = uri.lastPathSegment
                logd("Trying public download id $id")
                try {
                    filename = getFilenameFromDownloadProvider(context, id!!.toLong())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (filename == null) {
                filename = getSAFDisplayName(context, uri)
            }
            if (filename == null) {
                val last = uri.lastPathSegment
                if (last != null) {
                    if (isValidFilename(last)) {
                        filename = last
                    }
                }
            }
            val inputStream =
                context.contentResolver.openInputStream(uri)
            val fileExtension = FilePickerUriHelper.getFileType(context, uri)
            logd("fileExtension from contentprovider url is $fileExtension")
            logd("fileName: $filename")
            val filesDir = context.cacheDir
            var file: File? = null
            file = if (filename == null) {
                if (fileExtension != null) File(
                    filesDir,
                    "f" + String.format(
                        "%04d",
                        Random().nextInt(10000)
                    ) + "." + fileExtension
                ) else File(
                    filesDir,
                    "f" + String.format("%04d", Random().nextInt(10000))
                )
            } else {
                if (!filename.contains(".") && fileExtension != null) File(
                    filesDir,
                    "$filename.$fileExtension"
                ) else File(filesDir, filename)
            }
            if (!file.exists()) {
                file.createNewFile()
            }
            val fileOutputStream = FileOutputStream(file)
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var len = 0
            while (inputStream!!.read(buffer).also { len = it } != -1) {
                fileOutputStream.write(buffer, 0, len)
            }
            val intent = Intent()
            if (mimeType != null) intent.putExtra("mimeType", mimeType)
            intent.putExtra(URI, file.absolutePath)
            uriProcessListener?.onProcessingSuccess(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            loge(e.toString())
            uriProcessListener?.onProcessingFailure()
        }
    }

    private fun generateDCIMFilename(
        prefix: String,
        timestamp: Long
    ): String? {
        return try {
            val d = Date(timestamp)
            val sdf =
                SimpleDateFormat("yyyyMMdd_hhmmss", Locale.US)
            prefix + "_" + sdf.format(d)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val ReservedChars =
        arrayOf("|", "\\", "?", "*", "<", "\"", ":", ">")

    private fun isValidFilename(name: String): Boolean {
        for (c in ReservedChars) {
            if (name.indexOf(c) > 0) return false
        }
        return true
    }

    private fun getFilenameFromDownloadProvider(
        context: Context?,
        id: Long
    ): String? {
        return try {
            val contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), id
            )
            var selectedPath =
                getDataColumn(
                    context,
                    contentUri,
                    null,
                    null
                )
            if (selectedPath != null) {
                if (selectedPath.contains("/")) {
                    val parts = selectedPath.split("/").toTypedArray()
                    if (parts.size > 0) {
                        selectedPath = parts[parts.size - 1]
                    }
                }
            }
            selectedPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFilenameFromMediaStore(
        context: Context?,
        id: String?
    ): String? {
        return try {
            if (id == null) return null
            val imageColumns =
                arrayOf(MediaStore.Images.Media.DATA)
            val imageOrderBy: String? = null
            val baseUri: Uri
            val state = Environment.getExternalStorageState()
            baseUri = if (!state.equals(Environment.MEDIA_MOUNTED, ignoreCase = true)) {
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            var selectedPath: String? = null
            var cursor: Cursor? = null
            cursor = context!!.contentResolver.query(
                baseUri,
                imageColumns,
                MediaStore.Images.Media._ID + "=" + id,
                null,
                imageOrderBy
            )
            if (cursor!!.moveToFirst()) {
                selectedPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
            if (selectedPath != null) {
                if (selectedPath.contains("/")) {
                    val parts = selectedPath.split("/").toTypedArray()
                    if (parts.size > 0) {
                        selectedPath = parts[parts.size - 1]
                    }
                }
            }
            selectedPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFilenameFromMediaProvider(
        context: Context?,
        uri: Uri?
    ): String? {
        return try {
            if (uri == null) return null
            if (!uri.lastPathSegment!!.contains(":")) return null
            val id = uri.lastPathSegment!!.split(":").toTypedArray()[1]
            val isVideo =
                uri.lastPathSegment!!.split(":").toTypedArray()[0].contains("video")
            val imageColumns =
                arrayOf(MediaStore.Images.Media.DATA)
            val videoColumns =
                arrayOf(MediaStore.Video.Media.DATA)
            val imageOrderBy: String? = null
            val baseUri: Uri
            val state = Environment.getExternalStorageState()
            baseUri = if (!isVideo) {
                if (!state.equals(Environment.MEDIA_MOUNTED, ignoreCase = true)) {
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (!state.equals(Environment.MEDIA_MOUNTED, ignoreCase = true)) {
                    MediaStore.Video.Media.INTERNAL_CONTENT_URI
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            }
            var selectedPath: String? = null
            var cursor: Cursor? = null
            cursor = if (!isVideo) {
                context!!.contentResolver.query(
                    baseUri, imageColumns,
                    MediaStore.Images.Media._ID + "=" + id, null, imageOrderBy
                )
            } else {
                context!!.contentResolver.query(
                    baseUri, videoColumns,
                    MediaStore.Video.Media._ID + "=" + id, null, imageOrderBy
                )
            }
            if (cursor!!.moveToFirst()) {
                selectedPath = if (!isVideo) {
                    cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                } else {
                    cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                }
            }
            cursor.close()
            if (selectedPath != null) {
                if (selectedPath.contains("/")) {
                    val parts = selectedPath.split("/").toTypedArray()
                    if (parts.size > 0) {
                        selectedPath = parts[parts.size - 1]
                    }
                }
            }
            selectedPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun stripExtension(selectedPath: String?): String? {
        var selectedPath = selectedPath
        if (selectedPath != null) {
            if (selectedPath.contains(".")) {
                val parts = selectedPath.split("\\.").toTypedArray()
                if (parts.size > 0) {
                    selectedPath = parts[0]
                }
            }
        }
        return selectedPath
    }

    private fun getLastSegmentString(selectedPath: String): String? {
        var selectedPath: String? = selectedPath
        if (selectedPath != null) {
            if (selectedPath.contains("/")) {
                val parts = selectedPath.split("/").toTypedArray()
                if (parts.size > 0) {
                    selectedPath = parts[parts.size - 1]
                }
            }
        }
        return selectedPath
    }

    private fun getSAFDisplayName(
        context: Context?,
        uri: Uri?
    ): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return null
        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        var cursor: Cursor? = null
        try {
            cursor = context!!.contentResolver.query(uri!!, null, null, null, null, null)
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                val displayName = cursor.getString(
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                )
                loge("SAF Display Name: $displayName")
                return displayName
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri?): Boolean {
        return "com.android.externalstorage.documents" == uri!!.authority
    }

    private fun isDropboxFilecache(uri: Uri?): Boolean {
        return "com.dropbox.android.FileCache" == uri!!.authority
    }

    private fun isGoogleDrive(uri: Uri): Boolean {
        return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.files" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    private fun isDownloadsDocument(uri: Uri?): Boolean {
        return "com.android.providers.downloads.documents" == uri!!.authority
    }

    companion object {
        val TAG = GenericContentProviderProcessor::class.java.simpleName
        private fun isValidUri(uri: Uri?): Boolean {
            return "content".equals(uri!!.scheme, ignoreCase = true)
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context The context.
         * @param uri The Uri to query.
         * @param selection (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         * @author paulburke
         */
        fun getDataColumn(
            context: Context?, uri: Uri?, selection: String?,
            selectionArgs: Array<String?>?
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                column
            )
            try {
                cursor = context!!.contentResolver.query(
                    uri!!, projection, selection, selectionArgs,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    if (isDebug()) DatabaseUtils.dumpCursor(cursor)
                    val column_index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(column_index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }
    }
}