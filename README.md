[ ![Download](https://api.bintray.com/packages/paseuht/WeLoop/WeLoop/images/download.svg?version=1.0.9) ](https://bintray.com/paseuht/WeLoop/WeLoop/1.0.9/link)

## Requirements

minSdkVersion 21

## Setup

### Gradle
Add it in your root build.gradle (project level) at the end of repositories:
```gradle
    buildscript {
    	repositories {
        jcenter()//add jcenter()
    	}
    }
    
    allprojects {
    repositories {
        jcenter() //add jcenter()
    }
}
```

Add the dependency in your build.gradle (app level)
```gradle
implementation 'com.github.WeLoopTeam:weloop:1.0.9'
```

### Updating the manifest

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```
Do not forget to check the permission at the runtime : https://developer.android.com/training/permissions/requesting

## Usage

### Invocation

First you must implement the floating button and the webview :  
webview :
```xml
        <com.weloop.weloop.WeLoop
            android:id="@+id/webview"
            android:layout_width="match_parent"
            android:layout_height="match_parent"/>
```
fab :
```xml
<com.weloop.weloop.FloatingWidget
        app:badgeBackgroundColor="@color/defaultColorBadge"
        android:background="@android:color/black"
        android:id="@+id/fab"
        android:layout_gravity="bottom|start"
        android:layout_margin="20dp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
```
if your app has a minSDK < 23 ; Put this in the parent activity of the weloop webview:  

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


Init your WeLoop var and uploadMessage (for attachment) :  
Java:
```java
private String PICKFILE_REQUEST_CODE = 100;
private ValueCallback<Uri[]> uploadMessage;
private WeLoop weloopWebView;
```
kotlin:
```kotlin
val PICKFILE_REQUEST_CODE = 100
var uploadMessage: ValueCallback<Array<Uri>>? = null
private lateinir var weloopWebview: WeLoop
```

Init your weloopWebview:  
Kotlin:
```kotlin
weLoopWebView = webview
```
Java
```Java
weloopWebView = findViewById(R.id.webview)
```

In order to invoke WeLoop you have two options. 

1. You provide the user identity. Simply provide your project key, and identity the current user by calling `authenticateUser`.

Kotlin:
```kotlin
//fab is the FloatingWeidget view
weLoopWebView.initialize("YOUR_PROJECT_GUID", fab, this.window)// from a fragment : activity.window
weLoopWebView.authenticateUser(User(id = "3", email = "toto@gmail.com", firstName = "tata", lastName = "titi"))
```
Java:
```java
//fab is the FloatingWeidget view
weLoopWebView.initialize("YOUR_PROJECT_GUID", fab, this.getWindow())// from a fragment : activity.getWindow()
weLoopWebView.authenticateUser(User("3","toto@gmail.com","tata","titi"))
```

2. You let the user provide its login infos: don't call `authenticateUser``, and the widget will show the login page when it's launched.

```kotlin
weLoopWebView.initialize("YOUR_PROJECT_GUID", fab, this.getWindow())// from a fragment : activity.getWindow()
```
3. Add webChromeClient (for attachment)
Kotlin:
```kotlin
weLoopWebView.webChromeClient = object:WebChromeClient() {
            override fun onShowFileChooser(webView: WebView, filePathCallback:ValueCallback<Array<Uri>>, fileChooserParams:FileChooserParams):Boolean {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                if (uploadMessage != null){
                    uploadMessage!!.onReceiveValue(null)
                    uploadMessage = null
                }
                uploadMessage = filePathCallback
                intent.setType("*/*")
                startActivityForResult(intent, PICKFILE_REQUEST_CODE)
                return true
            }
        }
```

Java:
```Java
	@Override
    	protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                startActivityForResult(intent, PICKFILE_REQUEST_CODE);

                return true;
            }
        });
    }
```

4. Handle the data in onActivityResult
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

### Listener
If you want to get the notification number in real time :  
Kotlin:
```kotlin
weLoopWebView.addListener(object : WeLoop.NotificationListener{
            override fun getNotification(number: Int){
                //doSomeStuff
            }
        })
```
Java:
```java
weloopWebView.addListener(new WeLoop.NotificationListener(){
            @Override
            public void getNotification(int number){
                //doSomeStuff
            }
        });
```

### Invocation method

You can choose between different methods to invoke the WeLoop widget inside your application:

1. Floating Action Button

```kotlin
weLoopWebView.setInvocationMethod(WeLoop.FAB)
```

Customisation options for the button (color, icon, placement) can be done inside your WeLoop project settings.

2. Manual

```kotlin 
weLoopWebView.setInvocationMethod(WeLoop.MANUAL)

// Then, in your own button or control:

weLoopWebView.invoke()

```

## License

WeLoop is available under the MIT license. See the LICENSE file for more info.
