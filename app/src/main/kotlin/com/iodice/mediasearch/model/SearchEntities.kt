package com.iodice.mediasearch.model

import com.google.gson.annotations.SerializedName


data class SearchIndexRequest(
        @SerializedName("value")
        val indexOperations: List<IndexOperation>
)

data class IndexOperation(
        @SerializedName("@search.action")
        val action: Action,
        val id: String,
        val sourceId: String,
        val mediaId: String,
        val offset: Double,
        val duration: Double,
        val content: String
)

data class Index(
        @SerializedName("@search.score")
        val score: Double,
        val sourceId: String,
        val mediaId: String,
        val offset: Double,
        val duration: Double,
        val content: String
)

data class SearchIndexResponse(
        @SerializedName("value")
        val indices: List<Index>
)


enum class Action {
    @SerializedName("upload")
    UPLOAD,
    @SerializedName("merge")
    MERGE,
    @SerializedName("mergeOrUpload")
    MERGE_OR_UPLOAD,
    @SerializedName("delete")
    DELETE,
}