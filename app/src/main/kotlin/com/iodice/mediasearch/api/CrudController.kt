package com.iodice.mediasearch.api

import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.util.stream
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import kotlin.streams.toList

@RestController
@RequestMapping("source")
class CrudController(
        @Inject var sourceRepo: EntityRepository<SourceDocument>,
        @Inject var mediaRepo: EntityRepository<MediaDocument>,
        @Inject var indexRepo: EntityRepository<IndexStatusDocument>
) {
    @PostMapping("/")
    fun postSource(@RequestBody source: Source): Source {
        return sourceRepo.put(SourceDocument(
                id = source.id,
                data = source
        )).data
    }

    @GetMapping("/{sourceId}")
    fun getSource(@PathVariable sourceId: String): Source {
        return sourceRepo.get(sourceId, sourceId).data
    }

    @GetMapping("/")
    fun getAllSources(): List<Source> {
        return sourceRepo.getAll().stream().map { it.data }.toList()
    }


    @DeleteMapping("/{sourceId}")
    fun deleteSource(@PathVariable sourceId: String) {
        sourceRepo.delete(sourceId, sourceId)
    }

    @PostMapping("/{sourceId}/media")
    fun postMedia(@PathVariable sourceId: String, @RequestBody media: Media): Media {
        // ensure referenced entities exists
        sourceRepo.get(sourceId, sourceId)
        return mediaRepo.put(MediaDocument(
                id = media.id,
                sourceId = sourceId,
                data = media
        )).data
    }

    @GetMapping("/{sourceId}/media/{mediaId}")
    fun getMedia(@PathVariable sourceId: String, @PathVariable mediaId: String): Media {
        return mediaRepo.get(mediaId, sourceId).data
    }

    @GetMapping("/{sourceId}/media/")
    fun getAllMedia(@PathVariable sourceId: String): List<Media> {
        return mediaRepo.getAllWithPartitionKey(sourceId).stream().map { it.data }.toList()
    }

    @DeleteMapping("/{sourceId}/media/{mediaId}")
    fun deleteMedia(@PathVariable sourceId: String, @PathVariable mediaId: String) {
        mediaRepo.delete(mediaId, sourceId)
    }
}
