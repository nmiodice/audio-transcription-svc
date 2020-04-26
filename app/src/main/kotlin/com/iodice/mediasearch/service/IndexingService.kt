package com.iodice.mediasearch.service

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.google.gson.Gson
import com.iodice.mediasearch.AUDIO
import com.iodice.mediasearch.client.FFMPEGClient
import com.iodice.mediasearch.client.STTClient
import com.iodice.mediasearch.client.SearchIndexClient
import com.iodice.mediasearch.di.Beans
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.util.stream
import com.iodice.mediasearch.util.throwIfStatusIsNot
import kong.unirest.UnirestInstance
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.*
import java.net.HttpURLConnection
import java.net.URLEncoder
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
        @Inject private val searchClient: SearchIndexClient,
        @Inject @Named(Beans.RAW_MEDIA_CONTAINER) private val blobClient: BlobContainerClient,
        @Inject private val ffMPEGClient: FFMPEGClient
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        private val executor = Executors.newFixedThreadPool(4)

        private val gson = Gson()
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun uploadSourcesTask() {
        applyToIndexesInState("upload media", IndexState.NOT_STARTED, ::uploadMedia)
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun submitSTTTask() {
        applyToIndexesInState("submit STT", IndexState.CONTENT_UPLOADED, ::submitSTT)
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun checkSTTTask() {
        applyToIndexesInState("check STT status", IndexState.STT_IN_PROGRESS, ::checkSTT)
    }

    @Scheduled(fixedDelayString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun indexFinishedSTTTask() {
        applyToIndexesInState("index STT", IndexState.STT_FINISHED, ::indexFinishedSTT)
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

    fun submitSTT(source: SourceDocument, indexStatusDocument: IndexStatusDocument): IndexState {
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
        val blobName = blobUrlToName(indexStatusDocument.data.mediaUploadUrl!!)
        val sas = blobClient.getBlobClient(blobName)
                .generateSas(BlobServiceSasSignatureValues(
                        OffsetDateTime.now().plusHours(10),
                        BlobSasPermission().setReadPermission(true)))
        return blobClient.blobContainerUrl + "/" + blobName + "?$sas"
    }

    fun uploadMedia(source: SourceDocument, indexStatusDocument: IndexStatusDocument): IndexState? {
        var sourceAudio: File? = null
        var destinationAudio: File? = null

        return try {
            sourceAudio = File.createTempFile(indexStatusDocument.id!!, "source.mp3")
            sourceAudio.delete() // the next line assumes the file does not yet exist!
            sourceAudio = restClient.get(indexStatusDocument.mediaUrl)
                    .asFile(sourceAudio.absolutePath)
                    .throwIfStatusIsNot(HttpURLConnection.HTTP_OK)
                    .body

            destinationAudio = File.createTempFile(indexStatusDocument.id!!, "destination.wav")

            logger.info("${indexStatusDocument.id}: processing audio file $sourceAudio with output $destinationAudio")
            ffMPEGClient.process(sourceAudio, destinationAudio, AUDIO.BITRATE, AUDIO.SAMPLE_RATE)
            logger.info("${indexStatusDocument.id}: done processing audio file $sourceAudio with output $destinationAudio")

            logger.info("${indexStatusDocument.id}: Uploading processed audio file to Azure Storage")
            val blobName = "${source.id}:${indexStatusDocument.id}.wav"
            blobClient.getBlobClient(blobName)
                    .blockBlobClient
                    .upload(FileInputStream(destinationAudio!!), destinationAudio.length(), true)
            val uploadUrl = blobClient.blobContainerUrl + "/" + URLEncoder.encode(blobName, "utf-8")
            indexStatusDocument.data.mediaUploadUrl = uploadUrl

            logger.info("${indexStatusDocument.id}: Upload complete")
            IndexState.CONTENT_UPLOADED
        } catch (e: Exception) {
            logger.info("Unable to upload for index status ${indexStatusDocument.id} (source: ${source.id}), $e")
            IndexState.CONTENT_UPLOADED_ERROR
        } finally {
            try {
                sourceAudio?.delete()
            } catch (ignored: java.lang.Exception) {
            }
            try {
                destinationAudio?.delete()
            } catch (ignored: java.lang.Exception) {
            }
        }
    }

    fun checkSTT(source: SourceDocument, indexStatusDocument: IndexStatusDocument): IndexState? {
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
                    val name = "${source.id}/${indexStatusDocument.id}/sttResults.json"
                    blobClient.getBlobClient(name)
                            .blockBlobClient
                            .upload(it.content, lengthAsString.toLong(), true)
                    val uploadUrl = blobClient.blobContainerUrl + "/" + name
                    indexStatusDocument.data.sttResultsUpload = uploadUrl
                }

        return IndexState.STT_FINISHED
    }

    fun indexFinishedSTT(source: SourceDocument, indexStatusDocument: IndexStatusDocument): IndexState? {
        val blobName = blobUrlToName(indexStatusDocument.data.sttResultsUpload!!)
        val sttResults = ByteArrayOutputStream().let { os ->
            blobClient.getBlobClient(blobName).download(os)
            val reader = InputStreamReader(ByteArrayInputStream(os.toByteArray()))
            gson.fromJson(reader, TranscriptionResult::class.java)
        }

        return try {
            searchClient.index(indexStatusDocument, sttResults)
            IndexState.INDEXING_FINISHED
        } catch (e: java.lang.Exception) {
            IndexState.INDEXING_FAILED
        }
    }

    fun setIndexingState(indexStatusDocument: IndexStatusDocument, state: IndexState) {
        logger.info("updating state of ${indexStatusDocument.id} from ${indexStatusDocument.data.state} to $state")
        indexRepo.delete(indexStatusDocument.id!!, indexStatusDocument.sourceIdIndexStatusCompositeKey!!)
        indexStatusDocument.data.state = state
        indexRepo.put(indexStatusDocument)
    }

    fun blobUrlToName(url: String) = url
            .replace(blobClient.blobContainerUrl, "")
            .replaceFirst("/", "")
}