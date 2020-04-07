package com.iodice.mediasearch.repository

import com.iodice.mediasearch.model.EntityDocument

interface EntityRepository<T : EntityDocument<*>> {
    fun put(entity: T): T
    fun exists(id: String, partitionKey: String): Boolean
    fun get(id: String, partitionKey: String): T
    fun getAll(): Iterator<T>
    fun delete(id: String, partitionKey: String)
}