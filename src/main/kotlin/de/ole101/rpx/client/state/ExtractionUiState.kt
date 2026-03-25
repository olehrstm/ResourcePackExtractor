package de.ole101.rpx.client.state

import net.minecraft.network.chat.Component

data class ExtractionUiState(
    val isExtracting: Boolean = false,
    val isCompleted: Boolean = false,
    val extractedBytes: Long = 0,
    val totalBytes: Long = 0,
    val currentFile: String? = null,
    val message: Component? = null,
    val error: Component? = null
)
