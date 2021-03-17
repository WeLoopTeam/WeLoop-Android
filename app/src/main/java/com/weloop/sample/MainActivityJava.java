package com.weloop.sample;
/* Created by *-----* Alexandre Thauvin *-----* */

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weloop.weloop.FloatingWidget;
import com.weloop.weloop.WeLoop;

class MainActivityJava extends AppCompatActivity {

    private ValueCallback<Uri[]> uploadMessage;
    WeLoop weloopWebView = findViewById(R.id.webview);
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weloopWebView.initialize("", (FloatingWidget) findViewById(R.id.fab), getWindow(), this, null);
        MainActivityJava.class.getName();
        weloopWebView.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams
                    fileChooserParams) {
                // make sure there is no existing message
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                if (uploadMessage != null){
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;
                intent.setType("*/*");
                startActivityForResult(intent, 100);

                return true;
            }
        });
        weloopWebView.addNotificationListener(new WeLoop.NotificationListener(){
            @Override
            public void getNotification(int number){
                //doSomeStuff
            }
        });
    }
}
