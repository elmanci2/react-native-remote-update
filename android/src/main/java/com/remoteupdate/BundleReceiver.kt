package com.remoteupdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.io.File

class BundleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (action == "com.remoteupdate.ACTION_CHECK_BUNDLE") {
                val bundleFilePath = getJSBundleFile(context)
            }
        }
    }

    private fun getJSBundleFile(context: Context?): String? {
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

                return "assets://index.android.bundle" 
            } catch (e: Exception) {
                e.printStackTrace()
                return "assets://index.android.bundle" 
            }
        } else {
            return "assets://index.android.bundle" 
        }
    }
}
