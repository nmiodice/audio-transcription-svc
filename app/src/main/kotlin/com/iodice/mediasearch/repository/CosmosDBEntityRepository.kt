package com.iodice.mediasearch.repository

import com.azure.cosmos.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.iodice.mediasearch.model.EntityDocument
import com.iodice.mediasearch.model.InternalServerError
import com.iodice.mediasearch.model.NotFoundException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.pow
import kotlin.random.Random
import kotlin.reflect.KClass


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
            retry { cosmosContainer.upsertItem(entity) }
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
            return retry {
                val asJson = cosmosContainer
                        .readItem(id, PartitionKey(partitionKey), JsonNode::class.java)
                        .resource
                objectMapper.readValue(asJson.toString(), clazz)
            }
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
        return retry {
            cosmosContainer.readAllItems(FeedOptions(), JsonNode::class.java)
                    .stream()
                    .map { objectMapper.readValue(it.toString(), clazz) }
                    .iterator()
        }
    }

    override fun delete(id: String, partitionKey: String) {
        retry { cosmosContainer.deleteItem(id, PartitionKey(partitionKey), CosmosItemRequestOptions()) }
    }

    private fun <T> retry(
            maxAttempts: Int = 3,
            initialDelayMillis: Double = 1.0,
            retryOn: List<KClass<out Exception>> = listOf(RequestRateTooLargeException::class),
            logic: () -> T
    ): T {
        if (maxAttempts <= 0) throw IllegalStateException("Cannot retry fewer than 1 times!")
        var attempts = 0
        var lastException: Exception? = null
        while (attempts < maxAttempts) {
            attempts += 1
            try {
                return logic()
            } catch (e: Exception) {
                when (e::class) {
                    !in retryOn -> throw e
                    else -> lastException = e
                }
            }
            val delayFixedComponentMillis = initialDelayMillis * 2.0.pow(attempts.toDouble())
            val delayRandomComponentMillis = Random.nextDouble(-0.1, 0.1) * delayFixedComponentMillis
            val finalDelay = (delayFixedComponentMillis + delayRandomComponentMillis).toLong()
            runBlocking {
                logger.warn("Waiting for $finalDelay milliseconds to retry failed request")
                delay(finalDelay)
            }
        }
        throw lastException!!
    }
}