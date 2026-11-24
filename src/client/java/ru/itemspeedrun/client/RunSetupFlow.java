package ru.itemspeedrun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import ru.itemspeedrun.Challenge;
import ru.itemspeedrun.Challenges;
import ru.itemspeedrun.ItemPool;
import ru.itemspeedrun.PendingRun;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Цепочка окон после нажатия "Создать новый мир":
 * "Начать спидран?" -> колесо предметов -> "Поставить усложнение?" -> колесо усложнений -> создание мира.
 */
public final class RunSetupFlow {
	private RunSetupFlow() {
	}

	private static boolean bypass;

	/** Миксин спрашивает: пропустить перехват (мы сами повторно вызываем создание мира). */
	public static boolean consumeBypass() {
		boolean b = bypass;
		bypass = false;
		return b;
	}

	public static void begin(Screen createWorldScreen, Runnable createWorld) {
		PendingRun.clear();
		Minecraft mc = Minecraft.getInstance();

		Runnable proceedWithoutRun = () -> finish(createWorldScreen, createWorld, null, null);

		mc.gui.setScreen(new PromptScreen(createWorldScreen,
				Component.literal("Начать спидран предмета?"),
				() -> spinItem(createWorldScreen, createWorld),
				proceedWithoutRun));
	}

	private static void spinItem(Screen parent, Runnable createWorld) {
		Minecraft mc = Minecraft.getInstance();

		// до создания мира неизвестно, какие эксперименты включены - проверим предмет уже на сервере
		List<Item> pool = ItemPool.all(null, i -> false);
		if (pool.isEmpty()) {
			finish(parent, createWorld, null, null);
			return;
		}
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		int size = 70;
		int target = 55;
		List<WheelScreen.Entry> entries = new ArrayList<>();
		Item chosen = null;
		for (int i = 0; i < size; i++) {
			Item it = pool.get(rnd.nextInt(pool.size()));
			if (i == target) {
				chosen = it;
			}
			entries.add(new WheelScreen.Entry(it.getDefaultInstance().getHoverName(), it.getDefaultInstance()));
		}
		Item finalChosen = chosen;

		mc.gui.setScreen(new WheelScreen(parent, Component.literal("Твой предмет:"), entries, target, 44,
				idx -> askChallenge(parent, createWorld, finalChosen)));
	}

	private static void askChallenge(Screen parent, Runnable createWorld, Item item) {
		Minecraft mc = Minecraft.getInstance();
		mc.gui.setScreen(new PromptScreen(parent,
				Component.literal("Поставить усложнение?"),
				() -> spinChallenge(parent, createWorld, item),
				() -> finish(parent, createWorld, item, null)));
	}

	private static void spinChallenge(Screen parent, Runnable createWorld, Item item) {
		Minecraft mc = Minecraft.getInstance();
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		int size = 40;
		int target = 30;
		List<WheelScreen.Entry> entries = new ArrayList<>();
		Challenge chosen = null;
		for (int i = 0; i < size; i++) {
			Challenge c = Challenges.ALL.get(rnd.nextInt(Challenges.ALL.size()));
			if (i == target) {
				chosen = c;
			}
			entries.add(new WheelScreen.Entry(Component.literal(c.name), net.minecraft.world.item.ItemStack.EMPTY));
		}
		Challenge finalChosen = chosen;

		mc.gui.setScreen(new WheelScreen(parent, Component.literal("Твоё усложнение:"), entries, target, 150,
				idx -> finish(parent, createWorld, item, finalChosen)));
	}

	private static void finish(Screen createWorldScreen, Runnable createWorld, Item item, Challenge challenge) {
		if (item != null) {
			PendingRun.set(new PendingRun(item, challenge));
		}
		// возвращаемся на экран создания мира и запускаем настоящее создание
		Minecraft.getInstance().gui.setScreen(createWorldScreen);
		bypass = true;
		createWorld.run();
	}
}
