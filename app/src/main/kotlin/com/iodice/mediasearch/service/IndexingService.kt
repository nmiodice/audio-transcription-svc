package com.iodice.mediasearch.service

import com.iodice.mediasearch.model.IndexStatusDocument
import com.iodice.mediasearch.model.SourceDocument
import com.iodice.mediasearch.repository.EntityRepository
import kong.unirest.UnirestInstance
import org.slf4j.LoggerFactory
import javax.inject.Inject

class IndexingService(
        @Inject private val sourceRepo: EntityRepository<SourceDocument>,
        @Inject private val indexRepo: EntityRepository<IndexStatusDocument>,
        @Inject private val restClient: UnirestInstance
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

//    @Scheduled(fixedRateString = "\${service.index.refresh.delay_millis}", initialDelay = 0)
//    fun refreshSources() {
//        MediaRefreshService.logger.info("Starting asynchronous task to refresh all sources")
//        runBlocking {
//            val tasks: MutableList<Deferred<Unit>> = mutableListOf()
//            sourceRepo.getAll().forEach {
//                tasks.add(GlobalScope.async {
//                    refreshSource(it.data)
//                })
//            }
//            awaitAll(*tasks.toTypedArray())
//        }
//        MediaRefreshService.logger.info("Finished refreshing all sources")
//    }
}