package com.iodice.mediasearch.model

import com.google.gson.annotations.SerializedName

class STTException(msg: String) : Exception(msg)

open class STTStatus
class STTSuccess(val resultsUrls: List<String>): STTStatus()
class STTFailed(val message: String): STTStatus()
class STTInProgress: STTStatus()

data class TranscriptionDefinition(
        val recordingsUrl: String,
        val locale: String,
        val name: String
)


enum class TranscriptionStatus {
    @SerializedName("NotStarted") NOT_STARTED,
    @SerializedName("Running") RUNNING,
    @SerializedName("Succeeded") SUCCEEDED,
    @SerializedName("Failed") FAILED,
}

data class Transcription (
        val resultsUrls: Map<String, String>? = null,
        val statusMessage: String? = null,
        val status: TranscriptionStatus? = null
)