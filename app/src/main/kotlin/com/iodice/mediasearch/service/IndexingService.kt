package com.iodice.mediasearch.service

import com.azure.storage.blob.BlobContainerClient
import com.iodice.mediasearch.di.Beans
import com.iodice.mediasearch.model.IndexState
import com.iodice.mediasearch.model.IndexStatusDocument
import com.iodice.mediasearch.model.Source
import com.iodice.mediasearch.model.SourceDocument
import com.iodice.mediasearch.repository.EntityRepository
import kong.unirest.UnirestInstance
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.inject.Inject
import javax.inject.Named
import kotlin.streams.toList

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
    }

    @Scheduled(fixedRateString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
    fun uploadSources() {
        logger.info("Starting asynchronous task to upload all sources")
        runBlocking {
            val tasks = invokeActionForAllSourceMediaInState(
                    IndexState.NOT_STARTED,
                    ::uploadMediaToStorage
            )
            awaitAll(*tasks.toList().toTypedArray())
        }
        logger.info("Finished uploading all sources")
    }

    fun uploadMediaToStorage(source: Source, indexStatusDocument: IndexStatusDocument) {
        logger.info("Uploading for index status ${indexStatusDocument.id} (source: ${source.id})")

        restClient.get(indexStatusDocument.mediaUrl)
                .thenConsume {
                    val lengthAsString = it.headers.getFirst("Content-Length")
                    logger.info("Found $lengthAsString bytes for index status ${indexStatusDocument.id} (source: ${source.id})")
                    blobClient.getBlobClient("${source.id}:${indexStatusDocument.id}")
                            .blockBlobClient
                            .upload(it.content, lengthAsString.toLong(), true)
                }

        indexRepo.delete(indexStatusDocument.id!!, indexStatusDocument.sourceIdIndexStatusCompositeKey!!)
        indexStatusDocument.data.state = IndexState.CONTENT_UPLOADED
        indexRepo.put(indexStatusDocument)
        logger.info("Upload complete for index status ${indexStatusDocument.id} (source: ${source.id})")
    }

    private fun resolveRedirect(url: String): String {
        val con: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = "HEAD"
        con.connect()
        return con.getHeaderField("Location")
    }

    private fun <T> invokeActionForAllSourceMediaInState(
            state: IndexState,
            action: (Source, IndexStatusDocument) -> T): Stream<Deferred<Unit>> {
        val sources = Spliterators.spliteratorUnknownSize(sourceRepo.getAll(), 0)

        return StreamSupport
                .stream(sources, true)
                .map {
                    GlobalScope.async {
                        invokeActionForSourceMediaInState(it.data, state, action)
                    }
                }
    }

    private fun <T> invokeActionForSourceMediaInState(
            source: Source,
            state: IndexState,
            action: (Source, IndexStatusDocument) -> T) {
        val indices = Spliterators.spliteratorUnknownSize(
                indexRepo.getAllWithPartitionKey("${source.id}:$state"), 0)
        StreamSupport
                .stream(indices, true)
                .forEach {
                    action(source, it)
                }
    }
}