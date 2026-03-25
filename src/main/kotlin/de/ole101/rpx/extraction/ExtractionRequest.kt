package de.ole101.rpx.extraction

import java.nio.file.Path

data class ExtractionRequest(
    val source: ExtractionSource,
    val destination: Path
)

sealed interface ExtractionSource {
    val path: Path

    data class Archive(override val path: Path) : ExtractionSource

    data class Directory(override val path: Path) : ExtractionSource
}
