package com.yvan.clara

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ClaraView(this))
    }
}

class ClaraView(context: Context) : View(context) {
    private val fond = Paint().apply { color = Color.parseColor("#0A1230") }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val anneau = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#4FC3F7")
        strokeWidth = 5f
    }
    private val onde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }
    private val texte = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E1F5FE")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.15f
        setShadowLayer(30f, 0f, 0f, Color.parseColor("#4FC3F7"))
    }
    private var temps = 0f

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        temps += 0.06f

        canvas.drawRect(0f, 0f, w, h, fond)

        val cx = w / 2f
        val cy = h * 0.38f
        val rayon = w * 0.26f

        halo.shader = RadialGradient(
            cx, cy, rayon * 2f,
            Color.parseColor("#664FC3F7"), Color.parseColor("#004FC3F7"),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, rayon * 2f, halo)

        val pulsation = 1f + 0.03f * sin(temps)
        canvas.drawCircle(cx, cy, rayon * pulsation, anneau)

        texte.textSize = w * 0.13f
        canvas.drawText("Clara", cx, cy + texte.textSize / 3f, texte)

        val nombre = 21
        val milieu = nombre / 2f
        val espace = (w * 0.8f) / nombre
        val debut = w * 0.1f + espace / 2f
        val ligne = h * 0.72f
        for (i in 0 until nombre) {
            val enveloppe = 1f - abs(i - milieu) / milieu * 0.6f
            val vague = sin(temps * 2f + i * 0.6f) * 0.5f + 0.5f
            val demi = 15f + vague * enveloppe * h * 0.08f
            val x = debut + i * espace
            canvas.drawLine(x, ligne - demi, x, ligne + demi, onde)
        }

        postInvalidateOnAnimation()
    }
}
