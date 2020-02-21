package com.weloop.library

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import com.weloop.weloop.WeLoop
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webView = findViewById<WeLoop>(R.id.webview)
        fab.setOnClickListener { webview.invoke("https://google.com") }
    }
}
