package com.iodice.mediasearch.model

import com.google.gson.annotations.SerializedName


data class SearchIndexRequest(
        @SerializedName("value")
        val indices: List<Index>
)

data class Index(
        @SerializedName("@search.action")
        val action: Action,
        val sourceId: String,
        val mediaId: String,
        val offset: Double,
        val duration: Double,
        val content: String
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