package com.iodice.mediasearch.service

import com.iodice.mediasearch.METRICS
import com.iodice.mediasearch.model.IndexState
import com.iodice.mediasearch.model.IndexStatus
import com.iodice.mediasearch.model.IndexStatusDocument
import com.iodice.mediasearch.model.MediaDocument
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.util.stream
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.stream.Collectors
import javax.inject.Inject

@Component
class ConsistencyService(
        @Inject private val mediaRepo: EntityRepository<MediaDocument>,
        @Inject private val indexRepo: EntityRepository<IndexStatusDocument>,
        @Inject private val metricsClient: TelemetryClient
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @Scheduled(fixedDelayString = "\${service.consistency.refresh.delay_millis}", initialDelay = 0)
    fun backFillMissingIndices() {
        try {
            doIndexBackFill()
        } catch (e: Exception) {
            logger.error("error back-filling missing indicess: $e")
        }
    }

    fun doIndexBackFill() {
        val mediaIDsWithIndex = indexRepo.getAll()
                .stream()
                .map { it.mediaId }
                .collect(Collectors.toSet())

        val mediaSourceMap = mediaRepo.getAll()
                .stream()
                .map { it.id!! to it.sourceId }
                .collect(Collectors.toSet())


        var missingIndices = 0
        mediaSourceMap.forEach { (mediaId, sourceId) ->
            if (mediaIDsWithIndex.contains(mediaId)) {
                return
            }
            missingIndices++
            logger.info("Found missing index for media $mediaId (source $sourceId)")
            val mediaDoc = mediaRepo.get(mediaId, sourceId)
            indexRepo.put(IndexStatusDocument(
                    sourceId = sourceId,
                    mediaId = mediaId,
                    mediaUrl = mediaDoc.data.url,
                    data = IndexStatus(state = IndexState.NOT_STARTED)
            ))
        }

        metricsClient.trackMetric(METRICS.MISSING_INDEX_IDENTIFIED, missingIndices.toDouble())
    }
}
