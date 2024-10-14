# react-native-remote-update

## What is this?

`react-native-remote-update` is a module for React Native that allows you to update your application remotely. It uses a JSON file to store the application's version and the commit hash of the latest version. When the app starts, it checks if there is an available update and downloads the new version if there is one.

## Installation 📦

Install the module using one of the following commands:

```sh
npm install react-native-remote-update
```

```sh
yarn add react-native-remote-update
```

```sh
bun add react-native-remote-update
```

Direct to your Android project at the path `android/app/src/main/java/com/yourapp/MainActivity.java`.
Import this line:

```kotlin
    import com.remoteupdate.BundleFileManager
```

add this fragment of code to the reactNativeHost block

```kotlin
override fun getJSBundleFile(): String? {
                return BundleFileManager.getJSBundleFile(applicationContext)
 }
```

your archvo should look like this

```kotlin
package com.remote

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader
import java.io.File
import com.remoteupdate.BundleFileManager // <-- 👈 Import the BundleFileManager module

class MainApplication : Application(), ReactApplication {

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> =
                PackageList(this).packages.apply {
                    // Packages that cannot be autolinked yet can be added manually here

                }

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
            override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
        //================== add this =================
            override fun getJSBundleFile(): String? {
                return BundleFileManager.getJSBundleFile(applicationContext)
            }
        //===============================
        }

    override val reactHost: ReactHost
        get() = getDefaultReactHost(applicationContext, reactNativeHost)

    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, false)
        if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
            // Load the native entry point for this app if the new architecture is enabled
            load()
        }
    }
}
```

**¡Atención!** La función de actualizaciones remotas actualmente no está disponible en iOS. Estamos trabajando en implementarla y te informaremos tan pronto como esté lista. Gracias por tu comprensión.

## Uso

Here is an example of a test server [test server](https://github.com/elmanci2/react-native-remote-update-server-test)

To use the `react-native-remote-update` module, follow these steps:

1. **Import the module** in your application:

   ```javascript
   import {
     RemoteUpdate,
     RemoteUpdateProvider,
   } from 'react-native-remote-update';
   ```

# ⚠ IMPORTANT WARNING ⚠

Download links **MUST BE DIRECT** and must not return a JSON response.

1. **Configure the update URL** that returns the JSON with the version information:

   ```javascript
   const uri = 'https://your-server.com/update.json';
   ```

2. **Call the `RemoteUpdate` function inside a `useEffect` hook to check for updates when your application starts:**

```javascript
useEffect(() => {
  RemoteUpdate({ uri }, (error, success) => {
    if (error) {
      console.error('Error al actualizar:', error);
    } else {
      console.log('Actualización exitosa:', success);
    }
  });
}, []);
```

3. **Wrap your application** in the `RemoteUpdateProvider` component. This component handles the update state and captures errors. If an error prevents the application from starting, the `RemoteUpdateProvider` component will display an error or update component as desired. This also provides the opportunity to send a new version and delete bundles that cause this error system:

```javascript
const App = () => {
  return (
    <RemoteUpdateProvider
      fallback={<Text>Error reverting to previous version, etc...</Text>} // <-- 👈 Fallback component to display if an error occurs in the app when updating the new version
      dev={!__DEV__}
    >
      <YourMainComponent />
    </RemoteUpdateProvider>
  );
};

export default App;
```

### Example Usage

Here is an example of how to implement `react-native-remote-update` in your application:

```javascript
import React, { useEffect } from 'react';
import { Text, ToastAndroid, View } from 'react-native';
import { RemoteUpdate, RemoteUpdateProvider } from 'react-native-remote-update';

const uri = 'https://your-server.com/update'; // <-- 👈 URL de actualización directa para descargar el archivo JSON

const MainComponent = () => {
  return (
    <View
      style={{
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'pink',
      }}
    >
      <Text
        style={{
          fontSize: 30,
          textAlign: 'center',
          color: 'white',
          fontWeight: 'bold',
        }}
      >
        ¡Hola Mundo!
      </Text>
    </View>
  );
};

const App = () => {
  // ==== this implementation will automatically update when the app starts and if there is a change, the changes will be reflected when the app is restarted ====

  useEffect(() => {
    RemoteUpdate({ uri }, (error, success) => {
      if (error) {
        console.error(error);
        ToastAndroid.show('Error en la actualización', ToastAndroid.SHORT);
      } else {
        console.log(success);
        ToastAndroid.show('Actualización completa', ToastAndroid.SHORT);
      }
    });
  }, []);

  return (
    <RemoteUpdateProvider // <-- 👈 this is not mandatory but it is recommended
      fallback={<Text>Error Regresando a una version anterior etc...</Text>} // <-- 👈 Fallback component to display if an error occurs in the app when updating the new version
      dev={!__DEV__} // <-- 👈 Enable development mode to view the fallback component
    >
      <MainComponent />
    </RemoteUpdateProvider>
  );
};

export default App;
```

# ⚠ WARNING ⚠

this dosn't work in development to test your implementation you need to generate an apk and test it there

```sh
cd android
```

and run this command

```sh
./gradlew assembleRelease
```

this will leave an apk in the android/app/build/outputs/apk/release path
the file is called app-release.apk

## creation of bundle file

# ⚠ WARNING ⚠

modify everything you want in your app at the javascript level you can install new packages that are only javascript and don't act if you install packages that require native code because that code is not compiled into the bundle only javascript

go to your project native folder in a console and run this command

```sh
npx react-native bundle --platform android --dev false --entry-file index.js --bundle-output ./dist/index.bundle --assets-dest ./assets/
```

this will create a bundle in the dist folder `./dist/index.bundle`

```json
 "bundle": {
    "uri": "https://your-server.com/bundle" // <-- 👈 URL to download the bundle directly
  }
```

# ⚠ warning ⚠

Do not update the app if you have used non-native code. You can only modify the JSON in the app. If you install or configure packages that overwrite native functions, these changes will not work in the app that is already published. You will need to generate a new file and publish it as you would normally do. However, if you only modified the JavaScript or installed packages that are purely JavaScript, the update will work perfectly.

## JSON structure

This should be a downloadable JSON file. Here is an example of the structure of the JSON that should be returned by your server:
\_\_
json example:

```json
{
  "version": "1.0.0", // <-- 👈 version number
  "versionCode": 4, // <-- 👈 version code of the app this is exlusive to make the app increment this every update begins at 0
  "commit": "4444", // <-- 👈  hash of the last version
  "fallback": true, // <-- 👈 allow the system to automatically fallback
  "fallbackDetails": {
    "commit": "3333", // <-- 👈 hash of the last version or the hash of the version specified that should be returned
    "enable": false // <-- 👈 activate or deactivate the fallback
  },
  "bundle": {
    "uri": "https://your-server.com/bundle" // <-- 👈 URL to download the bundle directly
  }
}
```

### Explanation of JSON fields

| Field             | Type    | Description                                           |
| ----------------- | ------- | ----------------------------------------------------- |
| `version`         | string  | The version number of the application                 |
| `versionCode`     | number  | The version code used for the application's update.   |
| `commit`          | string  | The hash of the commit for the latest version.        |
| `fallback`        | boolean | Indicates if a fallback is available.                 |
| `fallbackDetails` | object  | Contains details about the fallback version.          |
| `bundle`          | object  | Contains information about the bundle for the update. |
| `uri`             | string  | URL to download the bundle file.                      |

## Methods and Parameters

| Method             | Parameters                                            | Description                                                      |
| ------------------ | ----------------------------------------------------- | ---------------------------------------------------------------- |
| `RemoteUpdate`     | `{ uri: string, callback?: (error, result) => void }` | Initiates a remote update check.                                 |
| `getCurrentJson`   | `callback: (error, result) => void`                   | Retrieves the current JSON configuration from the remote source. |
| `getBackupBundles` | `callback: (error, result) => void`                   | Retrieves the backup bundles if available.                       |
| `getCurrentBundle` | `callback: (error, result) => void`                   | Gets information about the current bundle.                       |
|                    |

## Tips and Warnings

- Ensure that your server is properly configured to host the JSON file and the bundle.
- Use HTTPS for secure communications when retrieving updates.
- Remember to thoroughly test the integration on both platforms, Android and iOS.
- Keep in mind that iOS integration for remote updates will be available soon and may currently be unavailable.

## Contributing

Check the [contribution guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.
