package ru.itemspeedrun;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Список предметов, которые можно получить в режиме выживания.
 * <p>
 * В игре нет флага "добывается в выживании", поэтому исключаем вручную:
 * предметы только для креатива, блоки без предмета-дропа и т.п.
 * Если в игре попался предмет, который добыть нельзя, добавьте его id
 * (по одному в строке) в config/itemspeedrun-blacklist.txt - перекомпилировать ничего не нужно.
 */
public final class ItemPool {
	private ItemPool() {
	}

	private static final Set<String> BUILTIN_DENY = Set.of(
			"air",
			"bedrock",
			"barrier",
			"light",
			"structure_void",
			"structure_block",
			"jigsaw",
			"command_block",
			"chain_command_block",
			"repeating_command_block",
			"command_block_minecart",
			"debug_stick",
			"knowledge_book",
			"end_portal_frame",
			"reinforced_deepslate",
			"spawner",
			"trial_spawner",
			"vault",
			"budding_amethyst",
			"petrified_oak_slab",
			"chorus_plant",
			"farmland",
			"frogspawn",
			"suspicious_sand",
			"suspicious_gravel",
			"player_head",
			"test_block",
			"test_instance_block"
	);

	private static final List<String> DENY_PREFIX = List.of("infested_");
	private static final List<String> DENY_SUFFIX = List.of("_spawn_egg");

	/**
	 * Все подходящие предметы.
	 *
	 * @param features включённые в мире фичи; null - не проверять (нужно до создания мира)
	 * @param excluded предметы, которые выпасть не должны
	 */
	public static List<Item> all(FeatureFlagSet features, Predicate<Item> excluded) {
		Set<String> custom = loadCustomDeny();
		List<Item> pool = new ArrayList<>();

		for (Item item : BuiltInRegistries.ITEM) {
			if (features != null && !item.isEnabled(features)) {
				continue;
			}
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			// только ванильные предметы: у модовых нет гарантии, что они добываются в выживании
			if (!"minecraft".equals(id.getNamespace())) {
				continue;
			}
			String path = id.getPath();
			if (BUILTIN_DENY.contains(path) || custom.contains(path)) {
				continue;
			}
			if (DENY_PREFIX.stream().anyMatch(path::startsWith) || DENY_SUFFIX.stream().anyMatch(path::endsWith)) {
				continue;
			}
			if (excluded.test(item)) {
				continue;
			}
			pool.add(item);
		}
		return pool;
	}

	/** Случайный предмет с равной вероятностью среди всех подходящих; null, если таких нет. */
	public static Item pick(FeatureFlagSet features, Predicate<Item> excluded) {
		List<Item> pool = all(features, excluded);
		if (pool.isEmpty()) {
			return null;
		}
		return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
	}

	private static Set<String> loadCustomDeny() {
		Set<String> result = new HashSet<>();
		Path file = FabricLoader.getInstance().getConfigDir().resolve("itemspeedrun-blacklist.txt");
		try {
			if (!Files.exists(file)) {
				Files.writeString(file, """
						# Чёрный список предметов для /itemrun.
						# Один id в строке, например: sponge или minecraft:sponge
						# Строки, начинающиеся с #, игнорируются.
						""");
				return result;
			}
			for (String line : Files.readAllLines(file)) {
				String s = line.strip();
				if (s.isEmpty() || s.startsWith("#")) {
					continue;
				}
				if (s.startsWith("minecraft:")) {
					s = s.substring("minecraft:".length());
				}
				result.add(s);
			}
		} catch (IOException ignored) {
			// не удалось прочитать/создать файл - работаем только со встроенным списком
		}
		return result;
	}
}
