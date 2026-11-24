package ru.itemspeedrun.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.itemspeedrun.TimeFormat;
import ru.itemspeedrun.TimerPayload;

/** Секундомер вверху по центру экрана: иконка предмета слева, время, название цели и усложнение. */
public final class TimerHud {
	private TimerHud() {
	}

	private static final long HIDE_AFTER_FINISH_MS = 60_000L;
	private static final long HIDE_AFTER_CANCEL_MS = 8_000L;

	private static int state = TimerPayload.CLEAR;
	private static Item target = Items.AIR;
	private static ItemStack icon = ItemStack.EMPTY;
	private static Component targetName = Component.empty();
	private static String challenge = "";
	private static long startNanos;
	private static long frozenMs;
	private static long endedAtNanos;

	public static void apply(TimerPayload payload) {
		long now = System.nanoTime();
		state = payload.state();
		target = payload.item();
		icon = target.getDefaultInstance();
		targetName = icon.getHoverName();
		challenge = payload.challenge();

		switch (state) {
			case TimerPayload.RUNNING -> startNanos = now;
			case TimerPayload.FINISHED, TimerPayload.CANCELLED -> {
				frozenMs = payload.elapsedMs();
				endedAtNanos = now;
			}
			default -> {
			}
		}
	}

	public static void reset() {
		state = TimerPayload.CLEAR;
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (state == TimerPayload.CLEAR) {
			return;
		}

		long now = System.nanoTime();
		long ms;
		int color;
		switch (state) {
			case TimerPayload.RUNNING -> {
				ms = (now - startNanos) / 1_000_000L;
				color = 0xFFFFFFFF;
			}
			case TimerPayload.FINISHED -> {
				if ((now - endedAtNanos) / 1_000_000L > HIDE_AFTER_FINISH_MS) {
					state = TimerPayload.CLEAR;
					return;
				}
				ms = frozenMs;
				color = 0xFF55FF55;
			}
			default -> {
				if ((now - endedAtNanos) / 1_000_000L > HIDE_AFTER_CANCEL_MS) {
					state = TimerPayload.CLEAR;
					return;
				}
				ms = frozenMs;
				color = 0xFFFF5555;
			}
		}

		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int screenWidth = mc.getWindow().getGuiScaledWidth();

		String time = TimeFormat.format(ms);
		Component line2 = Component.literal("Цель: ").append(targetName);
		Component line3 = challenge.isEmpty() ? null : Component.literal("Усложнение: " + challenge);

		int textWidth = Math.max(font.width("88:88.888"), font.width(line2));
		if (line3 != null) {
			textWidth = Math.max(textWidth, font.width(line3));
		}
		int lines = line3 == null ? 2 : 3;

		int iconBox = 20;
		int boxWidth = iconBox + textWidth + 14;
		int boxHeight = 9 * lines + 4 + lines;
		int x1 = (screenWidth - boxWidth) / 2;
		int y1 = 4;

		graphics.fill(x1, y1, x1 + boxWidth, y1 + boxHeight, 0x90000000);

		// иконка предмета слева от времени
		graphics.item(icon, x1 + 4, y1 + (boxHeight - 16) / 2);

		int textCenter = x1 + iconBox + 6 + textWidth / 2;
		int y = y1 + 4;
		graphics.text(font, time, textCenter - font.width(time) / 2, y, color, true);
		y += 10;
		graphics.text(font, line2, textCenter - font.width(line2) / 2, y, 0xFFFFFFFF, true);
		if (line3 != null) {
			y += 10;
			graphics.text(font, line3, textCenter - font.width(line3) / 2, y, 0xFFFFAA00, true);
		}
	}
}
