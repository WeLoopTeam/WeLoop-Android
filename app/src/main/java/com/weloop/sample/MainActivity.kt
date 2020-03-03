package com.weloop.sample

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.tabs.TabLayout
import com.weloop.weloop.WeLoop
import com.weloop.weloop.model.User
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var weLoopWebView: WeLoop

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        weLoopWebView = webview
        weLoopWebView.initialize("742382b0-531e-11ea-8733-0fb1656485aa", fab, window)
        weLoopWebView.authenticateUser(User(id = "3", email = "toto@gmail.com", firstName = "tata", lastName = "titi"))
        weLoopWebView.addListener(object : WeLoop.NotificationListener{
            override fun getNotification(number: Int){
                //doSomeStuff
                Log.e("Notif", number.toString())
            }
        })
        initListeners()
        askForPermissions()
        tabs.getTabAt(2)!!.select()
    }

    override fun onStart() {
        super.onStart()
        weLoopWebView.resumeWeLoop()
    }

    private fun initListeners(){
        tvManualInvocation.setOnClickListener {
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

    private fun askForPermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 36)
        }
    }

    /***
     * Request permission results
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 36) {

            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            } else {
                Toast.makeText(this, "L'application a besoin des permissions pour fonctionner", Toast.LENGTH_LONG).show()
            }

        }
    }

    override fun onBackPressed() {
        if (webview.visibility == View.VISIBLE){
            webview.visibility = View.GONE
            fab.visibility = View.VISIBLE
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        weLoopWebView.destroyWeLoop()
    }

    override fun onStop() {
        super.onStop()
        weLoopWebView.stopWeLoop()
    }
}
