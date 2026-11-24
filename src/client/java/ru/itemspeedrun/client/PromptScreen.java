package ru.itemspeedrun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Вопрос "Да / Нет". Esc возвращает на экран создания мира. */
public class PromptScreen extends Screen {
	private final Screen parent;
	private final Component question;
	private final Runnable onYes;
	private final Runnable onNo;

	public PromptScreen(Screen parent, Component question, Runnable onYes, Runnable onNo) {
		super(question);
		this.parent = parent;
		this.question = question;
		this.onYes = onYes;
		this.onNo = onNo;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 2;
		this.addRenderableWidget(Button.builder(Component.literal("Да"), b -> onYes.run())
				.bounds(cx - 105, y, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Нет"), b -> onNo.run())
				.bounds(cx + 5, y, 100, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, question, this.width / 2, this.height / 2 - 30, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}
}
