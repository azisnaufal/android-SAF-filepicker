package com.oazisn.filepicker

class FilePickerConstants {
    companion object {
        /**
         * MIME IMAGE
         */
        const val MIME_IMAGE = "image/*"
        const val MIME_IMAGE_PNG = "image/png"
        const val MIME_IMAGE_BMP = "image/bmp"
        const val MIME_IMAGE_JPG = "image/jpg"
        const val MIME_IMAGE_GIF = "image/gif"

        /**
         * MIME VIDEO
         */
        const val MIME_VIDEO = "video/*"
        const val MIME_VIDEO_WAV = "video/wav"
        const val MIME_VIDEO_MP4 = "video/mp4"

        /**
         * MIME OTHERS
         */
        const val MIME_PDF = "application/pdf"
        const val MIME_TEXT_PLAIN = "text/plain"
        const val MIME_ALL = "*/*"

        const val URI = "URI"

        /**
         * Response codes
         */
        const val RESULT_CODE_FAILURE = 10
        const val REQUEST_CODE = 2
        const val PERMISSION_REQUEST_CODE = 3
    }
}