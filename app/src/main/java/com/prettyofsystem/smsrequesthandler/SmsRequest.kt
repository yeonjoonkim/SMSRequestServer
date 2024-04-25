package com.prettyofsystem.smsrequesthandler

data class SMSRequest(
    var id: String = "",
    var to: List<String> = emptyList(),
    var message: String = "",
    var eventType: String = "",
    var shopId: String = "",
    var shopTimezone: String = "",
    var requestedDateTime: String = "",
    var status: String = ""

)