package com.driver.portal.network

data class IssueRequest(
    val action: String = "issue",
    val driverName: String,
    val docNumber: String,
    val issueType: String,
    val note: String
)