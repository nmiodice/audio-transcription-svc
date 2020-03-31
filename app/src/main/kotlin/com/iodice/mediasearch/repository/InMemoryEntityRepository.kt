package com.iodice.mediasearch.repository

import com.iodice.mediasearch.model.Entity
import com.iodice.mediasearch.model.NotFoundException
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.HashMap

@Service
class InMemoryEntityRepository<T: Entity> : EntityRepository<T> {
    private val map: HashMap<String, T> = HashMap();

    override fun put(entity: T): T {
        if (entity.id == null) {
            entity.id = UUID.randomUUID().toString()
        }
        map[entity.id!!] = entity
        return entity
    }

    override fun get(id: String): T {
        val entity = map[id]
        if (entity != null) {
            return entity
        }

        throw NotFoundException("Entity not found for ID = $id")
    }

    override fun delete(id: String) {
        map.remove(id)
    }
}