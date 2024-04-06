package com.prettyofsystem.smsrequesthandler

data class SMSRequest(
    var id: String = "",
    var shopId: String = "",
    var shopPhoneNumber: String = "",
    var shopDateTime: String = "",
    var shopTimezone: String = "",
    var shopAddress: String = "",
    var shopName: String = "",
    var status: String = "", // Adjust the type if necessary
    var to: List<String> = emptyList(),
    var message: String = ""
)