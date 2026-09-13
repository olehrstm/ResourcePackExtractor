package de.ole101.rpx.client.ui

import de.ole101.rpx.util.formatFileSize
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component.literal
import net.minecraft.network.chat.Component.translatable
import kotlin.math.roundToInt

class ProgressPanelRenderer {
    fun render(
        graphics: GuiGraphicsExtractor,
        font: Font,
        width: Int,
        height: Int,
        isExtracting: Boolean,
        isCompleted: Boolean,
        extractedBytes: Long,
        totalBytes: Long,
        currentFile: String?,
        message: Component?,
        error: Component?
    ) {
        if (!isExtracting && !isCompleted && error == null) return

        val panelWidth = minOf(320, width - 12 * 2)
        val panelLeft = (width - panelWidth) / 2
        val contentWidth = panelWidth - 9 * 2
        val statusLines = (error ?: message)
            ?.let { font.split(it, contentWidth) }
            .orEmpty()
            .take(3)
        val showProgressBar = totalBytes > 0
        val showCurrentFile = currentFile != null && error == null && !isCompleted
        val panelHeight = 18 +
                statusLines.size * font.lineHeight +
                (if (statusLines.isNotEmpty()) 6 else 0) +
                (if (showProgressBar) 10 else 0) +
                font.lineHeight +
                (if (showCurrentFile) font.lineHeight + 4 else 0)
        val panelBottom = height - 70
        val panelTop = panelBottom - panelHeight
        val accentColor = when {
            error != null -> 0xFFFF5555.toInt()
            isCompleted -> 0xFF55FF55.toInt()
            else -> 0xFF55AA55.toInt()
        }

        renderPanel(graphics, panelLeft, panelTop, panelWidth, panelHeight, accentColor)

        var cursorY = panelTop + 9
        val textColor = if (error != null) 0xFFFF5555.toInt() else 0xFFFFFFFF.toInt()
        statusLines.forEachIndexed { index, line ->
            graphics.centeredText(font, line, width / 2, cursorY + index * font.lineHeight, textColor)
        }
        cursorY += statusLines.size * font.lineHeight

        if (statusLines.isNotEmpty()) {
            cursorY += 6
        }

        if (showProgressBar) {
            renderProgressBar(graphics, panelLeft + 10, cursorY, panelWidth - 20, extractedBytes, totalBytes)
            cursorY += 10
        }

        renderProgressText(graphics, font, width / 2, cursorY, extractedBytes, totalBytes)

        if (showCurrentFile) {
            cursorY += font.lineHeight + 4
            renderCurrentFile(graphics, font, width / 2, cursorY, contentWidth, currentFile)
        }
    }

    private fun renderPanel(
        graphics: GuiGraphicsExtractor,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        accentColor: Int
    ) {
        graphics.fill(left + 2, top + 2, left + width + 2, top + height + 2, 0x66000000)
        graphics.fill(left, top, left + width, top + height, 0xE61A1A1A.toInt())
        graphics.outline(left, top, width, height, 0xFF555555.toInt())
        graphics.fill(left + 1, top + 1, left + width - 1, top + 3, accentColor)
    }

    private fun renderProgressBar(
        graphics: GuiGraphicsExtractor,
        left: Int,
        y: Int,
        width: Int,
        extractedBytes: Long,
        totalBytes: Long
    ) {
        val progress = (extractedBytes.toDouble() / totalBytes).coerceIn(0.0, 1.0)
        graphics.fill(left, y, left + width, y + 6, 0xFF303030.toInt())
        graphics.fill(left, y, left + (width * progress).roundToInt(), y + 6, 0xFF55AA55.toInt())
    }

    private fun renderProgressText(
        graphics: GuiGraphicsExtractor,
        font: Font,
        centerX: Int,
        y: Int,
        extractedBytes: Long,
        totalBytes: Long
    ) {
        val progressText = if (totalBytes > 0) {
            translatable(
                "rpx.status.progress",
                (extractedBytes.toDouble() / totalBytes * 100).roundToInt().coerceIn(0, 100),
                formatFileSize(extractedBytes),
                formatFileSize(totalBytes)
            )
        } else {
            translatable("rpx.status.bytes_extracted", formatFileSize(extractedBytes))
        }
        graphics.centeredText(font, progressText, centerX, y, 0xFFA0A0A0.toInt())
    }

    private fun renderCurrentFile(
        graphics: GuiGraphicsExtractor,
        font: Font,
        centerX: Int,
        y: Int,
        maxWidth: Int,
        currentFile: String
    ) {
        val filename = currentFile.substringAfterLast('/').substringAfterLast('\\')
        val ellipsis = "..."
        val fittedFilename = if (font.width(filename) <= maxWidth) {
            filename
        } else {
            font.plainSubstrByWidth(filename, maxWidth - font.width(ellipsis)) + ellipsis
        }
        graphics.centeredText(font, literal(fittedFilename), centerX, y, 0xFFA0A0A0.toInt())
    }
}
