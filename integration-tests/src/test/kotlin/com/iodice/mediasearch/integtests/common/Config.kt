package com.iodice.mediasearch.integtests.common

fun getStringEnv(key: String) = requireNotNull(System.getenv(key)) {
    "Missing environment variable for `$key` property!"
}

class Config {
    companion object {
        val SERVICE_ENDPOINT_BASE: String
            get() = "${getStringEnv("SERVICE_ENDPOINT")}/api/v1"

        const val ROUTE_PART_SOURCE = "source/"
        const val ROUTE_PART_INDEX_RESULT = "indexresult/"
        const val ROUTE_PART_MEDIA = "media/"
    }
}