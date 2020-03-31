package com.oazisn.filepicker.processors.tasks

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import com.oazisn.filepicker.uriHelper.FilePickerUriHelper
import com.oazisn.filepicker.utils.loge
import java.io.*

/**
 * Created by joso on 26/03/15.
 */
class GetPhotosTask(
    private val context: Context?,
    private val uri: Uri?,
    private val listener: PhotosListener?
) : AsyncTask<Void?, String?, String?>() {
    override fun doInBackground(vararg params: Void?): String? {
        try {
            val parcelFileDescriptor = context!!.contentResolver
                .openFileDescriptor(uri!!, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val inputStream: InputStream = FileInputStream(fileDescriptor)
            val reader = BufferedInputStream(inputStream)
            val outputPath = FilePickerUriHelper.makeImageUri().toString()

            // Create an output stream to a file that you want to save to
            val outStream =
                BufferedOutputStream(FileOutputStream(outputPath))
            val buf = ByteArray(2048)
            var len: Int
            while (reader.read(buf).also { len = it } > 0) {
                outStream.write(buf, 0, len)
            }
            return outputPath
        } catch (e: Exception) {
            loge(e.toString())
        }
        return null
    }

    override fun onPostExecute(s: String?) {
        if (listener != null && context != null) {
            if (s != null) {
                listener.didDownloadBitmap(s)
            } else {
                listener.didFail()
            }
        }
    }

    interface PhotosListener {
        fun didDownloadBitmap(path: String?)
        fun didFail()
    }

    companion object {
        val TAG = GetPhotosTask::class.java.simpleName
    }

}