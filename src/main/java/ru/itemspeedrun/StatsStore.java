package ru.itemspeedrun;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** История забегов в config/itemspeedrun-stats.json и красивый вывод в чат. */
public final class StatsStore {
	private StatsStore() {
	}

	public static final class Entry {
		public String player;
		public String item;
		public long ms;
		public boolean success;
		public String challenge;
		public long at;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static List<Entry> entries;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("itemspeedrun-stats.json");
	}

	private static synchronized List<Entry> load() {
		if (entries != null) {
			return entries;
		}
		entries = new ArrayList<>();
		Path f = file();
		if (Files.exists(f)) {
			try {
				Entry[] arr = GSON.fromJson(Files.readString(f), Entry[].class);
				if (arr != null) {
					entries.addAll(List.of(arr));
				}
			} catch (Exception ignored) {
				// битый файл - начинаем с чистого листа, не роняя игру
			}
		}
		return entries;
	}

	private static void save() {
		try {
			Files.writeString(file(), GSON.toJson(entries));
		} catch (IOException ignored) {
			// не удалось сохранить - статистика останется только до выхода из игры
		}
	}

	public static synchronized void add(UUID player, Item item, long ms, boolean success, String challenge) {
		Entry e = new Entry();
		e.player = player.toString();
		e.item = BuiltInRegistries.ITEM.getKey(item).toString();
		e.ms = ms;
		e.success = success;
		e.challenge = challenge == null ? "" : challenge;
		e.at = System.currentTimeMillis();
		load().add(e);
		save();
	}

	public static synchronized void sendSummary(ServerPlayer player) {
		String me = player.getUUID().toString();
		List<Entry> mine = new ArrayList<>();
		for (Entry e : load()) {
			if (me.equals(e.player)) {
				mine.add(e);
			}
		}

		List<Entry> wins = new ArrayList<>();
		int fails = 0;
		for (Entry e : mine) {
			if (e.success) {
				wins.add(e);
			} else {
				fails++;
			}
		}

		player.sendSystemMessage(Component.literal("===== Статистика =====")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

		List<Entry> fastest = new ArrayList<>(wins);
		fastest.sort(Comparator.comparingLong(e -> e.ms));
		sendTop(player, "Рекорды (самые быстрые)", fastest, ChatFormatting.GREEN);

		List<Entry> slowest = new ArrayList<>(wins);
		slowest.sort(Comparator.comparingLong((Entry e) -> e.ms).reversed());
		sendTop(player, "Антирекорды (самые медленные)", slowest, ChatFormatting.RED);

		player.sendSystemMessage(Component.literal("Удачи (без смерти): ").withStyle(ChatFormatting.GREEN)
				.append(Component.literal(String.valueOf(wins.size())).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)));
		player.sendSystemMessage(Component.literal("Неудачи (смерть): ").withStyle(ChatFormatting.RED)
				.append(Component.literal(String.valueOf(fails)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)));
	}

	private static void sendTop(ServerPlayer player, String title, List<Entry> sorted, ChatFormatting color) {
		player.sendSystemMessage(Component.literal(title + ":").withStyle(color, ChatFormatting.BOLD));
		if (sorted.isEmpty()) {
			player.sendSystemMessage(Component.literal("  пока нет").withStyle(ChatFormatting.GRAY));
			return;
		}
		for (int i = 0; i < Math.min(3, sorted.size()); i++) {
			Entry e = sorted.get(i);
			MutableComponent line = Component.literal("  " + (i + 1) + ". ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(TimeFormat.format(e.ms)).withStyle(ChatFormatting.AQUA))
					.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
					.append(itemName(e.item).copy().withStyle(ChatFormatting.GOLD));
			if (e.challenge != null && !e.challenge.isEmpty()) {
				line.append(Component.literal(" [" + e.challenge + "]").withStyle(ChatFormatting.DARK_GRAY));
			}
			player.sendSystemMessage(line);
		}
	}

	private static Component itemName(String id) {
		for (Item item : BuiltInRegistries.ITEM) {
			if (BuiltInRegistries.ITEM.getKey(item).toString().equals(id)) {
				return item.getDefaultInstance().getHoverName();
			}
		}
		return Component.literal(id);
	}
}
