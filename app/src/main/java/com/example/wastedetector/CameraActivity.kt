package com.example.wastedetector

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.wastedetector.databinding.ActivityCameraBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var  previewView: PreviewView
    private lateinit var captureBtn: FloatingActionButton
    private lateinit var topBar: MaterialToolbar

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var activityCameraBinding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCameraBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(activityCameraBinding.root)

        val value = intent.getStringExtra("activity_type")

        objectDetectorHelper = ObjectDetectorHelper(
            context = this,
            objectDetectorListener = this)

        previewView = activityCameraBinding.previewView
        captureBtn = activityCameraBinding.captureBtn
        topBar = activityCameraBinding.topAppBar

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera(value)
        if (value == "real_time") {
            captureBtn.visibility = View.INVISIBLE
        }else {
            outputDirectory = getOutputDirectory()
            captureBtn.setOnClickListener {
                takePicture()
            }
        }

        topBar.setNavigationOnClickListener {
            onBackPressed() // Close and navigate back to Home
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun startCamera(value: String?) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener( {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview- using 4:3 ratio as it is the closest to the model
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                if (value == "real_time") {
                    // ImageAnalysis. Using RGBA 8888 to match how our models work
                    imageAnalyzer =
                        ImageAnalysis.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                            // The analyzer can then be assigned to the instance
                            .also {
                                it.setAnalyzer(cameraExecutor) { image ->
                                    if (!::bitmapBuffer.isInitialized) {
                                        // The image rotation and RGB image buffer are initialized only once
                                        // the analyzer has started running
                                        bitmapBuffer = Bitmap.createBitmap(
                                            image.width,
                                            image.height,
                                            Bitmap.Config.ARGB_8888
                                        )
                                    }
                                    detectObjects(image)
                                }
                            }
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer)
                } else {
                    imageCapture = ImageCapture.Builder()
                        .setTargetResolution(Size(512,384))
                        .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture)
                }
            } catch (exc: Exception) {
                Log.e(TAG, "Failed to start camera", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer)
    }

    private fun takePicture() {
        val imageCapture = imageCapture?:return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener,
        // which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val imagePath = photoFile.absolutePath
                    val msg = "Photo capture succeeded: $imagePath"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)

//                    val imagePath = getImagePathFromUri(imageUri)
                    val resultIntent = Intent()
                    resultIntent.putExtra("captureImage", imagePath)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        cameraExecutor.shutdown()
        navigateBack()
    }

    private fun navigateBack() {
        supportFragmentManager.popBackStack()
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this@CameraActivity, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(results: MutableList<Detection>?, imageHeight: Int, imageWidth: Int) {
        activityCameraBinding.overlay.setResults(
            results ?: LinkedList<Detection>(),
            imageHeight,
            imageWidth
        )

        activityCameraBinding.overlay.invalidate()
    }
}