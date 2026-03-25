package de.ole101.rpx.mixin;

import de.ole101.rpx.client.screen.ExtractPackScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.network.chat.Component.translatable;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {

    protected PackSelectionScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void addExtractButton(CallbackInfo callbackInfo) {
        Button.Builder builder = Button.builder(translatable("rpx.pack.extract"),
                button -> this.minecraft.setScreen(new ExtractPackScreen(this.minecraft.screen))).size(98, 20);

        addRenderableWidget(builder.pos(this.width - 98 - 5, 5).build());
    }
}
