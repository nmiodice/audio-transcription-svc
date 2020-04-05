package com.iodice.mediasearch.di

import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosContainer
import com.iodice.mediasearch.model.*
import com.iodice.mediasearch.repository.CosmosDBEntityRepository
import com.iodice.mediasearch.repository.EntityRepository
import org.apache.commons.lang3.Validate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class Beans {
    // TODO: pull from KeyVault
    @Value("\${AZ_COSMOS_KEY}")
    lateinit var cosmosKey: String

    // TODO: pull from KeyVault
    @Value("\${AZ_COSMOS_ENDPOINT}")
    lateinit var cosmosEndpoint: String

    // TODO: pull from KeyVault
    @Value("\${AZ_COSMOS_DB}")
    lateinit var cosmosDatabase: String


    @Bean
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
    fun mediaRepository(cosmosClient: CosmosClient): EntityRepository<MediaDocument> {
        return CosmosDBEntityRepository(
                cosmosContainer(
                        Media::class.simpleName!!.toLowerCase(),
                        cosmosClient,
                        MediaDocument::id.name
                ),
                MediaDocument::class.java
        )
    }

    @Bean
    fun indexRepository(cosmosClient: CosmosClient): EntityRepository<IndexResultDocument> {
        return CosmosDBEntityRepository(
                cosmosContainer(
                        IndexResult::class.simpleName!!.toLowerCase(),
                        cosmosClient,
                        IndexResultDocument::id.name
                ),
                IndexResultDocument::class.java
        )
    }
}
