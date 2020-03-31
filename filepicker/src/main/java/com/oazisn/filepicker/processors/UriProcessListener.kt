package com.oazisn.filepicker.processors

import android.content.Intent

/**
 * Created by bison on 31/10/17.
 */
interface UriProcessListener {
    fun onProcessingSuccess(intent: Intent?)
    fun onProcessingFailure()
}