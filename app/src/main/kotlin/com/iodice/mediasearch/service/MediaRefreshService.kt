package com.iodice.mediasearch.service

import com.iodice.mediasearch.adapter.SourceListAdapterFactory
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.util.stream
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.inject.Inject
import javax.inject.Named

@Component
class MediaRefreshService(
        @Inject private val sourceRepo: EntityRepository<SourceDocument>,
        @Inject private val mediaRepo: EntityRepository<MediaDocument>,
        @Inject private val indexRepo: EntityRepository<IndexStatusDocument>,
        @Inject private val sourceListAdapterFactory: SourceListAdapterFactory,
        @Inject @Named("NEW_DOCUMENT_LIMIT") private val newDocumentLimit: Int
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @Scheduled(fixedDelayString = "\${service.media.refresh.delay_millis}", initialDelay = 0)
    fun refreshSources() {
        logger.info("Begin: refreshing source media")
        sourceRepo.getAll()
                .stream()
                .forEach { refreshSource(it.data) }
        logger.info("Finished: refreshing source media")
    }

    fun refreshSource(source: Source) {
        val sourceAdapter = sourceListAdapterFactory.forSource(source)
        var newDocCount = 0
        sourceAdapter.queryForMedia(source)
                .forEach {
                    val isNew = saveIfNew(MediaDocument(it.id, it, source.id!!))
                    if (isNew) {
                        newDocCount++
                    }

                    if (!isNew && source.trackListIsSorted) {
                        logger.info("Exiting source refresh for ${source.id} due to track sorted condition")
                        return
                    }

                    if (newDocCount >= newDocumentLimit) {
                        logger.info("Exiting source refresh for ${source.id} due to new document exceeded condition ($newDocCount documents were saved)")
                        return
                    }
                }
    }

    /**
     * Returns true if the document was saved
     */
    private fun saveIfNew(doc: MediaDocument): Boolean {
        if (mediaRepo.exists(doc.id!!, doc.sourceId)) {
            return false
        }
        logger.info("Storing new media document for source ${doc.sourceId} with ID ${doc.id} and URL ${doc.data.url}")
        mediaRepo.put(doc)
        indexRepo.put(IndexStatusDocument(
                sourceId = doc.sourceId,
                mediaId = doc.id!!,
                mediaUrl = doc.data.url,
                data = IndexStatus(state = IndexState.NOT_STARTED)
        ))
        return true
    }
}