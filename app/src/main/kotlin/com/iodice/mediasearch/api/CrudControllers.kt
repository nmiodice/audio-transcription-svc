package com.iodice.mediasearch.api

import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.EntityRepository
import org.springframework.web.bind.annotation.*
import javax.inject.Inject


open class CrudBase<T : Entity>(
        private var repo: EntityRepository<T>
) {
    // a hook to execute logic before a POST
    open fun beforePost(entity: T) {
    }

    // a hook to execute logic after a POST
    open fun afterPost(entity: T) {
    }

    // a hook to execute logic before a GET
    open fun beforeGet(id: String) {
    }

    // a hook to execute logic after a GET
    open fun afterGet(entity: T) {
    }

    // a hook to execute logic before a DELETE
    open fun beforeDelete(id: String) {
    }

    // a hook to execute logic after a DELETE
    open fun afterDelete(id: String) {
    }

    @PostMapping("/")
    fun post(@RequestBody entity: T): T {
        beforePost(entity)
        return repo.put(entity).let {
            afterPost(it)
            it
        }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): T {
        beforeGet(id)
        return repo.get(id).let {
            afterGet(it)
            it
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String) {
        beforeDelete(id)
        repo.delete(id)
        afterDelete(id)
    }
}

@RestController
@RequestMapping("source")
class SourceCrud(
        @Inject var repo: EntityRepository<Source>
) : CrudBase<Source>(repo)

@RestController
@RequestMapping("media")
class MediaCrud(
        @Inject var sourceRepo: EntityRepository<Source>,
        @Inject var mediaRepo: EntityRepository<Media>
) : CrudBase<Media>(mediaRepo) {
    override fun beforePost(entity: Media) {
        if (null == sourceRepo.getIfExists(entity.sourceId)) {
            throw NotFoundException("The referenced source ${entity.sourceId} does not exist")
        }
    }
}

@RestController
@RequestMapping("indexresult")
class IndexResultCrud(
        @Inject var sourceRepo: EntityRepository<Source>,
        @Inject var mediaRepo: EntityRepository<Media>,
        @Inject var indexRepo: EntityRepository<IndexResult>
) : CrudBase<IndexResult>(indexRepo) {
    override fun beforePost(entity: IndexResult) {
        if (null == sourceRepo.getIfExists(entity.sourceId)) {
            throw NotFoundException("The referenced source ${entity.sourceId} does not exist")
        }

        if (null == mediaRepo.getIfExists(entity.mediaId)) {
            throw NotFoundException("The referenced media ${entity.mediaId} does not exist")
        }
    }
}