package de.ole101.rpx.extraction

sealed class ExtractionEvent {
    data class Started(val totalBytes: Long) : ExtractionEvent()
    data class Progress(val extractedBytes: Long, val fileName: String) : ExtractionEvent()
    data class Error(val exception: Exception) : ExtractionEvent()
    data class Completed(val extractedBytes: Long, val extractedEntries: Int, val skippedEntries: Int) : ExtractionEvent()
}
