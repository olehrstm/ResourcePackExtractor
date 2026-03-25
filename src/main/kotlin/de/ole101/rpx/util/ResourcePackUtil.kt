package de.ole101.rpx.util

import de.ole101.rpx.exception.InvalidResourcePackException
import de.ole101.rpx.exception.ResourcePackNotFoundException
import de.ole101.rpx.extraction.ExtractionRequest
import de.ole101.rpx.extraction.ExtractionSource
import net.minecraft.server.packs.FilePackResources
import net.minecraft.server.packs.PathPackResources
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier
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
            resolveSourceFromSupplier(pack)?.let { return it }
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

    private fun resolveSourceFromSupplier(pack: Pack): ExtractionSource? {
        val supplier = findFieldValue(pack, Pack::class.java) { field -> ResourcesSupplier::class.java.isAssignableFrom(field.type) }
                as? ResourcesSupplier
            ?: return null

        val content = findFieldValue(supplier, supplier.javaClass) { field -> field.type == File::class.java || field.type == Path::class.java }
            ?: return null

        return when (content) {
            is File -> ExtractionSource.Archive(content.toPath())
            is Path -> if (content.toFile().isDirectory) ExtractionSource.Directory(content) else ExtractionSource.Archive(content)
            else -> null
        }
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
        val zipAccess = findFieldValue(resources, FilePackResources::class.java) { field ->
            !field.type.isPrimitive && field.type.declaredFields.any { nestedField -> nestedField.type == File::class.java }
        }
            ?: throw InvalidResourcePackException("Archive resource is unavailable")

        val archiveFile = findFieldValue(zipAccess, zipAccess.javaClass) { field -> field.type == File::class.java } as? File
            ?: throw InvalidResourcePackException("Archive resource is unavailable")

        return archiveFile.toPath()
    }

    private fun getDirectoryPath(resources: PathPackResources): Path {
        return findFieldValue(resources, PathPackResources::class.java) { field -> field.type == Path::class.java } as? Path
            ?: throw InvalidResourcePackException("Directory resource is unavailable")
    }

    private fun findFieldValue(instance: Any, type: Class<*>, predicate: (java.lang.reflect.Field) -> Boolean): Any? {
        var currentType: Class<*>? = type
        while (currentType != null) {
            val field = currentType.declaredFields.firstOrNull(predicate)
            if (field != null) {
                field.isAccessible = true
                return field.get(instance)
            }
            currentType = currentType.superclass
        }
        return null
    }
}
