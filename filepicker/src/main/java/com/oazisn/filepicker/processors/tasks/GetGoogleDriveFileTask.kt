package com.oazisn.filepicker.processors.tasks

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import com.oazisn.filepicker.uriHelper.FilePickerUriHelper
import com.oazisn.filepicker.utils.logd
import com.oazisn.filepicker.utils.loge
import java.io.*

/**
 * Created by Nicolaj on 15-10-2015.
 */
class GetGoogleDriveFileTask(
    private val context: Context?,
    private val uri: Uri?,
    private val listener: TaskListener?
) : AsyncTask<Void?, String?, String?>() {
    override fun doInBackground(vararg params: Void?): String? {
        Log.i(TAG, "Google drive: " + uri.toString())
        val cursor =
            context!!.contentResolver.query(uri!!, null, null, null, null)
        var outputFile: File? = null
        try {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                val displayName =
                    cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                Log.i(
                    TAG,
                    "Display Name: $displayName"
                )
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                var size: String? = null
                size = if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    cursor.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                Log.i(TAG, "Size: $size")
                //outputFile = new File(Environment.getExternalStorageDirectory(), displayName.replace(" ","_"));
                outputFile =
                    File(Environment.getExternalStorageDirectory(), displayName)
                val os: OutputStream = FileOutputStream(outputFile)
                var `is`: InputStream? = null
                `is` = if (FilePickerUriHelper.isVirtualFile(context, uri)) {
                    logd("File is virtual, commencing virtual file procedures")
                    try {
                        FilePickerUriHelper.getInputStreamForVirtualFile(context, uri, "*/*")
                    } catch (e: IOException) {
                        e.printStackTrace()
                        throw FileNotFoundException("Could not get file descriptor for virtual file")
                    }
                } else {
                    loge("Looks like we got ourselves a regular ole' file")
                    context.contentResolver.openInputStream(uri)
                }
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
                    ex.printStackTrace()
                    loge(ex.toString())
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            loge(e.toString())
        } finally {
            cursor?.close()
        }
        return Uri.fromFile(outputFile).toString()
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

    interface TaskListener {
        fun didSucceed(path: String?)
        fun didFail()
    }

    companion object {
        private val TAG = GetGoogleDriveFileTask::class.java.simpleName
    }

}