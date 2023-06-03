package com.example.wastedetector

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min


class ImageDetectionActivity : AppCompatActivity(), OnClickListener {

    companion object {
        const val TAG = "IMG_DETECT"
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
    private lateinit var categoryView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var recycleBtn: Button
    private lateinit var topBar: MaterialToolbar
    private lateinit var imagePath: String
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detection)

        defaultLayout = findViewById(R.id.defaultImage)
        resultView = findViewById(R.id.resultedImage)
        categoryView = findViewById(R.id.resultCategory)
        descriptionView = findViewById(R.id.resultDescription)
        uploadImage = findViewById(R.id.uploadImage)
        captureImage = findViewById(R.id.captureImage)
        recycleBtn = findViewById(R.id.recycle)
        topBar = findViewById(R.id.topAppBar)

        uploadImage.setOnClickListener(this)
        captureImage.setOnClickListener(this)

        auth = Firebase.auth
        storage = Firebase.storage
        db = FirebaseFirestore.getInstance()

        topBar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed() // Close and navigate back to Home
        }
    }

//    Handle the button click action
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.captureImage -> {
                try{
                    startTakePictureIntent() // Call the CameraX for capture image intent
                }catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
            R.id.uploadImage -> {
                try {
//                    Call the image picker intent
                    val uploadPictureIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    @Suppress("DEPRECATION")
                    startActivityForResult(uploadPictureIntent, REQUEST_UPLOAD_IMAGE)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, e.message.toString())
                }
            }
        }
    }

    @Suppress("DEPRECATION")
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

//    Start CameraX activity
    @Suppress("DEPRECATION")
    private fun startTakePictureIntent() {
        val cameraIntent = Intent(this, CameraActivity::class.java)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

//    Pre-process, detect and display result for the image upload
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

//    Check if the size of image size match with training dataset used for model training
    private fun isImageSizeValid(uri: String): Boolean {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(uri, this)
        }

        val imageW = options.outWidth
        val imageH = options.outHeight

        return imageW <= EXPECTED_WIDTH && imageH <= EXPECTED_HEIGHT
    }

//    Allow user to crop the waste image for better detection result
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

//    Scale the image to match the size of resultView used for display
    private fun getFormattedImage(): Bitmap {
        val targetW: Int = resultView.width
        val targetH: Int = resultView.height

        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(imagePath, this)

            val imageW: Int = outWidth
            val imageH: Int = outHeight

            Log.d(TAG, "Original image size: $imageW, $imageH")

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

//    Put the image into detection model, get and display the result
    private fun setViewAndDetect(bitmap: Bitmap) {
        resultView.setImageBitmap(bitmap)
        defaultLayout.visibility = INVISIBLE
        uploadImage.visibility = INVISIBLE
        captureImage.visibility = INVISIBLE
        recycleBtn.visibility = VISIBLE

        lifecycleScope.launch(Dispatchers.Default) { runObjectDetection(bitmap) }
    }

//    Perform object detection on the image
    private fun runObjectDetection(bitmap: Bitmap) {
//        Initialize an object detector helper for detection
        objectDetectorHelper = ObjectDetectorHelper(
            context = this,
            objectDetectorListener = object : ObjectDetectorHelper.DetectorListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        Toast.makeText(this@ImageDetectionActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResults(results: MutableList<Detection>?, imageHeight: Int, imageWidth: Int) {
                    debugPrint(results!!, imageHeight, imageWidth) // Print the results for checking
//                  Parse the detection result
                    val displayResult = results.map {
                        val category = it.categories.first()
                        val text = "${category.label}, ${category.score.times(100).toInt()}%"

                        // Create a data obj to display the detection result
                        DetectionResult(it.boundingBox, text)
                    }

                    val categories = HashSet<String>()
                    val cate = StringBuilder()
                    val method = StringBuilder()

                    // Display is recyclable or not and
                    // some info regarding the importance of recycling the detected waste
                    for (result in results) {
                        val category = result.categories.first()
                        val label = category.label

                        if (!categories.contains(label)) {
                            categories.add(label)
                            when (label.trim()) {
                                "cardboard" -> {
                                    cate.append("Cardboard (Recyclable)\n")
                                    method.append("Cardboard is recyclable and has a lower environmental impact " +
                                            "compared to other packaging materials." +
                                            "Recycling cardboard saves energy and reduces carbon emissions. " +
                                            "Flatten and remove non-recyclable elements, then place it in recycling bins or facilities.\n")
                                }
                                "glass" -> {
                                    cate.append("Glass (Recyclable)\n")
                                    method.append("Glass is endlessly recyclable and has a low carbon footprint when recycled. " +
                                            "To recycle glass, separate it by color (clear, green, brown), " +
                                            "remove any non-recyclable elements like metal caps, and place it in designated recycling bins.")
                                }
                                "metal" -> {
                                    cate.append("Metal (Recyclable)\n")
                                    method.append("Metal recycling helps conserve natural resources and significantly reduces " +
                                            "carbon emissions compared to primary metal production." +
                                            "You can repurpose the metal items for creative DIY projects or functional use.")
                                }
                                "plastic" -> {
                                    cate.append("Plastic (Recyclable)\n")
                                    method.append("Plastics' production involves fossil fuel extraction, " +
                                            "releasing greenhouse gases. Improper disposal leads to more emissions." +
                                            "To recycle plastic, clean and sort them properly and rinsing out any residue. " +
                                            "Then, deposit them in designated recycling bins or take them to local recycling centers.")
                                }
                                "paper" -> {
                                    cate.append("Paper (Recyclable)\n")
                                    method.append("Recycling paper is an eco-friendly choice that helps reduce deforestation and " +
                                            "the carbon emissions associated with paper production. " +
                                            "By separating paper waste and placing it in designated recycling bins or centers, " +
                                            "you can contribute to the conservation of trees and energy resources.")
                                }
                                "trash" -> {
                                    cate.append("Trash (Non-Recyclable)\n")
                                    method.append("Items that cannot be recycled and belong in the trash contribute to carbon emissions when disposed of improperly." +
                                            "Dispose of non-recyclable items in designated trash bins or follow local waste management guidelines " +
                                            "to promote a cleaner and more sustainable environment.")
                                }
                            }
                        }
                    }

//                  Draw bounding box, label and score on the bitmap and display
                    val imgResult = drawDetectionResult(bitmap, displayResult)
                    runOnUiThread {
                        resultView.setImageBitmap(imgResult)
                        categoryView.text = cate.toString()
                        descriptionView.text = method.toString()

                        // Change the text of button accordingly
                        if (cate.toString().contains("Trash")) {
                            recycleBtn.text = "Throw it"
                        } else {
                            recycleBtn.text = "Recycle it"
                        }

                        // Assume user recycle the detected item
                        // Save the result and perform related computational (mission progress, points, etc)
                        recycleBtn.setOnClickListener { saveDetectionResult(categories) }
                    }
                }
            }
        )
        objectDetectorHelper.detect(bitmap) // Run waste detection
    }

    private fun debugPrint(results: List<Detection>, imageHeight: Int, imageWidth: Int) {
        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.d(TAG, "Image size: $imageWidth, $imageHeight")
            Log.d(TAG, "Detected object: $i ")
            Log.d(TAG, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")

            for ((j, category) in obj.categories.withIndex()) {
                Log.d(TAG, "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d(TAG, "    Confidence: ${confidence}%")
            }
        }
    }

//    Draw the detection results on the image
    private fun drawDetectionResult(bitmap: Bitmap, result: List<DetectionResult>): Bitmap {
//        Initialize the canvas to draw the result on image
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

//        Draw the detection results: bounding box, text: category, confidence score
        result.forEach {
//            Set the style of bounding box
            if (it.text.contains("trash")){
                pen.color = Color.RED
            }else {
                pen.color = Color.GREEN
            }
            pen.strokeWidth = 4F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen) // Draw the bounding box

//            Set the style of text box
            val tagSize = Rect(0, 0, 0, 0)
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.WHITE
            pen.strokeWidth = 1F

//            Calculate the right font size
            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

//            Ensure text inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

//            Draw the text background
            val textBackgroundPaint = Paint()
            textBackgroundPaint.color = Color.BLACK
            val backgroundRect = Rect(
                box.left.toInt(),
                box.top.toInt(),
                (box.left + tagSize.width()).toInt(),
                (box.top + tagSize.height()).toInt()
            )
            canvas.drawRect(backgroundRect, textBackgroundPaint)

//            Draw the text
            canvas.drawText(
                it.text, box.left,
                box.top + tagSize.height()-tagSize.bottom, pen
            )
        }

        return outputBitmap // Resulted image
    }

//    Save the result into Firebase Storage
    private fun saveDetectionResult(categories: HashSet<String>) {

        // Inflate the custom layout for dialog
        val customView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_layout, null)
        // Find the TextView in the custom layout
        val contentTextView = customView.findViewById<TextView>(R.id.contentTextView)
        val btn = customView.findViewById<Button>(R.id.customBtn)
        val dialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
            .setCustomView(customView)
            .setTitleText("Loading")
            .hideConfirmButton()
            .also {
                it.setCancelable(false)
                it.setCanceledOnTouchOutside(false)
            }

//        Get the image display on the resultView
        val storeBitmap = (resultView.drawable as BitmapDrawable).bitmap

        val outStream = ByteArrayOutputStream()
        storeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        val imageBytes = outStream.toByteArray()

//        Store the image bytes to Firebase Storage
        val userId = auth.currentUser?.uid
        val storageRef = storage.reference
        val imageRef = storageRef.child("users/$userId/images/${System.currentTimeMillis()}.jpg")

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo

        if (networkInfo == null || !networkInfo.isConnected) {
            // Network is unavailable
            dialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
            dialog.titleText = "Network Unavailable"
            contentTextView.text = "Please check your internet connection."
            btn.visibility = VISIBLE
        } else {
            val uploadTask = imageRef.putBytes(imageBytes)
            uploadTask.addOnSuccessListener { taskSnapshot ->
//              Retrieve the image URL when upload successful
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()

//                  Store the detection result into Firestore based on appropriate category
                    for (category in categories) {
                        val detectionResultRef = db.collection("users").document(userId.toString()).collection(category.trim())
                        val currentDate = taskSnapshot.metadata?.creationTimeMillis
                        val detectionResultData = hashMapOf(
                            "imageUrl" to downloadUrl,
                            "date" to currentDate,
                        )

                        detectionResultRef
                            .add(detectionResultData)
                            .addOnSuccessListener { documentReference ->
                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.id)
                                dialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                                dialog.titleText = "Congratulations!"
                                contentTextView.text = "You have earned one point!"
                                btn.visibility = VISIBLE
                                recycleBtn.visibility = INVISIBLE
                                uploadImage.visibility = VISIBLE
                                captureImage.visibility = VISIBLE
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error adding document", e)
                                dialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                                dialog.titleText = "Opps..."
                                contentTextView.text = "Something went wrong! Try Again!"
                                btn.visibility = VISIBLE
                            }
                    }
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Error getting download URL", e)
                }
            }.addOnFailureListener { e ->
                // Image upload failed
                Log.w(TAG, "Error adding document", e)
                dialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                dialog.titleText = "Opps..."
                contentTextView.text = "Something went wrong! Try Again!"
                btn.visibility = VISIBLE
            }
        }

        customView.findViewById<Button>(R.id.customBtn).setOnClickListener {
            dialog.dismissWithAnimation()
        }
        dialog.show()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        navigateBack()
    }

    private fun navigateBack() {
        supportFragmentManager.popBackStack()
    }
}

data class DetectionResult(val boundingBox: RectF, val text: String)