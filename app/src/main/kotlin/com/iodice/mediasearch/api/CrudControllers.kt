package com.iodice.mediasearch.api

import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import org.springframework.web.bind.annotation.*
import javax.inject.Inject

@RestController
@RequestMapping("source")
class SourceCrud(
        @Inject var sourceRepo: EntityRepository<SourceDocument>,
        @Inject var mediaRepo: EntityRepository<MediaDocument>,
        @Inject var indexRepo: EntityRepository<IndexResultDocument>
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

    @DeleteMapping("/{sourceId}/media/{mediaId}")
    fun deleteMedia(@PathVariable sourceId: String, @PathVariable mediaId: String) {
        mediaRepo.delete(mediaId, sourceId)
    }

    @PostMapping("/{sourceId}/media/{mediaId}/indexresult")
    fun postIndexResult(@PathVariable sourceId: String, @PathVariable mediaId: String, @RequestBody indexResult: IndexResult): IndexResult {
        // ensure referenced entities exists
        sourceRepo.get(sourceId, sourceId)
        mediaRepo.get(mediaId, sourceId)

        return indexRepo.put(IndexResultDocument(
                id = indexResult.id,
                mediaId = mediaId,
                sourceId = sourceId,
                data = indexResult
        )).data
    }

    @GetMapping("/{sourceId}/media/{mediaId}/indexresult/{indexResultId}")
    fun getIndexResult(@PathVariable sourceId: String, @PathVariable indexResultId: String): IndexResult {
        return indexRepo.get(indexResultId, sourceId).data
    }

    @DeleteMapping("/{sourceId}/media/{mediaId}/indexresult/{indexResultId}")
    fun deleteIndexResult(@PathVariable sourceId: String, @PathVariable indexResultId: String) {
        indexRepo.delete(indexResultId, sourceId)
    }
}
