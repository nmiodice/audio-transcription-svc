package com.iodice.mediasearch.api

import com.iodice.mediasearch.METRICS
import com.iodice.mediasearch.model.Media
import com.iodice.mediasearch.model.MediaDocument
import com.iodice.mediasearch.model.Source
import com.iodice.mediasearch.model.SourceDocument
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.util.stream
import com.iodice.mediasearch.util.trackDuration
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import kotlin.streams.toList

@RestController
@RequestMapping("/api/v1/source")
class CrudController(
        @Inject var sourceRepo: EntityRepository<SourceDocument>,
        @Inject var mediaRepo: EntityRepository<MediaDocument>,
        @Inject var metricsClient: TelemetryClient
) {
    @PostMapping("/")
    fun postSource(@RequestBody source: Source): Source {
        return metricsClient.trackDuration(METRICS.API_SOURCE_POST_DURATION) {
            sourceRepo.put(SourceDocument(
                    id = source.id,
                    data = source
            )).data
        }
    }

    @GetMapping("/{sourceId}")
    fun getSource(@PathVariable sourceId: String): Source {
        return metricsClient.trackDuration(METRICS.API_SOURCE_GET_DURATION) {
            sourceRepo.get(sourceId, sourceId).data
        }
    }

    @GetMapping("/")
    fun getAllSources(): List<Source> {
        return metricsClient.trackDuration(METRICS.API_SOURCE_GET_ALL_DURATION) {
            sourceRepo.getAll().stream().map { it.data }.toList()
        }
    }


//    @DeleteMapping("/{sourceId}")
//    fun deleteSource(@PathVariable sourceId: String) {
//        sourceRepo.delete(sourceId, sourceId)
//    }

    @PostMapping("/{sourceId}/media")
    fun postMedia(@PathVariable sourceId: String, @RequestBody media: Media): Media {
        return metricsClient.trackDuration(METRICS.API_MEDIA_POST_DURATION) {
            // ensure referenced entities exists
            sourceRepo.get(sourceId, sourceId)
            mediaRepo.put(MediaDocument(
                    id = media.id,
                    sourceId = sourceId,
                    data = media
            )).data
        }
    }

    @GetMapping("/{sourceId}/media/{mediaId}")
    fun getMedia(@PathVariable sourceId: String, @PathVariable mediaId: String): Media {
        return metricsClient.trackDuration(METRICS.API_MEDIA_GET_DURATION) {
            mediaRepo.get(mediaId, sourceId).data
        }
    }

    @GetMapping("/{sourceId}/media/")
    fun getAllMedia(@PathVariable sourceId: String): List<Media> {
        return metricsClient.trackDuration(METRICS.API_MEDIA_GET_ALL_DURATION) {
            mediaRepo.getAllWithPartitionKey(sourceId).stream().map { it.data }.toList()
        }
    }

//    @DeleteMapping("/{sourceId}/media/{mediaId}")
//    fun deleteMedia(@PathVariable sourceId: String, @PathVariable mediaId: String) {
//        mediaRepo.delete(mediaId, sourceId)
//    }
}
