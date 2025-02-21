package com.example.facedetection

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.facedetection.databinding.ActivityFaceDetectionBinding
import com.example.facedetection.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

class FaceDetectionActivity : AppCompatActivity() {



    private lateinit var imageUri: Uri
    private lateinit var binding: ActivityFaceDetectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val imageUriString = intent.getStringExtra("imageUri")
        imageUri = Uri.parse(imageUriString)

        binding.imageView.setImageURI(imageUri)

        // Yüz tanıma əməliyyatını aparmaq
        processImageFromUri(imageUri)

    }

    private fun processImageFromUri(uri: Uri) {
        val image = InputImage.fromFilePath(this, uri)

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


                        facesDetected.append(
                            """
                       
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
                        
                        
                        
                    """.trimIndent()
                        )
                    }
                    runOnUiThread {
                        binding.resultTextView.text = facesDetected.toString()
                    }


                } else {
                    binding.resultTextView.text = "No faces detected"
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetection", "Error: ${e.localizedMessage}")
            }
    }
}
