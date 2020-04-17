package com.iodice.mediasearch.model

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.*


@ResponseStatus(value = HttpStatus.NOT_FOUND)
class NotFoundException(msg: String) : Exception(msg)

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class InternalServerError(msg: String) : Exception(msg)



interface Entity {
    var id: String?
}

interface EntityDocument<T : Entity> : Entity {
    var data: T
}

data class Source(
        override var id: String? = null,
        var name: String,
        var trackListEndpoint: String,
        var trackListIsSorted: Boolean,
        var image: String,
        var titleFilter: String? = null,
        var homepage: String
) : Entity

data class SourceDocument(
        override var id: String? = null,
        override var data: Source
) : EntityDocument<Source>

data class Media(
        override var id: String? = null,
        var url: String,
        var title: String,
        var description: String,
        var image: String,
        var publishedAt: Date
) : Entity

data class MediaDocument(
        override var id: String? = null,
        override var data: Media,
        var sourceId: String
) : EntityDocument<Media>

data class IndexStatus(
        override var id: String? = null,
        var state: IndexState,
        var sttCallbackUrl: String? = null,
        var sttResultsUpload: String? = null,
        var mediaUploadUrl: String? = null
) : Entity

data class IndexStatusDocument(
        override var id: String? = null,
        override var data: IndexStatus,
        var sourceId: String,
        var mediaId: String,
        var mediaUrl: String,
        var sourceIdIndexStatusCompositeKey: String? = null
) : EntityDocument<IndexStatus>


data class AnnotatedIndices(
        var indices: List<Index>,
        var source: Source,
        var media: Media
)

data class AggregatedQueryResponse(
        var aggregatedByMedia: Map<String, AnnotatedIndices>
)

enum class IndexState {
    NOT_STARTED,
    CONTENT_UPLOADED,
    CONTENT_UPLOADED_ERROR,
    STT_IN_PROGRESS,
    STT_SUBMISSION_FAILED,
    STT_JOB_FAILED,
    STT_FINISHED,
    INDEXING_FINISHED,
    INDEXING_FAILED
}

