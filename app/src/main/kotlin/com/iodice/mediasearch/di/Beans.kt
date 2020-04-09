package com.iodice.mediasearch.di

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobContainerClientBuilder
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.CosmosDBEntityRepository
import com.iodice.mediasearch.repository.EntityRepository
import kong.unirest.Config
import kong.unirest.UnirestInstance
import org.apache.commons.lang3.Validate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import javax.inject.Named
import javax.inject.Singleton

@Component
class Beans {
    companion object {
        const val RAW_MEDIA_CONTAINER = "RAW_MEDIA_CONTAINER"
    }

    // TODO: pull from KeyVault
    @Value("\${AZ_COSMOS_KEY}")
    lateinit var cosmosKey: String

    // TODO: pull from KeyVault
    @Value("\${AZ_COSMOS_ENDPOINT}")
    lateinit var cosmosEndpoint: String

    // TODO: pull from KeyVault
    @Value("\${AZ_COSMOS_DB}")
    lateinit var cosmosDatabase: String

    // TODO: pull from KeyVault
    @Value("\${AZ_MEDIA_STORAGE_CONTAINER}")
    lateinit var mediaStorageContainer: String

    // TODO: pull from KeyVault
    @Value("\${AZ_STORAGE_CONNECTION_STRING}")
    lateinit var storageConnectionString: String


    @Bean
    @Singleton
    fun cosmosClient(): CosmosClient {
        Validate.notBlank(cosmosEndpoint)
        Validate.notBlank(cosmosKey)

        return CosmosClient.cosmosClientBuilder()
                .setEndpoint(cosmosEndpoint)
                .setKey(cosmosKey)
                .buildClient()
    }

    private fun cosmosContainer(
            containerName: String,
            cosmosClient: CosmosClient,
            partitionKey: String
    ): CosmosContainer {
        Validate.notBlank(cosmosDatabase)
        Validate.notBlank(partitionKey)
        Validate.notBlank(containerName)

        fun ensureStartsWithSlash(s: String): String = if (s.startsWith("/")) s else "/$s"
        return cosmosClient
                .createDatabaseIfNotExists(cosmosDatabase)
                .database
                .createContainerIfNotExists(
                        containerName,
                        ensureStartsWithSlash(partitionKey))
                .container
    }

    @Bean
    @Singleton
    fun sourceRepository(cosmosClient: CosmosClient): EntityRepository<SourceDocument> {
        return CosmosDBEntityRepository(
                cosmosContainer(
                        Source::class.simpleName!!.toLowerCase(),
                        cosmosClient,
                        SourceDocument::id.name
                ),
                SourceDocument::class.java
        )
    }

    @Bean
    @Singleton
    fun mediaRepository(cosmosClient: CosmosClient): EntityRepository<MediaDocument> {
        return CosmosDBEntityRepository(
                cosmosContainer(
                        Media::class.simpleName!!.toLowerCase(),
                        cosmosClient,
                        MediaDocument::sourceId.name
                ),
                MediaDocument::class.java
        )
    }

    @Bean
    @Singleton
    fun indexRepository(cosmosClient: CosmosClient): EntityRepository<IndexStatusDocument> {
        return CosmosDBEntityRepository(
                cosmosContainer(
                        IndexStatus::class.simpleName!!.toLowerCase(),
                        cosmosClient,
                        IndexStatusDocument::sourceIdIndexStatusCompositeKey.name
                ),
                IndexStatusDocument::class.java,
                onBeforePut = {
                    it.sourceIdIndexStatusCompositeKey = "${it.sourceId}:${it.data.state}"
                }
        )
    }

    @Bean
    @Singleton
    fun restClient() = UnirestInstance(Config())

    @Bean
    @Named(RAW_MEDIA_CONTAINER)
    @Singleton
    fun mediaStorageContainer(): BlobContainerClient {
        return BlobContainerClientBuilder()
                .connectionString(storageConnectionString)
                .containerName(mediaStorageContainer)
                .buildClient()
    }
}
