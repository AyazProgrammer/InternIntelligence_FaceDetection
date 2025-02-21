package com.example.facedetection

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.facedetection.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var imageAnalyzer: ImageAnalysis

    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    private val REQUEST_CODE_PERMISSIONS = 1
    private val REQUEST_CODE_GALLERY = 1
    private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)


    private lateinit var binding: ActivityMainBinding




    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)

        cameraExecutor = Executors.newSingleThreadExecutor()


        binding.galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_GALLERY)
        }


        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set Rotate Camera Button click listener
        binding.rotateCameraButton.setOnClickListener {
            toggleCamera()
        }
        setContentView(binding.root)

    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }


            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            analyzeImage(inputImage) {

                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }

    }


    @SuppressLint("SetTextI18n")
    private fun analyzeImage(image: InputImage, onComplete: () -> Unit) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val facesDetected = StringBuilder("Faces detected!\n\n")

                    faces.forEachIndexed { index, face ->
                        val left = face.boundingBox.left
                        val top = face.boundingBox.top
                        val right = face.boundingBox.right
                        val bottom = face.boundingBox.bottom
                        val width = face.boundingBox.width()
                        val height = face.boundingBox.height()

                        val smilingProbability = face.smilingProbability ?: 0f
                        val leftEyeOpenProbability = face.leftEyeOpenProbability ?: 0f
                        val rightEyeOpenProbability = face.rightEyeOpenProbability ?: 0f
                        val headEulerAngleY = face.headEulerAngleY // Y-axis (yaw)
                        val headEulerAngleZ = face.headEulerAngleZ // Z-axis (roll)
                        val rollAngle = face.headEulerAngleX // X-axis (pitch)

                        // Landmarks
                        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
                        val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)
                        val leftMouth = face.getLandmark(FaceLandmark.MOUTH_LEFT)
                        val rightMouth = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

                        val emotion = when {
                            smilingProbability >= 0.5 -> "Happy"
                            smilingProbability <= 0.1 -> "Sad"
                            else -> "Neutral"
                        }


                        facesDetected.append("""
                             
                        Face #${index + 1}:
                        Coordinates: Left: $left, Top: $top, Right: $right, Bottom: $bottom
                        Width: $width, Height: $height
                        
                        Smiling Probability: $smilingProbability - Emotion: $emotion
                        Left Eye Open Probability: $leftEyeOpenProbability
                        Right Eye Open Probability: $rightEyeOpenProbability
                        Head Rotation (Y): $headEulerAngleY
                        Head Rotation (Z): $headEulerAngleZ
                        Head Rotation (X - Roll): $rollAngle
                        
                        Left Eye Position: ${leftEye?.position}
                        Right Eye Position: ${rightEye?.position}
                        Nose Base Position: ${noseBase?.position}
                        Left Mouth Position: ${leftMouth?.position}
                        Right Mouth Position: ${rightMouth?.position}
                        
                       
                    """.trimIndent())
                    }


                    runOnUiThread {
                        binding.resultTextView.text = facesDetected.toString()
                    }

                } else {
                    runOnUiThread {
                        binding.resultTextView.text = "No faces detected"
                    }
                }
                onComplete()

            }
            .addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Error detecting face: ${e.localizedMessage}", e)
                onComplete()
            }
    }


    private fun toggleCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        startCamera()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK) {
            data?.data?.let { imageUri ->

                val intent = Intent(this, FaceDetectionActivity::class.java)
                intent.putExtra("imageUri", imageUri.toString())
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
