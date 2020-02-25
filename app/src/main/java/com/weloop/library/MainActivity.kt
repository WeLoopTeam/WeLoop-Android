package com.weloop.library

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.tabs.TabLayout
import com.weloop.weloop.FloatingWidget
import com.weloop.weloop.WeLoop
import com.weloop.weloop.model.User
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var weLoopWebView: WeLoop

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        weLoopWebView = findViewById(R.id.webview)
        weLoopWebView.initialize("742382b0-531e-11ea-8733-0fb1656485aa", findViewById<FloatingWidget>(R.id.fab))
        weLoopWebView.authenticateUser(User(id = "3", email = "toto@gmail.com", firstName = "tata", lastName = "titi"))
        initListeners()
        tabs.getTabAt(2)!!.select()
    }

    private fun initListeners(){
        tvManualInvocation.setOnClickListener {
            weLoopWebView.resumeWeLoop()
            if (tabs.selectedTabPosition == 0) {
                weLoopWebView.invoke()
            }
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {

            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                when (p0!!.position) {
                    0 -> weLoopWebView.setInvocationMethod(WeLoop.MANUAL)
                    1 -> weLoopWebView.setInvocationMethod(WeLoop.SHAKE_GESTURE)
                    2 -> weLoopWebView.setInvocationMethod(WeLoop.FAB)
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {

            }
        })
    }

    override fun onBackPressed() {
        if (webview.visibility == View.VISIBLE){
            webview.visibility = View.GONE
        }
        else {
            super.onBackPressed()
        }
    }
}
