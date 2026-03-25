package de.ole101.rpx.util

import de.ole101.rpx.exception.ResourcePackNotFoundException
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import java.io.File

object ResourcePackUtil {

    /**
     * Gets the short ID for a resource pack (last 36 characters).
     */
    fun getShortId(pack: Pack): String {
        return pack.id.takeLast(36)
    }

    /**
     * Gets the source file for a resource pack.
     */
    fun getSourceFile(pack: Pack, runDirectory: File): File {
        val id = getShortId(pack)
        val sourceDirectory = runDirectory.resolve("downloads").resolve(id)
        return sourceDirectory.listFiles()?.firstOrNull()
            ?: throw ResourcePackNotFoundException("No source file found for resource pack: $id")
    }

    /**
     * Gets the extraction directory for a resource pack.
     */
    fun getExtractionDirectory(pack: Pack, runDirectory: File): File {
        val id = getShortId(pack)
        return runDirectory.resolve("extracted").resolve(id)
    }

    /**
     * Filters resource pack profiles to only include server and world packs.
     */
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
}
