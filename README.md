# react-native-remote-update

## ¿Qué es esto?

`react-native-remote-update` es un módulo para React Native que permite actualizar tu aplicación de forma remota. Utiliza un archivo JSON para almacenar la versión de la aplicación y el hash del commit de la última versión. Al iniciar la aplicación, verifica si hay una actualización disponible y descarga la nueva versión si la hay.

## Instalación

Puedes instalar el módulo utilizando uno de los siguientes comandos:

```sh
npm install react-native-remote-update
```

```sh
yarn add react-native-remote-update
```

```sh
bun add react-native-remote-update
```

dirija a su oiryecto android en la aruta androi/app/src/main/java/com/yourapp/MainActivity.java

import tis line

```kotlin
import com.remoteupdate.BundleFileManager
```

y agrgeu este fragmeto de codigo en el bloque reactNativeHost

```kotlin
override fun getJSBundleFile(): String? {
                return BundleFileManager.getJSBundleFile(applicationContext)
 }
```

su archvo devera que dar asi

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
import com.remoteupdate.BundleFileManager // <-- 👈 Importa el modulo BundleFileManager

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

## Uso

aqui hay un ejemplo dem un servidor de prueva com9o se deve entregarlos archivo [servidor de prueba](https://github.com/elmanci2/react-native-remote-update-server-test)

Para utilizar el módulo `react-native-remote-update`, sigue estos pasos:

1. **Importar el módulo** en tu aplicación:

   ```javascript
   import {
     RemoteUpdate,
     RemoteUpdateProvider,
   } from 'react-native-remote-update';
   ```

# ⚠ ADVERTENCIA IMPORTANTE ⚠

Los enlaces de descarga **DEBEN SER DIRECTOS** y no deben devolver una respuesta JSON. El servidor debe proporcionar un archivo descargable

1. **Configurar la URL de actualización** que devuelve el JSON con la información de la versión:

   ```javascript
   const uri = 'https://your-server.com/update.json';
   ```

2. **Llamar a la función `RemoteUpdate`** dentro de un hook `useEffect` para verificar actualizaciones cuando tu aplicación se inicia:

   ```javascrip
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

3. **Envolver tu aplicación** en el `RemoteUpdateProvider`. Este componente maneja el estado de la actualización y captura los errores. Si ocurre un error que impide que la aplicación se inicie, el `RemoteUpdateProvider` mostrará un componente de error o actualización según lo que desees. Esto también proporciona la oportunidad de enviar una nueva versión y eliminar los bundles que causan este error del sistema:

   ```javascript
   const App = () => {
     return (
       <RemoteUpdateProvider
         fallback={<Text>Actualizando...</Text>}
         dev={!__DEV__}
       >
         <YourMainComponent />
       </RemoteUpdateProvider>
     );
   };

   export default App;
   ```

### Ejemplo de Uso

Aquí tienes un ejemplo de cómo implementar `react-native-remote-update` en tu aplicación:

```javascript
import React, { useEffect } from 'react';
import { Text, ToastAndroid, View } from 'react-native';
import { RemoteUpdate, RemoteUpdateProvider } from 'react-native-remote-update';

const uri = 'https://your-server.com/update.json'; // <-- 👈 URL de actualización directa para descargar el archivo JSON

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
  // ==== esta implemetacion buwscara actulizaciones al iniciar la app y si hay procede a actulizarce automatiocamete los cambis se veran reflejado al reabrir la app  =====

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
    <RemoteUpdateProvider // <-- 👈 no es obligatorio pero si recomendable
      fallback={<Text>Error Regresando a una version anterior etc...</Text>} // <-- 👈 Componente de fallback para mostrar si hubo un eror en la app al actulisar la nueva versión
      dev={!__DEV__} // <-- 👈 Activar modo de desarrollo para ver el componente de fallback
    >
      <MainComponent />
    </RemoteUpdateProvider>
  );
};

export default App;
```

# ⚠ TIP ⚠

esto no funcionara en desarrollo para provar su impolemetacion deve gerar un apk y probarlo hay

abra un terminal en su proyecto y dirijace a la acarpeta android

```sh
cd android
```

y ejecute este comando

```sh
./gradlew assembleRelease
```

esto le dejara una apk en la ruta android/app/build/outputs/apk/release
el archivo se llama app-release.apk

instale y pruebe el sistema

## creasion de archivo bundle

modifique todo lo que quiera de su app a nivel de jvascript puede isntalar nuevos paquete que solo sean de javascript no actulise si a instalao paquetes que requieren de codigo nativo ya que esete codigo no se compila en el bundle solo el jsvascript

dirikace a la rais de su proyecto rect native en una consola i ejecute este comando

```sh
npx react-native bundle --platform android --dev false --entry-file index.js --bundle-output ./dist/index.bundle --assets-dest ./assets/
```

se creara un bundle en la carpeta dist

ese archvi que descargara su app para actulisar el codigo de la app ya en producion

./dist/index.bundle
y la ur que esta en el json bundle.uri deve proporcinar la descarga dirtecta de ese archivo

```json
 "bundle": {
    "uri": "https://your-server.com/bundle" // <-- 👈 URL de descarga directo para el bundle
  }
```

# ⚠ ADVERTENCIA IMPORTANTE ⚠

no actulise la app si a usado codigo no timo solo puede modificar el json en la app si intalo o configuro paquete que rewuiran funciones nativas esto no funcionaran en la app que ya tine publicada devera genera un nubo archi y ppublicarlo como nornmalmete lo haria pero si solo modificao el javascriv o intalo paqutes que solo sea javascrib la actulizacion funcinara perfectamete

## Estructura del JSON

esto deve ser un archivo json desacrgable uno una respuesta guet
\_\_
Aquí tienes un ejemplo de la estructura JSON que debería devolver tu servidor:

```json
{
  "version": "1.0.0", // <-- 👈 numero de version
  "versionCode": 4, // <-- 👈 numero de version del codigo de la app esto es exencial para que la app se actulise incremete esto en cada actulizacion cominece en 0
  "commit": "4444", // <-- 👈 hash del commit de la ultima version
  "fallback": true, // <-- 👈 permite que el sitema hatga un fallback automatico
  "fallbackDetails": {
    "commit": "3333", // <-- 👈 hash del commit de la ultima version o el hash del la version espesiifica quedeseas regresar
    "enable": false // <-- 👈 activar o desactivar el fallback
  },
  "bundle": {
    "uri": "https://your-server.com/bundle" // <-- 👈 URL de descarga directo para el bundle
  }
}
```

### Explicación de los Campos JSON

| Campo             | Tipo    | Descripción                                                            |
| ----------------- | ------- | ---------------------------------------------------------------------- |
| `version`         | string  | El número de versión de la aplicación.                                 |
| `versionCode`     | number  | El código de versión utilizado para la actualización de la aplicación. |
| `commit`          | string  | El hash del commit de la última versión.                               |
| `fallback`        | boolean | Indica si hay un fallback disponible.                                  |
| `fallbackDetails` | object  | Contiene detalles sobre la versión de fallback.                        |
| `bundle`          | object  | Contiene la información del bundle para la actualización.              |
| `uri`             | string  | URL para descargar el archivo de bundle.                               |

## Métodos y Parámetros

| Método             | Parámetros                                            | Descripción                                                  |
| ------------------ | ----------------------------------------------------- | ------------------------------------------------------------ |
| `RemoteUpdate`     | `{ uri: string, callback?: (error, result) => void }` | Inicia una verificación de actualización remota.             |
| `getCurrentJson`   | `callback: (error, result) => void`                   | Obtiene la configuración JSON actual desde la fuente remota. |
| `getBackupBundles` | `callback: (error, result) => void`                   | Recupera los bundles de respaldo si están disponibles.       |
| `getCurrentBundle` | `callback: (error, result) => void`                   | Obtiene la información del bundle actual.                    |

## Consejos y Advertencias

- Asegúrate de que tu servidor esté configurado correctamente para alojar el archivo JSON y el bundle.
- Utiliza HTTPS para comunicaciones seguras al recuperar actualizaciones.
- Recuerda probar la integración a fondo en ambas plataformas, Android e iOS.
- Ten en cuenta que la integración de iOS para actualizaciones remotas llegará pronto y actualmente puede no estar disponible.

## Contribuyendo

Consulta la [guía de contribución](CONTRIBUTING.md) para aprender cómo contribuir al repositorio y al flujo de trabajo de desarrollo.

## Licencia

MIT

---

Hecho con [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
