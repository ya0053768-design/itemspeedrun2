package ru.itemspeedrun;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/** Все усложнения. Добавить своё: допиши ещё один add(...) / forbid(...) в блоке static. */
public final class Challenges {
	private Challenges() {
	}

	public static final List<Challenge> ALL;

	private static final String[] TOOL_SUFFIX = {"_pickaxe", "_axe", "_shovel", "_hoe", "_sword"};
	private static final String[] ARMOR_SUFFIX = {"_helmet", "_chestplate", "_leggings", "_boots"};

	static {
		List<Challenge> l = new ArrayList<>();

		// ---------- Движение ----------
		add(l, "no_jump", "Без прыжков", "Нельзя прыгать.",
				(p, s, ms) -> s.jumped ? "ты прыгнул" : null);
		add(l, "no_sprint", "Без бега", "Нельзя бегать.",
				(p, s, ms) -> p.isSprinting() ? "ты побежал" : null);
		add(l, "no_sneak", "Без приседания", "Нельзя приседать.",
				(p, s, ms) -> p.isShiftKeyDown() ? "ты присел" : null);
		add(l, "keep_moving", "Не стой на месте", "Нельзя стоять на месте дольше 15 секунд.",
				(p, s, ms) -> s.ticks - s.lastMoveTick > 300 ? "ты простоял на месте больше 15 секунд" : null);
		add(l, "marathon", "Марафонец", "Нельзя не бежать дольше 20 секунд.",
				(p, s, ms) -> s.ticks - s.lastSprintTick > 400 ? "ты не бежал больше 20 секунд" : null);
		add(l, "grasshopper", "Кузнечик", "Нельзя не прыгать дольше 12 секунд.",
				(p, s, ms) -> s.ticks - s.lastJumpTick > 240 ? "ты не прыгал больше 12 секунд" : null);
		add(l, "no_water", "Вода - лава", "Нельзя касаться воды.",
				(p, s, ms) -> p.isInWater() ? "ты залез в воду" : null);
		add(l, "stay_near", "Привязанный", "Нельзя уходить дальше 200 блоков от точки старта.",
				(p, s, ms) -> Math.hypot(p.getX() - s.startX, p.getZ() - s.startZ) > 200
						? "ты ушёл дальше 200 блоков от старта" : null);
		add(l, "stay_low", "Низкий полёт", "Нельзя подниматься выше 15 блоков над стартом.",
				(p, s, ms) -> p.getY() > s.startY + 15 ? "ты поднялся слишком высоко" : null);
		add(l, "stay_shallow", "Мелководье", "Нельзя спускаться глубже 15 блоков под старт.",
				(p, s, ms) -> p.getY() < s.startY - 15 ? "ты спустился слишком глубоко" : null);

		// ---------- Время ----------
		add(l, "time_5", "Блиц: 5 минут", "Нужно уложиться в 5 минут.",
				(p, s, ms) -> ms > 5 * 60_000L ? "время вышло (5 минут)" : null);
		add(l, "time_10", "Спринт: 10 минут", "Нужно уложиться в 10 минут.",
				(p, s, ms) -> ms > 10 * 60_000L ? "время вышло (10 минут)" : null);
		add(l, "time_20", "Марш-бросок: 20 минут", "Нужно уложиться в 20 минут.",
				(p, s, ms) -> ms > 20 * 60_000L ? "время вышло (20 минут)" : null);

		// ---------- Здоровье и голод ----------
		add(l, "glass", "Хрупкое стекло", "Любой полученный урон - смерть.",
				(p, s, ms) -> s.damaged ? "ты получил урон" : null);
		add(l, "no_heal", "Без лечения", "Нельзя восстанавливать здоровье.",
				(p, s, ms) -> s.healed ? "ты восстановил здоровье" : null);
		add(l, "no_eat", "Голодовка", "Нельзя есть.",
				(p, s, ms) -> s.ate ? "ты поел" : null);
		add(l, "no_sleep", "Бессонница", "Нельзя спать в кровати.",
				(p, s, ms) -> p.isSleeping() ? "ты лёг спать" : null);
		add(l, "no_xp", "Без опыта", "Нельзя получать опыт.",
				(p, s, ms) -> s.gainedXp ? "ты получил опыт" : null);

		// ---------- Инвентарь ----------
		forbid(l, "no_pickaxe", "Без кирок", "Нельзя иметь кирку в инвентаре.",
				path -> path.endsWith("_pickaxe"));
		forbid(l, "no_sword", "Без мечей", "Нельзя иметь меч в инвентаре.",
				path -> path.endsWith("_sword"));
		forbid(l, "no_axe", "Без топоров", "Нельзя иметь топор в инвентаре.",
				path -> path.endsWith("_axe") && !path.endsWith("pickaxe"));
		forbid(l, "no_torch", "Без факелов", "Нельзя иметь факелы в инвентаре.",
				path -> path.contains("torch"));
		forbid(l, "no_table", "Без верстака", "Нельзя иметь верстак в инвентаре.",
				path -> path.equals("crafting_table"));
		forbid(l, "no_furnace", "Без печей", "Нельзя иметь печь в инвентаре.",
				path -> path.equals("furnace") || path.equals("blast_furnace") || path.equals("smoker"));
		forbid(l, "bare_hands", "Голые руки", "Нельзя иметь никакие инструменты и оружие.",
				path -> endsWithAny(path, TOOL_SUFFIX));
		forbid(l, "wood_age", "Деревянный век", "Нельзя иметь инструменты и броню лучше деревянных.",
				path -> startsWithAny(path, "stone_", "copper_", "iron_", "golden_", "diamond_", "netherite_")
						&& (endsWithAny(path, TOOL_SUFFIX) || endsWithAny(path, ARMOR_SUFFIX)));
		add(l, "naked", "Голышом", "Нельзя носить броню.", (p, s, ms) -> {
			for (EquipmentSlot slot : new EquipmentSlot[]{
					EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				if (!p.getItemBySlot(slot).isEmpty()) {
					return "ты надел броню";
				}
			}
			return null;
		});
		add(l, "hotbar_only", "Тесные карманы", "Можно использовать только хотбар, основной инвентарь должен быть пуст.",
				(p, s, ms) -> {
					Inventory inv = p.getInventory();
					for (int i = 9; i < 36; i++) {
						if (!inv.getItem(i).isEmpty()) {
							return "предмет попал в основной инвентарь";
						}
					}
					return null;
				});
		add(l, "minimalist", "Минималист", "Нельзя занимать больше 6 слотов инвентаря.",
				(p, s, ms) -> {
					Inventory inv = p.getInventory();
					int used = 0;
					for (int i = 0; i < 36; i++) {
						if (!inv.getItem(i).isEmpty()) {
							used++;
						}
					}
					return used > 6 ? "занято больше 6 слотов" : null;
				});

		// ---------- Мир ----------
		add(l, "overworld_only", "Только Верхний мир", "Нельзя покидать Верхний мир.",
				(p, s, ms) -> p.level().dimension() != Level.OVERWORLD ? "ты покинул Верхний мир" : null);
		add(l, "sky_needed", "Под небом", "Нельзя находиться без неба над головой дольше 20 секунд.",
				(p, s, ms) -> s.ticks - s.lastSkyTick > 400 ? "ты слишком долго не видел неба" : null);
		add(l, "cave_dweller", "Пещерный житель", "Нельзя находиться под открытым небом дольше 30 секунд.",
				(p, s, ms) -> s.ticks - s.lastNoSkyTick > 600 ? "ты слишком долго был на солнце" : null);

		// ---------- Дальний старт ----------
		far(l, "far_1500", "Дальний старт: ~1500", "Старт случайно в ~1500 блоков от спавна.", 1500);
		far(l, "far_5000", "Дальний старт: ~5000", "Старт случайно в ~5000 блоков от спавна.", 5000);
		far(l, "far_12000", "Дальний старт: ~12000", "Старт случайно в ~12000 блоков от спавна.", 12000);

		ALL = List.copyOf(l);
	}

	public static Challenge byId(String id) {
		for (Challenge c : ALL) {
			if (c.id.equalsIgnoreCase(id)) {
				return c;
			}
		}
		return null;
	}

	public static Challenge random() {
		return ALL.get(ThreadLocalRandom.current().nextInt(ALL.size()));
	}

	// ---------- helpers ----------

	private static void add(List<Challenge> l, String id, String name, String desc, Challenge.Check check) {
		l.add(new Challenge(id, name, desc, check, 0));
	}

	private static void far(List<Challenge> l, String id, String name, String desc, int radius) {
		l.add(new Challenge(id, name, desc, (p, s, ms) -> null, radius));
	}

	private static void forbid(List<Challenge> l, String id, String name, String desc, Predicate<String> forbidden) {
		add(l, id, name, desc, (p, s, ms) -> {
			Inventory inv = p.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack st = inv.getItem(i);
				if (!st.isEmpty() && forbidden.test(path(st))) {
					return "в инвентаре запрещённый предмет: " + st.getHoverName().getString();
				}
			}
			return null;
		});
	}

	private static String path(ItemStack st) {
		return BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
	}

	private static boolean endsWithAny(String s, String... suffixes) {
		for (String x : suffixes) {
			if (s.endsWith(x)) {
				return true;
			}
		}
		return false;
	}

	private static boolean startsWithAny(String s, String... prefixes) {
		for (String x : prefixes) {
			if (s.startsWith(x)) {
				return true;
			}
		}
		return false;
	}
}
