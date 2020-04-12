package com.iodice.mediasearch.service

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.iodice.mediasearch.client.*
import com.iodice.mediasearch.di.Beans
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.util.stream
import kong.unirest.UnirestInstance
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
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
        @Inject private val sttClient: STTClient,
        @Inject @Named(Beans.RAW_MEDIA_CONTAINER) private val blobClient: BlobContainerClient
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        private val executor = Executors.newFixedThreadPool(2)
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun uploadSourcesTask() {
        applyToIndexesInState("upload media", IndexState.NOT_STARTED, ::uploadMedia)
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun submitSpeechToTextTask() {
        applyToIndexesInState("submit speech to text", IndexState.CONTENT_UPLOADED, ::submitSpeechToText)
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun checkSpeechToTextTask() {
        applyToIndexesInState("check speech to text status", IndexState.STT_IN_PROGRESS, ::checkSpeechToText)
    }

    private fun applyToIndexesInState(
            description: String,
            startingState: IndexState,
            action: (SourceDocument, IndexStatusDocument) -> IndexState?) {
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
                        val newState = action(it.first, it.second)
                        if (newState != null) {
                            setIndexingState(it.second, newState)
                        }
                    }
                }
                .collect(Collectors.toList())

        executor.invokeAll(asyncTasks).forEach { it.get() }
        logger.info("Finished: applying $description to entities of type ${IndexStatusDocument::class.java} in state $startingState")
    }

    fun submitSpeechToText(source: SourceDocument, indexStatusDocument: IndexStatusDocument): IndexState {
        logger.info("Submitting speech to text for index ${indexStatusDocument.id}")
        return try {
            indexStatusDocument.data.sttCallbackUrl = sttClient.submitAsync(
                    getAccessUrl(indexStatusDocument),
                    indexStatusDocument.id!!)
            IndexState.STT_IN_PROGRESS
        } catch (e: Exception) {
            logger.info("STT submission for ${indexStatusDocument.id} failed: $e")
            IndexState.STT_SUBMISSION_FAILED
        }
    }

    fun getAccessUrl(indexStatusDocument: IndexStatusDocument): String {
        val blobName = indexStatusDocument.data.mediaUploadUrl!!.replace(blobClient.blobContainerUrl, "").replaceFirst("/", "")
        val sas = blobClient.getBlobClient(blobName)
                .generateSas(BlobServiceSasSignatureValues(
                        OffsetDateTime.now().plusHours(10),
                        BlobSasPermission().setReadPermission(true)))
        return "${indexStatusDocument.data.mediaUploadUrl}?$sas"
    }

    fun uploadMedia(source: SourceDocument, indexStatusDocument: IndexStatusDocument): IndexState {
        logger.info("Uploading for index status ${indexStatusDocument.id} (source: ${source.id})")
        return try {
            restClient.get(indexStatusDocument.mediaUrl)
                    .thenConsume {
                        val lengthAsString = it.headers.getFirst("Content-Length")
                        logger.info("Found $lengthAsString bytes for index status ${indexStatusDocument.id} (source: ${source.id})")
                        val name = "${source.id}/${indexStatusDocument.id}"
                        blobClient.getBlobClient(name)
                                .blockBlobClient
                                .upload(it.content, lengthAsString.toLong(), true)

                        val uploadUrl = blobClient.blobContainerUrl + "/" + name
                        indexStatusDocument.data.mediaUploadUrl = uploadUrl
                    }
            logger.info("Upload complete for index status ${indexStatusDocument.id} (source: ${source.id})")
            IndexState.CONTENT_UPLOADED
        } catch (e: Exception) {
            logger.info("Unable to upload for index status ${indexStatusDocument.id} (source: ${source.id}), $e")
            IndexState.CONTENT_UPLOADED_ERROR
        }
    }

    fun checkSpeechToText(source: SourceDocument, indexStatusDocument: IndexStatusDocument): IndexState? {
        val status: STTStatus?
        try {
            status = sttClient.checkStatus(indexStatusDocument.data.sttCallbackUrl!!)
        } catch (e: Exception) {
            logger.warn("Unexpected error processing STT results for ${indexStatusDocument.id}. Error was $e")
            return null
        }

        when (status) {
            is STTFailed -> {
                logger.warn("STT job for ${indexStatusDocument.id} failed: ${status.message}")
                return IndexState.STT_JOB_FAILED
            }
            is STTInProgress -> {
                logger.debug("STT job for ${indexStatusDocument.id} is still in progress")
                return null
            }
            !is STTSuccess -> {
                logger.warn("STT job for ${indexStatusDocument.id} has unknown status! $status")
                return null
            }
        }

        // the results represent different channels, so any are OK
        val resultsUrl = (status as STTSuccess).resultsUrls.first()
        restClient.get(resultsUrl)
                .header("content-type", "application/json")
                .thenConsume {
                    val lengthAsString = it.headers.getFirst("Content-Length")
                    logger.info("Found $lengthAsString bytes for index status ${indexStatusDocument.id} (source: ${source.id})")
                    val name = "${source.id}/${indexStatusDocument.id}/sttResults.json"
                    blobClient.getBlobClient(name)
                            .blockBlobClient
                            .upload(it.content, lengthAsString.toLong(), true)
                    val uploadUrl = blobClient.blobContainerUrl + "/" + name
                    indexStatusDocument.data.sttResultsUpload = uploadUrl
                }

        return IndexState.STT_FINISHED
    }

    fun setIndexingState(indexStatusDocument: IndexStatusDocument, state: IndexState) {
        indexRepo.delete(indexStatusDocument.id!!, indexStatusDocument.sourceIdIndexStatusCompositeKey!!)
        indexStatusDocument.data.state = state
        indexRepo.put(indexStatusDocument)
    }
}