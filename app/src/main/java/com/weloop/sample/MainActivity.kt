package com.weloop.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.weloop.sample.databinding.ActivityMainBinding
import com.weloop.weloop.WeLoop
import com.weloop.weloop.model.User
import timber.log.Timber

private const val PICKFILE_REQUEST_CODE = 100


class MainActivity : AppCompatActivity() {

    private lateinit var weLoop: WeLoop
    var uploadMessage: ValueCallback<Array<Uri>>? = null
    private var email = "charles.tatibouet@gmail.com"
    private var projectId = "117c2085-d555-4b7e-b39e-3663afffd924"
    private var apiKey = "1234"

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        weLoop = WeLoop(
            mContext = this,
            mProjectId = projectId,
            mApiKey = apiKey
        )

        viewBinding.webview.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                if (uploadMessage != null) {
                    uploadMessage!!.onReceiveValue(null)
                    uploadMessage = null
                }
                uploadMessage = filePathCallback
                intent.setType("*/*")
                startActivityForResult(intent, PICKFILE_REQUEST_CODE)
                return true
            }


            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Timber.e("console message: ${consoleMessage?.message()}")
                if (consoleMessage?.message()?.contains("Scripts may close only the windows that were opened by them") == true) {
                    Timber.d("reloading webview")
                    weLoop.restartWebview()
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }
        weLoop.initialize(
            email = "test@email.com",
            window = window,
            weloopLocation = MainActivity::class.java.name,
            sideWidget = viewBinding.sideWidget,
            webView = viewBinding.webview
        )
        weLoop.registerPushNotification(this, "first", " last", email, "language")
        weLoop.authenticateUser(User(id = "4", email = email, firstName = "John", lastName = "Doe"))
        weLoop.addNotificationListener(object : WeLoop.NotificationListener {
            override fun getNotification(number: Int) {
                //doSomeStuff
                Log.e("NOTIF:", "$number")
                Toast.makeText(this@MainActivity, "NOTIF: $number", Toast.LENGTH_SHORT).show()
            }
        })
        viewBinding.buttonStartNotifLoop.setOnClickListener {
            weLoop.startRequestingNotificationsEveryTwoMinutes(email)
        }
        viewBinding.buttonStopNotifLoop.setOnClickListener {
            weLoop.stopRequestingNotificationsEveryTwoMinutes()
            weLoop.unregisterPushNotification()
        }
        initListeners()
        if (Build.VERSION.SDK_INT >= 23) {
            askForPermissions()
        }
        viewBinding.buttonNotif.setOnClickListener {
            weLoop.requestNotification(email)
        }


    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        if (Build.VERSION.SDK_INT in 21..22) {
            return
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    private fun initListeners() {
        viewBinding.tvManualInvocation.setOnClickListener {
            weLoop.invoke()
        }
    }

    private fun askForPermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 36
            )
        }
    }

    /***
     * Request permission results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 36) {

            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            } else {
                Toast.makeText(
                    this,
                    "L'application a besoin des permissions pour fonctionner",
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICKFILE_REQUEST_CODE) {
            if (uploadMessage == null) return
            uploadMessage?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    data
                )
            );
            uploadMessage = null
        }
    }

    override fun onBackPressed() {
        weLoop.backButtonHasBeenPressed()
        if (viewBinding.webview.visibility == View.VISIBLE) {
            viewBinding.webview.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (it.action.equals(WeLoop.INTENT_FILTER_WELOOP_NOTIFICATION)) {
                weLoop.redirectToWeLoopFromPushNotification()
            }
        }
    }

    override fun onDestroy() {
        weLoop.unregisterPushNotification()
        super.onDestroy()
    }
}
