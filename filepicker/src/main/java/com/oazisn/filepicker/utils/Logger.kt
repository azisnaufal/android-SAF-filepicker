package com.oazisn.filepicker.utils

import android.content.pm.ApplicationInfo
import android.util.Log
import com.oazisn.filepicker.FilePickerActivity

fun isDebug(): Boolean {
    return 0 != FilePickerActivity.instance.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE

}

fun loge(msg: String) {
    if (isDebug()) {
        Log.e(FilePickerActivity.TAG, msg)
    }
}

fun logd(msg: String) {
    if (isDebug()) {
        Log.d(FilePickerActivity.TAG, msg)
    }
}


