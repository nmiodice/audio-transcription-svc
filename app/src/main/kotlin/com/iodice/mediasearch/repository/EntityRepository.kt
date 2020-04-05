package com.iodice.mediasearch.repository

import com.iodice.mediasearch.model.EntityDocument

interface EntityRepository<T : EntityDocument<*>> {
    fun put(entity: T): T
    fun getIfExists(id: String, partitionKey: String): T?
    fun get(id: String, partitionKey: String): T
    fun delete(id: String, partitionKey: String)
}