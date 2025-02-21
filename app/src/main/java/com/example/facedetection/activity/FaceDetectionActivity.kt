package com.example.facedetection.activity

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.facedetection.databinding.ActivityFaceDetectionBinding
import com.example.facedetection.helper.FaceDetectionHelper
import com.google.mlkit.vision.common.InputImage

class FaceDetectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaceDetectionBinding
    private lateinit var imageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageUri")
        imageUri = Uri.parse(imageUriString)

        binding.imageView.setImageURI(imageUri)



        val image = InputImage.fromFilePath(this, imageUri)
        FaceDetectionHelper.detectFaces(this, image) {
            binding.resultTextView.text = it
        }
    }
}
