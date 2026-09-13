package de.ole101.rpx.extraction

import de.ole101.rpx.exception.ExtractionDirectoryException
import de.ole101.rpx.exception.InvalidResourcePackException
import de.ole101.rpx.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

object ExtractionService {
    private const val BUFFER_SIZE = 4096

    fun extract(request: ExtractionRequest): Flow<ExtractionEvent> = when (val source = request.source) {
        is ExtractionSource.Archive -> extractArchive(source.path, request.destination)
        is ExtractionSource.Directory -> copyDirectory(source.path, request.destination)
    }

    private fun extractArchive(sourceFile: Path, destinationDirectory: Path): Flow<ExtractionEvent> = flow {
        validateArchive(sourceFile)
        prepareDestinationDirectory(destinationDirectory)

        ZipFile(sourceFile.toFile()).use { zipFile ->
            emit(ExtractionEvent.Started(0L))

            var extractedBytes = 0L
            var extractedEntries = 0
            var skippedEntries = 0

            zipFile.entries().asSequence().forEach { zipEntry ->
                currentCoroutineContext().ensureActive()

                val outputPath = resolveDestinationPath(destinationDirectory, zipEntry.name)
                if (outputPath == null) {
                    skippedEntries++
                    Logger.warn("Skipping unsafe zip entry ${zipEntry.name}")
                    return@forEach
                }

                if (zipEntry.isDirectory) {
                    Files.createDirectories(outputPath)
                    return@forEach
                }

                try {
                    Files.createDirectories(outputPath.parent)
                    zipFile.getInputStream(zipEntry).use { inputStream ->
                        extractedBytes += writeFile(inputStream, outputPath)
                    }
                    extractedEntries++
                    emit(ExtractionEvent.Progress(extractedBytes, zipEntry.name))
                } catch (exception: Exception) {
                    skippedEntries++
                    Logger.warn("Skipping unreadable zip entry ${zipEntry.name}", exception)
                }
            }

            emit(ExtractionEvent.Completed(extractedBytes, extractedEntries, skippedEntries))
        }
    }.flowOn(Dispatchers.IO)

    private fun copyDirectory(sourceDirectory: Path, destinationDirectory: Path): Flow<ExtractionEvent> = flow {
        validateDirectory(sourceDirectory)
        prepareDestinationDirectory(destinationDirectory)

        Files.walk(sourceDirectory).use { paths ->
            val sourceFiles = paths.asSequence()
                .filter { it.isRegularFile() }
                .toList()

            emit(ExtractionEvent.Started(sourceFiles.sumOf(Files::size)))

            var extractedBytes = 0L
            var extractedEntries = 0
            var skippedEntries = 0

            sourceFiles.forEach { sourceFile ->
                currentCoroutineContext().ensureActive()

                val relativePath = sourceDirectory.relativize(sourceFile)
                val outputPath = resolveDestinationPath(destinationDirectory, relativePath.toString())
                if (outputPath == null) {
                    skippedEntries++
                    Logger.warn("Skipping unsafe source entry $relativePath")
                    return@forEach
                }

                try {
                    Files.createDirectories(outputPath.parent)
                    Files.newInputStream(sourceFile).use { inputStream ->
                        extractedBytes += writeFile(inputStream, outputPath)
                    }
                    extractedEntries++
                    emit(ExtractionEvent.Progress(extractedBytes, relativePath.toString()))
                } catch (exception: Exception) {
                    skippedEntries++
                    Logger.warn("Skipping unreadable source entry $relativePath", exception)
                }
            }

            emit(ExtractionEvent.Completed(extractedBytes, extractedEntries, skippedEntries))
        }
    }.flowOn(Dispatchers.IO)

    private fun validateArchive(sourceFile: Path) {
        if (!sourceFile.exists()) {
            throw InvalidResourcePackException("Source file does not exist: $sourceFile")
        }
        if (!sourceFile.isRegularFile()) {
            throw InvalidResourcePackException("Source is not a file: $sourceFile")
        }
        if (Files.size(sourceFile) == 0L) {
            throw InvalidResourcePackException("Source file is empty: $sourceFile")
        }
    }

    private fun validateDirectory(sourceDirectory: Path) {
        if (!sourceDirectory.exists()) {
            throw InvalidResourcePackException("Source directory does not exist: $sourceDirectory")
        }
        if (!sourceDirectory.isDirectory()) {
            throw InvalidResourcePackException("Source is not a directory: $sourceDirectory")
        }
    }

    private fun prepareDestinationDirectory(destinationDirectory: Path) {
        try {
            val normalizedDestination = destinationDirectory.toAbsolutePath().normalize()
            if (Files.exists(normalizedDestination)) {
                val cleaned = normalizedDestination.toFile().deleteRecursively()
                if (!cleaned && Files.exists(normalizedDestination)) {
                    throw ExtractionDirectoryException(normalizedDestination.toString())
                }
            }
            Files.createDirectories(normalizedDestination)
        } catch (exception: Exception) {
            throw ExtractionDirectoryException(destinationDirectory.toString(), exception)
        }
    }

    private fun resolveDestinationPath(destinationRoot: Path, entryName: String): Path? {
        val normalizedRoot = destinationRoot.toAbsolutePath().normalize()
        val normalizedPath = normalizedRoot.resolve(entryName).normalize()
        return normalizedPath.takeIf { it.startsWith(normalizedRoot) }
    }

    private fun writeFile(inputStream: InputStream, targetFile: Path): Long {
        val extractedBytes = Files.newOutputStream(targetFile).use { output ->
            inputStream.copyTo(output, BUFFER_SIZE)
        }

        try {
            if (PngCrcRepairer.repair(targetFile) == PngCrcRepairer.Result.REPAIRED) {
                Logger.debug("Repaired invalid PNG chunk CRCs in $targetFile")
            }
        } catch (exception: Exception) {
            Logger.warn("Could not repair PNG chunk CRCs in $targetFile; preserving extracted bytes", exception)
        }

        return extractedBytes
    }
}
