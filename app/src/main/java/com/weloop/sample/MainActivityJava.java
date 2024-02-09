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

import com.weloop.weloop.SideWidget;
import com.weloop.weloop.WeLoop;
import com.weloop.weloop.model.User;

class MainActivityJava extends AppCompatActivity {

    private ValueCallback<Uri[]> uploadMessage;
    WebView webView = findViewById(R.id.webview);
    SideWidget sideWidget = findViewById(R.id.side_widget);
    String apiKey = "apiKey";
    String projectId = "e19340c0-b453-11e9-8113-1d4bacf0614e";
    private WeLoop weLoop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weLoop = new WeLoop(this, projectId, apiKey);
        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams
                    fileChooserParams) {
                // make sure there is no existing message
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;
                intent.setType("*/*");
                startActivityForResult(intent, 100);

                return true;
            }
        });
        weLoop.initialize(
                "test@email.com",
                getWindow(),
                null,
                sideWidget,
                webView);
        weLoop.authenticateUser(new User("4", "toto@email.fr", "John", "Doe"));
        MainActivityJava.class.getName();
        weLoop.addNotificationListener(new WeLoop.NotificationListener() {
            @Override
            public void getNotification(int number) {
                //doSomeStuff
            }
        });
    }
}
