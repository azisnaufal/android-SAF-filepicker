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
import androidx.annotation.RequiresApi
import com.oazisn.filepicker.FilePickerConstants.Companion.URI
import com.oazisn.filepicker.utils.logd
import com.oazisn.filepicker.utils.loge
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class FilePickerUriHelper {
    fun isPDF(file: File): Boolean {
        return try {
            val inputStream: InputStream = FileInputStream(file)
            val buf = ByteArray(4)

            // getting file signature
            if (inputStream.read(buf, 0, 4) == 4) {
                // compare every byte with 25 50 44 46 (PDF File signature)
                if (buf[0].compareTo(0x25) == 0 && buf[1].compareTo(0x50) == 0 && buf[2].compareTo(
                        0x44
                    ) == 0 && buf[3].compareTo(0x46) == 0
                ) {
                    inputStream.close()
                    return true
                }
            }
            inputStream.close()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        val TAG = FilePickerUriHelper::class.java.simpleName

        fun getUriString(intent: Intent): String? {
            return if (intent.data != null) {
                intent.data.toString()
            } else intent.extras!!.getString(URI)
        }

        fun getUri(intent: Intent): Uri = Uri.parse(getUriString(intent))

        fun getFile(context: Context, intent: Intent): File? =
            getFile(context, getUriString(intent))

        fun getFile(context: Context, uri: Uri): File? = getFile(context, uri.toString())

        @TargetApi(19)
        private fun getFile(
            context: Context,
            uriString: String?
        ): File? {
            val filePath = getFilePath(context, uriString) ?: return null
            return File(filePath)
        }

        @TargetApi(19)
        private fun getFilePath(
            context: Context,
            uriString: String?
        ): String? {
            if (!uriString.isNullOrEmpty()) {
//                1st attempt
                val fileCheck = File(uriString)
                if (fileCheck.exists()) {
                    return uriString
                }

//                2nd attemp
                val uri = Uri.parse(uriString) ?: return null
                val uriPath = uri.path
                if (!uriPath.isNullOrEmpty() && File(uriPath).exists()) {
                    return uri.path
                }

//                3rd attempt
                var filePath: String? = null
                var cursor: Cursor? = null
                // Used the new photos app which uses a different API
                if (uriString.contains("providers.media.documents/")) {
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
            } else {
                return null
            }
        }

        fun makeImageUri(): Uri {
            val dateFormat =
                SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
            val fileName = dateFormat.format(Date()) + ".jpg"
            val photo =
                File(Environment.getExternalStorageDirectory(), fileName)
            return Uri.fromFile(photo)
        }

        fun getFileType(context: Context?, uri: Uri?): String? {
            return try {
                var mimeType = context!!.contentResolver.getType(uri!!)
                val extension: String?
                logd("uri: $uri")
                if (uri.scheme == null) {
                    logd("Uri.scheme == null")
                }
                if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    loge("mime.getExtensionFromMimeType: $mimeType")
                    // WORKAROUND: we got a device with a buggy cam app that sets the incorrect mimetype, correct it
                    if ("image/jpg".contentEquals(mimeType!!)) {
                        mimeType = "image/jpeg"
                    }
                    val mime = MimeTypeMap.getSingleton()
                    extension = mime.getExtensionFromMimeType(mimeType)
                    logd("extension: $extension")
                } else {
                    logd("MimeTypeMap.getFileExtensionFromUrl")
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

        @RequiresApi(Build.VERSION_CODES.N)
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
            val openableMimeTypes = resolver.getStreamTypes(uri!!, mimeTypeFilter!!)
            if (openableMimeTypes == null || openableMimeTypes.isEmpty()) {
                throw FileNotFoundException()
            }
            return resolver
                .openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)!!
                .createInputStream()
        }
    }
}