package com.example.facedetection.widget



import android.content.Context
import android.graphics.Color
import android.util.AttributeSet

class CustomTextView(context: Context, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {

    init {
        // Set the default text size, color, and other attributes
        setTextColor(Color.WHITE)  // Text color
        textSize = 16f  // Text size
        setPadding(10, 10, 10, 10)  // Padding
        elevation = 4f  // Elevation
        setTypeface(typeface, android.graphics.Typeface.BOLD)  // Text style
        gravity = android.view.Gravity.CENTER  // Center text
        isVerticalScrollBarEnabled = true  // Enable scrolling
    }
}
