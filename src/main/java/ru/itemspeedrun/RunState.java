package ru.itemspeedrun;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/** Состояние активного забега одного игрока. Обновляется каждый тик сервера. */
public final class RunState {
	public final Item target;
	public final Challenge challenge;
	public final long startNanos = System.nanoTime();

	public int ticks;
	public final double startX, startY, startZ;

	private double prevX, prevY, prevZ;
	private boolean prevOnGround;
	private float prevHealth;
	private int prevFood;
	private int prevXp;

	public int lastMoveTick;
	public int lastSprintTick;
	public int lastJumpTick;
	public int lastSkyTick;
	public int lastNoSkyTick;

	/** События именно этого тика. */
	public boolean jumped, damaged, healed, ate, gainedXp;

	public RunState(Item target, Challenge challenge, ServerPlayer p) {
		this.target = target;
		this.challenge = challenge;
		this.startX = p.getX();
		this.startY = p.getY();
		this.startZ = p.getZ();
		this.prevX = startX;
		this.prevY = startY;
		this.prevZ = startZ;
		this.prevOnGround = p.onGround();
		this.prevHealth = p.getHealth();
		this.prevFood = p.getFoodData().getFoodLevel();
		this.prevXp = p.totalExperience;
	}

	public void update(ServerPlayer p) {
		double x = p.getX();
		double y = p.getY();
		double z = p.getZ();
		double dx = x - prevX;
		double dy = y - prevY;
		double dz = z - prevZ;
		if (dx * dx + dy * dy + dz * dz > 0.0004) {
			lastMoveTick = ticks;
		}

		boolean onGround = p.onGround();
		jumped = prevOnGround && !onGround && y > prevY + 0.01;
		if (jumped) {
			lastJumpTick = ticks;
		}
		if (p.isSprinting()) {
			lastSprintTick = ticks;
		}

		float health = p.getHealth();
		damaged = health < prevHealth - 0.001f;
		healed = health > prevHealth + 0.001f;

		int food = p.getFoodData().getFoodLevel();
		ate = food > prevFood;

		int xp = p.totalExperience;
		gainedXp = xp > prevXp;

		if (p.level().canSeeSky(p.blockPosition())) {
			lastSkyTick = ticks;
		} else {
			lastNoSkyTick = ticks;
		}

		prevX = x;
		prevY = y;
		prevZ = z;
		prevOnGround = onGround;
		prevHealth = health;
		prevFood = food;
		prevXp = xp;
	}
}
