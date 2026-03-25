package de.ole101.rpx.client.controller

import de.ole101.rpx.client.state.ExtractionState
import de.ole101.rpx.extraction.ExtractionEvent
import de.ole101.rpx.extraction.ExtractionService
import de.ole101.rpx.util.Logger
import de.ole101.rpx.util.ResourcePackUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.server.packs.repository.Pack
import java.io.File

class ExtractionController(
    private val minecraft: Minecraft,
    private val runDirectory: File,
    private val extractionState: ExtractionState,
    private val onStateChanged: () -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var extractionJob: Job? = null

    var lastExtractionDir: File? = null
        private set

    fun startExtraction(pack: Pack) {
        cancelCurrentExtraction()
        extractionState.reset()

        val request = try {
            ResourcePackUtil.resolveExtractionRequest(pack, runDirectory)
        } catch (exception: Exception) {
            Logger.error("Failed to prepare extraction", exception)
            extractionState.updateFromEvent(ExtractionEvent.Error(exception))
            onStateChanged()
            return
        }

        lastExtractionDir = request.destination.toFile()
        onStateChanged()

        extractionJob = scope.launch {
            try {
                ExtractionService.extract(request).collect { event ->
                    minecraft.execute {
                        extractionState.updateFromEvent(event)
                        onStateChanged()
                    }
                }
            } catch (_: CancellationException) {
                Logger.debug("Extraction cancelled")
            } catch (exception: Exception) {
                Logger.error("Extraction failed", exception)
                minecraft.execute {
                    extractionState.updateFromEvent(ExtractionEvent.Error(exception))
                    onStateChanged()
                }
            }
        }
    }

    fun dispose() {
        cancelCurrentExtraction()
        scope.cancel()
    }

    private fun cancelCurrentExtraction() {
        extractionJob?.cancel()
        extractionJob = null
    }
}
