package com.weloop.sample;
/* Created by *-----* Alexandre Thauvin *-----* */

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.weloop.sample.databinding.ActivityMainBinding;
import com.weloop.weloop.SideWidget;
import com.weloop.weloop.WeLoop;

import timber.log.Timber;

class MainActivityJava extends AppCompatActivity {

    private ValueCallback<Uri[]> uploadMessage;
    WebView webView = findViewById(R.id.webview);
    SideWidget sideWidget = findViewById(R.id.side_widget);
    String apiKey = "apiKey";
    String projectId = "e19340c0-b453-11e9-8113-1d4bacf0614e";
    private WeLoop weLoop;

    private ActivityMainBinding viewBinding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weLoop = new WeLoop(this, projectId, apiKey);
        viewBinding = ActivityMainBinding.inflate(this.getLayoutInflater());
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
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Timber.e("console message: " + (consoleMessage != null ? consoleMessage.message() : null));
                if (consoleMessage != null && consoleMessage.message() != null && consoleMessage.message().contains("Scripts may close only the windows that were opened by them")) {
                    Timber.d("reloading webview");
                    weLoop.restartWebview();
                }
                return super.onConsoleMessage(consoleMessage);
            }
        });
        weLoop.initialize(
                "test@email.com",
                getWindow(),
                null,
                sideWidget,
                webView);
        MainActivityJava.class.getName();
        weLoop.addNotificationListener(new WeLoop.NotificationListener() {
            @Override
            public void getNotification(int number) {
                //doSomeStuff
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && WeLoop.INTENT_FILTER_WELOOP_NOTIFICATION.equals(intent.getAction())) {
            weLoop.redirectToWeLoopFromPushNotification();
        }
    }

    @Override
    public void onBackPressed() {
        weLoop.backButtonHasBeenPressed();
        if (viewBinding.webview.getVisibility() == View.VISIBLE) {
            viewBinding.webview.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }


}
