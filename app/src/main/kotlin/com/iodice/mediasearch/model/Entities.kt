package com.iodice.mediasearch.model

import java.util.*

interface Entity {
    var id: String?
}

interface EntityDocument<T : Entity> : Entity {
    var data: T
}

data class Source(
        override var id: String?,
        var name: String,
        var trackListEndpoint: String,
        var refreshInterval: Long
) : Entity

data class SourceDocument(
        override var id: String?,
        override var data: Source
) : EntityDocument<Source>

data class Media(
        override var id: String?,
        var url: String,
        var title: String,
        var description: String,
        var image: String,
        var publishedAt: Date
) : Entity

data class MediaDocument(
        override var id: String?,
        override var data: Media,
        var sourceId: String
) : EntityDocument<Media>

data class IndexResult(
        override var id: String?,
        var status: IndexStatus,
        var resultsUrl: String
) : Entity

data class IndexResultDocument(
        override var id: String?,
        override var data: IndexResult,
        var mediaId: String,
        var sourceId: String
) : EntityDocument<IndexResult>

enum class IndexStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED, DOWNLOADING
}
