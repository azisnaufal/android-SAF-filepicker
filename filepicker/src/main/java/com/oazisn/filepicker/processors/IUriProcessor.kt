package com.oazisn.filepicker.processors

import android.content.Context
import android.net.Uri

/**
 * Created by bison on 31/10/17.
 */
interface IUriProcessor {
    fun process(
        context: Context?,
        uri: Uri?,
        uriProcessListener: UriProcessListener?
    )
}