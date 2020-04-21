package com.iodice.mediasearch.repository

import com.azure.cosmos.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.iodice.mediasearch.METRICS
import com.iodice.mediasearch.model.EntityDocument
import com.iodice.mediasearch.model.InternalServerError
import com.iodice.mediasearch.model.NotFoundException
import com.iodice.mediasearch.util.trackDuration
import com.microsoft.applicationinsights.TelemetryClient
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
        private var metricsClient: TelemetryClient,
        private val onBeforePut: (T) -> Unit = { _ -> Unit },
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

    private var partitionKeyProperty: String? = null

    override fun put(entity: T): T {
        if (entity.id == null) {
            entity.id = UUID.randomUUID().toString()
        }
        entity.data.id = entity.id

        onBeforePut(entity)
        try {
            logger.debug("Upsert for ${clazz.simpleName} with id=${entity.id}")
            metricsClient.trackDuration("${METRICS.REPOSITORY_PUT_DURATION}.${clazz.simpleName}") {
                retry { cosmosContainer.upsertItem(entity) }
            }
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
            return metricsClient.trackDuration("${METRICS.REPOSITORY_GET_DURATION}.${clazz.simpleName}") {
                retry {
                    val asJson = cosmosContainer
                            .readItem(id, PartitionKey(partitionKey), JsonNode::class.java)
                            .resource
                    objectMapper.readValue(asJson.toString(), clazz)
                }
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
        return metricsClient.trackDuration("${METRICS.REPOSITORY_GET_ALL_DURATION}.${clazz.simpleName}") {
            retry {
                cosmosContainer.readAllItems(FeedOptions(), JsonNode::class.java)
                        .stream()
                        .map { objectMapper.readValue(it.toString(), clazz) }
                        .iterator()
            }
        }
    }

    override fun getAllWithPartitionKey(partitionKey: String): Iterator<T> {
        val query = SqlQuerySpec(
                "SELECT * FROM c WHERE c.${getPartitionKeyPath()} = @pk",
                SqlParameterList(SqlParameter("@pk", partitionKey)))
        return metricsClient.trackDuration("${METRICS.REPOSITORY_GET_ALL_WITH_PK_DURATION}.${clazz.simpleName}") {
            retry {
                // note: the cosmos client does not enable configuration of the ObjectMapper used.
                // Kotlin data classes require the following module to be registered. Because it is
                // not possible, we need this (unfortunate) workaround...
                //      https://github.com/FasterXML/jackson-module-kotlin
                cosmosContainer
                        .queryItems(query, FeedOptions(), JsonNode::class.java)
                        .map { objectMapper.readValue(it.toString(), clazz) }
                        .iterator()
            }
        }
    }

    private fun getPartitionKeyPath(): String {
        if (partitionKeyProperty != null) {
            return partitionKeyProperty!!
        }

        partitionKeyProperty = cosmosContainer
                .read()
                .properties
                .partitionKeyDefinition
                .paths.joinToString(".") { it.removePrefix("/") }
        logger.info("Found partition key path $partitionKeyProperty for type $clazz")
        return partitionKeyProperty!!
    }


    override fun delete(id: String, partitionKey: String) {
        metricsClient.trackDuration("${METRICS.REPOSITORY_DELETE_DURATION}.${clazz.simpleName}") {
            retry { cosmosContainer.deleteItem(id, PartitionKey(partitionKey), CosmosItemRequestOptions()) }
        }
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
                val response = logic()
                metricsClient.trackMetric("${METRICS.REPOSITORY_CALL_ATTEMPTS}.${clazz.simpleName}", attempts.toDouble())
                return response
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