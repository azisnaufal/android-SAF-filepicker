package com.oazisn.filepicker.processors

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.oazisn.filepicker.FilePickerConstants.Companion.URI
import com.oazisn.filepicker.utils.loge

/**
 * Created by bison on 31/10/17.
 */
class GoogleDocumentsProcessor : IUriProcessor {
    var uriProcessListener: UriProcessListener? = null
    override fun process(
        context: Context?,
        uri: Uri?,
        uriProcessListener: UriProcessListener?
    ) {
        this.uriProcessListener = uriProcessListener
        if (!isValidUri(uri)) {
            if (uriProcessListener != null) {
                loge(
                    "URI not recognized, bailing out"
                )
                uriProcessListener.onProcessingFailure()
                return
            }
        }
        val mimeType = context!!.contentResolver.getType(uri!!)
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
        var selectedPath = "path"
        var cursor: Cursor? = null
        cursor = if (!isVideo) {
            context.contentResolver.query(
                baseUri, imageColumns,
                MediaStore.Images.Media._ID + "=" + id, null, imageOrderBy
            )
        } else {
            context.contentResolver.query(
                baseUri, videoColumns,
                MediaStore.Video.Media._ID + "=" + id, null, imageOrderBy
            )
        }
        if (cursor == null) {
            loge("cursor is null")
            uriProcessListener?.onProcessingFailure()
            return
        }
        if (cursor.moveToFirst()) {
            selectedPath = if (!isVideo) {
                cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            } else {
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
            }
        }
        cursor.close()
        val intent = Intent()
        if (mimeType != null) intent.putExtra("mimeType", mimeType)
        intent.putExtra(URI, Uri.parse(selectedPath))
        uriProcessListener?.onProcessingSuccess(intent)
    }

    companion object {
        val TAG = GoogleDocumentsProcessor::class.java.simpleName
        private fun isValidUri(uri: Uri?): Boolean {
            return "com.google.android.providers.media.documents" == uri!!.authority
        }
    }
}