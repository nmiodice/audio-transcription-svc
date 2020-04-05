package com.iodice.mediasearch.api

import com.iodice.mediasearch.model.Entity
import com.iodice.mediasearch.model.IndexResult
import com.iodice.mediasearch.model.Media
import com.iodice.mediasearch.model.Source
import com.iodice.mediasearch.repository.EntityRepository
import org.springframework.web.bind.annotation.*
import javax.inject.Inject


abstract class CrudBase<T : Entity>(
        private var repo: EntityRepository<T>
) {
    @PostMapping("/")
    fun post(@RequestBody entity: T): T = repo.put(entity)

    @GetMapping("/{id}")
    fun get(@PathVariable id: String) = repo.get(id)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String) = repo.delete(id)
}

@RestController
@RequestMapping("source")
class SourceCrud(
        @Inject var repo: EntityRepository<Source>
) : CrudBase<Source>(repo)

@RestController
@RequestMapping("media")
class MediaCrud(
        @Inject var repo: EntityRepository<Media>
) : CrudBase<Media>(repo)

@RestController
@RequestMapping("indexresult")
class IndexResultCrud(
        @Inject var repo: EntityRepository<IndexResult>
) : CrudBase<IndexResult>(repo)