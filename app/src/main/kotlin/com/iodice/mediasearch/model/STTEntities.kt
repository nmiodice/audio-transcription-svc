package com.iodice.mediasearch.model

import com.google.gson.annotations.SerializedName

class STTException(msg: String) : Exception(msg)

open class STTStatus
class STTSuccess(val resultsUrls: List<String>) : STTStatus()
class STTFailed(val message: String) : STTStatus()
class STTInProgress : STTStatus()

enum class ProfanityFilterMode {
    @SerializedName("None")
    NONE,

    @SerializedName("Removed")
    REMOVED,

    @SerializedName("Tags")
    TAGS,

    @SerializedName("Masked")
    MASKED,
}

data class TranscriptionProperties(
        @SerializedName("ProfanityFilterMode")
        val profanityFilterMode: ProfanityFilterMode = ProfanityFilterMode.NONE,
        @SerializedName("AddWordLevelTimestamps")
        val wordLevelTimestamps: Boolean = true
)

data class TranscriptionDefinition(
        val recordingsUrl: String,
        val locale: String,
        val name: String,
        val properties: TranscriptionProperties = TranscriptionProperties()
)


enum class TranscriptionStatus {
    @SerializedName("NotStarted")
    NOT_STARTED,

    @SerializedName("Running")
    RUNNING,

    @SerializedName("Succeeded")
    SUCCEEDED,

    @SerializedName("Failed")
    FAILED,
}

data class TranscriptionReference(
        val resultsUrls: Map<String, String>? = null,
        val statusMessage: String? = null,
        val status: TranscriptionStatus? = null
)

data class TranscriptionResult(
        val AudioFileResults: List<AudioFileResult>
)

data class AudioFileResult(
        val AudioLengthInSeconds: Double,
        val SegmentResults: List<SegmentResult>
)

data class SegmentResult(
        val DurationInSeconds: Double,
        val NBest: List<NBest>,
        val OffsetInSeconds: Double,
        val RecognitionStatus: String
)

data class Word(
        val Word: String,
        val OffsetInSeconds: Double,
        val DurationInSeconds: Double
)

data class NBest(
        val Confidence: Double,
        val Display: String,
        val ITN: String,
        val Lexical: String,
        val MaskedITN: String,
        val Words: List<Word>
)