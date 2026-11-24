package ru.itemspeedrun;

import net.minecraft.server.level.ServerPlayer;

/** Усложнение забега. Нарушение = смерть игрока. */
public final class Challenge {
	/** Возвращает причину нарушения или null, если всё в порядке. */
	@FunctionalInterface
	public interface Check {
		String test(ServerPlayer player, RunState state, long elapsedMs);
	}

	public final String id;
	public final String name;
	public final String description;
	public final Check check;
	/** Если больше 0 - в начале забега игрока телепортируют примерно на столько блоков от спавна. */
	public final int farStartRadius;

	public Challenge(String id, String name, String description, Check check, int farStartRadius) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.check = check;
		this.farStartRadius = farStartRadius;
	}
}
