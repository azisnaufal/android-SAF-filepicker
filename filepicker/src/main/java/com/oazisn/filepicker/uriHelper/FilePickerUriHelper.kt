package com.oazisn.filepicker.uriHelper

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import dk.nodes.filepicker.FilePickerConstants.URI
import dk.nodes.filepicker.utils.Logger
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class FilePickerUriHelper {
    fun detectPDF(file: File?): Boolean {
        return try {
            if (file == null) return false
            val `is`: InputStream = FileInputStream(file)
            val buf = ByteArray(4)

            // 25 50 44 46
            if (`is`.read(buf, 0, 4) == 4) {
                if (buf[0] == 0x25 && buf[1] == 0x50 && buf[2] == 0x44 && buf[3] == 0x46
                ) {
                    `is`.close()
                    return true
                }
            }
            `is`.close()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        val TAG = FilePickerUriHelper::class.java.simpleName
        fun getUriString(@NonNull intent: Intent): String? {
            return if (intent.data != null) {
                intent.data.toString()
            } else intent.extras!!.getString(URI)
        }

        fun getUri(@NonNull intent: Intent): Uri {
            return Uri.parse(getUriString(intent))
        }

        fun getFile(
            @NonNull context: Context,
            @NonNull intent: Intent
        ): File? {
            return getFile(
                context,
                getUriString(intent)
            )
        }

        fun getFile(
            @NonNull context: Context,
            @NonNull uri: Uri
        ): File? {
            return getFile(context, uri.toString())
        }

        @TargetApi(19)
        fun getFile(
            @NonNull context: Context,
            @NonNull uriString: String?
        ): File? {
            val filePath =
                getFilePath(context, uriString) ?: return null
            return File(filePath)
        }

        @TargetApi(19)
        private fun getFilePath(
            @NonNull context: Context,
            @NonNull uriString: String?
        ): String? {
            val fileCheck = File(uriString)
            if (fileCheck.exists()) {
                return uriString
            }
            var filePath: String? = null
            val uri = Uri.parse(uriString) ?: return null
            if (File(uri.path).exists()) {
                return uri.path
            }
            var cursor: Cursor? = null
            // Used the new photos app which uses a different API
            if (uriString!!.contains("providers.media.documents/")) {
                // Will return "image:x*"
                val wholeID = DocumentsContract.getDocumentId(uri)
                // Split at colon, use second item in the array
                val id = wholeID.split(":").toTypedArray()[1]
                var column: Array<String>? = null
                if (wholeID.contains("image")) {
                    val iColumn =
                        arrayOf(MediaStore.Images.Media.DATA)
                    // where id is equal to
                    val sel = MediaStore.Images.Media._ID + "=?"
                    cursor = context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        iColumn,
                        sel,
                        arrayOf(id),
                        null
                    )
                    column = iColumn
                } else if (wholeID.contains("video")) {
                    val vColumn =
                        arrayOf(MediaStore.Video.Media.DATA)
                    // where id is equal to
                    val videoSel = MediaStore.Video.Media._ID + "=?"
                    cursor = context.contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        vColumn,
                        videoSel,
                        arrayOf(id),
                        null
                    )
                    column = vColumn
                }
                if (cursor == null) { // nor video
                    return null
                }
                val columnIndex = cursor.getColumnIndex(column!![0])
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex)
                }
                cursor.close()
            } else {
                val filePathColumn = arrayOf(
                    MediaStore.Images.Media.DATA,
                    MediaStore.MediaColumns.DATA
                )
                cursor = context.contentResolver.query(uri, filePathColumn, null, null, null)
                if (cursor == null) {
                    return null
                }
                cursor.moveToFirst()
                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                filePath = cursor.getString(columnIndex)
                cursor.close()
            }
            return filePath
        }

        fun makeImageUri(): Uri {
            val dateFormat =
                SimpleDateFormat("yyyyMMdd-HHmmss")
            val fileName = dateFormat.format(Date()) + ".jpg"
            val photo =
                File(Environment.getExternalStorageDirectory(), fileName)
            return Uri.fromFile(photo)
        }

        fun getFileType(context: Context?, uri: Uri?): String? {
            return try {
                var mimeType = context!!.contentResolver.getType(uri!!)
                val extension: String?
                Logger.logd(TAG, "uri: $uri")
                if (uri.scheme == null) {
                    Logger.logd(TAG, "Uri.scheme == null")
                }
                if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    Logger.loge(
                        TAG,
                        "mime.getExtensionFromMimeType: $mimeType"
                    )
                    // WORKAROUND: we got a device with a buggy cam app that sets the incorrect mimetype, correct it
                    if ("image/jpg".contentEquals(mimeType!!)) {
                        mimeType = "image/jpeg"
                    }
                    val mime = MimeTypeMap.getSingleton()
                    extension = mime.getExtensionFromMimeType(mimeType)
                    Logger.logd(TAG, "extension: $extension")
                } else {
                    Logger.logd(
                        TAG,
                        "MimeTypeMap.getFileExtensionFromUrl"
                    )
                    extension = MimeTypeMap.getFileExtensionFromUrl(
                        Uri.fromFile(
                            File(uri.path)
                        ).toString()
                    )
                }
                extension
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun isVirtualFile(
            context: Context?,
            uri: Uri?
        ): Boolean {
            // virtual files is an android 7 thing
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                return false
            }
            if (!DocumentsContract.isDocumentUri(context, uri)) {
                return false
            }
            val cursor = context!!.contentResolver.query(
                uri!!, arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
                null, null, null
            )
            var flags = 0
            if (cursor!!.moveToFirst()) {
                flags = cursor.getInt(0)
            }
            cursor.close()
            return flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
        }

        @Throws(IOException::class)
        fun getInputStreamForVirtualFile(
            context: Context?,
            uri: Uri?,
            mimeTypeFilter: String?
        ): InputStream {
            val resolver = context!!.contentResolver
            val openableMimeTypes =
                resolver.getStreamTypes(uri!!, mimeTypeFilter!!)
            if (openableMimeTypes == null ||
                openableMimeTypes.size < 1
            ) {
                throw FileNotFoundException()
            }
            return resolver
                .openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
                .createInputStream()
        }
    }
}