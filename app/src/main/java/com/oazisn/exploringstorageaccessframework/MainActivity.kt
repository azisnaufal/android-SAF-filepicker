package com.oazisn.exploringstorageaccessframework

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.oazisn.filepicker.FilePickerActivity
import com.oazisn.filepicker.FilePickerConstants.Companion.RESULT_CODE_FAILURE
import com.oazisn.filepicker.uriHelper.FilePickerUriHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MY_REQUEST_CODE = 47
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val intent = Intent(this, FilePickerActivity::class.java)
            //change chooser text
            intent.putExtra(FilePickerActivity.CHOOSER_TEXT, "Please select an action")

            //start picking
            startActivityForResult(intent, MY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                location.text = FilePickerUriHelper.getUriString(data)
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "User Canceled", Toast.LENGTH_SHORT).show()
            } else if (resultCode == RESULT_CODE_FAILURE) {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
