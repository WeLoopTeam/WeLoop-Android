[![Version](https://img.shields.io/cocoapods/v/WeLoop.svg?style=flat)](https://cocoapods.org/pods/WeLoop)
[![Platform](https://img.shields.io/cocoapods/p/WeLoop.svg?style=flat)](https://cocoapods.org/pods/WeLoop)


## Requirements

Since WeLoop builds in swift 5.0, Xcode 10.2 is required to build the project.

The dependency requires iOS 9.0 or above to be built.

## Setup

### Gradle

gradle stuff

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

### Updating your plist

Since WeLoop offers the possibility to upload photos from the user photo gallery and from the camera, you will have to add the following entries to your plist, if they are not already present:

```Android
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

## License

WeLoop is available under the MIT license. See the LICENSE file for more info.
