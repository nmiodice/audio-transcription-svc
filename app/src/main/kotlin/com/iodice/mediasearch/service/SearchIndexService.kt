package com.iodice.mediasearch.service

import com.iodice.mediasearch.METRICS
import com.iodice.mediasearch.client.SearchIndexClient
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.inject.Inject

@Component
class SearchIndexService(
        @Inject var searchIndexClient: SearchIndexClient,
        @Inject val sourceRepo: EntityRepository<SourceDocument>,
        @Inject val mediaRepo: EntityRepository<MediaDocument>,
        @Inject val metricsClient: TelemetryClient
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
        private val executor = Executors.newFixedThreadPool(25)
    }

    fun queryAggregatedByMedia(query: String): AggregatedQueryResponse {
        val indicesGroupedByMedia = getIndicesGroupedByMedia(query)
        return annotateIndicesWithSource(indicesGroupedByMedia)
    }

    fun getIndicesGroupedByMedia(query: String): Map<String, List<Index>> = searchIndexClient.query(query)
            .indices
            .groupBy { String(Base64.getDecoder().decode(it.mediaId)) }

    fun annotateIndicesWithSource(indices: Map<String, List<Index>>): AggregatedQueryResponse {
        val sourcesFuture = indices
                .values
                .flatMap { ids -> ids.map { index -> index.sourceId } }
                .toSet()
                .let { Callable { sourceRepo.getAll(it) } }
                .let { executor.submit(it) }

        val mediaFuture = indices
                .values
                .flatMap { ids -> ids.map { index -> index.mediaId } }
                .toSet()
                .let { Callable { mediaRepo.getAll(it) } }
                .let { executor.submit(it) }

        val sourceMap = mutableMapOf<String, SourceDocument>()
        sourcesFuture.get().forEachRemaining {
            sourceMap[it.id!!] = it
        }

        val mediaMap = mutableMapOf<String, MediaDocument>()
        mediaFuture.get().forEachRemaining {
            mediaMap[it.id!!] = it
        }

        return indices
                .filter {
                    val isMediaMissing = !mediaMap.containsKey(it.value[0].mediaId)
                    val isSourceMissing = !sourceMap.containsKey(it.value[0].sourceId)

                    metricsClient.trackMetric(METRICS.INDEX_DANGLING_SOURCE, if (isSourceMissing) 1.0 else 0.0)
                    metricsClient.trackMetric(METRICS.INDEX_DANGLING_MEDIA, if (isMediaMissing) 1.0 else 0.0)

                    when {
                        isSourceMissing || isMediaMissing -> {
                            logger.warn("index for ${it.key} refers source ${it.value[0].sourceId} and media ${it.value[0].mediaId}, one or both of which are missing!")
                            false
                        }
                        else -> {
                            true
                        }
                    }
                }
                .map {
                    it.key to AnnotatedIndices(
                            indices = it.value,
                            source = sourceMap[it.value[0].sourceId]?.data ?: error("Impossible Case to Hit!"),
                            media = mediaMap[it.value[0].mediaId]?.data ?: error("Impossible Case to Hit!")
                    )
                }
                .toMap()
                .let { AggregatedQueryResponse(it) }
    }
}