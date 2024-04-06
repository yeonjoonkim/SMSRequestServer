package com.prettyofsystem.smsrequesthandler
import android.telephony.SmsManager
import android.telephony.SubscriptionManager

class SMSManager {
    fun send(request: SMSRequest): Boolean {
        if (request.to.isEmpty()) return false
        val messagePrefix = if (request.shopId.isNotEmpty()) "" else "[Pretty Of System]\n\n"

        return sendMessage(request.to, messagePrefix, request.message)
    }

    private fun sendMessage(recipients: List<String>, prefix: String, message: String): Boolean {
        val smsManager = getSMSManager()
        val fullMessage = if(prefix.isNotEmpty()) "$prefix$message" else message

        return try {
            recipients.forEach { phoneNumber ->
                val parts = smsManager.divideMessage(fullMessage)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getSMSManager(): SmsManager{
        val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subscriptionId = SubscriptionManager.getDefaultSubscriptionId()
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getDefault()
        }
        return smsManager
    }
}
