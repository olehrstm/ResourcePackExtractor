package de.ole101.rpx.client.state

import de.ole101.rpx.extraction.ExtractionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component.translatable
import kotlin.math.max

class ExtractionState {
    private val _uiState = MutableStateFlow(ExtractionUiState())
    val uiState: StateFlow<ExtractionUiState> = _uiState.asStateFlow()

    fun updateFromEvent(event: ExtractionEvent) {
        _uiState.update { currentState ->
            when (event) {
                is ExtractionEvent.Started -> currentState.copy(
                    isExtracting = true,
                    isCompleted = false,
                    extractedBytes = 0,
                    totalBytes = event.totalBytes,
                    currentFile = null,
                    message = translatable("rpx.status.starting"),
                    error = null
                )

                is ExtractionEvent.Progress -> currentState.copy(
                    extractedBytes = event.extractedBytes,
                    currentFile = event.fileName,
                    message = translatable("rpx.status.extracting", Component.literal(shorten(event.fileName))),
                    error = null
                )

                is ExtractionEvent.Error -> currentState.copy(
                    isExtracting = false,
                    currentFile = null,
                    message = null,
                    error = translatable("rpx.status.error", event.exception.message ?: translatable("rpx.error.unknown"))
                )

                is ExtractionEvent.Completed -> currentState.copy(
                    isExtracting = false,
                    isCompleted = true,
                    extractedBytes = event.extractedBytes,
                    totalBytes = max(currentState.totalBytes, event.extractedBytes),
                    currentFile = null,
                    message = completedMessage(event.extractedEntries, event.skippedEntries),
                    error = null
                )
            }
        }
    }

    fun reset() {
        _uiState.value = ExtractionUiState()
    }

    private fun completedMessage(extractedEntries: Int, skippedEntries: Int): Component {
        return if (skippedEntries > 0) {
            translatable("rpx.status.completed_with_skips", extractedEntries, skippedEntries)
        } else {
            translatable("rpx.status.completed", extractedEntries)
        }
    }

    private fun shorten(path: String): String = path.substringAfterLast('/').substringAfterLast('\\').take(60)
}
