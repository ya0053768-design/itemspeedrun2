package ru.itemspeedrun.mixin;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.itemspeedrun.client.RunSetupFlow;

/**
 * Перехватывает кнопку "Создать новый мир": сначала показываем цепочку окон спидрана,
 * и только потом настоящее создание мира.
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
	@Shadow
	protected abstract void onCreate();

	@Inject(method = "onCreate", at = @At("HEAD"), cancellable = true, require = 0)
	private void itemspeedrun$intercept(CallbackInfo ci) {
		if (RunSetupFlow.consumeBypass()) {
			return; // это наш повторный вызов после выбора - пусть идёт как обычно
		}
		ci.cancel();
		CreateWorldScreen self = (CreateWorldScreen) (Object) this;
		RunSetupFlow.begin(self, this::onCreate);
	}
}
