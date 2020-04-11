package com.iodice.mediasearch.service

import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.util.stream
import kong.unirest.UnirestInstance
import kong.unirest.json.JSONObject
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.stream.Collectors
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

    @Scheduled(fixedDelayString = "\${service.media.refresh.delay_millis}", initialDelay = 0)
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
                var pageNum = 0
                while (true) {
                    pageNum += 1
                    val pageResponse = getPage(source, pageNum)
                    val docs = getMediaFromPage(pageResponse, source)
                    val allWereNew = saveNew(docs)

                    // if not all of the tracks were new AND the track listing endpoint returns sorted results,
                    // then we can exit early because we can safely say that all future documents will already
                    // be stored
                    if (!allWereNew && source.trackListIsSorted) {
                        logger.info("Exiting refresh loop early for ${source.trackListEndpoint} due to tracks sorted condition")
                        return
                    }

                    // if we are at the end of the results, then we can return because there are no more pages
                    if (pageNum == pageResponse.getInt("noOfPages")) {
                        break
                    }
                }
            }
            logger.info("Finished refreshing ${source.name} with endpoint ${source.trackListEndpoint} in $elapsedMillis milliseconds")
        } catch (e: Exception) {
            logger.error("Unable to refresh ${source.name} with endpoint ${source.trackListEndpoint}", e)
        }
    }

    private fun getPage(source: Source, page: Int) = restClient.get("${source.trackListEndpoint}&page=$page")
            .header("accept", "application/json")
            .asJson()
            .body
            .`object`

    private fun getMediaFromPage(results: JSONObject, source: Source): Iterator<MediaDocument> =
            results.getJSONArray("episodes").mapNotNull {
                val episode = it as JSONObject
                val url = episode.getJSONObject("play").getString("url")

                // this removes any illegal characters while preserving the uniqueness per URL
                val id = Base64.getEncoder().encodeToString(url.toByteArray())
                val title = episode.getString("title")

                if (source.titleFilter != null && !title.toLowerCase().contains(source.titleFilter!!.toLowerCase())) {
                    logger.info("Filtered `$title` due to title filter of `${source.titleFilter}`")
                    null
                } else {
                    MediaDocument(
                            id = id,
                            sourceId = source.id!!,
                            data = Media(
                                    id = id,
                                    url = url,
                                    title = title,
                                    description = episode.getString("description"),
                                    image = episode.getString("image"),
                                    publishedAt = Date(episode.getLong("publishedOn"))
                            )
                    )
                }
            }.iterator()

    /**
     * Returns true if all of the documents were saved
     */
    private fun saveNew(docs: Iterator<MediaDocument>): Boolean {
        return docs.stream()
                .map { saveNew(it) }
                .collect(Collectors.toList())
                .stream()
                .allMatch { it == true }
    }

    /**
     * Returns true if the document was saved
     */
    private fun saveNew(doc: MediaDocument): Boolean {
        if (mediaRepo.exists(doc.id!!, doc.sourceId)) {
            return false
        }
        mediaRepo.put(doc)
        val status = IndexStatus(
                id = null,
                state = IndexState.NOT_STARTED,
                resultsUrl = null
        )
        indexRepo.put(IndexStatusDocument(
                id = null,
                sourceIdIndexStatusCompositeKey = null, // repository will fix this in the PUT call
                sourceId = doc.sourceId,
                mediaId = doc.id!!,
                mediaUrl = doc.data.url,
                data = status
        ))

        return true
    }
}