


## Requirements

minSdkVersion 23

## Setup

### Gradle

gradle stuff

### Updating the manifest

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

## Usage

### Invocation

In order to invoke WeLoop you have two options. 

1. You provide the user identity. Simply provide your project key, and identity the current user by calling `identifyUser`.


```kotlin
//fab is the FloatingWeidget view
weLoopWebView.initialize("YOUR_PROJECT_GUID", fab, this.window)
weLoopWebView.authenticateUser(User(id = "3", email = "toto@gmail.com", firstName = "tata", lastName = "titi"))
```

2. You let the user provide its login infos: don't call `authenticateUser``, and the widget will show the login page when it's launched.

```kotlin
weLoopWebView.initialize("YOUR_PROJECT_GUID", fab, window)
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
