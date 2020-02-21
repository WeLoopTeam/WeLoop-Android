package com.weloop.library

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import com.weloop.weloop.FloatingWidget
import com.weloop.weloop.WeLoop
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webView = findViewById<WeLoop>(R.id.webview)
        webView.initialize("742382b0-531e-11ea-8733-0fb1656485aa", findViewById<FloatingWidget>(R.id.fab))
    }
}
