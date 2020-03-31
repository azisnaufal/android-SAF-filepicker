package com.oazisn.filepicker.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.oazisn.filepicker.FilePickerConstants.Companion.URI
import com.oazisn.filepicker.processors.tasks.GetGoogleDriveFileTask
import com.oazisn.filepicker.utils.loge

/**
 * Created by bison on 31/10/17.
 */
class GoogleDriveProcessor : IUriProcessor {
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
        GetGoogleDriveFileTask(context, uri, object : GetGoogleDriveFileTask.TaskListener {
            override fun didSucceed(newPath: String?) {
                val intent = Intent()
                if (mimeType != null) intent.putExtra("mimeType", mimeType)
                intent.putExtra(URI, newPath)
                uriProcessListener?.onProcessingSuccess(intent)
            }

            override fun didFail() {
                uriProcessListener?.onProcessingFailure()
            }
        }).execute()
    }

    companion object {
        val TAG = GoogleDriveProcessor::class.java.simpleName
        private fun isValidUri(uri: Uri?): Boolean {
            return "com.google.android.apps.docs.storage" == uri!!.authority || "com.google.android.apps.docs.files" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
        }
    }
}