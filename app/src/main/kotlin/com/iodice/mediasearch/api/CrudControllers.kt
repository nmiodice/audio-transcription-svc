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
    fun postSource(@RequestBody source: Source):Source {
        return sourceRepo.put(SourceDocument(
                id = source.id,
                data = source
        )).data
    }

    @GetMapping("/{sourceId}")
    fun getSource(@PathVariable sourceId: String): Source {
        return sourceRepo.get(sourceId).data
    }

    @DeleteMapping("/{sourceId}")
    fun deleteSource(@PathVariable sourceId: String) {
        sourceRepo.delete(sourceId)
    }

    @PostMapping("/{sourceId}/media")
    fun postMedia(@PathVariable sourceId: String, @RequestBody media: Media):Media {
        // ensure referenced entities exists
        sourceRepo.get(sourceId)
        return  mediaRepo.put(MediaDocument(
                id = media.id,
                sourceId = sourceId,
                data = media
        )).data
    }

    @GetMapping("/{sourceId}/media/{mediaId}")
    fun getMedia(@PathVariable sourceId: String, @PathVariable mediaId: String): Media {
        return mediaRepo.get(mediaId).data
    }

    @DeleteMapping("/{sourceId}/media/{mediaId}")
    fun deleteMedia(@PathVariable sourceId: String, @PathVariable mediaId: String) {
        mediaRepo.delete(mediaId)
    }

    @PostMapping("/{sourceId}/media/{mediaId}/indexresult")
    fun postIndexResult(@PathVariable sourceId: String, @PathVariable mediaId: String, @RequestBody indexResult: IndexResult):IndexResult {
        // ensure referenced entities exists
        sourceRepo.get(sourceId)
        mediaRepo.get(mediaId)

        return indexRepo.put(IndexResultDocument(
                id = indexResult.id,
                mediaId = mediaId,
                sourceId = sourceId,
                data = indexResult
        )).data
    }

    @GetMapping("/{sourceId}/media/{mediaId}/indexresult/{indexResultId}")
    fun getIndexResult(@PathVariable sourceId: String, @PathVariable mediaId: String, @PathVariable indexResultId: String): IndexResult {
        return indexRepo.get(indexResultId).data
    }

    @DeleteMapping("/{sourceId}/media/{mediaId}/indexresult/{indexResultId}")
    fun deleteIndexResult(@PathVariable sourceId: String, @PathVariable mediaId: String, @PathVariable indexResultId: String) {
        indexRepo.delete(indexResultId)
    }
}

//@RestController
//@RequestMapping("media")
//class MediaCrud(
//        @Inject var sourceRepo: EntityRepository<Source>,
//        @Inject var mediaRepo: EntityRepository<Media>){
//
//    @PostMapping("/")
//    fun post(@RequestBody media: Media):Media {
//        if (null == sourceRepo.getIfExists(media.sourceId)) {
//            throw NotFoundException("The referenced source ${media.sourceId} does not exist")
//        }
//        return mediaRepo.put(media)
//    }
//
//    @GetMapping("/{id}")
//    fun get(@PathVariable id: String): Media {
//        return mediaRepo.get(id)
//    }
//
//    @DeleteMapping("/{id}")
//    fun delete(@PathVariable id: String) {
//        mediaRepo.delete(id)
//    }
//}

//@RestController
//@RequestMapping("indexresult")
//class IndexResultCrud(
//        @Inject var sourceRepo: EntityRepository<Source>,
//        @Inject var mediaRepo: EntityRepository<Media>,
//        @Inject var indexRepo: EntityRepository<IndexResult>){
//
//    @PostMapping("/")
//    fun post(@RequestBody indexResult: IndexResult):IndexResult {
//        if (null == sourceRepo.getIfExists(indexResult.sourceId)) {
//            throw NotFoundException("The referenced source ${indexResult.sourceId} does not exist")
//        }
//
//        if (null == mediaRepo.getIfExists(indexResult.mediaId)) {
//            throw NotFoundException("The referenced media ${indexResult.mediaId} does not exist")
//        }
//        return indexRepo.put(indexResult)
//    }
//
//    @GetMapping("/{id}")
//    fun get(@PathVariable id: String): IndexResult {
//        return indexRepo.get(id)
//    }
//
//    @DeleteMapping("/{id}")
//    fun delete(@PathVariable id: String) {
//        indexRepo.delete(id)
//    }
//}
