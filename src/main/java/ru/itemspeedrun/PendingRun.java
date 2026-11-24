package ru.itemspeedrun;

import net.minecraft.world.item.Item;

/**
 * Выбор, сделанный на клиенте ДО создания мира (колесо предметов и усложнение).
 * Одиночная игра - это один процесс, поэтому встроенный сервер просто забирает значение отсюда,
 * когда игрок зашёл в новый мир.
 */
public final class PendingRun {
	public final Item item;
	public final Challenge challenge; // может быть null

	private static volatile PendingRun current;

	public PendingRun(Item item, Challenge challenge) {
		this.item = item;
		this.challenge = challenge;
	}

	public static void set(PendingRun run) {
		current = run;
	}

	public static void clear() {
		current = null;
	}

	/** Забрать и сбросить. */
	public static PendingRun take() {
		PendingRun r = current;
		current = null;
		return r;
	}
}
