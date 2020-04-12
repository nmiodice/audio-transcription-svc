package com.iodice.mediasearch.client

import com.iodice.mediasearch.di.Beans
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.util.log
import com.iodice.mediasearch.util.throwIfStatusIsNot
import kong.unirest.HttpResponse
import kong.unirest.UnirestInstance
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Named


@Component
class STTClient(
        @Inject private val restClient: UnirestInstance,
        @Inject @Named(Beans.STT_API_KEY) private val sttApiKey: String,
        @Inject @Named(Beans.STT_API_ENDPOINT) private val sttApiEndpoint: String
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    fun submitAsync(audioUrl: String, name: String, locale: String = "en-US"): String {
        val request = TranscriptionDefinition(
                recordingsUrl = audioUrl,
                name = name,
                locale = locale)
        val response = restClient.post(sttApiEndpoint)
                .header("content-type", "application/json")
                .header("Ocp-Apim-Subscription-Key", sttApiKey)
                .body(request)
                .asJson()
                .log(logger)
                .throwIfStatusIsNot(
                    HttpURLConnection.HTTP_OK,
                    HttpURLConnection.HTTP_ACCEPTED,
                    HttpURLConnection.HTTP_CREATED)
        return response.headers.getFirst("location")
    }

    fun checkStatus(callbackUrl: String): STTStatus {
        val callbackResponse = restClient.get(callbackUrl)
                .header("content-type", "application/json")
                .header("Ocp-Apim-Subscription-Key", sttApiKey)
                .asObject(TranscriptionReference::class.java)
                .log(logger)
                .throwIfStatusIsNot(HttpURLConnection.HTTP_OK)

        val transcription = callbackResponse.body
        return when (transcription.status) {
            TranscriptionStatus.RUNNING, TranscriptionStatus.NOT_STARTED -> STTInProgress()
            TranscriptionStatus.FAILED -> STTFailed(transcription.statusMessage!!)
            TranscriptionStatus.SUCCEEDED -> STTSuccess(transcription.resultsUrls!!.values.toList())
            else -> throw STTException("Unknown STT status: $transcription")
        }
    }

}