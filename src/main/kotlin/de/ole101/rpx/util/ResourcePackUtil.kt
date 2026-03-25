package de.ole101.rpx.util

import de.ole101.rpx.exception.InvalidResourcePackException
import de.ole101.rpx.exception.ResourcePackNotFoundException
import de.ole101.rpx.extraction.ExtractionRequest
import de.ole101.rpx.extraction.ExtractionSource
import net.minecraft.server.packs.FilePackResources
import net.minecraft.server.packs.PathPackResources
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import java.io.File
import java.nio.file.Path

object ResourcePackUtil {
    private val packIdPattern = Regex("([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$")

    fun resolveExtractionRequest(pack: Pack, runDirectory: File): ExtractionRequest {
        return ExtractionRequest(
            source = resolveSource(pack, runDirectory.toPath()),
            destination = getExtractionDirectory(pack, runDirectory).toPath()
        )
    }

    fun getExtractionDirectory(pack: Pack, runDirectory: File): File {
        return runDirectory.resolve("extracted").resolve(getStorageId(pack))
    }

    fun filterExtractableProfiles(packs: Collection<Pack>): List<Pack> {
        return packs.filter { pack ->
            pack.packSource == PackSource.SERVER || pack.packSource == PackSource.WORLD
        }
    }

    fun getSafeDisplayName(pack: Pack): String {
        return try {
            pack.title.string
        } catch (_: Throwable) {
            pack.toString()
        }
    }

    private fun resolveSource(pack: Pack, runDirectory: Path): ExtractionSource {
        try {
            pack.open().use { resources ->
                when (resources) {
                    is FilePackResources -> return ExtractionSource.Archive(getArchivePath(resources))
                    is PathPackResources -> return ExtractionSource.Directory(getDirectoryPath(resources))
                }
            }
        } catch (exception: Exception) {
            Logger.warn("Failed to resolve pack source via pack resources for ${pack.id}", exception)
        }

        return resolveDownloadedArchive(pack, runDirectory)
    }

    private fun resolveDownloadedArchive(pack: Pack, runDirectory: Path): ExtractionSource.Archive {
        val storageId = packIdPattern.find(pack.id)?.groupValues?.get(1)
            ?: throw ResourcePackNotFoundException(pack.id)
        val sourceDirectory = runDirectory.resolve("downloads").resolve(storageId).toFile()
        val sourceFile = sourceDirectory.listFiles()
            ?.filter(File::isFile)
            ?.maxByOrNull(File::lastModified)
            ?: throw ResourcePackNotFoundException(pack.id)
        return ExtractionSource.Archive(sourceFile.toPath())
    }

    private fun getStorageId(pack: Pack): String {
        return packIdPattern.find(pack.id)?.groupValues?.get(1)
            ?: pack.id.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .trim('_')
                .ifBlank { "resource-pack" }
    }

    private fun getArchivePath(resources: FilePackResources): Path {
        val zipAccessField = FilePackResources::class.java.getDeclaredField("zipFileAccess")
        zipAccessField.isAccessible = true

        val zipAccess = zipAccessField.get(resources)
            ?: throw InvalidResourcePackException("Archive resource is unavailable")

        val fileField = zipAccess.javaClass.getDeclaredField("file")
        fileField.isAccessible = true

        val archiveFile = fileField.get(zipAccess) as? File
            ?: throw InvalidResourcePackException("Archive resource is unavailable")

        return archiveFile.toPath()
    }

    private fun getDirectoryPath(resources: PathPackResources): Path {
        val rootField = PathPackResources::class.java.getDeclaredField("root")
        rootField.isAccessible = true

        return rootField.get(resources) as? Path
            ?: throw InvalidResourcePackException("Directory resource is unavailable")
    }
}
