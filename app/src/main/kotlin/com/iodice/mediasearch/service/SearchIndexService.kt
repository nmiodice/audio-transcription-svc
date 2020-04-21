package com.iodice.mediasearch.service

import com.iodice.mediasearch.client.SearchIndexClient
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.inject.Inject

@Component
class SearchIndexService(
        @Inject var searchIndexClient: SearchIndexClient,
        @Inject val sourceRepo: EntityRepository<SourceDocument>,
        @Inject val mediaRepo: EntityRepository<MediaDocument>
) {
    companion object {
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
        val sourceMap = indices
                .values
                .flatMap { ids -> ids.map { index -> index.sourceId } }
                .toSet()
                .map { Callable { sourceRepo.get(it, it).data } }
                .let { executor.invokeAll(it.toList()) }
                .map { it.get() }
                .map { it.id!! to it }
                .toMap()

        val mediaMap = indices
                .values
                .flatMap { ids -> ids.map { index -> index.mediaId to index.sourceId } }
                .distinctBy { it.first }
                .map { Callable { mediaRepo.get(it.first, it.second).data } }
                .let { executor.invokeAll(it.toList()) }
                .map { it.get() }
                .map { it.id!! to it }
                .toMap()

        return indices
                .map {
                    it.key to AnnotatedIndices(
                            indices = it.value,
                            source = sourceMap[it.value[0].sourceId] ?: error("Impossible Case to Hit!"),
                            media = mediaMap[it.value[0].mediaId] ?: error("Impossible Case to Hit!")
                    )
                }
                .toMap()
                .let { AggregatedQueryResponse(it) }
    }
}