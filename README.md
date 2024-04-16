[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.WeLoopTeam/weloop/badge.svg?style=flat-plastic&gav=true)](https://maven-badges.herokuapp.com/maven-central/io.github.WeLoopTeam/weloop)

# Requirements

minSdkVersion 21

# Setup

## Gradle
Add it in your root build.gradle (project level) at the end of repositories:
```gradle
buildscript {
    ext.kotlin_version = '1.5.21'
    repositories {
        maven { url "https://jitpack.io" }
        maven { url "https://plugins.gradle.org/m2/" }
        google()
        mavenCentral()
    }
}

allprojects {
    repositories {
        maven { url "https://jitpack.io" }
        maven { url "https://plugins.gradle.org/m2/" }
        google()
        mavenCentral()
    }
}
```

Add the dependency in your build.gradle (app level)
```gradle
implementation 'io.github.WeLoopTeam:weloop:2.1.2'
```

## Updating the manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```
Do not forget to check the permission at the runtime : https://developer.android.com/training/permissions/requesting

# Usage

## Instantiation
Instantiate the WeLoop object (we now need to make a difference between projectId and apiKey):
```kotlin
var weloop = WeLoop(context, projectId, apiKey)
```
```java
WeLoop = WeLoop(context, projectId, apiKey)
```

### minSDK < 23

If your app minSDK < 23 ; Put this in the parent activity of the weloop webview:  

Java:
```java
    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }
```
kotlin:
```kotlin
override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        if (Build.VERSION.SDK_INT in 21..22) {
            return
        }
        super.applyOverrideConfiguration(overrideConfiguration)
}
```


## Init your uploadMessage (for attachment) :

1. Declare the variable

kotlin:
```kotlin
val PICKFILE_REQUEST_CODE = 100
var uploadMessage: ValueCallback<Array<Uri>>? = null
```
Java:
```java
private String PICKFILE_REQUEST_CODE = 100;
private ValueCallback<Uri[]> uploadMessage;
```

2. Handle the data in ```onActivityResult```

```kotlin
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICKFILE_REQUEST_CODE) {
            if (uploadMessage == null) return
            uploadMessage?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            uploadMessage = null
        }
    }
```

Java:
```Java
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == PICKFILE_REQUEST_CODE) {
            		if (uploadMessage == null) return
            		uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            		uploadMessage = null
        	}
    	}
```

## Initialization

1. Add ```webChromeClient``` to your ```webview``` (for attachment)

Kotlin:
```kotlin
        webview.webChromeClient = object:WebChromeClient() {
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

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Timber.e("console message: ${consoleMessage?.message()}")
                if (consoleMessage?.message()?.contains("Scripts may close only the windows that were opened by them") == true) {
                    Timber.d("reloading webview")
                    weLoop.restartWebview()
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }
```

Java:
```Java
        webView.setWebChromeClient(new WebChromeClient() {
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
```


### Initialize and authentication

Initialize: You must call the ```initialize``` method in order to pass several informations such as the essential webview.  

```YourClass``` is the class where ```Weloop``` is initialized, it is a nullable
```webview``` is your ```Webview``` in your layout (not a ```WeLoop``` component)
```email``` is the email of your weloop account, we need that in order to get the side widget visibility 
```sideWidget``` is the new weloop side widget, you can't control the visibility of it, indeed it depends on who will use your app and the role they have in weloop dashboard

Kotlin:
```kotlin
weloop.initialize(email, this.window, YourClass:class.java.name, sideWidget, webview)
```
Java:
```java
weloop.initialize(email, this.getWindow(), MainActivityJava.class.getName(), sideWidget, webview);
```

###

If you want to let the user signin in don't call ```authenticateUser```, and the webview will show the login page when it's invoked.

## Invocation
Once weloop is correctly initialized just call the ```invoke``` method
```kotlin
weLoop.invoke()
```

## Notification

### Push Notifications

You need to register for push notifications:
```language``` is the language that you want for the notification that will be received by your user.
The format is ISO 2 letters EN/FR/ES etc...
```kotlin
weLoop.registerPushNotification(activity, firstName, lastName, email, "EN")
```

in order to have the redirection working when the user click on the push notification you need to implement the ```onNewIntent``` method
```kotlin
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (it.action.equals(WeLoop.INTENT_FILTER_WELOOP_NOTIFICATION)) {
                weLoop.redirectToWeLoopFromPushNotification()
            }
        }
    }
```
```java
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent != null && WeLoop.INTENT_FILTER_WELOOP_NOTIFICATION.equals(intent.getAction())) {
        weLoop.redirectToWeLoopFromPushNotification();
    }
}
```

### VERY IMPORTANT POINT

You need to unregister from the push notification in order to avoid useless connection when the app is killed or when the user has finished his workflow (log out/unregister etc..)

call ```unregisterPushNotification``` it MUST be called at the end of your workflow (quoted above) AND in your ```onDestroy``` method:

```kotlin
    override fun onDestroy() {
        weLoop.unregisterPushNotification()
        super.onDestroy()
    }
```
```java
@Override
protected void onDestroy() {
    weLoop.unregisterPushNotification();
    super.onDestroy();
}
```

### Notifications number

In order to receive the number of notification you have to implement this callback
Notifications will be received after the first call of ```invoke()```  
Add the listener:  
Kotlin:
```kotlin
weloop.addNotificationListener(object : WeLoop.NotificationListener{
            override fun getNotification(number: Int){
                //doSomeStuff
            }
        })
```
Java:
```java
weloop.addNotificationListener(new WeLoop.NotificationListener(){
            @Override
            public void getNotification(int number){
                //doSomeStuff
            }
        });
```

Manually request the notification number:

Kotlin:
```kotlin
weloop.requestNotification(email)
```
Java:
```java
weloop.requestNotification(email)
```

Request notification number every 2 minutes  
This method will request notification number every 2 minutes, use it carefully.  
To start requesting:
```kotlin
weLoop.startRequestingNotificationsEveryTwoMinutes(email)
```
To stop requesting:
```kotlin
weLoop.stopRequestingNotificationsEveryTwoMinutes()
```

then the ```getNotification``` method of the ```NotificationListener``` will be triggered for the result 

### Invocation method

You can choose between different methods to invoke the WeLoop widget inside your application:

If you want to invoke the webview with the side widget button (if the user can according to his role in weloop dashboard) :
side widget :
```xml
    <com.weloop.weloop.SideWidget
        android:layout_centerVertical="true"
        android:layout_alignParentEnd="true"
        app:badgeBackgroundColor="@color/defaultColorBadge"
        android:id="@+id/side_widget"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
```

## Webview visibility handling  

In order to dismiss the webview when the user press the back button:
```kotlin
    override fun onBackPressed() {
        weLoop.backButtonHasBeenPressed()
        if (viewBinding.webview.visibility == View.VISIBLE) {
            viewBinding.webview.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
```
```java
@Override
public void onBackPressed() {
    weLoop.backButtonHasBeenPressed();
    if (viewBinding.webview.getVisibility() == View.VISIBLE) {
        viewBinding.webview.setVisibility(View.GONE);
    } else {
        super.onBackPressed();
    }
}

```
