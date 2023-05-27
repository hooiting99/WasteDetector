package com.example.wastedetector

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min


class ImageDetectionActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TAG = "TFLite - ODT"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        const val REQUEST_UPLOAD_IMAGE: Int = 2
        const val REQUEST_CROP_IMAGE: Int = 3
        private const val EXPECTED_WIDTH = 512
        private const val EXPECTED_HEIGHT = 384
        private const val MAX_FONT_SIZE = 30F
    }

    private lateinit var captureImage: Button
    private lateinit var uploadImage: Button
    private lateinit var defaultLayout: RelativeLayout
    private lateinit var resultView: ImageView
    private lateinit var imagePath: String
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detection)

        defaultLayout = findViewById(R.id.defaultImage)
        resultView = findViewById(R.id.resultedImage)
        uploadImage = findViewById(R.id.uploadImage)
        captureImage = findViewById(R.id.captureImage)

        uploadImage.setOnClickListener(this)
        captureImage.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.captureImage -> {
                try{
                    startTakePictureIntent()
                }catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
            R.id.uploadImage -> {
                try {
                    val uploadPictureIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(uploadPictureIntent, REQUEST_UPLOAD_IMAGE)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val path = data?.getStringExtra("captureImage")
                    if (path != null) {
                        handleImage(path)
                    }
                }
                REQUEST_UPLOAD_IMAGE -> {
                    val imageUri = data?.data
                    val path = getRealPathFromURI(imageUri, this)
                    Log.e(TAG, imageUri.toString())
                    handleImage(path)
                }
                REQUEST_CROP_IMAGE -> {
                    val croppedUri = UCrop.getOutput(data!!)
                    imagePath = croppedUri?.path.toString()
                    setViewAndDetect(getFormattedImage())
                }
            }
        }
    }

    private fun startTakePictureIntent() {
        val cameraIntent = Intent(this, CameraActivity::class.java)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

    private fun handleImage(path: String) {
        val requireCrop = !isImageSizeValid(path)
        if (requireCrop) {
            val uri = Uri.fromFile(File(path))
            cropImage(uri)
        }else {
            imagePath = path
            setViewAndDetect(getFormattedImage())
        }
    }

    //    Check if the size of image size valid
    private fun isImageSizeValid(uri: String): Boolean {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(uri, this)
        }

        val imageW = options.outWidth
        val imageH = options.outHeight

        return imageW <= EXPECTED_WIDTH && imageH <= EXPECTED_HEIGHT
    }

//    Crop the Image to the expected size
    private fun cropImage(uri: Uri?) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))
        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(100)
            setHideBottomControls(true)
            setFreeStyleCropEnabled(false)
            withAspectRatio(EXPECTED_WIDTH.toFloat(), EXPECTED_HEIGHT.toFloat())
        }

        UCrop.of(uri!!, destinationUri)
            .withOptions(options)
            .start(this, REQUEST_CROP_IMAGE)
    }

//    Get the path of image upload
    private fun getRealPathFromURI(imageUri: Uri?, context: Context): String {
        val returnCursor = imageUri?.let { context.contentResolver.query(it, null, null, null, null) }
        val nameIndex =  returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val file = File(context.filesDir, name)
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable: Int = inputStream?.available() ?: 0
            val bufferSize = min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream?.read(buffers).also {
                    if (it != null) {
                        read = it
                    }
                } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream?.close()
            outputStream.close()

        } catch (e: java.lang.Exception) {
            Log.e("Exception", e.message!!)
        }
        return file.path
    }

    //    Scale the image to match the size of resultView
    private fun getFormattedImage(): Bitmap {
        val targetW: Int = resultView.width
        val targetH: Int = resultView.height

        Log.e("target width", "width $targetW")
        Log.e("target height", "height $targetH")

        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(imagePath, this)

            val imageW: Int = outWidth
            val imageH: Int = outHeight

            Log.e("target height", "height $imageH")
            Log.e("target height", "width $imageW")

            val scaleFactor: Int = max(1, min(imageW / targetW, imageH / targetH))

            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inMutable = true
        }

        val exifInterface = ExifInterface(imagePath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val bitmap = BitmapFactory.decodeFile(imagePath, bmOptions)
        val finalBitmap: Bitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 270f)
            }
            else -> {
                bitmap
            }
        }
        return Bitmap.createScaledBitmap(finalBitmap, EXPECTED_WIDTH, EXPECTED_HEIGHT, true)
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun setViewAndDetect(bitmap: Bitmap) {
        resultView.setImageBitmap(bitmap)
        defaultLayout.visibility = View.INVISIBLE

        lifecycleScope.launch(Dispatchers.Default) { runObjectDetection(bitmap) }
    }

    private fun runObjectDetection(bitmap: Bitmap) {
        objectDetectorHelper = ObjectDetectorHelper(
            context = this,
            objectDetectorListener = object : ObjectDetectorHelper.DetectorListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        Toast.makeText(this@ImageDetectionActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResults(results: MutableList<Detection>?, imageHeight: Int, imageWidth: Int) {
                    if (results != null) {
                        debugPrint(results)
                    }
//                  Parse the detection result
                    val displayResult = results?.map {
                        val category = it.categories.first()
                        val text = "${category.label}, ${category.score.times(100).toInt()}%"

//                        Create a data obj to display the detection result
                        DetectionResult(it.boundingBox, text)
                    }

//                  Draw bounding box, label and score on the bitmap and display
                    val imgResult = drawDetectionResult(bitmap, displayResult!!)
                    runOnUiThread {
                        resultView.setImageBitmap(imgResult)
                    }
                }
            }
        )
        objectDetectorHelper.detect(bitmap)
    }

    private fun debugPrint(results : List<Detection>) {
        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.d(TAG, "Detected object: $i ")
            Log.d(TAG, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")

            for ((j, category) in obj.categories.withIndex()) {
                Log.d(TAG, "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d(TAG, "    Confidence: ${confidence}%")
            }
        }
    }

    private fun drawDetectionResult(bitmap: Bitmap, result: List<DetectionResult>): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        Log.e("test", "inside")

        result.forEach {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 4F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)

            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.BLACK
            pen.strokeWidth = 1F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                it.text, box.left + margin,
                box.top + tagSize.height().times(1F), pen
            )
        }
        return outputBitmap
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateBack()
    }

    private fun navigateBack() {
        supportFragmentManager.popBackStack()
    }
}

data class DetectionResult(val boundingBox: RectF, val text: String)