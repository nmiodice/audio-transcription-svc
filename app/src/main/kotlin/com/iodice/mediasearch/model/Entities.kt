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
        var trackListIsSorted: Boolean
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

data class IndexStatus(
        override var id: String?,
        var state: IndexState,
        var resultsUrl: String?
) : Entity

data class IndexStatusDocument(
        override var id: String?,
        override var data: IndexStatus,
        var mediaId: String,
        var mediaUrl: String,
        var sourceIdIndexStatusCompositeKey: String
) : EntityDocument<IndexStatus>

enum class IndexState {
    NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED, DOWNLOADING
}
