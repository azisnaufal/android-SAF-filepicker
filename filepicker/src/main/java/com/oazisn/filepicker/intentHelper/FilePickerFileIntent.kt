package com.oazisn.filepicker.intentHelper

import android.annotation.TargetApi
import android.content.Intent

object FilePickerFileIntent {
    fun fileIntent(type: String?): Intent {
        return Intent().setAction(Intent.ACTION_GET_CONTENT).setType(type ?: "image/*")
    }

    fun setType(intent: Intent, type: String?) {
        intent.type = type
    }

    @TargetApi(19)
    fun setTypes(intent: Intent, types: Array<String?>?) {
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, types)
    }
}