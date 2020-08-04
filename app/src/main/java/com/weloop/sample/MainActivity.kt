package com.weloop.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.weloop.weloop.WeLoop
import com.weloop.weloop.model.User
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var weLoopWebView: WeLoop
    var uploadMessage: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        weLoopWebView = webview
        weLoopWebView.initialize("e19340c0-b453-11e9-8113-1d4bacf0614e", fab, window, this)
        weLoopWebView.authenticateUser(User(id = "4", email = "toto@email.fr", firstName = "John", lastName = "Doe"))
        weLoopWebView.addListener(object : WeLoop.NotificationListener{
            override fun getNotification(number: Int){
                //doSomeStuff
                Log.e("NOTIF:", "$number")
            }
        })
        weLoopWebView.webChromeClient = object:WebChromeClient() {
            override fun onShowFileChooser(webView: WebView, filePathCallback:ValueCallback<Array<Uri>>, fileChooserParams:FileChooserParams):Boolean {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                if (uploadMessage != null){
                    uploadMessage!!.onReceiveValue(null)
                    uploadMessage = null
                }
                uploadMessage = filePathCallback
                intent.setType("*/*")
                val PICKFILE_REQUEST_CODE = 100
                startActivityForResult(intent, PICKFILE_REQUEST_CODE)
                return true
            }
        }
        initListeners()
        if (Build.VERSION.SDK_INT >= 23){
            askForPermissions()
        }
        tabs.getTabAt(1)!!.select()
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
                    1 -> weLoopWebView.setInvocationMethod(WeLoop.FAB)
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {

            }
        })
    }

    private fun askForPermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE), 36)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (uploadMessage == null) return
            uploadMessage?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            uploadMessage = null
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
}
