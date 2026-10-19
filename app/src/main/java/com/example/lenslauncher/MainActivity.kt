package com.example.lenslauncher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btnLens).setOnClickListener { openLens() }
    }

    /**
     * Google Lens খোলার চেষ্টা, একটির পর একটি:
     * 1) Google অ্যাপের Lens লিংক (googleapp://lens)
     * 2) যেকোনো অ্যাপ যেটি ওই লিংক সামলায়
     * 3) আলাদা Google Lens অ্যাপ (ইনস্টল থাকলে)
     * 4) Google অ্যাপের Play Store পেজ (অ্যাপ না থাকলে)
     * 5) ব্রাউজারে lens.google.com
     */
    private fun openLens() {
        val lensUri = Uri.parse("googleapp://lens")

        // ১) Google অ্যাপ সরাসরি
        if (tryStart(Intent(Intent.ACTION_VIEW, lensUri).setPackage(GOOGLE_APP))) return

        // ২) যেকোনো হ্যান্ডলার
        if (tryStart(Intent(Intent.ACTION_VIEW, lensUri))) return

        // ৩) আলাদা Lens অ্যাপ
        val standalone = packageManager.getLaunchIntentForPackage(LENS_APP)
        if (standalone != null && tryStart(standalone)) return

        // ৪) Google অ্যাপ না থাকলে Play Store
        if (tryStart(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_APP")))) {
            Toast.makeText(this, R.string.toast_install, Toast.LENGTH_LONG).show()
            return
        }

        // ৫) শেষ ভরসা: ওয়েব
        if (tryStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com/")))) {
            Toast.makeText(this, R.string.toast_web, Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, R.string.toast_fail, Toast.LENGTH_LONG).show()
    }

    private fun tryStart(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    companion object {
        private const val GOOGLE_APP = "com.google.android.googlequicksearchbox"
        private const val LENS_APP = "com.google.ar.lens"
    }
}
