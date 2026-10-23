package com.yvan.clara

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val titre = TextView(this)
        titre.text = "Clara"
        titre.textSize = 40f
        titre.setTextColor(Color.parseColor("#D8F2FF"))
        titre.gravity = Gravity.CENTER
        titre.setBackgroundColor(Color.parseColor("#02060F"))
        setContentView(titre)
    }
}
