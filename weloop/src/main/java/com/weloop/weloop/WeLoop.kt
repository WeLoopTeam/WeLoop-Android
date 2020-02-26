package com.weloop.weloop

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.tbouron.shakedetector.library.ShakeDetector
import com.github.tbouron.shakedetector.library.ShakeDetector.OnShakeListener
import com.weloop.weloop.model.User
import com.weloop.weloop.model.WebAppInterface
import com.weloop.weloop.network.ApiServiceImp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.security.SecureRandom
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread


/* Created by *-----* Alexandre Thauvin *-----* */

class WeLoop : WebView {
    private var currentInvocationMethod = 0
    private var apiKey: String = ""
    private lateinit var floatingWidget: FloatingWidget
    private var webViewInterface = WebAppInterface()
    private val disposable = CompositeDisposable()
    private lateinit var token: String
    private lateinit var window: Window
    private var screenshot: String = ""

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        visibility = View.GONE
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.javaScriptEnabled = true
    }

    fun initialize(apiKey: String, floatingWidget: FloatingWidget, window: Window) {
        this.floatingWidget = floatingWidget
        this.window = window
        this.apiKey = apiKey
        initWebAppListener()
        addJavascriptInterface(webViewInterface, "Android")
        ShakeDetector.create(context, OnShakeListener {
            invoke()
        })

        this.floatingWidget.setOnClickListener {
            invoke()
        }

        loadUrl(URL + apiKey)
        initWidgetPreferences()
    }

    private fun initWebAppListener() {
        webViewInterface.addListener(object : WebAppInterface.WebAppListener {
            override fun closePanel() {
                this@WeLoop.post { visibility = View.GONE ; floatingWidget.visibility = View.VISIBLE }
            }

            override fun getCapture(){
                loadUrl("javascript:GetCapture('data:image/jpg;base64, $screenshot')")
            }

            override fun getCurrentUser(){
               /* val map = mutableMapOf<String, String>()
                map["token"] = token
                map["apiKey"] = apiKey*/
                this@WeLoop.post { loadUrl("javascript:GetCurrentUser({ appGuid: $apiKey, token: $token})") }
                //return JSONObject(map.toMap()).toString()
            }

            override fun setNotificationCount(number: Int) {
                floatingWidget.count = number
            }
        })
    }

    private fun takeScreenshot(): Bitmap?{
        val now = Date()
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
        try {
            // image naming and path  to include sd card  appending name you choose for file
            val mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            val v1 = window.decorView.rootView
            v1.setDrawingCacheEnabled(true)
            val bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            return bitmap
        } catch (e: Throwable) {
            e.printStackTrace();
        }
        return null
    }

    private fun initWidgetPreferences() {
        disposable.add(ApiServiceImp.getWidgetPreferences(this.apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {

            }
            .subscribe {
                if (it.widgetPrimaryColor != null) {
                    this.floatingWidget.backgroundTintList = ColorStateList.valueOf(
                        Color.rgb(
                            it.widgetPrimaryColor!!["r"]!!.toInt(),
                            it.widgetPrimaryColor!!["g"]!!.toInt(),
                            it.widgetPrimaryColor!!["b"]!!.toInt()
                        )
                    )
                } else {
                    this.floatingWidget.backgroundTintList =
                        ColorStateList.valueOf(context.getColor(R.color.defaultColorWidget))
                }
                if (it.widgetIcon != null) {
                    Glide.with(context)
                        .asBitmap()
                        .load(it.widgetIcon)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                floatingWidget.setImageBitmap(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                // this is called when imageView is cleared on lifecycle call or for
                                // some other reason.
                                // if you are referencing the bitmap somewhere else too other than this imageView
                                // clear it here as you can no longer have the bitmap
                            }
                        })
                }
                if (it.widgetPosition.equals("right", ignoreCase = true)) {
                    val params = CoordinatorLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 10, 10)
                        gravity = Gravity.END or Gravity.BOTTOM
                    }
                    this.floatingWidget.layoutParams = params
                } else {
                    val params = CoordinatorLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(40, 0, 0, 40)
                        gravity = Gravity.START or Gravity.BOTTOM
                    }
                    this.floatingWidget.layoutParams = params
                }
            }
        )
    }

    fun authenticateUser(user: User) {
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
            val str = user.email + "|" + user.firstName + "|" + user.lastName + "|" + user.id
            token = AES256Cryptor.encrypt(str, apiKey)
            Log.e("token", token)
        } else {
            Toast.makeText(context, "email incorrecte", Toast.LENGTH_LONG).show()
        }
    }

    fun setInvocationMethod(invocationMethod: Int) {
        this.currentInvocationMethod = invocationMethod
        renderInvocation()
    }

    private fun renderInvocation() {
        when (currentInvocationMethod) {
            FAB -> {
                if (::floatingWidget.isInitialized) {
                    floatingWidget.visibility = View.VISIBLE
                }
                ShakeDetector.stop()
            }
            SHAKE_GESTURE -> {
                if (::floatingWidget.isInitialized) {
                    floatingWidget.visibility = View.GONE
                }
                ShakeDetector.start()
            }
            else -> {
                if (::floatingWidget.isInitialized) {
                    floatingWidget.visibility = View.GONE
                }
                ShakeDetector.stop()
            }
        }
    }

        /*private fun encryptTest(plaintext: String, passphrase: String): String? {
        try {
            val keySize = 256;
            val ivSize = 128;

            // Create empty key and iv
            val key =  byte[keySize / 8];
            val iv =  byte[ivSize / 8];

            // Create random salt
            val saltBytes = generateSalt(8);

            // Derive key and iv from passphrase and salt
            EvpKDF(passphrase.toByteArray(), keySize, ivSize, saltBytes, key, iv);

            // Actual encrypt
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,  SecretKeySpec(key, "AES"),  IvParameterSpec(iv));
            val cipherBytes = cipher.doFinal(plaintext.toByteArray());

            *//**
             * Create CryptoJS-like encrypted string from encrypted data
             * This is how CryptoJS do:
             * 1. Create new byte array to hold ecrypted string (b)
             * 2. Concatenate 8 bytes to b
             * 3. Concatenate salt to b
             * 4. Concatenate encrypted data to b
             * 5. Encode b using Base64
             *//*
            val sBytes = "Salted__".toByteArray();
            val b =  byte[sBytes.size + saltBytes.length + cipherBytes.size];
            System.arraycopy(sBytes, 0, b, 0, sBytes.size);
            System.arraycopy(saltBytes, 0, b, sBytes.size, saltBytes.length);
            System.arraycopy(cipherBytes, 0, b, sBytes.size + saltBytes.length, cipherBytes.size);

            val base64b = Base64.encode(b, Base64.DEFAULT);

            return String(base64b);
        } catch (e: Exception) {
            e.printStackTrace();
        }

        return null;
    }

    private fun decryptTest(ciphertext: String, passphrase: String): String? {
        try {
            val keySize = 256;
            val ivSize = 128;

            // Decode from base64 text
            val ctBytes = Base64.decode(ciphertext.toByteArray(), Base64.DEFAULT);

            // Get salt
            val saltBytes = Arrays.copyOfRange(ctBytes, 8, 16);

            // Get ciphertext
            val ciphertextBytes = Arrays.copyOfRange(ctBytes, 16, ctBytes.size);

            // Get key and iv from passphrase and salt
            val key =  byte[keySize / 8];
            val iv =  byte[ivSize / 8];
            EvpKDF(passphrase.toByteArray(), keySize, ivSize, saltBytes, key, iv);

            // Actual decrypt
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,  SecretKeySpec(key, "AES"),  IvParameterSpec(iv));
            val recoveredPlaintextBytes = cipher.doFinal(ciphertextBytes);

            return  String(recoveredPlaintextBytes);
        } catch (e: Exception) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("unused")
    private fun hexStringToByteArray(s: String) {
        val len = s.length
        val data = byte[len / 2];
        for (i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    *//**
     * @return a new pseudorandom salt of the specified length
     *//*
    private fun generateSalt(length: Int) {
        val r = SecureRandom();
        val salt = byte[length];
        r.nextBytes(salt);
        return salt;
    }

    private static byte[] EvpKDF(byte[] password, int keySize, int ivSize, byte[] salt, byte[] resultKey, byte[] resultIv) throws NoSuchAlgorithmException {
        return EvpKDF(password, keySize, ivSize, salt, 1, "MD5", resultKey, resultIv);
    }

    private static byte[] EvpKDF(byte[] password, int keySize, int ivSize, byte[] salt, int iterations, String hashAlgorithm, byte[] resultKey, byte[] resultIv) throws NoSuchAlgorithmException {
        keySize = keySize / 32;
        ivSize = ivSize / 32;
        int targetKeySize = keySize + ivSize;
        byte[] derivedBytes = new byte[targetKeySize * 4];
        int numberOfDerivedWords = 0;
        byte[] block = null;
        MessageDigest hasher = MessageDigest.getInstance(hashAlgorithm);
        while (numberOfDerivedWords < targetKeySize) {
            if (block != null) {
                hasher.update(block);
            }
            hasher.update(password);
            block = hasher.digest(salt);
            hasher.reset();

            // Iterations
            for (int i = 1; i < iterations; i++) {
                block = hasher.digest(block);
                hasher.reset();
            }

            System.arraycopy(block, 0, derivedBytes, numberOfDerivedWords * 4,
                    Math.min(block.length, (targetKeySize - numberOfDerivedWords) * 4));

            numberOfDerivedWords += block.length / 4;
        }

        System.arraycopy(derivedBytes, 0, resultKey, 0, keySize * 4);
        System.arraycopy(derivedBytes, keySize * 4, resultIv, 0, ivSize * 4);

        return derivedBytes; // key + iv
    }*/

    private fun encrypt(strToEncrypt: String, secret: String): String {
        val iv = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val ivSpec = IvParameterSpec(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), apiKey.toByteArray(), 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(secret.toByteArray(), "AES")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        return Base64.encodeToString(cipher.doFinal(strToEncrypt.toByteArray()), 0)
    }

    private fun decrypt(strToDecrypt: String, secret: String): String {
        val iv = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val ivSpec = IvParameterSpec(iv)

        val factory = SecretKeyFactory.getInstance(TRANSFORMATION)
        val spec = PBEKeySpec(secret.toCharArray(), apiKey.toByteArray(), 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(secret.toByteArray(), "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return String(cipher.doFinal(Base64.decode(strToDecrypt, 0)))
    }

    fun resumeWeLoop() {
        Toast.makeText(context, AES256Cryptor.decrypt(token, apiKey), Toast.LENGTH_LONG).show()
        ShakeDetector.start()
    }

    fun stopWeLoop() {
        ShakeDetector.stop()
    }

    fun destroyWeLoop() {
        ShakeDetector.destroy()
    }

    fun invoke() {
        floatingWidget.visibility = View.GONE
        val bitmap = takeScreenshot()
        val byteArrayOutputStream =  ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream .toByteArray()
        screenshot = Base64.encodeToString(byteArray, Base64.DEFAULT)
        visibility = View.VISIBLE
    }

    companion object {
        const val FAB = 0
        const val SHAKE_GESTURE = 1
        const val MANUAL = 2
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val URL = "https://staging-widget.30kg-rice.cooking/home?appGuid="
    }
}