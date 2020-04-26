package com.iodice.mediasearch.adapter

import com.iodice.mediasearch.model.Media
import com.iodice.mediasearch.model.Source
import kong.unirest.UnirestInstance
import kong.unirest.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import javax.inject.Inject

@Component
class HubHopperAdapter(
        @Inject private val restClient: UnirestInstance
) : SourceListAdapter {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun queryForMedia(source: Source): Iterator<Media> {
        val paginatedIterator = PaginatedIterator(
                hasPage = fun(pageNum) = hasPage(source, pageNum),
                getPage = fun(pageNum) = getPage(source, pageNum)
        )

        val filterCondition = fun(media: Media): Boolean {
            if (source.titleFilter == null || source.titleFilter.isNullOrBlank()) {
                return true
            }
            return media.title.toLowerCase().contains(source.titleFilter!!.toLowerCase())
        }

        return FilteredIterator(paginatedIterator, filterCondition)
    }

    private fun hasPage(source: Source, pageNum: Int): Boolean {
        try {
            val response = getResponseForPage(source, pageNum)
            if (response.has("error") || !response.has("episodes")) {
                return false
            }
            return response.getJSONArray("episodes").length() > 0
        } catch (ignored: Exception) {
            logger.error("Unexpected exception querying for ${source.trackListEndpoint} at page $pageNum")
            return false
        }
    }

    /**
     * Assumes `hasPage` has already been called, and so no error checking is done
     */
    private fun getPage(source: Source, pageNum: Int): Iterator<Media> {
        return getResponseForPage(source, pageNum)
                .getJSONArray("episodes")
                .mapNotNull { parseSingleEpisodeJson(it as JSONObject) }
                .iterator()
    }

    private fun parseSingleEpisodeJson(episodeJson: JSONObject): Media {
        val url = episodeJson.getJSONObject("play").getString("url")
        return Media(
                id = Base64.getEncoder().encodeToString(url.toByteArray()),
                url = url,
                title = episodeJson.getString("title"),
                description = episodeJson.getString("description"),
                image = episodeJson.getString("image"),
                publishedAt = Date(episodeJson.getLong("publishedOn")))
    }

    private fun getResponseForPage(source: Source, pageNum: Int) = restClient.get("${source.trackListEndpoint}&page=$pageNum")
            .header("accept", "application/json")
            .asJson()
            .body
            .`object`
//
//    private fun getMediaFromPage(results: JSONObject, source: Source): Iterator<MediaDocument> =
//            results.getJSONArray("episodes").mapNotNull {
//                val episode = it as JSONObject
//                val url = episode.getJSONObject("play").getString("url")
//
//                // this removes any illegal characters while preserving the uniqueness per URL
//                val id = Base64.getEncoder().encodeToString(url.toByteArray())
//                val title = episode.getString("title")
//
//                if (source.titleFilter != null && !title.toLowerCase().contains(source.titleFilter!!.toLowerCase())) {
//                    MediaRefreshService.logger.debug("Filtered `$title` due to title filter of `${source.titleFilter}`")
//                    null
//                } else {
//                    MediaDocument(
//                            id = id,
//                            sourceId = source.id!!,
//                            data = Media(
//                                    id = id,
//                                    url = url,
//                                    title = title,
//                                    description = episode.getString("description"),
//                                    image = episode.getString("image"),
//                                    publishedAt = Date(episode.getLong("publishedOn"))
//                            )
//                    )
//                }
//            }.iterator()
}