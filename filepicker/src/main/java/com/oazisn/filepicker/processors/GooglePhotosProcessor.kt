package com.oazisn.filepicker.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.oazisn.filepicker.FilePickerConstants.Companion.URI
import com.oazisn.filepicker.processors.tasks.GetPhotosTask
import com.oazisn.filepicker.processors.tasks.GetPhotosTask.PhotosListener
import com.oazisn.filepicker.utils.loge

/**
 * Created by bison on 31/10/17.
 */
class GooglePhotosProcessor : IUriProcessor {
    var uriProcessListener: UriProcessListener? = null
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
        GetPhotosTask(context, uri, object : PhotosListener {
            override fun didDownloadBitmap(path: String?) {
                val intent = Intent()
                if (mimeType != null) intent.putExtra("mimeType", mimeType)
                intent.putExtra(URI, path)
                uriProcessListener?.onProcessingSuccess(intent)
            }

            override fun didFail() {
                uriProcessListener?.onProcessingFailure()
                return
            }
        }).execute()
    }

    companion object {
        val TAG = GooglePhotosProcessor::class.java.simpleName
        private fun isValidUri(uri: Uri?): Boolean {
            return "com.google.android.apps.photos.content" == uri!!.authority
        }
    }
}