package de.ole101.rpx.exception

sealed class RPXException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ResourcePackNotFoundException(@Suppress("UNUSED_PARAMETER") packId: String) :
    RPXException("Could not locate the downloaded files for the selected resource pack")

class ExtractionDirectoryException(directory: String, cause: Throwable? = null) :
    RPXException("Failed to create or access extraction directory: $directory", cause)

class InvalidResourcePackException(reason: String) :
    RPXException("Invalid resource pack: $reason")

class ExtractionFailedException(fileName: String, cause: Throwable) :
    RPXException("Failed to extract file: $fileName", cause)
