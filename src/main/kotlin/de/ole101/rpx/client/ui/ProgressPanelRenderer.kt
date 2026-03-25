package de.ole101.rpx.client.ui

import de.ole101.rpx.util.formatFileSize
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component.literal
import net.minecraft.util.FormattedCharSequence
import java.io.File
import kotlin.math.roundToInt

class ProgressPanelRenderer {
    companion object {
        private const val MAX_STATUS_LINES = 2
    }

    fun render(
        graphics: GuiGraphics,
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

        val panelLeft = width / 2 - 150
        val panelRight = width / 2 + 150
        val contentWidth = panelRight - panelLeft - 20
        val messageLines = (error ?: message)
            ?.let { font.split(it, contentWidth) }
            .orEmpty()
            .take(MAX_STATUS_LINES)
        val visibleLines: List<FormattedCharSequence> = messageLines
        val panelHeight = if (visibleLines.size > 1) 58 else 46
        val panelTop = height - 70 - panelHeight

        // background
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + panelHeight, 0xAA000000.toInt())

        val statusY = panelTop + 6
        val progressBarY = panelTop + 12 + visibleLines.size * font.lineHeight
        val percentY = progressBarY + 12

        val textColor = if (error != null) 0xFFFF5555.toInt() else 0xFFFFFFFF.toInt()
        visibleLines.forEachIndexed { index, line ->
            graphics.drawCenteredString(font, line, width / 2, statusY + index * font.lineHeight, textColor)
        }

        if (totalBytes > 0) {
            renderProgressBar(graphics, panelLeft, panelRight, progressBarY, extractedBytes, totalBytes)
        }

        if (totalBytes > 0 || extractedBytes > 0) {
            renderProgressText(graphics, font, width / 2, percentY, extractedBytes, totalBytes)
        }

        if (currentFile != null && error == null && !isCompleted) {
            renderCurrentFile(graphics, font, width / 2, panelTop + panelHeight - 10, currentFile)
        }
    }

    private fun renderProgressBar(
        graphics: GuiGraphics,
        panelLeft: Int,
        panelRight: Int,
        progressBarY: Int,
        extractedBytes: Long,
        totalBytes: Long
    ) {
        val barLeft = panelLeft + 10
        val barRight = panelRight - 10
        val barBottom = progressBarY + 8

        // background
        graphics.fill(barLeft, progressBarY, barRight, barBottom, 0xFF222222.toInt())

        // fill
        val fraction = if (totalBytes > 0) (extractedBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0) else 0.0
        val filled = barLeft + ((barRight - barLeft) * fraction).roundToInt()
        graphics.fill(barLeft, progressBarY, filled, barBottom, 0xFF55AA55.toInt())
    }

    private fun renderProgressText(
        graphics: GuiGraphics,
        font: Font,
        centerX: Int,
        y: Int,
        extractedBytes: Long,
        totalBytes: Long
    ) {
        val fraction = if (totalBytes > 0) extractedBytes.toDouble() / totalBytes.toDouble() else 0.0
        val percentText = if (totalBytes > 0) {
            "${(fraction * 100).roundToInt()}% (${formatFileSize(extractedBytes)} / ${formatFileSize(totalBytes)})"
        } else {
            formatFileSize(extractedBytes)
        }
        graphics.drawCenteredString(font, literal(percentText), centerX, y, 0xA0FFFFFF.toInt())
    }

    private fun renderCurrentFile(
        graphics: GuiGraphics,
        font: Font,
        centerX: Int,
        y: Int,
        currentFile: String
    ) {
        val separator = File.separatorChar
        val filename = currentFile.substringAfterLast(separator).substringAfterLast('/')
        val truncatedFilename = filename.take(40)
        graphics.drawCenteredString(font, literal(truncatedFilename), centerX, y, 0x80FFFFFF.toInt())
    }
}
