package com.iodice.mediasearch.service

import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import kong.unirest.UnirestInstance
import kong.unirest.json.JSONObject
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.stream.StreamSupport
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@Component
class MediaRefreshService(
        @Inject private val sourceRepo: EntityRepository<SourceDocument>,
        @Inject private val mediaRepo: EntityRepository<MediaDocument>,
        @Inject private val indexRepo: EntityRepository<IndexStatusDocument>,
        @Inject private val restClient: UnirestInstance
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @Scheduled(fixedRateString = "\${service.media.refresh.delay_millis}", initialDelay = 0)
    fun refreshSources() {
        logger.info("Starting asynchronous task to refresh all sources")
        runBlocking {
            val tasks: MutableList<Deferred<Unit>> = mutableListOf()
            sourceRepo.getAll().forEach {
                tasks.add(GlobalScope.async {
                    refreshSource(it.data)
                })
            }
            awaitAll(*tasks.toTypedArray())
        }
        logger.info("Finished refreshing all sources")
    }

    suspend fun refreshSource(source: Source) {
        try {
            val elapsedMillis = measureTimeMillis {
                logger.info("Refreshing ${source.name} with endpoint ${source.trackListEndpoint}")
                var page = 0
                while (true) {
                    page += 1
                    val response = restClient.get("${source.trackListEndpoint}&page=$page")
                            .header("accept", "application/json")
                            .asJson()
                            .body
                            .`object`
                    processPage(response, source)
                    if (page == response.getInt("noOfPages")) {
                        break
                    }
                }
            }
            logger.info("Finished refreshing ${source.name} with endpoint ${source.trackListEndpoint} in $elapsedMillis milliseconds")
        } catch (e: Exception) {
            logger.error("Unable to refresh ${source.name} with endpoint ${source.trackListEndpoint}", e)
        }
    }

    private fun processPage(results: JSONObject, source: Source) {
        val docs = getMediaFromPage(results, source)
        saveNew(docs)
    }

    private fun getMediaFromPage(results: JSONObject, source: Source): Iterable<MediaDocument> = results.getJSONArray("episodes").map {
        val episode = it as JSONObject
        val url = episode.getJSONObject("play").getString("url")

        // this removes any illegal characters while preserving the uniqueness
        // per URL
        val id = Base64.getEncoder().encodeToString(url.toByteArray())
        MediaDocument(
                id = id,
                sourceId = source.id!!,
                data = Media(
                        id = id,
                        url = url,
                        title = episode.getString("title"),
                        description = episode.getString("description"),
                        image = episode.getString("image"),
                        publishedAt = Date(episode.getLong("publishedOn"))
                )
        )
    }

    private fun saveNew(docs: Iterable<MediaDocument>) = StreamSupport.stream(docs.spliterator(), true).forEach {
        if (!mediaRepo.exists(it.id!!, it.sourceId)) {
            mediaRepo.put(it)
            val status = IndexStatus(
                    id = null,
                    state = IndexState.NOT_STARTED,
                    resultsUrl = null
            )
            indexRepo.put(IndexStatusDocument(
                    id = null,
                    sourceIdIndexStatusCompositeKey = "${it.sourceId}:${status.state}",
                    mediaId = it.id!!,
                    mediaUrl = it.data.url,
                    data = status
            ))
        }
    }
}