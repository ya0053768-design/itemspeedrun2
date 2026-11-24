package ru.itemspeedrun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;

/**
 * Рулетка: горизонтальная лента, которая прокручивается и плавно останавливается на выбранной ячейке.
 * Результат (targetIndex) выбран заранее - анимация лишь красиво к нему подводит.
 */
public class WheelScreen extends Screen {
	/** Одна ячейка ленты: подпись и, возможно, иконка предмета. */
	public record Entry(Component label, ItemStack icon) {
	}

	private static final long SPIN_MS = 6500L;

	private final Screen parent;
	private final Component heading;
	private final List<Entry> entries;
	private final int targetIndex;
	private final int cellWidth;
	private final IntConsumer onDone;
	private final double finalOffset;

	private long startMs = -1;
	private Button continueButton;

	public WheelScreen(Screen parent, Component heading, List<Entry> entries, int targetIndex,
					   int cellWidth, IntConsumer onDone) {
		super(heading);
		this.parent = parent;
		this.heading = heading;
		this.entries = entries;
		this.targetIndex = targetIndex;
		this.cellWidth = cellWidth;
		this.onDone = onDone;
		// останавливаемся не ровно по центру ячейки, а с небольшим случайным сдвигом - выглядит живее
		this.finalOffset = targetIndex * (double) cellWidth + cellWidth * (0.2 + 0.6 * ThreadLocalRandom.current().nextDouble());
	}

	@Override
	protected void init() {
		continueButton = Button.builder(Component.literal("Далее"), b -> onDone.accept(targetIndex))
				.bounds(this.width / 2 - 60, this.height / 2 + 60, 120, 20).build();
		continueButton.visible = false;
		this.addRenderableWidget(continueButton);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		long now = System.currentTimeMillis();
		if (startMs < 0) {
			startMs = now;
		}
		double t = Math.min(1.0, (now - startMs) / (double) SPIN_MS);
		boolean done = t >= 1.0;
		double eased = 1.0 - Math.pow(1.0 - t, 4); // быстро в начале, плавное торможение в конце
		double offset = finalOffset * eased;

		int cx = this.width / 2;
		int stripY = this.height / 2 - 24;
		int stripH = 48;
		int left = Math.max(10, cx - 260);
		int right = Math.min(this.width - 10, cx + 260);

		graphics.centeredText(this.font, heading, cx, stripY - 24, 0xFFFFFFFF);

		graphics.fill(left - 2, stripY - 2, right + 2, stripY + stripH + 2, 0xFF000000);
		graphics.enableScissor(left, stripY, right, stripY + stripH);

		double origin = cx - offset;
		int first = Math.max(0, (int) Math.floor((left - origin) / cellWidth) - 1);
		int last = Math.min(entries.size() - 1, (int) Math.ceil((right - origin) / cellWidth) + 1);
		for (int i = first; i <= last; i++) {
			int x = (int) Math.round(origin + i * (double) cellWidth);
			boolean winner = done && i == targetIndex;
			int bg = winner ? 0xFF5A4B00 : ((i & 1) == 0 ? 0xFF2B2B2B : 0xFF373737);
			graphics.fill(x, stripY, x + cellWidth - 1, stripY + stripH, bg);

			Entry e = entries.get(i);
			if (!e.icon().isEmpty()) {
				graphics.item(e.icon(), x + (cellWidth - 16) / 2, stripY + (stripH - 16) / 2);
			} else {
				graphics.centeredText(this.font, e.label(), x + cellWidth / 2, stripY + (stripH - 8) / 2, 0xFFFFFFFF);
			}
		}
		graphics.disableScissor();

		// маркер по центру
		graphics.fill(cx - 1, stripY - 8, cx + 1, stripY + stripH + 8, 0xFFFFD700);

		if (done) {
			graphics.centeredText(this.font, entries.get(targetIndex).label(), cx, stripY + stripH + 18, 0xFFFFD700);
			continueButton.visible = true;
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}
}
