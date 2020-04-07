package com.iodice.mediasearch.repository

import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosItemRequestOptions
import com.azure.cosmos.FeedOptions
import com.azure.cosmos.PartitionKey
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.util.concurrent.RateLimiter
import com.iodice.mediasearch.model.EntityDocument
import com.iodice.mediasearch.model.InternalServerError
import com.iodice.mediasearch.model.NotFoundException
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.util.*
import java.util.stream.StreamSupport

class ThrottledCosmosDBEntityRepository<T: EntityDocument<*>>(
        private val repo: CosmosDBEntityRepository<T>,
        private val rateLimiter: RateLimiter
): EntityRepository<T> {
    private var lastOperation = System.currentTimeMillis()
    override fun put(entity: T): T {
        rateLimiter.acquire()
        return repo.put(entity)
    }

    override fun exists(id: String, partitionKey: String): Boolean {
        rateLimiter.acquire()
        return repo.exists(id, partitionKey)
    }

    override fun get(id: String, partitionKey: String): T {
        rateLimiter.acquire()
        return repo.get(id, partitionKey)
    }

    override fun getAll(): Iterator<T> {
        rateLimiter.acquire()
        return repo.getAll()
    }

    override fun delete(id: String, partitionKey: String) {
        rateLimiter.acquire()
        repo.delete(id, partitionKey)
    }

}

class CosmosDBEntityRepository<T : EntityDocument<*>>(
        private val cosmosContainer: CosmosContainer,
        private val clazz: Class<T>,
        private val objectMapper: ObjectMapper = ObjectMapper().let {
            it.registerModule(KotlinModule())
            it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            it
        }
) : EntityRepository<T> {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun put(entity: T): T {
        if (entity.id == null) {
            entity.id = UUID.randomUUID().toString()
        }
        entity.data.id = entity.id
        try {
            logger.debug("Upsert for ${clazz.simpleName} with id=${entity.id}")
            cosmosContainer.upsertItem(entity)
        } catch (e: Exception) {
            logger.warn("Upsert for ${clazz.simpleName} with id=${entity.id} failed due to $e")
            throw InternalServerError("Unknown error encountered inserting entity")
        }
        return entity
    }

    override fun exists(id: String, partitionKey: String): Boolean {
        return try {
            get(id, partitionKey)
            true
        } catch (e: NotFoundException) {
            false
        }
    }

    override fun get(id: String, partitionKey: String): T {
        try {
            // note: the cosmos client does not enable configuration of the ObjectMapper used.
            // Kotlin data classes require the following module to be registered. Because it is
            // not possible, we need this (unfortunate) workaround...
            //      https://github.com/FasterXML/jackson-module-kotlin
            val asJson = cosmosContainer
                    .readItem(id, PartitionKey(partitionKey), JsonNode::class.java)
                    .resource
            return objectMapper.readValue(asJson.toString(), clazz)
        } catch (e: java.lang.Exception) {
            when (e) {
                is com.azure.cosmos.NotFoundException, is IllegalArgumentException -> {
                    logger.info("${clazz.simpleName} with id=$id not found")
                    throw NotFoundException("Entity not found for ID = $id")
                }
                else -> {
                    logger.warn("Get for ${clazz.simpleName} with id=$id failed due to $e")
                    throw InternalServerError("Unknown error encountered querying for entity with ID = $id")
                }
            }
        }
    }

    override fun getAll(): Iterator<T> {
        return cosmosContainer.readAllItems(FeedOptions(), JsonNode::class.java)
                .stream()
                .map { objectMapper.readValue(it.toString(), clazz) }
                .iterator()
    }

    override fun delete(id: String, partitionKey: String) {
        cosmosContainer.deleteItem(id, PartitionKey(partitionKey), CosmosItemRequestOptions())
    }
}