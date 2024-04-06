package com.prettyofsystem.smsrequesthandler
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var firebaseAdmin: FirebaseAdmin = FirebaseAdmin()
    private var smsManager: SMSManager = SMSManager()
    private val smsPermissionRequestCode = 101 // Unique request code for SMS permission


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), smsPermissionRequestCode)
        } else {
            startSMSService() // Start the service if permissions are already granted
            checkForBatteryOptimization() // Check battery optimization settings
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == smsPermissionRequestCode && allRequiredPermissionsGranted(grantResults)) {
            startSMSService() // Start the service when permissions are granted
            checkForBatteryOptimization() // Also, check for battery optimization settings
        } else {
            displaySMSRequired() // Show rationale if permission is denied
        }
    }

    private fun allRequiredPermissionsGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun startSMSService() {
        listenToSMSRequests()
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

    private fun displaySMSRequired() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Sending SMS requires this permission. Please allow it in the next prompt for the app to function correctly.")
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), smsPermissionRequestCode)
            }
            .setNegativeButton("Cancel") {_,_ -> deepFinish()}
            .create()
            .show()
    }

    private fun deepFinish() {
        val cacheDir = cacheDir
        if (cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }

        finishAffinity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        }
        android.os.Process.killProcess(android.os.Process.myPid())
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
        val previousMessage = "Shop name: " + request.shopName +
                "\nShop Address:" + request.shopAddress +
                "\nShop Phone: " + request.shopPhoneNumber +
                "\nShop Date Time: " + request.shopDateTime +
                "\nShop Timezone: " + request.shopTimezone +
                "\nStatus: " +  messageStatus.toString() +
                "\n To: " + request.to.joinToString(separator = " ", prefix = "", postfix = "") +
                "\nMessage: " + request.message

        statusTitle.text = previousMessage
    }
}


