package com.iodice.mediasearch.service

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.iodice.gen.azure.speech.model.TranscriptionDefinition
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
import java.lang.Exception
import java.net.HttpURLConnection
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
        @Inject @Named(Beans.RAW_MEDIA_CONTAINER) private val blobClient: BlobContainerClient,
        @Inject @Named(Beans.STT_API_KEY) private val sttApiKey: String,
        @Inject @Named(Beans.STT_API_ENDPOINT) private val sttApiEndpoint: String
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
            val accessUrl = getAccessUrl(indexStatusDocument)
            val request = TranscriptionDefinition(
                    recordingsUrl = accessUrl,
                    name = indexStatusDocument.id!!,
                    locale = "en-US")
            val response = restClient.post(sttApiEndpoint)
                    .header("content-type", "application/json")
                    .header("Ocp-Apim-Subscription-Key", sttApiKey)
                    .body(request)
                    .asJson()

            val status = response.status
            val body = response.body
            val location = response.headers.getFirst("location")
            indexStatusDocument.data.resultsUrl = location

            logger.info("Got HTTP $status with location of $location for index ${indexStatusDocument.id}. Response was $body")
            when (status) {
                HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_ACCEPTED, HttpURLConnection.HTTP_CREATED -> IndexState.STT_IN_PROGRESS
                else -> IndexState.STT_SUBMISSION_FAILED
            }
        } catch (e: Exception) {
            logger.info("Speech to text submission for index ${indexStatusDocument.id} failed!")
            IndexState.STT_SUBMISSION_FAILED
        }
    }

    fun getAccessUrl(indexStatusDocument: IndexStatusDocument): String {
        val blobName = indexStatusDocument.data.uploadUrl!!.replace(blobClient.blobContainerUrl, "").replace("/", "")
        val sas = blobClient.getBlobClient(blobName)
                .generateSas(BlobServiceSasSignatureValues(
                        OffsetDateTime.now().plusHours(10),
                        BlobSasPermission().setReadPermission(true)
                ))
        return "${indexStatusDocument.data.uploadUrl}?$sas"
    }

    fun uploadMedia(source: SourceDocument, indexStatusDocument: IndexStatusDocument): IndexState {
        logger.info("Uploading for index status ${indexStatusDocument.id} (source: ${source.id})")
        return try {
            restClient.get(indexStatusDocument.mediaUrl)
                    .thenConsume {
                        val lengthAsString = it.headers.getFirst("Content-Length")
                        logger.info("Found $lengthAsString bytes for index status ${indexStatusDocument.id} (source: ${source.id})")
                        val name = "${source.id}:${indexStatusDocument.id}"
                        blobClient.getBlobClient(name)
                                .blockBlobClient
                                .upload(it.content, lengthAsString.toLong(), true)

                        val uploadUrl = blobClient.blobContainerUrl + "/" + name
                        indexStatusDocument.data.uploadUrl = uploadUrl
                    }
            logger.info("Upload complete for index status ${indexStatusDocument.id} (source: ${source.id})")
            IndexState.CONTENT_UPLOADED
        } catch (e: Exception) {
            logger.info("Unable to upload for index status ${indexStatusDocument.id} (source: ${source.id}), $e")
            IndexState.CONTENT_UPLOADED_ERROR
        }

    }

    fun setIndexingState(indexStatusDocument: IndexStatusDocument, state: IndexState) {
        indexRepo.delete(indexStatusDocument.id!!, indexStatusDocument.sourceIdIndexStatusCompositeKey!!)
        indexStatusDocument.data.state = state
        indexRepo.put(indexStatusDocument)
    }
}