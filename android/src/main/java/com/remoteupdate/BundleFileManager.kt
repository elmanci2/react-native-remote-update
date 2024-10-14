package com.remoteupdate
import android.content.Context
import org.json.JSONObject
import java.io.File

class BundleFileManager {
    
    companion object {
        fun getJSBundleFile(context: Context?): String? {
            val updatesDir = File(context?.filesDir, "updates/bundles")
            val existingFile = File(updatesDir.parent, "updatesRegister.json") 

            return if (existingFile.exists()) {
                try {
                    val jsonString = existingFile.readText()
                    val jsonObject = JSONObject(jsonString)
                    val currentCommit = jsonObject.getString("commit")
                    val antepenultimateCommit = jsonObject.optString("antepenultimate_commit") 
                    var bundleFile = File(updatesDir, "$currentCommit.bundle")
                    
                    if (bundleFile.exists()) {
                        return bundleFile.absolutePath 
                    }

                    bundleFile = File(updatesDir, "$antepenultimateCommit.bundle")
                    if (bundleFile.exists()) {
                        return bundleFile.absolutePath 
                    }

                    "assets://index.android.bundle" 
                } catch (e: Exception) {
                    e.printStackTrace()
                    "assets://index.android.bundle" 
                }
            } else {
                "assets://index.android.bundle" 
            }
        }
    }
}
