package de.ole101.rpx.client.screen

import de.ole101.rpx.client.state.ExtractionState
import de.ole101.rpx.client.ui.ProgressPanelRenderer
import de.ole101.rpx.client.ui.ResourcePackListRenderer
import de.ole101.rpx.coroutineScope
import de.ole101.rpx.extraction.ExtractionEvent
import de.ole101.rpx.util.Logger
import de.ole101.rpx.util.ResourcePackUtil
import de.ole101.rpx.util.ZipUtil
import kotlinx.coroutines.launch
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component.translatable
import net.minecraft.server.packs.repository.Pack
import net.minecraft.util.Util
import java.io.File

@Environment(EnvType.CLIENT)
class ExtractPackScreen(private val parent: Screen) : Screen(translatable("rpx.pack.extract")) {

    private var serverPacks: List<Pack> = emptyList()
    private var selectedIndex: Int = -1
    private lateinit var gameDirectory: File

    private var lastExtractionDir: File? = null
    private var selectButton: Button? = null
    private var openFolderButton: Button? = null

    private var doneButton: Button? = null
    private val extractionState = ExtractionState()
    private val listRenderer = ResourcePackListRenderer()
    private val progressRenderer = ProgressPanelRenderer()

    override fun init() {
        initializeData()
        createButtons()
        updateButtonStates()
    }

    private fun initializeData() {
        gameDirectory = minecraft.gameDirectory

        serverPacks = minecraft.resourcePackRepository.selectedPacks.let { ResourcePackUtil.filterExtractableProfiles(it.toList()) }
        selectedIndex = -1
    }

    private fun createButtons() {
        val totalWidth = 120 * 3 + 8 * 2
        val y = height - 28
        val startX = width / 2 - totalWidth / 2

        selectButton = addRenderableWidget(
            Button.builder(translatable("rpx.pack.select")) { onSelectPressed() }
                .bounds(startX, y, 120, 20)
                .build()
        )

        openFolderButton = addRenderableWidget(
            Button.builder(translatable("rpx.pack.open_folder")) { openExtractionFolder() }
                .bounds(startX + 120 + 8, y, 120, 20)
                .build()
        )

        doneButton = addRenderableWidget(
            Button.builder(translatable("gui.done")) { onClose() }
                .bounds(startX + (120 + 8) * 2, y, 120, 20)
                .build()
        )
    }

    private fun onSelectPressed() {
        val selectedProfile = getSelectedPack() ?: return

        extractionState.reset()
        lastExtractionDir = ResourcePackUtil.getExtractionDirectory(selectedProfile, gameDirectory)

        coroutineScope.launch {
            try {
                val sourceFile = ResourcePackUtil.getSourceFile(selectedProfile, gameDirectory)
                val destDir = ResourcePackUtil.getExtractionDirectory(selectedProfile, gameDirectory)
                ZipUtil.extractZipFlow(sourceFile, destDir).collect { event ->
                    runOnClientThread {
                        extractionState.updateFromEvent(event)
                        updateButtonStates()
                    }
                }
            } catch (e: Exception) {
                Logger.error("Extraction failed", e)
                runOnClientThread {
                    extractionState.updateFromEvent(
                        ExtractionEvent.Error(e)
                    )
                    updateButtonStates()
                }
            }
        }
    }

    private fun openExtractionFolder() {
        val dir = lastExtractionDir ?: return
        if (!dir.exists()) dir.mkdirs()
        Util.getPlatform().openFile(dir)
    }

    private fun runOnClientThread(action: () -> Unit) {
        minecraft.execute(action)
    }

    private fun getSelectedPack(): Pack? =
        if (selectedIndex in serverPacks.indices) serverPacks[selectedIndex] else null

    private fun updateButtonStates() {
        selectButton?.active = selectedIndex >= 0 && !extractionState.isExtracting.value
        openFolderButton?.active = lastExtractionDir != null &&
                (extractionState.isExtracting.value || extractionState.isCompleted.value || extractionState.error.value != null)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)

        context.drawCenteredString(font, title, width / 2, 15, -1)

        // resource pack list
        listRenderer.render(context, font, serverPacks, selectedIndex, width, height)

        // progress
        progressRenderer.render(
            context, font, width, height,
            extractionState.isExtracting.value,
            extractionState.isCompleted.value,
            extractionState.extractedBytes.value,
            extractionState.totalBytes.value,
            extractionState.currentFile.value,
            extractionState.message.value,
            extractionState.error.value
        )
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (extractionState.isExtracting.value) return true // block interaction while extracting

        val hovered = listRenderer.getHoveredIndex(event.x.toInt(), event.y.toInt(), width, serverPacks.size)
        if (hovered in serverPacks.indices) {
            selectedIndex = hovered
            updateButtonStates()
            return true
        }

        return super.mouseClicked(event, doubled)
    }

    override fun resize(width: Int, height: Int) {
        val previouslySelected = getSelectedPack()
        super.resize(width, height)

        serverPacks = minecraft.resourcePackRepository.selectedPacks
            .let { ResourcePackUtil.filterExtractableProfiles(it.toList()) }
        selectedIndex = serverPacks.indexOf(previouslySelected)
        updateButtonStates()
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }
}
