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
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.resource.ResourcePackProfile
import net.minecraft.text.Text
import net.minecraft.util.Util
import java.io.File

@Environment(EnvType.CLIENT)
class ExtractPackScreen(private val parent: Screen) : Screen(Text.translatable("rpx.pack.extract")) {

    private var serverPackProfiles: List<ResourcePackProfile> = emptyList()
    private var selectedIndex: Int = -1
    private lateinit var runDirectory: File

    private var lastExtractionDir: File? = null
    private var selectButton: ButtonWidget? = null
    private var openFolderButton: ButtonWidget? = null

    private var doneButton: ButtonWidget? = null
    private val extractionState = ExtractionState()
    private val listRenderer = ResourcePackListRenderer()
    private val progressRenderer = ProgressPanelRenderer()

    override fun init() {
        initializeData()
        createButtons()
        updateButtonStates()
    }

    private fun initializeData() {
        runDirectory = client?.runDirectory ?: return

        serverPackProfiles = client?.resourcePackManager?.enabledProfiles
            ?.let { ResourcePackUtil.filterExtractableProfiles(it.toList()) }
            ?: emptyList()
        selectedIndex = -1
    }

    private fun createButtons() {
        val totalWidth = 120 * 3 + 8 * 2
        val y = height - 28
        val startX = width / 2 - totalWidth / 2

        selectButton = addDrawableChild(
            ButtonWidget.builder(Text.translatable("rpx.pack.select")) { onSelectPressed() }
                .dimensions(startX, y, 120, 20)
                .build()
        )

        openFolderButton = addDrawableChild(
            ButtonWidget.builder(Text.translatable("rpx.pack.open_folder")) { openExtractionFolder() }
                .dimensions(startX + 120 + 8, y, 120, 20)
                .build()
        )

        doneButton = addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.done")) { close() }
                .dimensions(startX + (120 + 8) * 2, y, 120, 20)
                .build()
        )
    }

    private fun onSelectPressed() {
        val selectedProfile = getSelectedPackProfile() ?: return

        extractionState.reset()
        lastExtractionDir = ResourcePackUtil.getExtractionDirectory(selectedProfile, runDirectory)

        coroutineScope.launch {
            try {
                val sourceFile = ResourcePackUtil.getSourceFile(selectedProfile, runDirectory)
                val destDir = ResourcePackUtil.getExtractionDirectory(selectedProfile, runDirectory)
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
        Util.getOperatingSystem().open(dir)
    }

    private fun runOnClientThread(action: () -> Unit) {
        client?.execute(action)
    }

    private fun getSelectedPackProfile(): ResourcePackProfile? =
        if (selectedIndex in serverPackProfiles.indices) serverPackProfiles[selectedIndex] else null

    private fun updateButtonStates() {
        selectButton?.active = selectedIndex >= 0 && !extractionState.isExtracting.value
        openFolderButton?.active = lastExtractionDir != null &&
                (extractionState.isExtracting.value || extractionState.isCompleted.value || extractionState.error.value != null)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)
        context ?: return

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, -1)

        // resource pack list
        listRenderer.render(context, textRenderer, serverPackProfiles, selectedIndex, width, height)

        // progress
        progressRenderer.render(
            context, textRenderer, width, height,
            extractionState.isExtracting.value,
            extractionState.isCompleted.value,
            extractionState.extractedBytes.value,
            extractionState.totalBytes.value,
            extractionState.currentFile.value,
            extractionState.message.value,
            extractionState.error.value
        )
    }

    override fun mouseClicked(click: Click?, doubled: Boolean): Boolean {
        if (click == null) return super.mouseClicked(click, doubled)

        if (extractionState.isExtracting.value) return true // block interaction while extracting

        val hovered = listRenderer.getHoveredIndex(click.x.toInt(), click.y.toInt(), width, serverPackProfiles.size)
        if (hovered in serverPackProfiles.indices) {
            selectedIndex = hovered
            updateButtonStates()
            return true
        }

        return super.mouseClicked(click, doubled)
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        val previouslySelected = getSelectedPackProfile()
        super.resize(client, width, height)

        serverPackProfiles = client?.resourcePackManager?.enabledProfiles
            ?.let { ResourcePackUtil.filterExtractableProfiles(it.toList()) }
            ?: emptyList()
        selectedIndex = serverPackProfiles.indexOf(previouslySelected)
        updateButtonStates()
    }

    override fun close() {
        client?.setScreen(parent)
    }
}
