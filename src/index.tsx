import { NativeModules, Platform } from 'react-native';
import { RemoteUpdateProvider } from './Error';

const LINKING_ERROR =
  `The package 'react-native-remote-update' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RemoteUpdateModule = NativeModules.RemoteUpdate
  ? NativeModules.RemoteUpdate
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

type RemoteUpdateOptions = {
  /**
   * This is a URL to download the JSON from your server for remote updates in this app.
   */
  uri: string;

  /**
   * This is a callback function that will be called when the update is complete.
   */
  callback?: (error: string | null, result: string | null) => void;
};

const RemoteUpdate = (
  options: RemoteUpdateOptions,
  callback?: (error: any, result: any) => void
) => {
  const { uri } = options;
  const fn = callback || function () {};

  if (Platform.OS === 'ios') {
    // Lanza la excepción específica para iOS
    throw new Error(
      'Remote update is not available for iOS yet. Integration is coming soon.'
    );
  }

  return RemoteUpdateModule.update(uri, fn);
};

const getCurrentJson = (callback: (error: any, result: any) => void) => {
  if (Platform.OS === 'ios') {
    throw new Error(
      'Remote update is not available for iOS yet. Integration is coming soon.'
    );
  }

  return RemoteUpdateModule.getCurrentJson(callback ?? function () {});
};

const getBackupBundles = (callback: (error: any, result: any) => void) => {
  if (Platform.OS === 'ios') {
    throw new Error(
      'Remote update is not available for iOS yet. Integration is coming soon.'
    );
  }

  return RemoteUpdateModule.getBackupBundles(callback ?? function () {});
};

const getCurrentBundle = (callback: (error: any, result: any) => void) => {
  if (Platform.OS === 'ios') {
    throw new Error(
      'Remote update is not available for iOS yet. Integration is coming soon.'
    );
  }

  return RemoteUpdateModule.getCurrentBundle(callback ?? function () {});
};
export {
  RemoteUpdate,
  getCurrentJson,
  getBackupBundles,
  getCurrentBundle,
  RemoteUpdateProvider,
};
