package com.example.offlinescanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var cbBen: CheckBox
    private lateinit var cbEng: CheckBox
    private lateinit var cbAra: CheckBox
    private lateinit var cbEnhance: CheckBox
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var preview: ImageView
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var result: EditText

    // OCR শুধু এই একটি থ্রেডে চলে (TessBaseAPI একাধিক থ্রেডে নিরাপদ নয়)
    private val worker = Executors.newSingleThreadExecutor()
    private var tess: TessBaseAPI? = null
    private var tessLangs: String? = null
    private var dataCopied = false

    private var photoUri: Uri? = null

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok) photoUri?.let { startScan(it) }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) startScan(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cbBen = findViewById(R.id.cbBen)
        cbEng = findViewById(R.id.cbEng)
        cbAra = findViewById(R.id.cbAra)
        cbEnhance = findViewById(R.id.cbEnhance)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        preview = findViewById(R.id.preview)
        progress = findViewById(R.id.progress)
        status = findViewById(R.id.status)
        result = findViewById(R.id.result)

        // আগের বার বাছাই করা ভাষা ফিরিয়ে আনা
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        cbBen.isChecked = prefs.getBoolean("ben", true)
        cbEng.isChecked = prefs.getBoolean("eng", true)
        cbAra.isChecked = prefs.getBoolean("ara", false)
        val saveLangs = {
            prefs.edit()
                .putBoolean("ben", cbBen.isChecked)
                .putBoolean("eng", cbEng.isChecked)
                .putBoolean("ara", cbAra.isChecked)
                .apply()
        }
        cbBen.setOnCheckedChangeListener { _, _ -> saveLangs() }
        cbEng.setOnCheckedChangeListener { _, _ -> saveLangs() }
        cbAra.setOnCheckedChangeListener { _, _ -> saveLangs() }

        savedInstanceState?.getString("photoUri")?.let { photoUri = Uri.parse(it) }

        btnCamera.setOnClickListener { launchCamera() }
        btnGallery.setOnClickListener { pickImage.launch("image/*") }

        findViewById<Button>(R.id.btnCopy).setOnClickListener {
            val text = result.text.toString()
            if (text.isNotEmpty()) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("scan", text))
                Toast.makeText(this, R.string.status_copied, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val text = result.text.toString()
            if (text.isNotEmpty()) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(send, getString(R.string.share_chooser)))
            }
        }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            result.setText("")
            preview.setImageDrawable(null)
            preview.visibility = View.GONE
            status.setText(R.string.status_ready)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoUri?.let { outState.putString("photoUri", it.toString()) }
    }

    override fun onDestroy() {
        super.onDestroy()
        val api = tess
        tess = null
        worker.execute { api?.recycle() }
        worker.shutdown()
    }

    // ---------------------------------------------------------------- input

    private fun launchCamera() {
        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "capture.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        photoUri = uri
        takePicture.launch(uri)
    }

    private fun selectedLanguages(): List<String> {
        val list = mutableListOf<String>()
        if (cbBen.isChecked) list += "ben"
        if (cbEng.isChecked) list += "eng"
        if (cbAra.isChecked) list += "ara"
        return list
    }

    // ----------------------------------------------------------------- scan

    private fun startScan(uri: Uri) {
        val langs = selectedLanguages()
        if (langs.isEmpty()) {
            Toast.makeText(this, R.string.err_no_lang, Toast.LENGTH_SHORT).show()
            return
        }
        val langCode = langs.joinToString("+")
        val enhance = cbEnhance.isChecked
        setBusy(true)

        worker.execute {
            try {
                val bitmap = loadBitmap(uri)
                runOnUiThread {
                    preview.setImageBitmap(bitmap)
                    preview.visibility = View.VISIBLE
                }
                val prepared = if (enhance) enhance(bitmap) else bitmap
                val text = recognize(prepared, langs, langCode)
                runOnUiThread {
                    result.setText(text)
                    status.setText(if (text.isEmpty()) R.string.status_empty else R.string.status_done)
                }
            } catch (t: Throwable) {
                val msg = t.message ?: t.javaClass.simpleName
                runOnUiThread { status.text = getString(R.string.err_generic, msg) }
            } finally {
                runOnUiThread { setBusy(false) }
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        btnCamera.isEnabled = !busy
        btnGallery.isEnabled = !busy
        if (busy) status.setText(R.string.status_working)
    }

    // ------------------------------------------------------------------ OCR

    private val tessRoot: File get() = File(filesDir, "ocr")

    /** assets/tessdata থেকে ভাষার ফাইল অ্যাপের নিজস্ব ফোল্ডারে একবার কপি করে। */
    private fun ensureLanguageData() {
        if (dataCopied) return
        val outDir = File(tessRoot, "tessdata").apply { mkdirs() }
        val names = assets.list("tessdata")?.filter { it.endsWith(".traineddata") } ?: emptyList()
        for (name in names) {
            val dst = File(outDir, name)
            val expected = assets.open("tessdata/$name").use { it.available().toLong() }
            if (dst.exists() && dst.length() == expected) continue
            assets.open("tessdata/$name").use { input ->
                dst.outputStream().use { output -> input.copyTo(output) }
            }
        }
        dataCopied = true
    }

    private fun recognize(bitmap: Bitmap, langs: List<String>, langCode: String): String {
        ensureLanguageData()
        for (l in langs) {
            if (!File(tessRoot, "tessdata/$l.traineddata").exists()) {
                throw IllegalStateException(getString(R.string.err_missing_lang, l))
            }
        }

        var api = tess
        if (api == null || tessLangs != langCode) {
            api?.recycle()
            tess = null
            val fresh = TessBaseAPI()
            if (!fresh.init(tessRoot.absolutePath, langCode)) {
                fresh.recycle()
                throw IllegalStateException(getString(R.string.err_init))
            }
            fresh.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            tess = fresh
            tessLangs = langCode
            api = fresh
        }

        api.setImage(bitmap)
        val text = api.getUTF8Text() ?: ""
        api.clear()
        return text.trim()
    }

    // ------------------------------------------------------- image handling

    private fun loadBitmap(uri: Uri): Bitmap {
        // ১) আকার জেনে নিয়ে বড় ছবি ছোট করে লোড (মেমরি বাঁচাতে)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("ছবিটি খোলা যায়নি")

        // ২) ফোন ঘোরানো অবস্থায় তোলা ছবি সোজা করা
        val degrees = contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        // ৩) দরকার হলে ঘোরানো ও ছোট করা
        val longest = max(bmp.width, bmp.height)
        val scale = if (longest > MAX_SIDE) MAX_SIDE.toFloat() / longest else 1f
        if (degrees != 0f || scale != 1f) {
            val m = Matrix().apply {
                postRotate(degrees)
                postScale(scale, scale)
            }
            val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (out !== bmp) bmp.recycle()
            bmp = out
        }
        return bmp
    }

    /** সাদাকালো করে কনট্রাস্ট বাড়ায়, যাতে লেখা ভালো পড়া যায়। */
    private fun enhance(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val contrast = 1.35f
        val shift = (-0.5f * contrast + 0.5f) * 255f
        val cm = ColorMatrix().apply { setSaturation(0f) }
        cm.postConcat(
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, shift,
                    0f, contrast, 0f, 0f, shift,
                    0f, 0f, contrast, 0f, shift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    companion object {
        private const val MAX_SIDE = 2400
    }
}
