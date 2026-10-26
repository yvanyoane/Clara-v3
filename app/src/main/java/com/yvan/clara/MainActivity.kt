package com.yvan.clara

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.sin

private val MODELES = listOf(
    "gemini-flash-latest",
    "gemini-flash-lite-latest",
    "gemini-2.5-flash"
)

private const val PERSONNALITE =
    "Tu es Clara, l'assistante personnelle vocale de Yvan, inspirée de Jarvis. " +
    "Tu es calme, élégante, chaleureuse et complice, avec un humour léger et taquin. " +
    "Tu appelles Yvan par son prénom. Tu donnes toujours ton avis honnête et tu n'hésites " +
    "pas à le contredire quand c'est dans son intérêt. Tu réponds en français, en phrases " +
    "courtes et naturelles, comme à l'oral, sans listes ni formatage."

class MainActivity : Activity() {
    private lateinit var reponse: TextView
    private lateinit var saisie: EditText
    private lateinit var bouton: Button
    private val prefs by lazy { getSharedPreferences("clara", Context.MODE_PRIVATE) }

    private fun cle(): String = prefs.getString("cle", "") ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val racine = LinearLayout(this)
        racine.orientation = LinearLayout.VERTICAL
        racine.setBackgroundColor(Color.parseColor("#0A1230"))
        racine.fitsSystemWindows = true

        racine.addView(
            ClaraView(this),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        reponse = TextView(this)
        reponse.setTextColor(Color.parseColor("#E1F5FE"))
        reponse.textSize = 16f
        reponse.setPadding(40, 20, 40, 20)
        reponse.maxLines = 8
        racine.addView(reponse)

        saisie = EditText(this)
        saisie.setTextColor(Color.WHITE)
        saisie.setHintTextColor(Color.parseColor("#7FA6C8"))
        saisie.setPadding(40, 20, 40, 20)
        racine.addView(saisie)

        bouton = Button(this)
        bouton.setOnClickListener { clic() }
        racine.addView(bouton)

        majInterface()
        setContentView(racine)
    }

    private fun majInterface() {
        if (cle().isEmpty()) {
            reponse.text = "Bonjour Yvan. Colle ta clé Gemini ci-dessous, puis appuie sur le bouton."
            saisie.hint = "Colle ta clé Gemini ici"
            bouton.text = "Enregistrer la clé"
        } else {
            reponse.text = "Je suis là, Yvan. Écris-moi quelque chose."
            saisie.hint = "Écris à Clara"
            bouton.text = "Envoyer"
        }
    }

    private fun clic() {
        val texte = saisie.text.toString().trim()
        if (texte.isEmpty()) return
        saisie.setText("")
        if (texte == "/cle") {
            prefs.edit().remove("cle").apply()
            majInterface()
        } else if (cle().isEmpty()) {
            prefs.edit().putString("cle", texte).apply()
            majInterface()
        } else {
            envoyer(texte)
        }
    }

    private fun envoyer(question: String) {
        reponse.text = "Clara réfléchit..."
        val cle = cle()
        Thread {
            val resultat = demanderAGemini(cle, question)
            runOnUiThread { reponse.text = resultat }
        }.start()
    }

    private fun demanderAGemini(cle: String, question: String): String {
        var derniereErreur = ""
        for (tour in 1..3) {
            for (modele in MODELES) {
                runOnUiThread { reponse.text = "Clara réfléchit... (essai $tour)" }
                try {
                    val (code, texte) = appeler(cle, modele, question)
                    if (code in 200..299) return extraireReponse(texte)
                    derniereErreur = "Erreur $code : " + extraireErreur(texte)
                    if (code == 400 || code == 401 || code == 403) return derniereErreur
                } catch (e: Exception) {
                    derniereErreur = "Erreur : " + (e.message ?: "réseau")
                }
            }
            Thread.sleep(2000)
        }
        return derniereErreur + "\n(Clara a réessayé plusieurs fois. Réessaie dans quelques minutes.)"
    }

    private fun appeler(cle: String, modele: String, question: String): Pair<Int, String> {
        val corps = JSONObject()
        corps.put(
            "systemInstruction",
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", PERSONNALITE)))
        )
        corps.put(
            "contents",
            JSONArray().put(
                JSONObject().put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", question)))
            )
        )

        val adresse =
            "https://generativelanguage.googleapis.com/v1beta/models/$modele:generateContent"
        val connexion = URL(adresse).openConnection() as HttpURLConnection
        connexion.requestMethod = "POST"
        connexion.setRequestProperty("Content-Type", "application/json")
        connexion.setRequestProperty("x-goog-api-key", cle)
        connexion.connectTimeout = 10000
        connexion.readTimeout = 20000
        connexion.doOutput = true
        connexion.outputStream.use { it.write(corps.toString().toByteArray()) }

        val code = connexion.responseCode
        val flux = if (code in 200..299) connexion.inputStream else connexion.errorStream
        val texte = flux.bufferedReader().use { it.readText() }
        return Pair(code, texte)
    }

    private fun extraireReponse(texte: String): String {
        return try {
            JSONObject(texte).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            "Clara n'a pas réussi à formuler de réponse. Essaie de reformuler."
        }
    }

    private fun extraireErreur(texte: String): String {
        return try {
            JSONObject(texte).getJSONObject("error").getString("message")
        } catch (e: Exception) {
            texte
        }
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
        val rayon = minOf(w * 0.26f, h * 0.2f)

        halo.shader = RadialGradient(
            cx, cy, rayon * 2f,
            Color.parseColor("#664FC3F7"), Color.parseColor("#004FC3F7"),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, rayon * 2f, halo)

        val pulsation = 1f + 0.03f * sin(temps)
        canvas.drawCircle(cx, cy, rayon * pulsation, anneau)

        texte.textSize = rayon * 0.5f
        canvas.drawText("Clara", cx, cy + texte.textSize / 3f, texte)

        val nombre = 21
        val milieu = nombre / 2f
        val espace = (w * 0.8f) / nombre
        val debut = w * 0.1f + espace / 2f
        val ligne = h * 0.80f
        for (i in 0 until nombre) {
            val enveloppe = 1f - abs(i - milieu) / milieu * 0.6f
            val vague = sin(temps * 2f + i * 0.6f) * 0.5f + 0.5f
            val demi = 8f + vague * enveloppe * h * 0.06f
            val x = debut + i * espace
            canvas.drawLine(x, ligne - demi, x, ligne + demi, onde)
        }

        postInvalidateOnAnimation()
    }
}
