package com.example.wastedetector

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectorHelper(
    private var threshold: Float = 0.5f,
    private var numThreads: Int = 2,
    private var maxResults: Int = 3,
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {

    private lateinit var objectDetector: ObjectDetector

    init {
        setupObjectDetector()
    }

    // Initialize the object detector
    private fun setupObjectDetector() {
        // Create the base options for the detector using specifies max results and score threshold
        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)

        // Set general detection options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName ="model.tflite"

        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

    fun detect(image: Bitmap) {

//        Convert the image to TensorImage for detection
        val tensorImage = TensorImage.fromBitmap(image)

        val results = objectDetector?.detect(tensorImage)
        objectDetectorListener?.onResults(
            results,
            tensorImage.height,
            tensorImage.width)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: MutableList<Detection>?,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}