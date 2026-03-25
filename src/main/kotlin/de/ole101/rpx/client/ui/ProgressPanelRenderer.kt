package de.ole101.rpx.client.ui

import de.ole101.rpx.util.formatFileSize
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component.literal
import java.io.File
import kotlin.math.roundToInt

class ProgressPanelRenderer {

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

        val panelTop = height - 70 - 46
        val panelLeft = width / 2 - 150
        val panelRight = width / 2 + 150

        // background
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 46, 0xAA000000.toInt())

        val statusY = panelTop + 6
        val progressBarY = panelTop + 20
        val percentY = progressBarY + 12

        val displayMessage = error ?: message
        val textColor = if (error != null) 0xFFFF5555.toInt() else 0xFFFFFFFF.toInt()
        if (displayMessage != null) {
            graphics.drawCenteredString(font, displayMessage, width / 2, statusY, textColor)
        }

        if (totalBytes > 0) {
            renderProgressBar(graphics, panelLeft, panelRight, progressBarY, extractedBytes, totalBytes)
        }

        if (totalBytes > 0 || extractedBytes > 0) {
            renderProgressText(graphics, font, width / 2, percentY, extractedBytes, totalBytes)
        }

        if (currentFile != null && error == null && !isCompleted) {
            renderCurrentFile(graphics, font, width / 2, panelTop + 46 - 10, currentFile)
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
