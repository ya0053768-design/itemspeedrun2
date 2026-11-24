package ru.itemspeedrun;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Вся серверная логика. Команды /itemrun не требуют прав оператора,
 * поэтому работают и в хардкоре, и без читов.
 */
public final class ItemRunManager {
	private ItemRunManager() {
	}

	/** Пауза после входа в мир, чтобы чат успел появиться на экране. */
	private static final int JOIN_DELAY_TICKS = 60;

	private record JoinTask(int ticksLeft, PendingRun pending) {
	}

	private static final Map<UUID, RunState> RUNS = new HashMap<>();
	private static final Map<UUID, JoinTask> JOIN_TASKS = new HashMap<>();

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("itemrun")
						.executes(ctx -> startCommand(ctx.getSource(), false, null))
						.then(Commands.literal("start").executes(ctx -> startCommand(ctx.getSource(), false, null)))
						.then(Commands.literal("hard").executes(ctx -> startCommand(ctx.getSource(), false, Challenges.random())))
						.then(Commands.literal("challenge")
								.then(Commands.argument("id", StringArgumentType.word())
										.executes(ctx -> {
											String id = StringArgumentType.getString(ctx, "id");
											Challenge c = Challenges.byId(id);
											if (c == null) {
												ctx.getSource().getPlayerOrException().sendSystemMessage(
														Component.literal("Нет такого усложнения. Список: /itemrun challenges")
																.withStyle(ChatFormatting.RED));
												return 0;
											}
											return startCommand(ctx.getSource(), false, c);
										})))
						.then(Commands.literal("challenges").executes(ctx -> listChallenges(ctx.getSource())))
						.then(Commands.literal("reroll").executes(ctx -> startCommand(ctx.getSource(), true, null)))
						.then(Commands.literal("cancel").executes(ctx -> cancel(ctx.getSource())))
						.then(Commands.literal("stats").executes(ctx -> {
							StatsStore.sendSummary(ctx.getSource().getPlayerOrException());
							return 1;
						}))
				));

		ServerTickEvents.END_SERVER_TICK.register(ItemRunManager::tick);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			// выбор, сделанный до создания мира, относится только к одиночной игре
			PendingRun pending = server.isSingleplayer() ? PendingRun.take() : null;
			JOIN_TASKS.put(player.getUUID(), new JoinTask(JOIN_DELAY_TICKS, pending));
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			RUNS.remove(id);
			JOIN_TASKS.remove(id);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			RUNS.clear();
			JOIN_TASKS.clear();
		});
	}

	// ---------------------------------------------------------------- команды

	private static int startCommand(CommandSourceStack source, boolean reroll, Challenge requested)
			throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		UUID id = player.getUUID();
		RunState previous = RUNS.get(id);

		if (previous != null && !reroll) {
			player.sendSystemMessage(Component.literal(
					"Забег уже идёт. /itemrun reroll - другой предмет, /itemrun cancel - отмена.")
					.withStyle(ChatFormatting.YELLOW));
			return 0;
		}

		// Предмет не должен уже лежать в инвентаре (иначе время было бы 0) и не должен совпасть с прошлым.
		Item target = ItemPool.pick(source.enabledFeatures(),
				item -> hasItem(player, item) || (previous != null && previous.target == item));
		if (target == null) {
			player.sendSystemMessage(Component.literal("Не нашлось подходящего предмета.")
					.withStyle(ChatFormatting.RED));
			return 0;
		}

		Challenge challenge = reroll && previous != null ? previous.challenge : requested;
		beginRun(source.getServer(), player, target, challenge, !reroll);
		return 1;
	}

	private static int listChallenges(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		player.sendSystemMessage(Component.literal("Усложнения (/itemrun challenge <id>, /itemrun hard - случайное):")
				.withStyle(ChatFormatting.GOLD));
		for (Challenge c : Challenges.ALL) {
			player.sendSystemMessage(Component.literal(c.id).withStyle(ChatFormatting.AQUA)
					.append(Component.literal(" - " + c.name + ": " + c.description).withStyle(ChatFormatting.GRAY)));
		}
		return 1;
	}

	private static int cancel(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		RunState run = RUNS.remove(player.getUUID());
		if (run == null) {
			player.sendSystemMessage(Component.literal("Активного забега нет.").withStyle(ChatFormatting.YELLOW));
			return 0;
		}
		send(player, new TimerPayload(TimerPayload.CANCELLED, run.target, elapsedMs(run), challengeName(run)));
		player.sendSystemMessage(Component.literal("Забег отменён (в статистику не идёт).")
				.withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	// ---------------------------------------------------------------- забег

	private static void beginRun(MinecraftServer server, ServerPlayer player, Item target, Challenge challenge,
								 boolean applySetup) {
		if (applySetup && challenge != null && challenge.farStartRadius > 0) {
			farStart(server, player, challenge.farStartRadius);
		}

		RunState run = new RunState(target, challenge, player);
		RUNS.put(player.getUUID(), run);

		player.sendSystemMessage(Component.literal("Добудь: ")
				.withStyle(ChatFormatting.GRAY)
				.append(itemName(target).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
		if (challenge != null) {
			player.sendSystemMessage(Component.literal("Усложнение: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(challenge.name).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
					.append(Component.literal(" - " + challenge.description + " Нарушение = смерть.")
							.withStyle(ChatFormatting.GRAY)));
		}
		send(player, new TimerPayload(TimerPayload.RUNNING, target, 0L, challengeName(run)));
	}

	/** Телепорт в случайную безопасную точку примерно на radius блоков от спавна (командой /spreadplayers). */
	private static void farStart(MinecraftServer server, ServerPlayer player, int radius) {
		double fromX = player.getX();
		double fromZ = player.getZ();
		String name = player.getName().getString();

		for (int attempt = 0; attempt < 6; attempt++) {
			ThreadLocalRandom rnd = ThreadLocalRandom.current();
			double angle = rnd.nextDouble(Math.PI * 2);
			double dist = radius * (0.7 + 0.3 * rnd.nextDouble());
			int x = (int) (Math.cos(angle) * dist);
			int z = (int) (Math.sin(angle) * dist);
			try {
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
						"spreadplayers " + x + " " + z + " 0 16 false " + name);
			} catch (Exception ignored) {
				// не получилось (например, океан) - пробуем другую точку
			}
			if (Math.hypot(player.getX() - fromX, player.getZ() - fromZ) > radius * 0.5) {
				return;
			}
		}
		player.sendSystemMessage(Component.literal("Не удалось найти безопасную дальнюю точку, старт на месте.")
				.withStyle(ChatFormatting.YELLOW));
	}

	private static void tick(MinecraftServer server) {
		runJoinTasks(server);

		if (RUNS.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, RunState>> it = RUNS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, RunState> entry = it.next();
			RunState run = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

			if (player == null) {
				it.remove();
				continue;
			}
			if (!player.isAlive()) {
				it.remove();
				fail(player, run, "ты умер", false);
				continue;
			}
			if (hasItem(player, run.target)) {
				it.remove();
				success(player, run);
				continue;
			}

			run.ticks++;
			run.update(player);

			// 2 секунды после старта не проверяем (загрузка чанков, падение с телепорта и т.п.)
			if (run.challenge != null && run.ticks > 40) {
				String reason = run.challenge.check.test(player, run, elapsedMs(run));
				if (reason != null) {
					it.remove();
					fail(player, run, "усложнение «" + run.challenge.name + "» нарушено: " + reason, true);
				}
			}
		}
	}

	private static void runJoinTasks(MinecraftServer server) {
		if (JOIN_TASKS.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, JoinTask>> it = JOIN_TASKS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, JoinTask> entry = it.next();
			JoinTask task = entry.getValue();
			if (task.ticksLeft() > 1) {
				entry.setValue(new JoinTask(task.ticksLeft() - 1, task.pending()));
				continue;
			}
			it.remove();

			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				continue;
			}

			// Сид - только в одиночке (на чужом сервере раскрывать его всем незачем).
			if (server.isSingleplayer()) {
				long seed = server.overworld().getSeed();
				player.sendSystemMessage(Component.literal("Сид мира: ").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(String.valueOf(seed)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
			}

			PendingRun pending = task.pending();
			if (pending != null) {
				Item target = pending.item;
				if (!target.isEnabled(server.createCommandSourceStack().enabledFeatures())) {
					target = ItemPool.pick(server.createCommandSourceStack().enabledFeatures(), item -> false);
				}
				if (target != null) {
					beginRun(server, player, target, pending.challenge, true);
				}
			}
		}
	}

	private static void success(ServerPlayer player, RunState run) {
		long ms = elapsedMs(run);
		send(player, new TimerPayload(TimerPayload.FINISHED, run.target, ms, challengeName(run)));
		player.sendSystemMessage(Component.literal("Готово! ")
				.withStyle(ChatFormatting.GREEN)
				.append(itemName(run.target).copy().withStyle(ChatFormatting.GOLD))
				.append(Component.literal(" добыт за ").withStyle(ChatFormatting.GREEN))
				.append(Component.literal(TimeFormat.format(ms)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));

		StatsStore.add(player.getUUID(), run.target, ms, true, challengeName(run));
		StatsStore.sendSummary(player);
	}

	/** Забег провален. kill = true - игрока нужно убить (нарушено усложнение). */
	private static void fail(ServerPlayer player, RunState run, String reason, boolean kill) {
		long ms = elapsedMs(run);
		send(player, new TimerPayload(TimerPayload.CANCELLED, run.target, ms, challengeName(run)));
		player.sendSystemMessage(Component.literal("Забег провален: " + reason + ".").withStyle(ChatFormatting.RED));

		StatsStore.add(player.getUUID(), run.target, ms, false, challengeName(run));
		StatsStore.sendSummary(player);

		if (kill && player.level() instanceof ServerLevel level) {
			player.kill(level);
		}
	}

	// ---------------------------------------------------------------- утилиты

	/** Есть ли предмет в инвентаре (хотбар, основной инвентарь, броня, вторая рука). */
	private static boolean hasItem(ServerPlayer player, Item item) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).is(item)) {
				return true;
			}
		}
		return false;
	}

	private static long elapsedMs(RunState run) {
		return (System.nanoTime() - run.startNanos) / 1_000_000L;
	}

	private static String challengeName(RunState run) {
		return run.challenge == null ? "" : run.challenge.name;
	}

	private static Component itemName(Item item) {
		return item.getDefaultInstance().getHoverName();
	}

	private static void send(ServerPlayer player, TimerPayload payload) {
		// если у игрока нет мода на клиенте - просто не шлём пакет, чат продолжит работать
		if (ServerPlayNetworking.canSend(player, TimerPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
