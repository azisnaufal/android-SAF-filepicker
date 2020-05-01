package com.oazisn.filepicker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.oazisn.filepicker.FilePickerConstants.Companion.PERMISSION_REQUEST_CODE
import com.oazisn.filepicker.FilePickerConstants.Companion.REQUEST_CODE
import com.oazisn.filepicker.FilePickerConstants.Companion.RESULT_CODE_FAILURE
import com.oazisn.filepicker.FilePickerConstants.Companion.URI
import com.oazisn.filepicker.bitmapHelper.FilePickerBitmapHelper
import com.oazisn.filepicker.intentHelper.FilePickerCameraIntent
import com.oazisn.filepicker.intentHelper.FilePickerChooserIntent
import com.oazisn.filepicker.intentHelper.FilePickerFileIntent
import com.oazisn.filepicker.processors.UriProcessListener
import com.oazisn.filepicker.processors.UriProcessor
import com.oazisn.filepicker.utils.loge
import kotlinx.android.synthetic.main.activity_file_picker.*

class FilePickerActivity : AppCompatActivity(), UriProcessListener {

    private var chooserText = "Choose an action"
    private var download = false
    private var outputFileUri: Uri? = null
    private var uriString = ""
    private lateinit var uriProcessor: UriProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_picker)
        instance = this

        uriProcessor = UriProcessor()

        val extras = intent.extras
        if (extras != null && extras.containsKey(CHOOSER_TEXT)) {
            chooserText = intent.getStringExtra(CHOOSER_TEXT)!!
        }
        if (extras != null && extras.containsKey(DOWNLOAD_IF_NON_LOCAL)) {
            download = intent.getBooleanExtra(DOWNLOAD_IF_NON_LOCAL, true)
        }

        if (requirePermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        ) {
            askPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        } else {
            start()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (permissions.size == 0 || grantResults.size == 0) {
            setResult(RESULT_FIRST_USER)
            finish()
            return
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                && (permissions[1] == Manifest.permission.CAMERA && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                start()
            } else {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            if (data != null && data.data != null) {
                uriString = data.data.toString()
            } else if (outputFileUri != null) {
                uriString = outputFileUri.toString()
            } else if (data != null && data.extras != null && data.extras?.get("data") != null) {
                uriString = data.extras?.get("data").toString()
                try {
                    val file = FilePickerBitmapHelper.writeBitmap(
                        this,
                        data.extras?.get("data") as Bitmap,
                        false
                    )
                    uriString = Uri.fromFile(file).toString()
                } catch (e: Exception) {
                    loge(e.toString())
                }
            }

            if (uriString.isEmpty()) {
                setResult(RESULT_FIRST_USER)
                finish()
                return
            }

            loge("Original URI = ${uriString}")

            val uri = Uri.parse(uriString)

            // Android 4.4 throws:
            // java.lang.SecurityException: Permission Denial: opening provider com.android.providers.media.MediaDocumentsProvider
            // So we do this

            try {
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val takeFlags = data!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION

                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                }
            } catch (e: Exception) {
                loge(e.toString())
            }

            if (uri != null) {
                uriProcessor.process(baseContext, uri, this)
            } else {
                loge("Uri is null!")
                setResult(RESULT_CODE_FAILURE)
                finish()
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else {
            setResult(RESULT_CODE_FAILURE)
            finish()
        }
    }

    private fun start() {
        showProgress()
        lateinit var intentActivity: Intent

        if (intent.getBooleanExtra(CAMERA, false)) {
            outputFileUri = FilePickerCameraIntent.setUri(this)
            intentActivity = FilePickerCameraIntent.cameraIntent(outputFileUri)
        } else if (intent.getBooleanExtra(FILE, false)) {
            //only file
            intentActivity = FilePickerFileIntent.fileIntent("image/*")
            if (intent.getStringArrayExtra(MULTIPLE_TYPES) != null) {
                //User can specify multiple types for the intent.
                FilePickerFileIntent.setTypes(
                    intentActivity,
                    intent.getStringArrayExtra(MULTIPLE_TYPES)
                )
            } else if (intent.getStringExtra(TYPE) != null) {
                //If no types defaults to image files, if just 1 type applies type
                FilePickerFileIntent.setType(intentActivity, intent.getStringExtra(TYPE))
            }
        } else {
            //We assume its an image since developer didn't specify anything and we will show chooser with Camera, File explorers (including gdrive, dropbox...)
            outputFileUri = FilePickerCameraIntent.setUri(this)

            if (intent.getStringArrayExtra(MULTIPLE_TYPES) != null) {

                //User can specify multiple types for the intent.
                intent = FilePickerChooserIntent.chooserMultiIntent(
                    chooserText,
                    outputFileUri,
                    intent.getStringArrayExtra(MULTIPLE_TYPES)
                )

            } else if (null != intent.getStringExtra(TYPE)) {
                //If no types defaults to image files, if just 1 type applies type
                intentActivity = FilePickerChooserIntent.chooserSingleIntent(
                    chooserText,
                    outputFileUri,
                    intent.getStringExtra(TYPE)
                )
            } else {
                intentActivity = FilePickerChooserIntent.chooserIntent(chooserText, outputFileUri)
            }
        }

        if (intent.resolveActivity(packageManager) != null) {
            Handler().postDelayed({ startActivityForResult(intentActivity, REQUEST_CODE) }, 500)
        } else {
            setResult(RESULT_FIRST_USER)
            finish()
        }
    }

    private fun requirePermission(vararg permissions: String): Boolean {
        permissions.forEach {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(
                    instance,
                    it
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun askPermission(vararg permissions: String) {
        ActivityCompat.requestPermissions(
            instance,
            permissions,
            FilePickerConstants.PERMISSION_REQUEST_CODE
        )
    }

    private fun showProgress() {
        frame_layout.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        frame_layout.visibility = View.GONE
    }

    override fun onProcessingSuccess(intent: Intent?) {
        setResult(RESULT_OK, intent)
        hideProgress()
        finish()
    }

    override fun onProcessingFailure() {
        if (uriString.isEmpty()) {    // this shouldn't be necessary since we already check before doing the URI processing
            setResult(RESULT_FIRST_USER)
            hideProgress()
            finish()
        } else {
            // if no processors worked, return original uri
            val intent = Intent()
            intent.putExtra(URI, uriString)
            setResult(RESULT_OK, intent)
            hideProgress()
            finish()
        }
    }

    companion object {
        val TAG = this::class.java.simpleName

        lateinit var instance: Activity

        const val DOWNLOAD_IF_NON_LOCAL = "DOWNLOAD_IF_NON_LOCAL"
        const val CAMERA = "CAMERA"
        const val FILE = "FILE"
        const val TYPE = "TYPE"
        const val MULTIPLE_TYPES = "MULTIPLE_TYPES"
        const val CHOOSER_TEXT = "CHOOSER_TEXT"
    }
}
