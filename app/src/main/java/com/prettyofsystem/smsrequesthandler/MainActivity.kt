package com.prettyofsystem.smsrequesthandler
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private var firebaseAdmin: FirebaseAdmin = FirebaseAdmin()
    private var smsManager: SMSManager = SMSManager()
    private var refreshTimeInMillis = 180000
    private val requestPermissionCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        checkPermissionsAndStart()
    }

    //Message Logic
    private fun startSMSService() {
        refreshTimeInMillis = 180000
        startRefreshCountdown()
        listenToSMSRequests()
    }

    private fun startRefreshCountdown() {
        GlobalScope.launch(Dispatchers.IO) { // Launch coroutine in background
            var remainingTime = refreshTimeInMillis
            while (remainingTime > 0) {
                val statusTitle: TextView = findViewById(R.id.refreshDateTime)
                val refreshTimeText = "Refresh after: ${remainingTime / 1000} seconds"
                runOnUiThread { // Update UI on main thread
                    statusTitle.text = refreshTimeText
                }
                remainingTime -= 1000 // Decrement by 1 second
                delay(1000) // Wait for 1 second
            }

            // After countdown completes, refresh the app (optional)
            runOnUiThread {
                val packageManager = packageManager
                val intent = packageManager.getLaunchIntentForPackage(packageName)!!
                val componentName = intent.component!!
                val restartIntent = Intent.makeRestartActivityTask(componentName)
                startActivity(restartIntent)
                Runtime.getRuntime().exit(0)
            }
        }
    }

    private fun listenToSMSRequests() {
        GlobalScope.launch(Dispatchers.IO) {
            firebaseAdmin.startListeningForSMSRequests { request ->
                val isSuccess = smsManager.send(request)
                val messageStatus = if (isSuccess) SMSRequestStatusEnum.Sent else SMSRequestStatusEnum.Error
                updateLastMessageSent(request, messageStatus)
                firebaseAdmin.updateRequestStatus(request.id, messageStatus) {
                    listenToSMSRequests()
                }
            }
        }
    }

    private fun updateLastMessageSent(request: SMSRequest, messageStatus: SMSRequestStatusEnum){
        val statusTitle: TextView = findViewById(R.id.statusTitle)
        val previousMessage = "requestedDateTime: " + request.requestedDateTime +
                "\nShop Timezone: " + request.shopTimezone +
                "\nEvent Type: " + request.type +
                "\nStatus: " +  messageStatus.toString() +
                "\n To: " + request.to.joinToString(separator = " ", prefix = "", postfix = "") +
                "\nMessage: " + request.message

        statusTitle.text = previousMessage
    }

    //Permission
    private fun checkPermissionsAndStart() {
        val permissionsToCheck = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToCheck.add(Manifest.permission.SEND_SMS)
        }

        if (permissionsToCheck.isEmpty()) {
            startSMSService() // Assuming you have a function to start SMS service
            checkForBatteryOptimization()
            startRefreshCountdown() // Start refresh countdown
        } else {
            val permissionsToRequest = permissionsToCheck.toTypedArray()
            ActivityCompat.requestPermissions(this, permissionsToRequest, requestPermissionCode) // Use a common request code
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermissionCode) {
            val grantedPermissions = mutableListOf<String>()
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    grantedPermissions.add(permissions[i])
                }
            }
            if (grantedPermissions.contains(Manifest.permission.SEND_SMS)) {
                startSMSService()
            }
            if (grantedPermissions.isEmpty()) {
                displayPermissionRequiredDialog()
            } else {
                checkForBatteryOptimization()
            }
        }
    }

    private fun displayPermissionRequiredDialog() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app requires certain permissions to function properly.")

        builder.setPositiveButton("OK") { _, _ ->
            checkPermissionsAndStart() // Retry permission request
        }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog
            }
            .create()
            .show()
    }

    private fun checkForBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
    private fun allRequiredPermissionsGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }


}


