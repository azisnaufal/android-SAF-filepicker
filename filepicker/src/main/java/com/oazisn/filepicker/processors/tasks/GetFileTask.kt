package com.oazisn.filepicker.processors.tasks

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import com.oazisn.filepicker.utils.loge
import java.io.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Nicolaj on 15-10-2015.
 */
class GetFileTask(
    private val context: Context?,
    private val uri: Uri?,
    private val listener: TaskListener?
) : AsyncTask<Void?, String?, String?>() {
    override fun doInBackground(vararg params: Void?): String? {
        val cacheDir =
            File(Environment.getExternalStorageDirectory(), "eboks")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val fileName = "IMG_" + dateFormat.format(Date()) + ".jpg"
        val f = File(cacheDir, fileName)
        return try {
            val `is`: InputStream?
            `is` = if (uri.toString().startsWith("content://com.google.android")) {
                getSourceStream(uri, context)
            } else {
                URL(uri.toString()).openStream()
            }
            val os: OutputStream = FileOutputStream(f)
            copyStream(`is`, os)
            os.close()
            Uri.fromFile(f).toString()
        } catch (ex: Exception) {
            loge(ex.toString())
            null
        }
    }

    override fun onPostExecute(s: String?) {
        if (listener != null && context != null) {
            if (s != null) {
                listener.didSucceed(s)
            } else {
                listener.didFail()
            }
        }
    }

    private fun copyStream(`is`: InputStream?, os: OutputStream) {
        val bufferSize = 1024
        try {
            val bytes = ByteArray(bufferSize)
            while (true) {
                val count = `is`!!.read(bytes, 0, bufferSize)
                if (count == -1) {
                    break
                }
                os.write(bytes, 0, count)
            }
        } catch (ex: Exception) {
            loge(ex.toString())
        }
    }

    @Throws(FileNotFoundException::class)
    private fun getSourceStream(
        u: Uri?,
        context: Context?
    ): InputStream? {
        val out: InputStream?
        out = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val parcelFileDescriptor =
                context!!.contentResolver.openFileDescriptor(u!!, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            FileInputStream(fileDescriptor)
        } else {
            context!!.contentResolver.openInputStream(u!!)
        }
        return out
    }

    interface TaskListener {
        fun didSucceed(path: String?)
        fun didFail()
    }

    companion object {
        private val TAG = GetFileTask::class.java.simpleName
    }

}