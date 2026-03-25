package de.ole101.rpx.client.ui

import de.ole101.rpx.util.ResourcePackUtil.getSafeDisplayName
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component.literal
import net.minecraft.server.packs.repository.Pack

class ResourcePackListRenderer {

    fun render(
        graphics: GuiGraphics,
        font: Font,
        packs: List<Pack>,
        selectedIndex: Int,
        width: Int,
        height: Int
    ) {
        val top = 30
        val bottom = height - 40
        val left = width / 2 - 150
        val right = width / 2 + 150

        // background
        graphics.fill(left - 2, top - 2, right + 2, bottom + 2, 0x88000000.toInt())

        packs.forEachIndexed { index, profile ->
            val rowTop = top + index * 14
            if (rowTop + 14 > bottom) return@forEachIndexed

            // current selection
            if (index == selectedIndex) {
                graphics.fill(left, rowTop, right, rowTop + 14, 0x55FFFFFF)
            }

            val name: Component = literal(getSafeDisplayName(profile))
            graphics.drawString(font, name, left + 4, rowTop + 3, -1)
        }
    }

    fun getHoveredIndex(mouseX: Int, mouseY: Int, width: Int, profileCount: Int): Int {
        val top = 30
        val left = width / 2 - 150
        val right = width / 2 + 150

        if (mouseX !in left..right || mouseY < top) return -1

        val index = (mouseY - top) / 14
        return if (index in 0 until profileCount) index else -1
    }
}
