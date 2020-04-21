package com.iodice.mediasearch.util

import com.microsoft.applicationinsights.TelemetryClient
import kong.unirest.HttpResponse
import org.slf4j.Logger
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.system.measureTimeMillis

fun <T> Iterator<T>.stream(): Stream<T> {
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(this, 0),
            false)
}

fun <T> HttpResponse<T>.throwIfStatusIsNot(vararg statuses: Int): HttpResponse<T> {
    val status = this.status
    val body = this.body
    return when {
        !statuses.contains(status) -> throw Exception("API call failed with response $status: $body")
        else -> this
    }
}

fun <T> HttpResponse<T>.log(logger: Logger): HttpResponse<T> {
    val status = this.status
    logger.info("HTTP response was $status ${this.statusText}")
    return this
}

fun <T> TelemetryClient.trackDuration(metricName: String, action: () -> T): T {
    var response: T? = null
    val duration = measureTimeMillis { response = action() }
    trackMetric(metricName, duration.toDouble())
    return response!!
}

