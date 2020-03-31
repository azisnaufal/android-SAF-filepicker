package com.oazisn.filepicker.intentHelper

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

object FilePickerCameraIntent {
    fun cameraIntent(uri: Uri?): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            .putExtra(MediaStore.EXTRA_OUTPUT, uri)
    }

    fun setUri(activity: Activity): Uri? {
        val contentValues = ContentValues(1)
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        return activity.contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }
}