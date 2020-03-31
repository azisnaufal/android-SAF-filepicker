package com.oazisn.filepicker.bitmapHelper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

object FilePickerBitmapHelper {
    fun writeBitmap(
        context: Context,
        bitmap: Bitmap,
        externalStorage: Boolean
    ): File {
        return writeBitmap(
            context,
            bitmap,
            CompressFormat.PNG,
            externalStorage
        )
    }

    fun writeBitmap(
        context: Context,
        bitmap: Bitmap,
        compressFormat: CompressFormat?,
        externalStorage: Boolean
    ): File {
        val filesDir =
            if (externalStorage) context.externalCacheDir else context.cacheDir
        val file = File(filesDir, "image.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(compressFormat, 90, fileOutputStream)
        return file
    }

    fun getBitmapOptions(
        uri: Uri,
        context: Context
    ): BitmapFactory.Options {
        val o: BitmapFactory.Options
        o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        try {
            val parcelFileDescriptor =
                context.contentResolver.openFileDescriptor(uri, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, o)

            // If the uri is a "file path", this will throw java.io.FileNotFoundException: No content provider
            // Try the old way
        } catch (fnfe: FileNotFoundException) {
            val file = File(uri.path!!)
            if (file.exists()) {
                BitmapFactory.decodeFile(uri.path, o)
            }
        }
        return o
    }
}