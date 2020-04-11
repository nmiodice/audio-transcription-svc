package com.iodice.mediasearch.service

import com.azure.storage.blob.BlobContainerClient
import com.iodice.mediasearch.di.Beans
import com.iodice.mediasearch.model.IndexState
import com.iodice.mediasearch.model.IndexStatusDocument
import com.iodice.mediasearch.model.SourceDocument
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.util.stream
import kong.unirest.UnirestInstance
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Named

@Component
class IndexingService(
        @Inject private val sourceRepo: EntityRepository<SourceDocument>,
        @Inject private val indexRepo: EntityRepository<IndexStatusDocument>,
        @Inject private val restClient: UnirestInstance,
        @Inject @Named(Beans.RAW_MEDIA_CONTAINER) private val blobClient: BlobContainerClient
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        private val executor = Executors.newFixedThreadPool(6)
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun uploadSourcesTask() {
        applyToIndexesInState("upload media", IndexState.NOT_STARTED, IndexState.CONTENT_UPLOADED, ::uploadMedia)
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun submitSpeechToTextTask() {
        applyToIndexesInState("submit speech to text", IndexState.CONTENT_UPLOADED, IndexState.CONTENT_UPLOADED, ::submitSpeechToText)
    }

    private fun <T> applyToIndexesInState(
            description: String,
            startingState: IndexState,
            endingState: IndexState,
            action: (SourceDocument, IndexStatusDocument) -> T) {
        logger.info("Begin: applying $description to entities of type ${IndexStatusDocument::class.java} in state $startingState")
        val asyncTasks = sourceRepo.getAll()
                .stream()
                .parallel()
                .flatMap { source ->
                    indexRepo.getAllWithPartitionKey("${source.id}:$startingState")
                            .stream()
                            .map { Pair(source, it) }
                }
                .map {
                    Callable {
                        action(it.first, it.second)
                        if (startingState != endingState) {
                            setIndexingState(it.second, endingState)
                        }
                    }
                }
                .collect(Collectors.toList())

        executor.invokeAll(asyncTasks).forEach { it.get() }
        logger.info("Finished: applying $description to entities of type ${IndexStatusDocument::class.java} in state $startingState")
    }

    fun submitSpeechToText(source: SourceDocument, indexStatusDocument: IndexStatusDocument) {
        logger.info("Submitting speech to text for index ${indexStatusDocument.id} (source: ${source.id})")
        logger.info("Speech to text submission for index ${indexStatusDocument.id} (source: ${source.id}) complete")
    }

    fun uploadMedia(source: SourceDocument, indexStatusDocument: IndexStatusDocument) {
        logger.info("Uploading for index status ${indexStatusDocument.id} (source: ${source.id})")
        restClient.get(indexStatusDocument.mediaUrl)
                .thenConsume {
                    val lengthAsString = it.headers.getFirst("Content-Length")
                    logger.info("Found $lengthAsString bytes for index status ${indexStatusDocument.id} (source: ${source.id})")
                    blobClient.getBlobClient("${source.id}:${indexStatusDocument.id}")
                            .blockBlobClient
                            .upload(it.content, lengthAsString.toLong(), true)
                }
        logger.info("Upload complete for index status ${indexStatusDocument.id} (source: ${source.id})")
    }

    fun setIndexingState(indexStatusDocument: IndexStatusDocument, state: IndexState) {
        indexRepo.delete(indexStatusDocument.id!!, indexStatusDocument.sourceIdIndexStatusCompositeKey!!)
        indexStatusDocument.data.state = state
        indexRepo.put(indexStatusDocument)
    }
}