package com.oazisn.filepicker.intentHelper

import android.content.Intent
import android.net.Uri

object FilePickerChooserIntent {
    fun chooserIntent(chooserText: String?, uri: Uri?): Intent {
        return Intent.createChooser(
            Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            chooserText
        )
            .putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                arrayOf(FilePickerCameraIntent.cameraIntent(uri))
            )
    }

    fun chooserSingleIntent(
        chooserText: String?,
        uri: Uri?,
        type: String?
    ): Intent {
        return Intent.createChooser(
            Intent()
                .setType(type)
                .setAction(Intent.ACTION_GET_CONTENT)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            chooserText
        )
            .putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                arrayOf(FilePickerCameraIntent.cameraIntent(uri))
            )
    }

    fun chooserMultiIntent(
        chooserText: String?,
        uri: Uri?,
        types: Array<String?>?
    ): Intent {
        return Intent.createChooser(
            Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_MIME_TYPES, types)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            chooserText
        )
            .putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                arrayOf(FilePickerCameraIntent.cameraIntent(uri))
            )
    }
}