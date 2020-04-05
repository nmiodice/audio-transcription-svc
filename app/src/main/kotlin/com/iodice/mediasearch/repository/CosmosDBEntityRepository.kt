package com.iodice.mediasearch.repository

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosItemRequestOptions
import com.azure.cosmos.PartitionKey
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.iodice.mediasearch.model.Entity
import com.iodice.mediasearch.model.EntityDocument
import com.iodice.mediasearch.model.InternalServerError
import com.iodice.mediasearch.model.NotFoundException
import java.util.*

class CosmosDBEntityRepository<T : EntityDocument<*>>(
        private var cosmosContainer: CosmosContainer,
        private var clazz: Class<T>,
        private var objectMapper: ObjectMapper = ObjectMapper().let {
            it.registerModule(KotlinModule())
            it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            it
        }
) : EntityRepository<T> {

    override fun put(entity: T): T {
        if (entity.id == null) {
            entity.id = UUID.randomUUID().toString()
        }
        entity.data.id = entity.id
        try {
            cosmosContainer.upsertItem(entity)
        } catch (e: Exception) {
            throw InternalServerError("Unknown error encountered inserting entity")
        }
        return entity
    }

    override fun getIfExists(id: String): T? {
        return try {
            get(id)
        } catch (e: NotFoundException) {
            null
        }
    }

    override fun get(id: String): T {
        try {
            // note: the cosmos client does not enable configuration of the ObjectMapper used.
            // Kotlin data classes require the following module to be registered. Because it is
            // not possible, we need this (unfortunate) workaround...
            //      https://github.com/FasterXML/jackson-module-kotlin
            val asJson = cosmosContainer
                    .readItem(id, PartitionKey(id), JsonNode::class.java)
                    .resource
            return objectMapper.readValue(asJson.toString(), clazz)
        } catch (e: com.azure.cosmos.NotFoundException) {
            throw NotFoundException("Entity not found for ID = $id")
        } catch (e: Exception) {
            throw InternalServerError("Unknown error encountered querying for entity with ID = $id")
        }
    }

    override fun delete(id: String) {
        cosmosContainer.deleteItem(id, PartitionKey(id), CosmosItemRequestOptions())
    }
}