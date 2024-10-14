package com.remoteupdate;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.io.IOException;
import org.json.JSONException;
import java.net.URL;
import android.os.StatFs;
import javax.net.ssl.HttpsURLConnection;
import android.util.Log;

public class RemoteUpdateModule extends ReactContextBaseJavaModule {

  @Override
  public String getName() {
    return NAME;
  }

  private final File updatesDir = new File(reactContext.getFilesDir(), "updates");
  private final String fileName = "updatesRegister.json";
  private final CoroutineScope coroutineScope = new CoroutineScope(Dispatchers.Main);

  final File existingFile = new File(updatesDir, fileName);

  @ReactMethod
  public void update(String url, Callback callback) {
      coroutineScope.launch(() -> {
          try {
              // Create updates directory if it doesn't exist
              if (!updatesDir.exists()) {
                  updatesDir.mkdirs();
              }

              // Clean up old files if there are more than 5 backups
              manageBackupsLimit();

              // Create the bundles directory if it doesn't exist
              File bundlesDir = new File(updatesDir, "bundles");
              if (!bundlesDir.exists()) {
                bundlesDir.mkdirs();

                File newJsonFile = downloadJsonFileSecurely(url);
                validateJson(newJsonFile);

                // Read the downloaded JSON file
                JSONObject newJsonObject = new JSONObject(newJsonFile.readText());

                // Ensure versionCode is greater than 0
                int versionCode = newJsonObject.getInt("versionCode");
                if (versionCode > 0) {
                    String newCommit = newJsonObject.getString("commit");
                    String bundleUrl = newJsonObject.getJSONObject("bundle").getString("uri");
                    File bundleFile = new File(updatesDir, "bundles/" + newCommit + ".bundle");

                    // Download the bundle
                    downloadFileSecurely(bundleUrl, bundleFile);

                    // Update the existing JSON file
                    existingFile.writeText(newJsonObject.toString());

                    // Return success callback
                    callback.invoke(null, "First update completed successfully.");
                } else {
                    // If versionCode is 0 or less, no update is performed
                    callback.invoke("VersionCode is 0 or less, no update will be performed.", null);
                }
                
                return;
            }

              File existingFile = new File(updatesDir, fileName);
              if (!existingFile.exists()) {
                  // Download the JSON file if it doesn't exist
                  File newJsonFile = downloadJsonFileSecurely(url);
                  validateJson(newJsonFile);
                  newJsonFile.copyTo(existingFile, true);
                  callback.invoke(null, "JSON file downloaded successfully.");
                  return;
              }

              // Read the existing JSON file
              JSONObject existingJsonObject = withContext(Dispatchers.IO, () -> {
                  String existingJsonString = existingFile.readText();
                  return new JSONObject(existingJsonString);
              });

              // Download the new JSON file
              File newJsonFile = downloadJsonFileSecurely(url);
              validateJson(newJsonFile);
              JSONObject newJsonObject = withContext(Dispatchers.IO, () -> {
                  String jsonString = newJsonFile.readText();
                  return new JSONObject(jsonString);
              });

              // Update version or handle fallbacks
              updateVersion(existingFile, newJsonObject, existingJsonObject, callback);

          } catch (MalformedURLException e) {
              callback.invoke("Malformed URL: " + e.getMessage(), null);
          } catch (IOException e) {
              callback.invoke("Network or file read error: " + e.getMessage(), null);
          } catch (JSONException e) {
              callback.invoke("Error parsing JSON: " + e.getMessage(), null);
          } catch (Exception e) {
              callback.invoke("Unexpected error: " + e.getMessage(), null);
          }
      });
  }

  private suspend void handleUpdateError(Exception e, Callback callback) {
    logEvent("Error during update: " + e.getMessage());

    // Attempt to restore the last available backup
    List<File> backupFiles = getBackupFiles();
    if (!backupFiles.isEmpty()) {
        File lastBackup = backupFiles.get(backupFiles.size() - 1);
        restoreBackup(lastBackup, existingFile);
        callback.invoke(null, "Error during update. Automatically reverted to the last available backup.");
    } else {
        callback.invoke("Error during update and no valid backup found to restore: " + e.getMessage(), null);
    }
  }

  @ReactMethod
  public void getCurrentJson(Callback callback) {
      coroutineScope.launch(() -> {
          try {
              File existingFile = new File(updatesDir, fileName);

              // Read the existing JSON file
              if (existingFile.exists()) {
                  String jsonString = withContext(Dispatchers.IO, () -> existingFile.readText());
                  callback.invoke(null, jsonString);
              } else {
                  callback.invoke("JSON file does not exist.", null);
              }
          } catch (Exception e) {
              callback.invoke("Error getting current JSON: " + e.getMessage(), null);
          }
      });
  }

  private List<File> getBundleFiles() {
      File bundlesDir = new File(updatesDir, "bundles");
      File[] files = bundlesDir.listFiles(file -> file.getName().endsWith(".bundle"));
      return files != null ? Arrays.stream(files).sorted(Comparator.comparing(File::lastModified)).collect(Collectors.toList()) : Collections.emptyList();
  }

  @ReactMethod
  public void getCurrentBundle(Callback callback) {
      coroutineScope.launch(() -> {
          try {
              File existingFile = new File(updatesDir, fileName);

              // Read the existing JSON file
              if (existingFile.exists()) {
                  String jsonString = withContext(Dispatchers.IO, () -> existingFile.readText());
                  JSONObject jsonObject = new JSONObject(jsonString);
                  String currentCommit = jsonObject.getString("commit");

                  // Create the path for the current bundle
                  File currentBundleFile = new File(updatesDir, "bundles/" + currentCommit + ".bundle");

                  if (currentBundleFile.exists()) {
                      // Send the current bundle file name to the callback
                      callback.invoke(null, currentBundleFile.getName());
                  } else {
                      callback.invoke("Current bundle does not exist.", null);
                  }
              } else {
                  callback.invoke("JSON file does not exist.", null);
              }
          } catch (Exception e) {
              callback.invoke("Error getting the current bundle: " + e.getMessage(), null);
          }
      });
  }

  @ReactMethod
  public void getBackupBundles(Callback callback) {
      coroutineScope.launch(() -> {
          try {
              // Get bundle files
              List<File> bundleFiles = getBundleFiles();
              Log.d("UpdateModules", "Bundles found: " + bundleFiles.size());

              // Convert the file list to a WritableArray
              WritableArray bundleList = Arguments.createArray();
              for (File file : bundleFiles) {
                  bundleList.pushString(file.getName());  // Add the bundle name
              }

              // Send the bundle list to the callback
              callback.invoke(null, bundleList);
          } catch (Exception e) {
              callback.invoke("Error getting bundles: " + e.getMessage(), null);
          }
      });
  }

  private void validateJson(File file) {
      String jsonString = file.readText();
      JSONObject jsonObject = new JSONObject(jsonString);

      if (!jsonObject.has("versionCode") || !jsonObject.has("commit") || !jsonObject.has("bundle")) {
          throw new JSONException("The JSON file is not in the correct format.");
      }
  }

  private void logEvent(String message) {
      File logFile = new File(updatesDir, "update_log.txt");
      logFile.appendText(System.currentTimeMillis() + ": " + message + "\n");
  }

  @ReactMethod
  public void deleteBundle(String commit, Callback callback) {
      coroutineScope.launch(() -> {
          try {
              // Bundles directory
              File bundlesDir = new File(updatesDir, "bundles");
              // Create the path for the bundle file to delete
              File bundleFile = new File(bundlesDir, commit + ".bundle");

              // Verify if the file exists
              if (bundleFile.exists()) {
                  // Try to delete the file
                  if (bundleFile.delete()) {
                      logEvent("Deleted bundle: " + bundleFile.getName());
                      callback.invoke(null, "Bundle successfully deleted: " + bundleFile.getName());
                  } else {
                      callback.invoke("Error deleting bundle: " + bundleFile.getName(), null);
                  }
              } else {
                  callback.invoke("Bundle with commit " + commit + " does not exist.", null);
              }
          } catch (Exception e) {
              callback.invoke("Error deleting bundle: " + e.getMessage(), null);
          }
      });
  }

  @ReactMethod
  public void clearAllBundles(Callback callback) {
      coroutineScope.launch(() -> {
          try {
              // Bundles directory
              File bundlesDir = new File(updatesDir, "bundles");

              // Check if the bundles directory exists
              if (bundlesDir.exists()) {
                  // Get all files in the bundles directory
                  File[] bundleFiles = bundlesDir.listFiles();

                  // Delete all bundle files
                  if (bundleFiles != null && bundleFiles.length > 0) {
                      for (File file : bundleFiles) {
                          if (file.delete()) {
                              logEvent("Deleted bundle: " + file.getName());
                          } else {
                              logEvent("Error deleting bundle: " + file.getName());
                          }
                      }
                  }

                  // Optionally: Delete the bundles directory if it's empty
                  if (bundlesDir.listFiles().length == 0) {
                      bundlesDir.delete();
                      logEvent("Deleted bundles directory because it was empty.");
                  }

                  callback.invoke(null, "All bundles have been successfully deleted.");
              } else {
                  callback.invoke("Bundles directory does not exist.", null);
              }
          } catch (Exception e) {
              callback.invoke("Error clearing bundles: " + e.getMessage(), null);
          }
      });
  }

  private suspend void updateBundle(
      JSONObject newJsonObject,
      File existingFile,
      String newCommit,
      Callback callback
  ) {
      String oldCommit = existingFile.exists() ? new JSONObject(existingFile.readText()).getString("commit") : "";

      backupExistingFile(existingFile, oldCommit);

      String bundleUrl = newJsonObject.getJSONObject("bundle").getString("uri");
      File bundleFile = new File(updatesDir, "bundles/" + newCommit + ".bundle");

      // Check if a bundle with the same commit already exists
      if (bundleFile.exists()) {
          logEvent("A bundle with commit " + newCommit + " already exists, it will be replaced.");
      } else {
          logEvent("No bundle found with commit " + newCommit + ", a new one will be downloaded.");
      }

      // Check if there is enough space before downloading
      if (isEnoughSpace(bundleFile.length())) {
          try {
              downloadFileSecurely(bundleUrl, bundleFile); // Download or replace the bundle
              existingFile.writeText(newJsonObject.toString()); // Update the existing JSON file
              callback.invoke(null, "Update successful: new bundle downloaded or replaced.");
          } catch (IOException e) {
              logEvent("Error downloading the bundle: " + e.getMessage());
              // Automatic fallback if an error occurs
              if (newJsonObject.getBoolean("fallback")) {
                  handleAutomaticFallback(existingFile, callback);
              } else {
                  callback.invoke("Error downloading the bundle: " + e.getMessage(), null);
              }
          }
      } else {
          restoreBackup(new File(updatesDir, oldCommit + "-backup.json"), existingFile);
          callback.invoke("Error: Not enough space to download the new bundle.", null);
      }
  }

  private suspend void updateVersion(
      File existingFile,
      JSONObject newJsonObject,
      JSONObject existingJsonObject,
      Callback callback
  ) {
      int newVersionCode = newJsonObject.getInt("versionCode");
      int existingVersionCode = existingJsonObject.getInt("versionCode");
      String newCommit = newJsonObject.getString("commit");

      // Get the currently active commit
      String currentCommit = existingJsonObject.getString("commit");

      // 1. Manual fallback handling if enabled
      if (newJsonObject.getJSONObject("fallbackDetails").getBoolean("enable")) {
          logEvent("Manual fallback requested.");
          handleManualFallback(newJsonObject, existingFile, callback);
          return;
      }

      // 2. If the current versionCode is lower, update (even if folders are already created)
      if (newVersionCode > existingVersionCode) {
          logEvent("A new version detected: " + newVersionCode + " > " + existingVersionCode + ". Proceeding with update.");

          // Check if the new commit is different from the current one
          if (!newCommit.equals(currentCommit)) {
              // Backup the existing commit only if it's different
              backupExistingFile(existingFile, currentCommit);
              updateBundle(newJsonObject, existingFile, newCommit, callback);
          } else {
              callback.invoke("The current and new commits are identical. No update will be performed.", null);
          }
      } else {
         // callback.invoke("The new versionCode is not greater than the current one.", null);
      }
  }

  // Manual fallback to update the active commit with the fallback one
  private suspend void handleManualFallback(JSONObject newJsonObject, File existingFile, Callback callback) {
      JSONObject fallbackDetails = newJsonObject.getJSONObject("fallbackDetails");
      String fallbackCommit = fallbackDetails.getString("commit");

      // Check if the fallback is enabled and the commit has not yet been applied
      if (fallbackDetails.getBoolean("enable") && !isFallbackAlreadyApplied(fallbackCommit)) {
          // Read the current JSON file (the file where the active commit is located)
          JSONObject existingJson = new JSONObject(existingFile.readText());

          // Get the current commit and replace it with the fallback one
          String currentCommit = existingJson.getString("commit");
          existingJson.put("commit", fallbackCommit);

          // Write the JSON file back with the updated commit
          existingFile.writeText(existingJson.toString());

          logEvent("Commit replaced: " + currentCommit + " with the fallback commit: " + fallbackCommit);

          // Mark that the fallback has already been applied
          markFallbackAsApplied(fallbackCommit);

          callback.invoke(null, "The fallback commit was successfully replaced.");
      } else {
          logEvent("The fallback has already been applied or is not enabled.");
          callback.invoke(null, "The fallback has already been applied or is not enabled.");
      }
  }

  // Function to check if the fallback commit has already been applied
  private boolean isFallbackAlreadyApplied(String fallbackCommit) {
      File appliedFallbackFile = new File(updatesDir, "applied_fallbacks.txt");
      return appliedFallbackFile.exists() && appliedFallbackFile.readText().contains(fallbackCommit);
  }

  // Function to mark that the fallback commit has been applied
  private void markFallbackAsApplied(String fallbackCommit) {
      File appliedFallbackFile = new File(updatesDir, "applied_fallbacks.txt");
      appliedFallbackFile.writeText(fallbackCommit);
  }

  // Automatic fallback in case of error
  private suspend void handleAutomaticFallback(File existingFile, Callback callback) {
      List<File> backupFiles = getBackupFiles();
      if (!backupFiles.isEmpty()) {
          File lastBackup = backupFiles.get(backupFiles.size() - 1);
          restoreBackup(lastBackup, existingFile);
          callback.invoke(null, "Automatically reverted to the last available backup.");
      } else {
          callback.invoke("No valid backup found for automatic fallback.", null);
      }
  }

  // Manage a maximum of 5 backups and delete the oldest one
  private void manageBackupsLimit() {
      List<File> backupFiles = getBackupFiles();
      if (backupFiles.size() > 5) {
          File oldestBackup = backupFiles.get(0);
          oldestBackup.delete();
          logEvent("Deleted oldest backup: " + oldestBackup.getName());
      }
  }

  // Get a list of backups sorted by modification date
  private List<File> getBackupFiles() {
      File[] files = updatesDir.listFiles(file -> file.getName().endsWith("-backup.json"));
      return files != null ? Arrays.stream(files).sorted(Comparator.comparing(File::lastModified)).collect(Collectors.toList()) : Collections.emptyList();
  }

  private void backupExistingFile(File existingFile, String oldCommit) {
      File backupFile = new File(updatesDir, oldCommit + "-backup.json");
      if (!backupFile.exists()) {
          existingFile.copyTo(backupFile, true);
          logEvent("Backup of the existing file created: " + backupFile.getAbsolutePath());
      } else {
          logEvent("The backup already exists, no new one is required.");
      }
  }

  private boolean isEnoughSpace(long requiredSpace) {
      StatFs stat = new StatFs(updatesDir.getAbsolutePath());
      long availableBytes = stat.getAvailableBytes();
      return availableBytes >= requiredSpace;
  }

  private void restoreBackup(File backupFile, File existingFile) {
      if (backupFile.exists()) {
          backupFile.copyTo(existingFile, true);
          logEvent("Restored the file from backup: " + existingFile.getAbsolutePath());
      } else {
          logEvent("No backup found to restore.");
      }
  }

  private suspend File downloadJsonFileSecurely(String url) throws IOException {
      return withContext(Dispatchers.IO, () -> {
          File tempFile = File.createTempFile("tempBundle", ".json", updatesDir);
          HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
          connection.connect();

          if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
              throw new IOException("Error downloading JSON file: " + connection.getResponseMessage());
          }

          try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(tempFile)) {
              byte[] buffer = new byte[4096];
              int bytesRead;
              while ((bytesRead = input.read(buffer)) != -1) {
                  output.write(buffer, 0, bytesRead);
              }
          }
          return tempFile;
      });
  }

  private suspend void downloadFileSecurely(String url, File destinationFile) throws IOException {
      withContext(Dispatchers.IO, () -> {
          HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
          connection.connect();

          if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
              throw new IOException("Error downloading file: " + connection.getResponseMessage());
          }

          try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(destinationFile)) {
              byte[] buffer = new byte[4096];
              int bytesRead;
              while ((bytesRead = input.read(buffer)) != -1) {
                  output.write(buffer, 0, bytesRead);
              }
          }
      });
  }

  public static final String NAME = "RemoteUpdate";
}
