package com.prettyofsystem.smsrequesthandler
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAdmin {
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    fun startListeningForSMSRequests(onRequestFetched: (SMSRequest) -> Unit) {
        listenerRegistration = db.collection("smsRequest")
            .whereEqualTo("status", SMSRequestStatusEnum.Pending)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                val request = snapshots.documents.first().toObject(SMSRequest::class.java)
                request?.let { onRequestFetched(it) }
                stopListening()
            }
    }


    fun updateRequestStatus(id: String, status: SMSRequestStatusEnum, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("smsRequest").document(id)
                    .update("status", status).await()
                Log.d("FirebaseAdmin", "DocumentSnapshot successfully updated!")
                callback(true) // Successfully updated
            } catch (e: Exception) {
                Log.w("FirebaseAdmin", "Error updating document", e)
                callback(false) // Failed to update
            }
        }
    }

    private fun stopListening() {
        listenerRegistration?.remove()
    }
}