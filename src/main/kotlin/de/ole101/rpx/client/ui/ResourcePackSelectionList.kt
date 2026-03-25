package de.ole101.rpx.client.ui

import de.ole101.rpx.util.ResourcePackUtil.getSafeDisplayName
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.repository.Pack

@Environment(EnvType.CLIENT)
class ResourcePackSelectionList(
    minecraft: Minecraft,
    width: Int,
    height: Int,
    y: Int,
    private val onSelectionChanged: () -> Unit
) : ObjectSelectionList<ResourcePackSelectionList.Entry>(minecraft, width, height, y, ROW_HEIGHT) {

    private var selectedPack: Pack? = null

    fun setPacks(packs: List<Pack>, preferredSelection: Pack? = null) {
        selectedPack = preferredSelection
        replaceEntries(packs.map(::Entry))

        val selectedEntry = children().firstOrNull { it.pack == preferredSelection }
        super.setSelected(selectedEntry)
        selectedPack = selectedEntry?.pack
    }

    fun getSelectedPack(): Pack? = selectedPack

    override fun getRowWidth(): Int = ROW_WIDTH

    override fun scrollBarX(): Int = getRowRight() - 6

    override fun setSelected(entry: Entry?) {
        super.setSelected(entry)
        selectedPack = entry?.pack
        onSelectionChanged()
    }

    @Environment(EnvType.CLIENT)
    inner class Entry(val pack: Pack) : ObjectSelectionList.Entry<Entry>() {
        override fun renderContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, hovered: Boolean, deltaTicks: Float) {
            graphics.drawString(
                minecraft.font,
                Component.literal(getSafeDisplayName(pack)),
                contentX + 4,
                contentY + 5,
                -1
            )
        }

        override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
            if (event.button() != 0) {
                return false
            }

            this@ResourcePackSelectionList.setSelected(this)
            return true
        }

        override fun getNarration(): Component = pack.title
    }

    companion object {
        private const val ROW_HEIGHT = 18
        private const val ROW_WIDTH = 300
    }
}
