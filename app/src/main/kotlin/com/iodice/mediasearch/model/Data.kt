package com.iodice.mediasearch.model

import java.util.*

interface Entity{
    var id: String?
}

data class Source (
        override var id: String?,
        var name: String,
        var trackListEndpoint: String,
        var refreshInterval: Long
): Entity

data class Media(
        override var id: String?,
        var sourceId: String,
        var url: String,
        var title: String,
        var description: String,
        var image: String,
        var publishedAt: Date
): Entity

data class IndexResult(
        override var id: String?,
        var mediaId: String,
        var sourceId: String,     // unused?
        var status: IndexStatus,
        var resultsUrl: String
): Entity

enum class IndexStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED, DOWNLOADING
}
