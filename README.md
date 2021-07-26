## Requirements

minSdkVersion 21

## Setup

### Gradle
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
implementation 'io.github.WeLoopTeam:weloop:2.0.1'
```

### Updating the manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```
Do not forget to check the permission at the runtime : https://developer.android.com/training/permissions/requesting

## Usage

### Instantiation
Instantiate the WeLoop object:
```kotlin
var weloop = WeLoop(context, apiKey)
```
```java
WeLoop = WeLoop(context, apiKey)
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


### Init your uploadMessage (for attachment) :

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
        if (requestCode == 100) {
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

### Initialization

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
        });
```

2.a. Initialize and authentication

Initialize: You must call the ```initialize``` method in order to pass several informations such as the essential webview.  
Authentication user: You can provide the user identity in the code or let the user signin in. Simply provide the identity of the current user by calling `authenticateUser`.

```YourClass``` is the class where ```Weloop``` is initialized, it is a nullable
```webview``` is your ```Webview``` in your layout (not a ```WeLoop``` component)

Kotlin:
```kotlin
weloop.initialize(this.window, YourClass:class.java.name, webview)
weloop.authenticateUser(User(id = "3", email = "toto@gmail.com", firstName = "tata", lastName = "titi"))
```
Java:
```java
//fab is the FloatingWeidget view
weloop.initialize(this.getWindow(), MainActivityJava.class.getName(), webview);
weloop.authenticateUser(User("3","toto@gmail.com","tata","titi"));
```

2.b.

If you want to let the user signin in don't call ```authenticateUser```, and the webview will show the login page when it's invoked.

### Invocation
Once weloop is correctly initialized just call the ```invoke``` method
```kotlin
weLoop.invoke()
```

### Notification
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

1. Floating Action Button

If you want to invoke the webview with the floating button :
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

then init the widget with the preferences:
```kotlin
weLoop.initWidgetPreferences(fab)
```
finally set the invocationMethod
```kotlin
weloop.setInvocationMethod(WeLoop.FAB)
```

Customisation options for the button (color, icon, placement) can be done inside your WeLoop project settings.

2. Manual

```kotlin 
weloop.setInvocationMethod(WeLoop.MANUAL)

// Then, in your own button or control:
weloop.invoke()
```

## License

WeLoop is available under the MIT license. See the LICENSE file for more info.
