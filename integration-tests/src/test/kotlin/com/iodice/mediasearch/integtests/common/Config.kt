package com.iodice.mediasearch.integtests.common

fun getStringEnv(key: String) = requireNotNull(System.getenv(key)) {
    "Missing environment variable for `$key` property!"
}

class Config {
    companion object {
        val SERVICE_ENDPOINT: String
            get() = "${getStringEnv("SERVICE_ENDPOINT")}/api/v1"
        val SERVICE_ENDPOINT_MEDIACONFIG: String
            get() = "$SERVICE_ENDPOINT/source/"
    }
}