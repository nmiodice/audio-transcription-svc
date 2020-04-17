package com.iodice.mediasearch.client

import com.iodice.mediasearch.di.Beans
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.util.log
import com.iodice.mediasearch.util.throwIfStatusIsNot
import kong.unirest.UnirestInstance
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
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
                IndexOperation(
                        action = Action.MERGE_OR_UPLOAD,
                        id = "${indexStatusDocument.mediaId}_$index",
                        sourceId = indexStatusDocument.sourceId,
                        mediaId = indexStatusDocument.mediaId,
                        offset = segment.OffsetInSeconds,
                        duration = segment.DurationInSeconds,
                        content = segment.NBest[0].Display
                )
            }.let {
               SearchIndexRequest(it)
            }

    fun query(query: String): SearchIndexResponse {
        val url = "$searchApiEndpoint/indexes/$searchApiIndex/docs?api-version=2019-05-06&search=$query&\$orderby=search.score() desc"
        return restClient.get(url)
                .header("content-type", "application/json")
                .header("api-key", searchApiKey)
                .asObject(SearchIndexResponse::class.java)
                .log(logger)
                .body
    }
}