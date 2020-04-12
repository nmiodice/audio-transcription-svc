package com.iodice.mediasearch.util

import com.iodice.mediasearch.model.STTException
import kong.unirest.HttpResponse
import kong.unirest.UnirestInstance
import org.slf4j.Logger
import java.lang.Exception
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

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

