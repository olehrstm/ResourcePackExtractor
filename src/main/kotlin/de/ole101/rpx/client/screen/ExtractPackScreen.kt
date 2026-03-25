package de.ole101.rpx.client.screen

import de.ole101.rpx.client.controller.ExtractionController
import de.ole101.rpx.client.state.ExtractionState
import de.ole101.rpx.client.ui.ProgressPanelRenderer
import de.ole101.rpx.client.ui.ResourcePackSelectionList
import de.ole101.rpx.util.ResourcePackUtil
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component.translatable
import net.minecraft.server.packs.repository.Pack
import net.minecraft.util.Util
import java.io.File
import java.nio.file.Files

@Environment(EnvType.CLIENT)
class ExtractPackScreen(private val parent: Screen) : Screen(translatable("rpx.pack.extract")) {

    private var serverPacks: List<Pack> = emptyList()
    private lateinit var gameDirectory: File
    private lateinit var extractionController: ExtractionController
    private lateinit var selectionList: ResourcePackSelectionList

    private var selectButton: Button? = null
    private var openFolderButton: Button? = null
    private val extractionState = ExtractionState()
    private val progressRenderer = ProgressPanelRenderer()

    override fun init() {
        initializeData()
        createSelectionList()
        createButtons()
        updateButtonStates()
    }

    private fun initializeData() {
        gameDirectory = minecraft.gameDirectory
        if (!::extractionController.isInitialized) {
            extractionController = ExtractionController(minecraft, gameDirectory, extractionState, ::updateButtonStates)
        }

        serverPacks = minecraft.resourcePackRepository.selectedPacks.let { ResourcePackUtil.filterExtractableProfiles(it.toList()) }
    }

    private fun createSelectionList() {
        val previousSelection = if (::selectionList.isInitialized) selectionList.getSelectedPack() else null
        selectionList = addRenderableWidget(
            ResourcePackSelectionList(minecraft, width, height - 70, 30, ::updateButtonStates)
        )
        selectionList.setPacks(serverPacks, previousSelection)
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

        addRenderableWidget(
            Button.builder(translatable("gui.done")) { onClose() }
                .bounds(startX + (120 + 8) * 2, y, 120, 20)
                .build()
        )
    }

    private fun onSelectPressed() {
        val selectedProfile = getSelectedPack() ?: return
        extractionController.startExtraction(selectedProfile)
    }

    private fun openExtractionFolder() {
        val dir = extractionController.lastExtractionDir ?: return
        Files.createDirectories(dir.toPath())
        Util.getPlatform().openFile(dir)
    }

    private fun getSelectedPack(): Pack? = selectionList.getSelectedPack()

    private fun updateButtonStates() {
        val uiState = extractionState.uiState.value
        selectButton?.active = getSelectedPack() != null && !uiState.isExtracting
        openFolderButton?.active = extractionController.lastExtractionDir != null && !uiState.isExtracting &&
                (uiState.isCompleted || uiState.error != null)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)
        val uiState = extractionState.uiState.value

        context.drawCenteredString(font, title, width / 2, 15, -1)
        progressRenderer.render(
            context, font, width, height,
            uiState.isExtracting,
            uiState.isCompleted,
            uiState.extractedBytes,
            uiState.totalBytes,
            uiState.currentFile,
            uiState.message,
            uiState.error
        )
    }

    override fun resize(width: Int, height: Int) {
        val previouslySelected = getSelectedPack()
        super.resize(width, height)

        serverPacks = minecraft.resourcePackRepository.selectedPacks
            .let { ResourcePackUtil.filterExtractableProfiles(it.toList()) }
        selectionList.setPacks(serverPacks, previouslySelected)
        updateButtonStates()
    }

    override fun removed() {
        extractionController.dispose()
        super.removed()
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }
}
