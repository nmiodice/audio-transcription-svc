package com.iodice.mediasearch.client

import com.google.gson.Gson
import com.iodice.mediasearch.di.Beans
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.util.log
import com.iodice.mediasearch.util.throwIfStatusIsNot
import kong.unirest.UnirestInstance
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Named


@Component
class SearchIndexClient(
        @Inject private val restClient: UnirestInstance,
        @Inject @Named(Beans.SEARCH_API_KEY) private val searchApiKey: String,
        @Inject @Named(Beans.SEARCH_API_ENDPOINT) private val searchApiEndpoint: String,
        @Inject @Named(Beans.SEARCH_API_INDEX) private val searchApiIndex: String
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    fun index(indexStatusDocument: IndexStatusDocument, results: TranscriptionResult) {
        val request = transcriptionToIndexRequest(indexStatusDocument, results)

        restClient.post("$searchApiEndpoint/indexes/$searchApiIndex/docs/index?api-version=2019-05-06")
                .header("content-type", "application/json")
                .header("api-key", searchApiKey)
                .body(request)
                .asJson()
                .log(logger)
                .throwIfStatusIsNot(HttpURLConnection.HTTP_OK)
    }

    fun transcriptionToIndexRequest(indexStatusDocument: IndexStatusDocument, results: TranscriptionResult) = results.AudioFileResults
            .flatMap {
                it.SegmentResults
            }.mapIndexed { index, segment ->
                Index(
                        action = Action.MERGE_OR_UPLOAD,
                        sourceId = indexStatusDocument.sourceId,
                        mediaId = "${indexStatusDocument.mediaId}_$index",
                        offset = segment.OffsetInSeconds,
                        duration = segment.DurationInSeconds,
                        content = segment.NBest[0].Display
                )
            }.let {
               SearchIndexRequest(it)
            }
}