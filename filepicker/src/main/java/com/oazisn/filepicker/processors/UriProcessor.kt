package com.oazisn.filepicker.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.oazisn.filepicker.utils.logd
import com.oazisn.filepicker.utils.loge
import java.util.*

/**
 * Created by bison on 31/10/17.
 */
class UriProcessor : UriProcessListener {
    private val processors: MutableList<IUriProcessor> =
        ArrayList()
    private var currentProcessorIt: Iterator<IUriProcessor>? = null
    private var uri: Uri? = null
    private var context: Context? = null
    private var listener: UriProcessListener? = null
    fun process(
        context: Context?,
        uri: Uri?,
        listener: UriProcessListener?
    ) {
        this.listener = listener
        this.uri = uri
        this.context = context
        currentProcessorIt = processors.iterator()
        processNext(context, uri)
    }

    private fun processNext(context: Context?, uri: Uri?) {
        if (currentProcessorIt!!.hasNext()) {
            logd("Processing next")
            val processor = currentProcessorIt!!.next()
            processor.process(context, uri, this)
        } else {
            loge("No more processors to process, propagate failure back to caller")
            if (listener != null) listener!!.onProcessingFailure()
        }
    }

    override fun onProcessingSuccess(intent: Intent?) {
        logd("onProcessingSuccess")
        if (listener != null) listener!!.onProcessingSuccess(intent)
    }

    override fun onProcessingFailure() {
        processNext(context, uri)
    }

    companion object {
        private val TAG = UriProcessor::class.java.simpleName
    }

    init {
        // register URI processors
        processors.add(GooglePhotosProcessor())
        processors.add(GoogleDocumentsProcessor())
        processors.add(GoogleMediaProcessor())
        processors.add(GoogleDriveProcessor())
        processors.add(GenericContentProviderProcessor())
    }
}