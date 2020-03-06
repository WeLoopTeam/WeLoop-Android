[![](https://jitpack.io/v/WeLoopTeam/WeLoop-Android.svg)](https://jitpack.io/#WeLoopTeam/WeLoop-Android)

## Requirements

minSdkVersion 23

## Setup

### Gradle
Add it in your root build.gradle (project level) at the end of repositories:
```gradle
    allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
```

Add the dependency in your build.gradle (app level)
```gradle
implementation 'com.github.WeLoopTeam:WeLoop-Android:1.0.2'
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
Init your WeLoop var :  
Java:
```java
Weloop weloopWebView = findViewById(R.id.webview)
```
kotlin:
```kotlin
var weloopWebview = webview
```
Do not forget to destroy/stop/start your weloop var
Kotlin:
```kotlin
    override fun onStart() {
        super.onStart()
        weLoopWebView.resumeWeLoop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        weLoopWebView.destroyWeLoop()
    }

    override fun onStop() {
        super.onStop()
        weLoopWebView.stopWeLoop()
    }
```
Java:
```java
    void onStart() {
        super.onStart()
        weLoopWebView.resumeWeLoop()
    }
    
    void onDestroy() {
        super.onDestroy()
        weLoopWebView.destroyWeLoop()
    }

    void fun onStop() {
        super.onStop()
        weLoopWebView.stopWeLoop()
    }
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
weLoopWebView.addListener(object : WeLoop.NotificationListener{
            override fun getNotification(number: Int){
                //doSomeStuff
            }
        })
```

### Invocation method

You can choose between different methods to invoke the WeLoop widget inside your application:

1. Floating Action Button

```kotlin
weLoopWebView.setInvocationMethod(WeLoop.FAB)
```

Customisation options for the button (color, icon, placement) can be done inside your WeLoop project settings.

2. Shake Gesture

```kotlin
weLoopWebView.setInvocationMethod(WeLoop.SHAKE_GESTURE)
```

3. Manual

```kotlin 
weLoopWebView.setInvocationMethod(WeLoop.MANUAL)

// Then, in your own button or control:

weLoopWebView.invoke()

```

## License

WeLoop is available under the MIT license. See the LICENSE file for more info.
