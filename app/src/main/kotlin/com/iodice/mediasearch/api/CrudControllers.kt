package com.iodice.mediasearch.api

import com.iodice.mediasearch.model.Entity
import com.iodice.mediasearch.model.Source
import com.iodice.mediasearch.repository.EntityRepository
import com.iodice.mediasearch.repository.InMemoryEntityRepository
import org.springframework.web.bind.annotation.*
import javax.inject.Inject


abstract class CrudBase<T: Entity> {
    @Inject
    private lateinit var repo: EntityRepository<T>

    @PostMapping("/")
    fun postMedia(@RequestBody entity: T): T = repo.put(entity)

    @GetMapping("/{id}")
    fun getMedia(@PathVariable id: String) = repo.get(id)

    @DeleteMapping("/{id}")
    fun deleteMedia(@PathVariable id: String) = repo.delete(id)
}

@RestController
@RequestMapping("source")
class SourceCrud: CrudBase<Source>()