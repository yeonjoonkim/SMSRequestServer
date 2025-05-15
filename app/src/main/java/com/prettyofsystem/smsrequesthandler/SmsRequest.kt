package com.prettyofsystem.smsrequesthandler

data class SMSRequest(
    var id: String = "",
    var to: List<String> = emptyList(),
    var message: String = "",
    var type: String = "",
    var shopId: String = "",
    var shopTimezone: String = "",
    var requestedDateTime: String = "",
    var status: String = "",
    var createdShopISODateTime: String = "",
    var createdServerISODateTime: String = "",
    var createdShopTimestamp: Number = 0,
    var createdServerTimestamp: Number = 0,
    var errorName: String? = null,
    var errorMsg: String? = null,
)